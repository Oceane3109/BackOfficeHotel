package com.hotel.backoffice.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AssignmentReport {
    private LocalDate date;
    private double vitesseMoyenneKmh;
    private int tempsAttenteMaxMinutes;
    private List<TransferAssignment> assigned = new ArrayList<>();
    private List<TransferAssignment> unassigned = new ArrayList<>();

    public int getTotalReservations() {
        return assigned.size() + unassigned.size();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getVitesseMoyenneKmh() {
        return vitesseMoyenneKmh;
    }

    public void setVitesseMoyenneKmh(double vitesseMoyenneKmh) {
        this.vitesseMoyenneKmh = vitesseMoyenneKmh;
    }

    public int getTempsAttenteMaxMinutes() {
        return tempsAttenteMaxMinutes;
    }

    public void setTempsAttenteMaxMinutes(int tempsAttenteMaxMinutes) {
        this.tempsAttenteMaxMinutes = tempsAttenteMaxMinutes;
    }

    public List<TransferAssignment> getAssigned() {
        return assigned;
    }

    public void setAssigned(List<TransferAssignment> assigned) {
        this.assigned = assigned;
    }

    public List<TransferAssignment> getUnassigned() {
        return unassigned;
    }

    public void setUnassigned(List<TransferAssignment> unassigned) {
        this.unassigned = unassigned;
    }
}
