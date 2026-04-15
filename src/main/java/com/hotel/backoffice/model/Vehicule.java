package com.hotel.backoffice.model;

import java.time.LocalTime;

public class Vehicule {
    private int id;
    private String reference;
    private int nbPlace;
    private String typeCarburant; // Diesel, Essence, Hybride, ES
    private LocalTime heureDisponibiliteDefaut;

    public Vehicule() {
        this.heureDisponibiliteDefaut = LocalTime.MIDNIGHT;
    }

    public Vehicule(int id, String reference, int nbPlace, String typeCarburant, LocalTime heureDisponibiliteDefaut) {
        this.id = id;
        this.reference = reference;
        this.nbPlace = nbPlace;
        this.typeCarburant = typeCarburant;
        this.heureDisponibiliteDefaut = heureDisponibiliteDefaut != null ? heureDisponibiliteDefaut : LocalTime.MIDNIGHT;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public int getNbPlace() { return nbPlace; }
    public void setNbPlace(int nbPlace) { this.nbPlace = nbPlace; }

    public String getTypeCarburant() { return typeCarburant; }
    public void setTypeCarburant(String typeCarburant) { this.typeCarburant = typeCarburant; }

    public LocalTime getHeureDisponibiliteDefaut() { return heureDisponibiliteDefaut; }
    public void setHeureDisponibiliteDefaut(LocalTime heureDisponibiliteDefaut) {
        this.heureDisponibiliteDefaut = heureDisponibiliteDefaut != null ? heureDisponibiliteDefaut : LocalTime.MIDNIGHT;
    }
}
