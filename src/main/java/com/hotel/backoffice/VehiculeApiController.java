package com.hotel.backoffice;

import com.hotel.backoffice.model.ApiResponse;
import com.hotel.backoffice.model.Vehicule;
import mg.framework.annotations.Controlleur;
import mg.framework.annotations.GetMapping;
import mg.framework.annotations.Json;
import mg.framework.annotations.PostMapping;
import mg.framework.annotations.RequestParam;

import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;

@Controlleur
public class VehiculeApiController {
    private final VehiculeDao vehiculeDao = new VehiculeDao();
    private final TokenDao tokenDao = new TokenDao();

    @Json
    @GetMapping("/api/vehicules")
    public ApiResponse<String> list(@RequestParam("token") String token) {
        try {
            if (token == null || token.isEmpty() || !tokenDao.isTokenValid(token)) {
                return new ApiResponse<>("error", 401, "Token invalide");
            }
            List<Vehicule> vehicules = vehiculeDao.findAll();
            return new ApiResponse<>("success", 200, "Vehicules: " + vehicules.size());
        } catch (Exception e) {
            return new ApiResponse<>("error", 500, "Erreur: " + e.getMessage());
        }
    }

    @Json
    @PostMapping("/api/vehicules")
    public ApiResponse<String> create(
            @RequestParam("token") String token,
            @RequestParam("reference") String reference,
            @RequestParam("nb_place") int nbPlace,
            @RequestParam("type_carburant") String typeCarburant,
            @RequestParam("heure_disponibilite_defaut") String heureDisponibiliteDefautValue) throws SQLException {
        if (token == null || token.isEmpty() || !tokenDao.isTokenValid(token)) {
            return new ApiResponse<>("error", 401, "Token invalide ou expiré");
        }
        vehiculeDao.insert(reference, nbPlace, typeCarburant, parseAvailabilityTime(heureDisponibiliteDefautValue));
        return new ApiResponse<>("success", 201, "Véhicule créé");
    }

    @Json
    @PostMapping("/api/vehicules/update")
    public ApiResponse<String> update(
            @RequestParam("token") String token,
            @RequestParam("id") int id,
            @RequestParam("reference") String reference,
            @RequestParam("nb_place") int nbPlace,
            @RequestParam("type_carburant") String typeCarburant,
            @RequestParam("heure_disponibilite_defaut") String heureDisponibiliteDefautValue) throws SQLException {
        if (token == null || token.isEmpty() || !tokenDao.isTokenValid(token)) {
            return new ApiResponse<>("error", 401, "Token invalide ou expiré");
        }
        vehiculeDao.update(id, reference, nbPlace, typeCarburant, parseAvailabilityTime(heureDisponibiliteDefautValue));
        return new ApiResponse<>("success", 200, "Véhicule mis à jour");
    }

    @Json
    @PostMapping("/api/vehicules/delete")
    public ApiResponse<String> delete(
            @RequestParam("token") String token,
            @RequestParam("id") int id) throws SQLException {
        if (token == null || token.isEmpty() || !tokenDao.isTokenValid(token)) {
            return new ApiResponse<>("error", 401, "Token invalide ou expiré");
        }
        vehiculeDao.delete(id);
        return new ApiResponse<>("success", 200, "Véhicule supprimé");
    }

    private LocalTime parseAvailabilityTime(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return LocalTime.MIDNIGHT;
        }
        return LocalTime.parse(rawValue.trim());
    }
}
