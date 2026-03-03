package com.hotel.backoffice;

import mg.framework.annotations.Controlleur;
import mg.framework.annotations.GetMapping;
import mg.framework.annotations.PostMapping;
import mg.framework.annotations.RequestParam;
import mg.framework.model.ModelView;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.hotel.backoffice.model.Reservation;

@Controlleur
public class ReservationController {
    private final ReservationDao reservationDao = new ReservationDao();

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
