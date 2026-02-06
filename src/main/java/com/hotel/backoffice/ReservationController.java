package com.hotel.backoffice;

import mg.framework.annotations.Controlleur;
import mg.framework.annotations.GetMapping;
import mg.framework.annotations.PostMapping;
import mg.framework.annotations.RequestParam;
import mg.framework.model.ModelView;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Controlleur
public class ReservationController {
    private final ReservationDao reservationDao = new ReservationDao();

    @GetMapping("/reservations/new")
    public ModelView form() {
        return new ModelView("/WEB-INF/jsp/reservation-form.jsp");
    }

    @PostMapping("/reservations/new")
    public ModelView submit(
            @RequestParam("id_client") String idClient,
            @RequestParam("nb_passager") String nbPassager,
            @RequestParam("date_heure_arrive") String dateHeure,
            @RequestParam("id_hotel") String idHotel
    ) {
        ModelView mv = new ModelView("/WEB-INF/jsp/reservation-form.jsp");

        if (idClient == null || idClient.length() != 4) {
            mv.addAttribute("error", "id_client doit contenir 4 caractères");
            return mv;
        }

        try {
            int nb = Integer.parseInt(nbPassager);
            int hotel = Integer.parseInt(idHotel);
            LocalDateTime ldt = LocalDateTime.parse(dateHeure);
            reservationDao.insert(idClient, nb, Timestamp.valueOf(ldt), hotel);
            mv.addAttribute("success", "Réservation enregistrée");
        } catch (NumberFormatException | SQLException e) {
            mv.addAttribute("error", "Erreur lors de l'enregistrement");
        }

        return mv;
    }
}
