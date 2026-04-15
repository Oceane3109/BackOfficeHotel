package com.hotel.backoffice;

import com.hotel.backoffice.model.ApiResponse;
import com.hotel.backoffice.model.AssignmentReport;
import com.hotel.backoffice.model.Reservation;
import mg.framework.annotations.Controlleur;
import mg.framework.annotations.GetMapping;
import mg.framework.annotations.Json;
import mg.framework.annotations.RequestParam;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controlleur
public class ReservationApiController {
    private final ReservationDao reservationDao = new ReservationDao();
    private final VehicleAssignmentService assignmentService = new VehicleAssignmentService();

    @GetMapping("/api/reservations")
    @Json
    public List<java.util.Map<String,Object>> list(@RequestParam("date") String date) {
        try {
            List<Reservation> reservations;
            if (date != null && !date.isEmpty()) {
                reservations = reservationDao.findByDate(LocalDate.parse(date));
            } else {
                reservations = reservationDao.findAll();
            }
            java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
            for (Reservation r : reservations) {
                java.util.Map<String,Object> m = new java.util.HashMap<>();
                m.put("id", r.getId());
                m.put("idClient", r.getIdClient());
                m.put("nbPassager", r.getNbPassager());
                m.put("dateHeureArrive", r.getDateHeureArrive() != null ? r.getDateHeureArrive().toString() : null);
                m.put("idHotel", r.getIdHotel());
                out.add(m);
            }
            return out;
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    @GetMapping("/api/reservation/assign")
    @Json
    public java.util.Map<String, Object> assign(@RequestParam("date") String dateStr) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        try {
            LocalDate date = LocalDate.parse(dateStr);
            AssignmentReport report = assignmentService.buildDailyReport(date);
            
            response.put("date", date.toString());
            response.put("vitesseMoyenneKmh", report.getVitesseMoyenneKmh());
            response.put("waitTimeMinutes", report.getWaitTimeMinutes());
            response.put("totalReservations", report.getTotalReservations());
            response.put("totalTrajets", report.getTotalTrajets());
            response.put("assignedCount", report.getAssigned().size());
            response.put("unassignedCount", report.getUnassigned().size());
            
            java.util.List<java.util.Map<String, Object>> assigned = new java.util.ArrayList<>();
            for (var a : report.getAssigned()) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("reservationId", a.getReservationId());
                item.put("idClient", a.getIdClient());
                item.put("nbPassager", a.getNbPassager());
                item.put("nbPassagerReservation", a.getNbPassagerReservation());
                item.put("hotelNom", a.getHotelNom());
                item.put("vehiculeReference", a.getVehiculeReference());
                item.put("vehiculeNbPlace", a.getVehiculeNbPlace());
                item.put("vehiculeTripCount", a.getVehiculeTripCount());
                item.put("heureDepartAeroport", a.getHeureDepartAeroport() != null ? a.getHeureDepartAeroport().toString() : null);
                item.put("heureArriveeHotel", a.getHeureArriveeHotel() != null ? a.getHeureArriveeHotel().toString() : null);
                item.put("heureDisponibleVehicule", a.getHeureDisponibleVehicule() != null ? a.getHeureDisponibleVehicule().toString() : null);
                item.put("trajetId", a.getTrajetId());
                item.put("ordreDepot", a.getOrdreDepot());
                item.put("passagersTrajet", a.getPassagersTrajet());
                item.put("fractionnee", a.isFractionnee());
                assigned.add(item);
            }
            response.put("assigned", assigned);
            
            java.util.List<java.util.Map<String, Object>> unassigned = new java.util.ArrayList<>();
            for (var u : report.getUnassigned()) {
                java.util.Map<String, Object> item = new java.util.HashMap<>();
                item.put("reservationId", u.getReservationId());
                item.put("idClient", u.getIdClient());
                item.put("nbPassager", u.getNbPassager());
                item.put("hotelNom", u.getHotelNom());
                item.put("motif", u.getMotif());
                unassigned.add(item);
            }
            response.put("unassigned", unassigned);
            
            return response;
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return response;
        }
    }
}
