package com.hotel.backoffice;

import com.hotel.backoffice.model.AssignmentReport;
import com.hotel.backoffice.model.Hotel;
import com.hotel.backoffice.model.Reservation;
import com.hotel.backoffice.model.TransferAssignment;
import com.hotel.backoffice.model.Vehicule;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VehicleAssignmentService {
    private static final double DEFAULT_VITESSE_MOYENNE_KMH = 40.0;
    private static final int DEFAULT_TEMPS_ATTENTE_MAX_MINUTES = 30;

    private final ReservationDao reservationDao = new ReservationDao();
    private final VehiculeDao vehiculeDao = new VehiculeDao();
    private final HotelDao hotelDao = new HotelDao();
    private final DistanceDao distanceDao = new DistanceDao();

    public AssignmentReport buildDailyReport(LocalDate date) throws SQLException {
        double vitesseMoyenne = resolveDoubleEnv("VITESSE_MOYENNE_KMH", DEFAULT_VITESSE_MOYENNE_KMH);
        int tempsAttenteMax = resolveIntEnv("TEMPS_ATTENTE_MAX_MINUTES", DEFAULT_TEMPS_ATTENTE_MAX_MINUTES);

        List<Reservation> reservations = reservationDao.findByDate(date);
        reservations.sort(Comparator.comparing(Reservation::getDateHeureArrive));

        List<Vehicule> vehicules = vehiculeDao.findAll();
        List<Hotel> hotels = hotelDao.findAll();
        Map<Integer, Hotel> hotelsById = mapHotelsById(hotels);
        List<Hotel> airports = findAirports(hotels);
        Map<String, Double> distancesKm = distanceDao.findAllDistancesKm();
        Map<Integer, LocalDateTime> vehicleAvailableAtAirport = new HashMap<>();

        AssignmentReport report = new AssignmentReport();
        report.setDate(date);
        report.setVitesseMoyenneKmh(vitesseMoyenne);
        report.setTempsAttenteMaxMinutes(tempsAttenteMax);

        for (Reservation reservation : reservations) {
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
                markUnassigned(report, assignment, "Date d'arrivée manquante");
                continue;
            }

            AirportChoice nearestAirport = chooseNearestAirport(hotel.getCode(), airports, distancesKm);
            if (nearestAirport == null) {
                markUnassigned(report, assignment, "Distance introuvable entre aéroport et hôtel");
                continue;
            }

            long oneWayMinutes = computeTravelMinutes(nearestAirport.distanceKm, vitesseMoyenne);
            LocalDateTime arrival = reservation.getDateHeureArrive();
            LocalDateTime departure = arrival.minusMinutes(oneWayMinutes);

            assignment.setAeroportCode(nearestAirport.airportCode);
            assignment.setDistanceKm(nearestAirport.distanceKm);
            assignment.setDureeTrajetMinutes(oneWayMinutes);
            assignment.setHeureDepartAeroport(departure);
            assignment.setHeureArriveeHotel(arrival);

            List<Vehicule> capacityCandidates = findVehiclesWithCapacity(vehicules, reservation.getNbPassager());
            if (capacityCandidates.isEmpty()) {
                markUnassigned(report, assignment, "Pas de véhicule avec capacité suffisante");
                continue;
            }

            Vehicule selected = null;
            for (Vehicule candidate : capacityCandidates) {
                LocalDateTime availableFrom = vehicleAvailableAtAirport.getOrDefault(candidate.getId(), LocalDateTime.MIN);
                if (!departure.isBefore(availableFrom)) {
                    selected = candidate;
                    break;
                }
            }

            if (selected == null) {
                markUnassigned(report, assignment, "Conflit d'horaires (temps d'attente max: " + tempsAttenteMax + " min)");
                continue;
            }

            assignment.setVehiculeId(selected.getId());
            assignment.setVehiculeReference(selected.getReference());
            assignment.setVehiculeNbPlace(selected.getNbPlace());
            assignment.setVehiculeTypeCarburant(selected.getTypeCarburant());
            report.getAssigned().add(assignment);

            long returnMinutes = oneWayMinutes;
            LocalDateTime nextAvailable = arrival.plusMinutes(returnMinutes + tempsAttenteMax);
            vehicleAvailableAtAirport.put(selected.getId(), nextAvailable);
        }

        return report;
    }

    private TransferAssignment buildBaseAssignment(Reservation reservation, Map<Integer, Hotel> hotelsById) {
        TransferAssignment a = new TransferAssignment();
        a.setReservationId(reservation.getId());
        a.setIdClient(reservation.getIdClient());
        a.setNbPassager(reservation.getNbPassager());
        a.setIdHotel(reservation.getIdHotel());
        a.setHeureArriveeHotel(reservation.getDateHeureArrive());

        Hotel hotel = hotelsById.get(reservation.getIdHotel());
        if (hotel != null) {
            a.setHotelNom(hotel.getNom());
            a.setHotelCode(hotel.getCode());
        }
        return a;
    }

    private void markUnassigned(AssignmentReport report, TransferAssignment assignment, String motif) {
        assignment.setMotif(motif);
        report.getUnassigned().add(assignment);
    }

    private List<Vehicule> findVehiclesWithCapacity(List<Vehicule> vehicules, int nbPassager) {
        List<Vehicule> candidates = new ArrayList<>();
        for (Vehicule vehicule : vehicules) {
            if (vehicule.getNbPlace() >= nbPassager) {
                candidates.add(vehicule);
            }
        }

        candidates.sort(
            Comparator
                .comparingInt((Vehicule v) -> v.getNbPlace() - nbPassager)
                .thenComparingInt(v -> isDiesel(v) ? 0 : 1)
                .thenComparingInt(Vehicule::getNbPlace)
                .thenComparingInt(Vehicule::getId)
        );
        return candidates;
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
        List<Hotel> airports = new ArrayList<>();
        for (Hotel hotel : hotels) {
            if (hotel.isAeroport()) {
                airports.add(hotel);
            }
        }
        return airports;
    }

    private long computeTravelMinutes(double distanceKm, double vitesseMoyenneKmh) {
        double minutes = (distanceKm / vitesseMoyenneKmh) * 60.0;
        return Math.max(1L, (long) Math.ceil(minutes));
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

    private static class AirportChoice {
        private String airportCode;
        private Double distanceKm;
    }
}
