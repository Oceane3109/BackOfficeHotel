package com.hotel.backoffice;

import com.hotel.backoffice.model.Reservation;
import mg.framework.annotations.Controlleur;
import mg.framework.annotations.GetMapping;
import mg.framework.annotations.Json;
import mg.framework.annotations.RequestParam;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Controlleur
public class ReservationApiController {
    private final ReservationDao reservationDao = new ReservationDao();

    @Json
    @GetMapping("/api/reservations")
    public List<Reservation> list(@RequestParam("date") String date) throws SQLException {
        if (date != null && !date.isEmpty()) {
            return reservationDao.findByDate(LocalDate.parse(date));
        }
        return reservationDao.findAll();
    }
}
