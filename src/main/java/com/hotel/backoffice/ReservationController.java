package com.hotel.backoffice;

import com.hotel.backoffice.model.AssignmentReport;
import mg.framework.annotations.Controlleur;
import mg.framework.annotations.GetMapping;
import mg.framework.annotations.PostMapping;
import mg.framework.annotations.RequestParam;
import mg.framework.model.ModelView;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.hotel.backoffice.model.Reservation;

@Controlleur
public class ReservationController {
    private final ReservationDao reservationDao = new ReservationDao();
    private final VehicleAssignmentService vehicleAssignmentService = new VehicleAssignmentService();

    @GetMapping("/reservations")
    public ModelView list(@RequestParam("date") String dateValue) {
        ModelView mv = new ModelView("/WEB-INF/jsp/reservations.jsp");

        LocalDate selectedDate = LocalDate.now();
        if (dateValue != null && !dateValue.trim().isEmpty()) {
            try {
                selectedDate = LocalDate.parse(dateValue.trim());
            } catch (Exception e) {
                mv.addAttribute("error", "Date invalide. Format attendu: YYYY-MM-DD");
            }
        }

        try {
            AssignmentReport report = vehicleAssignmentService.buildDailyReport(selectedDate);
            mv.addAttribute("selectedDate", selectedDate.toString());
            mv.addAttribute("vitesseMoyenne", report.getVitesseMoyenneKmh());
            mv.addAttribute("tempsAttenteMax", report.getTempsAttenteMaxMinutes());
            mv.addAttribute("totalReservations", report.getTotalReservations());
            mv.addAttribute("assignedCount", report.getAssigned().size());
            mv.addAttribute("unassignedCount", report.getUnassigned().size());
            mv.addAttribute("assignations", report.getAssigned());
            mv.addAttribute("nonAssignees", report.getUnassigned());
        } catch (Exception e) {
            mv.addAttribute("error",
                "Erreur lors du calcul d'assignation: " + e.getMessage()
                + ". Vérifiez la migration SQL Sprint 3 (hotel.code, hotel.aeroport, table distance).");
        }

        return mv;
    }

    @GetMapping("/reservations")
    public ModelView list() {
        ModelView mv = new ModelView("/WEB-INF/jsp/reservations.jsp");
        try {
            List<Reservation> reservations = reservationDao.findAll();
            mv.addAttribute("reservations", reservations);
        } catch (SQLException e) {
            mv.addAttribute("error", "Erreur lors de la récupération des réservations");
        }
        return mv;
    }

    @GetMapping("/reservations/new")
    public ModelView form() {
        return new ModelView("/WEB-INF/jsp/reservation-form.jsp");
    }

    @GetMapping("/reservations/edit")
    public ModelView editForm(@RequestParam("id") String id) {
        ModelView mv = new ModelView("/WEB-INF/jsp/reservation-form.jsp");
        try {
            int reservationId = Integer.parseInt(id);
            Reservation reservation = reservationDao.findById(reservationId);
            if (reservation == null) {
                mv.addAttribute("error", "Réservation non trouvée");
                return mv;
            }

            mv.addAttribute("reservation", reservation);
            if (reservation.getDateHeureArrive() != null) {
                mv.addAttribute(
                    "dateHeureArriveValue",
                    reservation.getDateHeureArrive().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                );
            }
        } catch (NumberFormatException | SQLException e) {
            mv.addAttribute("error", "Erreur lors de la récupération de la réservation");
        }
        return mv;
    }

    @PostMapping("/reservations/save")
    public ModelView save(
            @RequestParam("id") String id,
            @RequestParam("id_client") String idClient,
            @RequestParam("nb_passager") String nbPassager,
            @RequestParam("date_heure_arrive") String dateHeure,
            @RequestParam("id_hotel") String idHotel
    ) {
        ModelView mv = new ModelView("/WEB-INF/jsp/reservations.jsp");

        if (idClient == null || idClient.length() != 4) {
            mv.addAttribute("error", "id_client doit contenir 4 caractères");
            return mv;
        }

        try {
            int nb = Integer.parseInt(nbPassager);
            int hotel = Integer.parseInt(idHotel);
            LocalDateTime ldt = LocalDateTime.parse(dateHeure, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            reservationDao.insert(idClient, nb, Timestamp.valueOf(ldt), hotel);
            mv.addAttribute("success", "Réservation enregistrée");
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de l'enregistrement: " + e.getMessage());
            if (id != null && !id.isEmpty()) {
                int reservationId = Integer.parseInt(id);
                reservationDao.update(reservationId, idClient, nb, Timestamp.valueOf(ldt), hotel);
                mv.addAttribute("success", "Réservation modifiée");
            } else {
                reservationDao.insert(idClient, nb, Timestamp.valueOf(ldt), hotel);
                mv.addAttribute("success", "Réservation ajoutée");
            }

            List<Reservation> reservations = reservationDao.findAll();
            mv.addAttribute("reservations", reservations);
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de l'enregistrement: " + e.getMessage());
            try {
                List<Reservation> reservations = reservationDao.findAll();
                mv.addAttribute("reservations", reservations);
            } catch (SQLException ex) {
                // ignore
            }
        }

        return mv;
    }

    @PostMapping("/reservations/delete")
    public ModelView delete(@RequestParam("id") String id) {
        ModelView mv = new ModelView("/WEB-INF/jsp/reservations.jsp");
        try {
            int reservationId = Integer.parseInt(id);
            reservationDao.delete(reservationId);
            mv.addAttribute("success", "Réservation supprimée");
            List<Reservation> reservations = reservationDao.findAll();
            mv.addAttribute("reservations", reservations);
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la suppression");
            try {
                List<Reservation> reservations = reservationDao.findAll();
                mv.addAttribute("reservations", reservations);
            } catch (SQLException ex) {
                // ignore
            }
        }
        return mv;
    }
}
