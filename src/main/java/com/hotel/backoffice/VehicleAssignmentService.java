package com.hotel.backoffice;

import com.hotel.backoffice.model.AssignmentReport;
import com.hotel.backoffice.model.Hotel;
import com.hotel.backoffice.model.Reservation;
import com.hotel.backoffice.model.TransferAssignment;
import com.hotel.backoffice.model.Vehicule;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VehicleAssignmentService {
    private static final double DEFAULT_VITESSE_MOYENNE_KMH = 50.0;
    private static final int DEFAULT_WAIT_TIME_MINUTES = 0;

    private final ReservationDao reservationDao = new ReservationDao();
    private final VehiculeDao vehiculeDao = new VehiculeDao();
    private final HotelDao hotelDao = new HotelDao();
    private final DistanceDao distanceDao = new DistanceDao();

    public AssignmentReport buildDailyReport(LocalDate date) throws SQLException {
        return buildDailyReport(date, -1, -1.0);
    }

    public AssignmentReport buildDailyReport(LocalDate date, int overrideWaitTime) throws SQLException {
        return buildDailyReport(date, overrideWaitTime, -1.0);
    }

    public AssignmentReport buildDailyReport(LocalDate date, int overrideWaitTime, double overrideVitesse) throws SQLException {
        double vitesseMoyenne = overrideVitesse > 0 ? overrideVitesse : resolveDoubleEnv("VITESSE_MOYENNE_KMH", DEFAULT_VITESSE_MOYENNE_KMH);
        int waitTimeMinutes = overrideWaitTime >= 0 ? overrideWaitTime : resolveIntEnv("WAIT_TIME_MINUTES", DEFAULT_WAIT_TIME_MINUTES);

        List<Reservation> reservations = reservationDao.findByDate(date);
        reservations.sort(
            Comparator
                .comparing(Reservation::getDateHeureArrive, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingInt(Reservation::getId)
        );

        List<Vehicule> vehicules = vehiculeDao.findAll();
        List<Hotel> hotels = hotelDao.findAll();
        Map<Integer, Hotel> hotelsById = mapHotelsById(hotels);
        List<Hotel> airports = findAirports(hotels);
        Map<String, Double> distancesKm = distanceDao.findAllDistancesKm();

        AssignmentReport report = new AssignmentReport();
        report.setDate(date);
        report.setVitesseMoyenneKmh(vitesseMoyenne);
        report.setWaitTimeMinutes(waitTimeMinutes);

        List<AssignmentCandidate> pending = new ArrayList<>();
        int reservationOrder = 0;

        for (Reservation reservation : reservations) {
            reservationOrder++;

            TransferAssignment assignment = buildBaseAssignment(reservation, hotelsById);
            Hotel hotel = hotelsById.get(reservation.getIdHotel());

            if (hotel == null) {
                markUnassigned(report, assignment, "Hôtel introuvable");
                continue;
            }
            if (hotel.isAeroport()) {
                markUnassigned(report, assignment, "Le lieu de destination est déjà un aéroport");
                continue;
            }
            if (isBlank(hotel.getCode())) {
                markUnassigned(report, assignment, "Code lieu manquant pour l'hôtel");
                continue;
            }
            if (airports.isEmpty()) {
                markUnassigned(report, assignment, "Aucun aéroport configuré (hotel.aeroport)");
                continue;
            }
            if (reservation.getDateHeureArrive() == null) {
                markUnassigned(report, assignment, "Date d'arrivée avion manquante");
                continue;
            }

            AirportChoice nearestAirport = chooseNearestAirport(hotel.getCode(), airports, distancesKm);
            if (nearestAirport == null) {
                markUnassigned(report, assignment, "Distance introuvable entre aéroport et hôtel");
                continue;
            }

            long directTravelMinutes = computeTravelMinutes(nearestAirport.distanceKm, vitesseMoyenne);

            assignment.setAeroportCode(nearestAirport.airportCode);
            assignment.setDistanceKm(nearestAirport.distanceKm);
            assignment.setDureeTrajetMinutes(directTravelMinutes);

            AssignmentCandidate candidate = new AssignmentCandidate();
            candidate.reservationId = reservation.getId();
            candidate.reservationOrder = reservationOrder;
            candidate.hotelCode = hotel.getCode();
            candidate.airportCode = nearestAirport.airportCode;
            candidate.remainingPassengers = reservation.getNbPassager();
            candidate.totalReservationPassengers = reservation.getNbPassager();
            candidate.readyAt = reservation.getDateHeureArrive();
            candidate.airportToHotelDistanceKm = nearestAirport.distanceKm;
            candidate.baseAssignment = assignment;
            pending.add(candidate);
        }

        if (vehicules.isEmpty()) {
            for (AssignmentCandidate candidate : pending) {
                markUnassigned(report, buildAssignmentFragment(candidate, candidate.remainingPassengers), "Aucun véhicule disponible");
            }
            return report;
        }

        List<VehicleState> vehicleStates = new ArrayList<>();
        for (Vehicule vehicule : vehicules) {
            VehicleState state = new VehicleState();
            state.vehicule = vehicule;
            state.availableAt = LocalDateTime.of(date, resolveInitialAvailability(vehicule));
            state.currentAirportCode = null;
            vehicleStates.add(state);
        }

        int nextTrajetId = 1;
        while (!pending.isEmpty()) {
            LocalDateTime batchTime = findNextBatchTime(pending, vehicleStates, waitTimeMinutes);
            if (batchTime == null) {
                for (AssignmentCandidate candidate : pending) {
                    markUnassigned(report, buildAssignmentFragment(candidate, candidate.remainingPassengers), "Aucun vehicule disponible");
                }
                pending.clear();
                break;
            }

            boolean assignedAny = false;
            while (true) {
                AssignmentCandidate focusCandidate = chooseNextFocusCandidate(pending, vehicleStates, batchTime);
                if (focusCandidate == null) {
                    break;
                }

                TripPlan bestTrip = chooseBestTrip(focusCandidate, pending, vehicleStates, batchTime, distancesKm, vitesseMoyenne);
                if (bestTrip == null) {
                    break;
                }

                bestTrip.trajetId = nextTrajetId++;
                applyTrip(bestTrip, pending, report);
                assignedAny = true;
            }

            if (!assignedAny) {
                AssignmentCandidate deferred = chooseNextDeferredCandidate(pending, batchTime);
                if (deferred == null) {
                    break;
                }
                markUnassigned(report, buildAssignmentFragment(deferred, deferred.remainingPassengers), "Aucun vehicule disponible");
                pending.remove(deferred);
            }
        }

        report.getAssigned().sort(
            Comparator
                .comparing(TransferAssignment::getHeureDepartAeroport, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(TransferAssignment::getTrajetId, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TransferAssignment::getOrdreDepot, Comparator.nullsLast(Integer::compareTo))
        );

        return report;
    }

    private TripPlan chooseBestTrip(
        AssignmentCandidate focusCandidate,
        List<AssignmentCandidate> pending,
        List<VehicleState> vehicleStates,
        LocalDateTime batchTime,
        Map<String, Double> distancesKm,
        double vitesseMoyenne
    ) {
        TripPlan best = null;

        for (VehicleState state : vehicleStates) {
            if (state.availableAt.isAfter(batchTime)) {
                continue;
            }
            if (!isBlank(state.currentAirportCode) && !focusCandidate.airportCode.equals(state.currentAirportCode)) {
                continue;
            }

            TripPlan candidatePlan = buildTripPlanForVehicleAirport(
                focusCandidate,
                pending,
                state,
                focusCandidate.airportCode,
                batchTime,
                distancesKm,
                vitesseMoyenne
            );
            if (candidatePlan == null) {
                continue;
            }
            if (best == null || compareTripPlans(candidatePlan, best) < 0) {
                best = candidatePlan;
            }
        }

        return best;
    }

    private TripPlan buildTripPlanForVehicleAirport(
        AssignmentCandidate focusCandidate,
        List<AssignmentCandidate> pending,
        VehicleState state,
        String airportCode,
        LocalDateTime batchTime,
        Map<String, Double> distancesKm,
        double vitesseMoyenne
    ) {
        if (focusCandidate == null || !airportCode.equals(focusCandidate.airportCode)) {
            return null;
        }

        int capacity = state.vehicule.getNbPlace();
        if (capacity <= 0) {
            return null;
        }
        if (state.availableAt.isAfter(batchTime)) {
            return null;
        }

        List<AssignmentCandidate> readyCandidates = new ArrayList<>();
        for (AssignmentCandidate candidate : pending) {
            if (candidate.remainingPassengers <= 0) {
                continue;
            }
            if (!airportCode.equals(candidate.airportCode)) {
                continue;
            }
            if (candidate.readyAt.isAfter(batchTime)) {
                continue;
            }
            readyCandidates.add(candidate);
        }

        if (!readyCandidates.contains(focusCandidate)) {
            return null;
        }

        List<TripSegment> selected = new ArrayList<>();
        int remainingCapacity = capacity;
        int allocatedOnFocusReservation = 0;
        int focusPassengersBefore = focusCandidate.remainingPassengers;

        int allocatedPassengers = Math.min(focusCandidate.remainingPassengers, remainingCapacity);
        if (allocatedPassengers <= 0) {
            return null;
        }

        TripSegment focusSegment = new TripSegment();
        focusSegment.candidate = focusCandidate;
        focusSegment.allocatedPassengers = allocatedPassengers;
        focusSegment.focusSegment = true;
        selected.add(focusSegment);

        remainingCapacity -= allocatedPassengers;
        allocatedOnFocusReservation = allocatedPassengers;

        boolean focusFitsVehicle = focusPassengersBefore <= capacity;
        boolean allowSupplements = focusFitsVehicle && remainingCapacity > 0;
        if (allowSupplements) {
            List<AssignmentCandidate> supplementalCandidates = new ArrayList<>();
            for (AssignmentCandidate candidate : readyCandidates) {
                if (candidate != focusCandidate) {
                    supplementalCandidates.add(candidate);
                }
            }
            while (remainingCapacity > 0) {
                AssignmentCandidate candidate = chooseClosestSupplementCandidate(supplementalCandidates, remainingCapacity);
                if (candidate == null) {
                    break;
                }
                TripSegment segment = new TripSegment();
                segment.candidate = candidate;
                segment.allocatedPassengers = Math.min(candidate.remainingPassengers, remainingCapacity);
                selected.add(segment);
                remainingCapacity -= segment.allocatedPassengers;
                supplementalCandidates.remove(candidate);
            }
        }

        if (selected.isEmpty()) {
            return null;
        }

        LocalDateTime departure = batchTime;
        int totalPassengers = 0;
        for (TripSegment segment : selected) {
            totalPassengers += segment.allocatedPassengers;
        }

        List<TripSegment> route = orderRoute(selected);

        String currentCode = airportCode;
        LocalDateTime currentTime = departure;
        double totalKm = 0;

        for (TripSegment segment : route) {
            AssignmentCandidate candidate = segment.candidate;

            if (candidate.hotelCode.equals(currentCode)) {
                segment.arrivalAtHotel = currentTime;
                continue;
            }

            Double legDistance = DistanceDao.findSymmetricDistanceKm(distancesKm, currentCode, candidate.hotelCode);
            if (legDistance == null) {
                if (airportCode.equals(currentCode)) {
                    legDistance = candidate.airportToHotelDistanceKm;
                } else {
                    legDistance = DistanceDao.findSymmetricDistanceKm(distancesKm, airportCode, candidate.hotelCode);
                }
            }
            if (legDistance == null) {
                return null;
            }

            long legMinutes = computeTravelMinutes(legDistance, vitesseMoyenne);
            currentTime = currentTime.plusMinutes(legMinutes);
            segment.arrivalAtHotel = currentTime;
            totalKm += legDistance;
            currentCode = candidate.hotelCode;
        }

        Double returnDistance = DistanceDao.findSymmetricDistanceKm(distancesKm, currentCode, airportCode);
        if (returnDistance == null && !route.isEmpty()) {
            returnDistance = route.get(route.size() - 1).candidate.airportToHotelDistanceKm;
        }
        if (returnDistance == null) {
            return null;
        }

        totalKm += returnDistance;
        long returnMinutes = computeTravelMinutes(returnDistance, vitesseMoyenne);
        LocalDateTime nextVehicleAvailableAt = currentTime.plusMinutes(returnMinutes);

        TripPlan tripPlan = new TripPlan();
        tripPlan.vehicleState = state;
        tripPlan.airportCode = airportCode;
        tripPlan.departure = departure;
        tripPlan.selectedSegments = selected;
        tripPlan.route = route;
        tripPlan.totalPassengers = totalPassengers;
        tripPlan.nextVehicleAvailableAt = nextVehicleAvailableAt;
        tripPlan.totalKmParcourus = totalKm;
        tripPlan.allocatedOnFocusReservation = allocatedOnFocusReservation;
        tripPlan.distinctReservationCount = countDistinctReservations(selected);
        tripPlan.focusVehicleFits = focusFitsVehicle;
        tripPlan.focusCapacityGap = computeCapacityGap(focusPassengersBefore, capacity);
        tripPlan.focusRemainingBeforeAssignment = focusPassengersBefore;
        tripPlan.focusCandidate = focusCandidate;
        return tripPlan;
    }

    private void applyTrip(TripPlan tripPlan, List<AssignmentCandidate> pending, AssignmentReport report) {
        int vehicleTripCount = tripPlan.vehicleState.tripCount + 1;
        int ordreDepot = 1;
        for (TripSegment segment : tripPlan.route) {
            TransferAssignment assignment = buildAssignmentFragment(segment.candidate, segment.allocatedPassengers);
            assignment.setVehiculeId(tripPlan.vehicleState.vehicule.getId());
            assignment.setVehiculeReference(tripPlan.vehicleState.vehicule.getReference());
            assignment.setVehiculeNbPlace(tripPlan.vehicleState.vehicule.getNbPlace());
            assignment.setVehiculeTypeCarburant(tripPlan.vehicleState.vehicule.getTypeCarburant());
            assignment.setVehiculeTripCount(vehicleTripCount);
            assignment.setHeureDepartAeroport(tripPlan.departure);
            assignment.setHeureArriveeHotel(segment.arrivalAtHotel);
            assignment.setHeureDisponibleVehicule(tripPlan.nextVehicleAvailableAt);
            assignment.setTrajetId(tripPlan.trajetId);
            assignment.setOrdreDepot(ordreDepot++);
            assignment.setPassagersTrajet(tripPlan.totalPassengers);
            assignment.setNbReservationsTrajet(tripPlan.distinctReservationCount);
            assignment.setKmParcourusTrajet(tripPlan.totalKmParcourus);
            report.getAssigned().add(assignment);
        }

        for (TripSegment segment : tripPlan.selectedSegments) {
            segment.candidate.remainingPassengers -= segment.allocatedPassengers;
        }
        if (tripPlan.focusCandidate != null
            && tripPlan.allocatedOnFocusReservation < tripPlan.focusRemainingBeforeAssignment
            && tripPlan.focusCandidate.remainingPassengers > 0) {
            tripPlan.focusCandidate.focusPriority = true;
        }

        pending.removeIf(candidate -> candidate.remainingPassengers <= 0);
        tripPlan.vehicleState.availableAt = tripPlan.nextVehicleAvailableAt;
        tripPlan.vehicleState.currentAirportCode = tripPlan.airportCode;
        tripPlan.vehicleState.tripCount++;
    }

    private int compareTripPlans(TripPlan left, TripPlan right) {
        // Sprint 8: Priorité aux véhicules qui peuvent partir immédiatement avec un chargement complet
        // Un véhicule est "immediate full" si:
        // 1. Il a déjà fait au moins 1 trajet (tripCount > 0)
        // 2. Il est complètement rempli (totalPassengers == capacité)
        boolean leftImmediateFull = left.vehicleState.tripCount > 0 
            && left.totalPassengers == left.vehicleState.vehicule.getNbPlace();
        boolean rightImmediateFull = right.vehicleState.tripCount > 0 
            && right.totalPassengers == right.vehicleState.vehicule.getNbPlace();
        
        int compare = Boolean.compare(rightImmediateFull, leftImmediateFull);
        if (compare != 0) {
            return compare;
        }

        compare = Boolean.compare(right.focusVehicleFits, left.focusVehicleFits);
        if (compare != 0) {
            return compare;
        }

        if (left.focusVehicleFits && right.focusVehicleFits) {
            compare = Integer.compare(left.focusCapacityGap, right.focusCapacityGap);
            if (compare != 0) {
                return compare;
            }
            compare = Integer.compare(right.totalPassengers, left.totalPassengers);
            if (compare != 0) {
                return compare;
            }
        } else {
            compare = Integer.compare(right.allocatedOnFocusReservation, left.allocatedOnFocusReservation);
            if (compare != 0) {
                return compare;
            }
        }

        compare = Integer.compare(left.vehicleState.tripCount, right.vehicleState.tripCount);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(isDiesel(left.vehicleState.vehicule) ? 0 : 1, isDiesel(right.vehicleState.vehicule) ? 0 : 1);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(left.focusCapacityGap, right.focusCapacityGap);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(right.vehicleState.vehicule.getNbPlace(), left.vehicleState.vehicule.getNbPlace());
        if (compare != 0) {
            return compare;
        }

        return Integer.compare(left.vehicleState.vehicule.getId(), right.vehicleState.vehicule.getId());
    }

    private List<TripSegment> orderRoute(List<TripSegment> selected) {
        if (selected.isEmpty()) {
            return new ArrayList<>();
        }

        List<TripSegment> ordered = new ArrayList<>(selected);
        ordered.sort((left, right) -> {
            int compare = Double.compare(left.candidate.airportToHotelDistanceKm, right.candidate.airportToHotelDistanceKm);
            if (compare != 0) {
                return compare;
            }
            compare = Integer.compare(right.focusSegment ? 1 : 0, left.focusSegment ? 1 : 0);
            if (compare != 0) {
                return compare;
            }
            compare = left.candidate.readyAt.compareTo(right.candidate.readyAt);
            if (compare != 0) {
                return compare;
            }
            return Integer.compare(left.candidate.reservationId, right.candidate.reservationId);
        });

        return ordered;
    }

    private TransferAssignment buildBaseAssignment(Reservation reservation, Map<Integer, Hotel> hotelsById) {
        TransferAssignment assignment = new TransferAssignment();
        assignment.setReservationId(reservation.getId());
        assignment.setIdClient(reservation.getIdClient());
        assignment.setNbPassager(reservation.getNbPassager());
        assignment.setNbPassagerReservation(reservation.getNbPassager());
        assignment.setIdHotel(reservation.getIdHotel());
        assignment.setHeureArriveeHotel(reservation.getDateHeureArrive());

        Hotel hotel = hotelsById.get(reservation.getIdHotel());
        if (hotel != null) {
            assignment.setHotelNom(hotel.getNom());
            assignment.setHotelCode(hotel.getCode());
        }
        return assignment;
    }

    private TransferAssignment buildAssignmentFragment(AssignmentCandidate candidate, int fragmentPassengers) {
        TransferAssignment assignment = new TransferAssignment();
        TransferAssignment baseAssignment = candidate.baseAssignment;

        assignment.setReservationId(baseAssignment.getReservationId());
        assignment.setIdClient(baseAssignment.getIdClient());
        assignment.setNbPassager(fragmentPassengers);
        assignment.setNbPassagerReservation(candidate.totalReservationPassengers);
        assignment.setIdHotel(baseAssignment.getIdHotel());
        assignment.setHotelNom(baseAssignment.getHotelNom());
        assignment.setHotelCode(baseAssignment.getHotelCode());
        assignment.setAeroportCode(baseAssignment.getAeroportCode());
        assignment.setHeureArriveeHotel(baseAssignment.getHeureArriveeHotel());
        assignment.setDistanceKm(baseAssignment.getDistanceKm());
        assignment.setDureeTrajetMinutes(baseAssignment.getDureeTrajetMinutes());
        return assignment;
    }

    private void markUnassigned(AssignmentReport report, TransferAssignment assignment, String motif) {
        assignment.setMotif(motif);
        report.getUnassigned().add(assignment);
    }

    private AirportChoice chooseNearestAirport(String hotelCode, List<Hotel> airports, Map<String, Double> distancesKm) {
        AirportChoice choice = null;
        for (Hotel airport : airports) {
            if (isBlank(airport.getCode())) {
                continue;
            }
            Double distance = DistanceDao.findSymmetricDistanceKm(distancesKm, airport.getCode(), hotelCode);
            if (distance == null) {
                continue;
            }

            if (choice == null
                || distance < choice.distanceKm
                || (distance.equals(choice.distanceKm) && airport.getCode().compareTo(choice.airportCode) < 0)) {
                choice = new AirportChoice();
                choice.airportCode = airport.getCode();
                choice.distanceKm = distance;
            }
        }
        return choice;
    }

    private Map<Integer, Hotel> mapHotelsById(List<Hotel> hotels) {
        Map<Integer, Hotel> map = new HashMap<>();
        for (Hotel hotel : hotels) {
            map.put(hotel.getIdHotel(), hotel);
        }
        return map;
    }

    private List<Hotel> findAirports(List<Hotel> hotels) {
        List<Hotel> forcedTnr = new ArrayList<>();
        List<Hotel> airports = new ArrayList<>();
        for (Hotel hotel : hotels) {
            if (hotel.isAeroport()) {
                if ("TNR".equalsIgnoreCase(hotel.getCode())) {
                    forcedTnr.add(hotel);
                }
                airports.add(hotel);
            }
        }
        if (!forcedTnr.isEmpty()) {
            return forcedTnr;
        }
        return airports;
    }

    private int countDistinctReservations(List<TripSegment> segments) {
        List<Integer> reservationIds = new ArrayList<>();
        for (TripSegment segment : segments) {
            if (!reservationIds.contains(segment.candidate.reservationId)) {
                reservationIds.add(segment.candidate.reservationId);
            }
        }
        return reservationIds.size();
    }

    private long computeTravelMinutes(double distanceKm, double vitesseMoyenneKmh) {
        double minutes = (distanceKm / vitesseMoyenneKmh) * 60.0;
        return Math.max(1L, (long) Math.ceil(minutes));
    }

    private LocalTime resolveInitialAvailability(Vehicule vehicule) {
        if (vehicule == null || vehicule.getHeureDisponibiliteDefaut() == null) {
            return LocalTime.MIDNIGHT;
        }
        return vehicule.getHeureDisponibiliteDefaut();
    }

    private LocalDateTime maxDateTime(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private boolean isDiesel(Vehicule vehicule) {
        return vehicule.getTypeCarburant() != null
            && "diesel".equals(vehicule.getTypeCarburant().trim().toLowerCase(Locale.ROOT));
    }

    private static double resolveDoubleEnv(String envName, double defaultValue) {
        String raw = System.getenv(envName);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            double parsed = Double.parseDouble(raw.trim());
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int resolveIntEnv(String envName, int defaultValue) {
        String raw = System.getenv(envName);
        if (raw == null || raw.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed >= 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private AssignmentCandidate chooseNextFocusCandidate(List<AssignmentCandidate> pending, List<VehicleState> vehicleStates, LocalDateTime batchTime) {
        AssignmentCandidate best = null;
        for (AssignmentCandidate candidate : pending) {
            if (candidate.remainingPassengers <= 0) {
                continue;
            }
            if (candidate.readyAt.isAfter(batchTime)) {
                continue;
            }
            if (!hasVehicleAvailableForBatch(candidate, vehicleStates, batchTime)) {
                continue;
            }
            if (best == null || compareFocusCandidates(candidate, best) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private int compareFocusCandidates(AssignmentCandidate left, AssignmentCandidate right) {
        int compare = Integer.compare(left.focusPriority ? 0 : 1, right.focusPriority ? 0 : 1);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(right.remainingPassengers, left.remainingPassengers);
        if (compare != 0) {
            return compare;
        }

        compare = left.readyAt.compareTo(right.readyAt);
        if (compare != 0) {
            return compare;
        }

        compare = Integer.compare(left.reservationOrder, right.reservationOrder);
        if (compare != 0) {
            return compare;
        }

        return Integer.compare(left.reservationId, right.reservationId);
    }

    private int computeCapacityGap(int passengers, int capacity) {
        if (capacity <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.abs(passengers - capacity);
    }

    private AssignmentCandidate chooseClosestSupplementCandidate(List<AssignmentCandidate> candidates, int remainingCapacity) {
        AssignmentCandidate best = null;
        for (AssignmentCandidate candidate : candidates) {
            if (candidate.remainingPassengers <= 0) {
                continue;
            }

            if (best == null) {
                best = candidate;
                continue;
            }

            int candidateGap = Math.abs(candidate.remainingPassengers - remainingCapacity);
            int bestGap = Math.abs(best.remainingPassengers - remainingCapacity);
            if (candidateGap < bestGap) {
                best = candidate;
                continue;
            }
            if (candidateGap > bestGap) {
                continue;
            }

            if (candidate.isSplitInProgress() && !best.isSplitInProgress()) {
                best = candidate;
                continue;
            }
            if (!candidate.isSplitInProgress() && best.isSplitInProgress()) {
                continue;
            }

            if (candidate.remainingPassengers > best.remainingPassengers) {
                best = candidate;
                continue;
            }
            if (candidate.remainingPassengers < best.remainingPassengers) {
                continue;
            }

            if (candidate.reservationOrder < best.reservationOrder) {
                best = candidate;
                continue;
            }
            if (candidate.reservationOrder > best.reservationOrder) {
                continue;
            }

            if (candidate.readyAt.isBefore(best.readyAt)) {
                best = candidate;
                continue;
            }
            if (candidate.readyAt.isAfter(best.readyAt)) {
                continue;
            }

            if (candidate.reservationId < best.reservationId) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean hasImmediateVehicleAvailable(AssignmentCandidate candidate, List<VehicleState> vehicleStates) {
        for (VehicleState state : vehicleStates) {
            if (state.availableAt.isAfter(candidate.readyAt)) {
                continue;
            }
            if (!isBlank(state.currentAirportCode) && !candidate.airportCode.equals(state.currentAirportCode)) {
                continue;
            }
            if (state.vehicule.getNbPlace() > 0) {
                return true;
            }
        }
        return false;
    }

    private AssignmentCandidate chooseNextDeferredCandidate(List<AssignmentCandidate> pending, LocalDateTime batchTime) {
        AssignmentCandidate best = null;
        for (AssignmentCandidate candidate : pending) {
            if (candidate.remainingPassengers <= 0) {
                continue;
            }
            if (candidate.readyAt.isAfter(batchTime)) {
                continue;
            }
            if (best == null || compareFocusCandidates(candidate, best) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private boolean hasVehicleAvailableForBatch(AssignmentCandidate candidate, List<VehicleState> vehicleStates, LocalDateTime batchTime) {
        for (VehicleState state : vehicleStates) {
            if (state.availableAt.isAfter(batchTime)) {
                continue;
            }
            if (!isBlank(state.currentAirportCode) && !candidate.airportCode.equals(state.currentAirportCode)) {
                continue;
            }
            if (state.vehicule.getNbPlace() > 0) {
                return true;
            }
        }
        return false;
    }

    private LocalDateTime findNextBatchTime(List<AssignmentCandidate> pending, List<VehicleState> vehicleStates, int waitTimeMinutes) {
        // Sprint 8: Check if any returned vehicle can be completely filled by already-arrived reservations
        LocalDateTime immediateBatch = findImmediateFullVehicleBatchTime(pending, vehicleStates);

        LocalDateTime anchor = null;
        for (VehicleState state : vehicleStates) {
            LocalDateTime candidateAnchor = state.availableAt;
            if (!hasPendingCandidateReadyBy(pending, candidateAnchor.plusMinutes(waitTimeMinutes))) {
                continue;
            }
            if (anchor == null || candidateAnchor.isBefore(anchor)) {
                anchor = candidateAnchor;
            }
        }
        if (anchor == null) {
            // Sprint 8: If no standard anchor but immediate batch is possible, use it
            return immediateBatch;
        }

        LocalDateTime windowEnd = anchor.plusMinutes(waitTimeMinutes);
        LocalDateTime batchTime = anchor;
        for (AssignmentCandidate candidate : pending) {
            if (candidate.remainingPassengers <= 0) {
                continue;
            }
            if (candidate.readyAt.isAfter(windowEnd)) {
                continue;
            }
            if (candidate.readyAt.isAfter(batchTime)) {
                batchTime = candidate.readyAt;
            }
        }

        while (true) {
            int currentMaxCapacity = findMaxCapacityAvailableBy(vehicleStates, batchTime);
            int requiredCapacity = findLargestReadyReservation(pending, batchTime);
            if (requiredCapacity <= currentMaxCapacity) {
                break;
            }
            LocalDateTime betterVehicleTime = findNextBetterVehicleAvailability(vehicleStates, batchTime, windowEnd, currentMaxCapacity);
            if (betterVehicleTime == null) {
                break;
            }
            batchTime = betterVehicleTime;
        }

        // Sprint 8: Return earliest between immediate full-vehicle batch and standard batch
        if (immediateBatch != null && immediateBatch.isBefore(batchTime)) {
            return immediateBatch;
        }
        return batchTime;
    }

    // Sprint 8: Find earliest time when a returned vehicle can be completely filled
    // by reservations that have already arrived (readyAt <= vehicle availability time)
    private LocalDateTime findImmediateFullVehicleBatchTime(List<AssignmentCandidate> pending, List<VehicleState> vehicleStates) {
        LocalDateTime best = null;

        for (VehicleState state : vehicleStates) {
            // Only applies to vehicles that have returned from at least one trip
            if (state.tripCount == 0) {
                continue;
            }

            int capacity = state.vehicule.getNbPlace();
            if (capacity <= 0) {
                continue;
            }

            // Sum passengers from reservations already arrived at vehicle's return time
            int totalReadyPassengers = 0;
            for (AssignmentCandidate candidate : pending) {
                if (candidate.remainingPassengers <= 0) {
                    continue;
                }
                if (candidate.readyAt.isAfter(state.availableAt)) {
                    continue;
                }
                if (!isBlank(state.currentAirportCode) && !candidate.airportCode.equals(state.currentAirportCode)) {
                    continue;
                }
                totalReadyPassengers += candidate.remainingPassengers;
            }

            // If the vehicle can be completely filled, it departs immediately
            if (totalReadyPassengers >= capacity) {
                if (best == null || state.availableAt.isBefore(best)) {
                    best = state.availableAt;
                }
            }
        }

        return best;
    }

    private boolean hasPendingCandidateReadyBy(List<AssignmentCandidate> pending, LocalDateTime time) {
        for (AssignmentCandidate candidate : pending) {
            if (candidate.remainingPassengers <= 0) {
                continue;
            }
            if (!candidate.readyAt.isAfter(time)) {
                return true;
            }
        }
        return false;
    }

    private int findMaxCapacityAvailableBy(List<VehicleState> vehicleStates, LocalDateTime time) {
        int maxCapacity = 0;
        for (VehicleState state : vehicleStates) {
            if (state.availableAt.isAfter(time)) {
                continue;
            }
            if (state.vehicule.getNbPlace() > maxCapacity) {
                maxCapacity = state.vehicule.getNbPlace();
            }
        }
        return maxCapacity;
    }

    private int findLargestReadyReservation(List<AssignmentCandidate> pending, LocalDateTime time) {
        int maxPassengers = 0;
        for (AssignmentCandidate candidate : pending) {
            if (candidate.remainingPassengers <= 0) {
                continue;
            }
            if (candidate.readyAt.isAfter(time)) {
                continue;
            }
            if (candidate.remainingPassengers > maxPassengers) {
                maxPassengers = candidate.remainingPassengers;
            }
        }
        return maxPassengers;
    }

    private LocalDateTime findNextBetterVehicleAvailability(
        List<VehicleState> vehicleStates,
        LocalDateTime batchTime,
        LocalDateTime windowEnd,
        int currentMaxCapacity
    ) {
        LocalDateTime best = null;
        for (VehicleState state : vehicleStates) {
            if (!state.availableAt.isAfter(batchTime) || state.availableAt.isAfter(windowEnd)) {
                continue;
            }
            if (state.vehicule.getNbPlace() <= currentMaxCapacity) {
                continue;
            }
            if (best == null || state.availableAt.isBefore(best)) {
                best = state.availableAt;
            }
        }
        return best;
    }

    private static class AssignmentCandidate {
        private int reservationId;
        private int reservationOrder;
        private String airportCode;
        private String hotelCode;
        private int remainingPassengers;
        private int totalReservationPassengers;
        private LocalDateTime readyAt;
        private double airportToHotelDistanceKm;
        private TransferAssignment baseAssignment;
        private boolean focusPriority;

        private boolean isSplitInProgress() {
            return remainingPassengers > 0 && remainingPassengers < totalReservationPassengers;
        }
    }

    private static class VehicleState {
        private Vehicule vehicule;
        private LocalDateTime availableAt;
        private String currentAirportCode;
        private int tripCount;
    }

    private static class TripPlan {
        private int trajetId;
        private VehicleState vehicleState;
        private String airportCode;
        private LocalDateTime departure;
        private List<TripSegment> selectedSegments;
        private List<TripSegment> route;
        private int totalPassengers;
        private LocalDateTime nextVehicleAvailableAt;
        private double totalKmParcourus;
        private int allocatedOnFocusReservation;
        private int distinctReservationCount;
        private boolean focusVehicleFits;
        private int focusCapacityGap;
        private int focusRemainingBeforeAssignment;
        private AssignmentCandidate focusCandidate;
    }

    private static class TripSegment {
        private AssignmentCandidate candidate;
        private int allocatedPassengers;
        private LocalDateTime arrivalAtHotel;
        private boolean focusSegment;
    }

    private static class AirportChoice {
        private String airportCode;
        private Double distanceKm;
    }
}
