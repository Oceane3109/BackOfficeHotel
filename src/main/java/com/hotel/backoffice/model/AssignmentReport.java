package com.hotel.backoffice.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AssignmentReport {
    private LocalDate date;
    private double vitesseMoyenneKmh;
    private int waitTimeMinutes;
    private List<TransferAssignment> assigned = new ArrayList<>();
    private List<TransferAssignment> unassigned = new ArrayList<>();

    public int getTotalReservations() {
        Set<Integer> reservationIds = new HashSet<>();
        collectReservationIds(assigned, reservationIds);
        collectReservationIds(unassigned, reservationIds);
        return reservationIds.size();
    }

    public int getTotalTrajets() {
        Set<Integer> trajetIds = new HashSet<>();
        for (TransferAssignment assignment : assigned) {
            if (assignment.getTrajetId() != null) {
                trajetIds.add(assignment.getTrajetId());
            }
        }
        return trajetIds.size();
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

    public int getWaitTimeMinutes() {
        return waitTimeMinutes;
    }

    public void setWaitTimeMinutes(int waitTimeMinutes) {
        this.waitTimeMinutes = waitTimeMinutes;
    }

    public int getAssignedReservationCount() {
        return countDistinctReservations(assigned);
    }

    public int getUnassignedReservationCount() {
        return countDistinctReservations(unassigned);
    }

    private int countDistinctReservations(List<TransferAssignment> assignments) {
        Set<Integer> reservationIds = new HashSet<>();
        collectReservationIds(assignments, reservationIds);
        return reservationIds.size();
    }

    private void collectReservationIds(List<TransferAssignment> assignments, Set<Integer> reservationIds) {
        for (TransferAssignment assignment : assignments) {
            reservationIds.add(assignment.getReservationId());
        }
    }
}
