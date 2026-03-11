<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="com.hotel.backoffice.model.TransferAssignment" %>
<!DOCTYPE html>
<html>
<head>
    <title>Planification des transferts</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=2026030402">
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🏨</text></svg>">
</head>
<body>
    <header class="app-header">
        <div class="brand-wrap">
            <a class="brand" href="${pageContext.request.contextPath}/reservations">Hotel Transfer Ops</a>
            <span class="brand-tag">Back Office</span>
        </div>
        <nav class="nav">
            <a href="${pageContext.request.contextPath}/reservations" class="active">Réservations</a>
            <a href="${pageContext.request.contextPath}/reservations/new">Nouvelle réservation</a>
            <a href="${pageContext.request.contextPath}/vehicules">Véhicules</a>
        </nav>
    </header>

    <main class="container">
        <% String selectedDate = (String) request.getAttribute("selectedDate");
           Double vitesseMoyenne = (Double) request.getAttribute("vitesseMoyenne");
           Integer tempsAttenteMax = (Integer) request.getAttribute("tempsAttenteMax");
           Boolean bufferActif = (Boolean) request.getAttribute("bufferActif");
           Integer bufferMinutes = (Integer) request.getAttribute("bufferMinutes");
           Integer totalReservations = (Integer) request.getAttribute("totalReservations");
           Integer totalTrajets = (Integer) request.getAttribute("totalTrajets");
           Integer assignedCount = (Integer) request.getAttribute("assignedCount");
           Integer unassignedCount = (Integer) request.getAttribute("unassignedCount");
           List<TransferAssignment> assignations = (List<TransferAssignment>) request.getAttribute("assignations");
           List<TransferAssignment> nonAssignees = (List<TransferAssignment>) request.getAttribute("nonAssignees");

           if (assignations == null) assignations = new ArrayList<>();
           if (nonAssignees == null) nonAssignees = new ArrayList<>();
           if (totalReservations == null) totalReservations = assignations.size() + nonAssignees.size();
           if (totalTrajets == null) totalTrajets = 0;
           if (assignedCount == null) assignedCount = assignations.size();
           if (unassignedCount == null) unassignedCount = nonAssignees.size();

           String bufferTexte = (bufferActif != null && bufferActif)
               ? "Activé (" + (bufferMinutes != null ? bufferMinutes : 0) + " min)"
               : "Désactivé";

           DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        %>

        <section class="page-head">
            <div class="page-eyebrow">Sprint 4 - Mutualisation</div>
            <h1>Planification des transferts aéroport</h1>
            <p class="page-subtitle">Pilotage journalier des affectations véhicules avec regroupement des réservations par trajet.</p>
        </section>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error"><%= request.getAttribute("error") %></div>
        <% } %>
        <% if (request.getAttribute("success") != null) { %>
            <div class="alert alert-success"><%= request.getAttribute("success") %></div>
        <% } %>

        <section class="toolbar">
            <form class="filter-form" method="get" action="${pageContext.request.contextPath}/reservations">
                <div class="form-group">
                    <label for="date">Date des arrivées avion</label>
                    <input class="form-control" type="date" id="date" name="date" value="<%= selectedDate != null ? selectedDate : "" %>" required>
                </div>
                <button type="submit" class="btn btn-primary">Filtrer les trajets</button>
            </form>

            <div class="actions">
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/reservations/new">Ajouter une réservation</a>
                <a class="btn btn-secondary" href="${pageContext.request.contextPath}/vehicules">Gérer la flotte</a>
            </div>
        </section>

        <section class="kpi-grid">
            <article class="kpi-card kpi-card--accent">
                <div class="kpi-label">Date sélectionnée</div>
                <div class="kpi-value"><%= selectedDate != null ? selectedDate : "-" %></div>
                <div class="kpi-note">Base de calcul du planning</div>
            </article>
            <article class="kpi-card kpi-card--accent">
                <div class="kpi-label">Réservations</div>
                <div class="kpi-value"><%= totalReservations %></div>
                <div class="kpi-note">Total sur la date filtrée</div>
            </article>
            <article class="kpi-card kpi-card--ok">
                <div class="kpi-label">Trajets planifiés</div>
                <div class="kpi-value"><%= totalTrajets %></div>
                <div class="kpi-note">Lots de transport créés</div>
            </article>
            <article class="kpi-card <%= unassignedCount > 0 ? "kpi-card--danger" : "kpi-card--ok" %>">
                <div class="kpi-label">Non assignées</div>
                <div class="kpi-value"><%= unassignedCount %></div>
                <div class="kpi-note">Assignées: <%= assignedCount %></div>
            </article>
        </section>

        <div class="meta-strip">
            <span class="badge badge--muted">Vitesse moyenne: <%= vitesseMoyenne != null ? String.format(Locale.US, "%.1f", vitesseMoyenne) : "-" %> km/h</span>
            <span class="badge badge--muted">Temps attente max: <%= tempsAttenteMax != null ? tempsAttenteMax : "-" %> min</span>
            <span class="badge <%= (bufferActif != null && bufferActif) ? "badge--info" : "badge--warning" %>">Buffer départ: <%= bufferTexte %></span>
        </div>

        <section class="section-card">
            <div class="section-header">
                <h2>Réservations assignées</h2>
                <span class="section-summary"><%= assignedCount %> réservation(s) affectée(s)</span>
            </div>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>Réservation</th>
                            <th>Lot</th>
                            <th>Passagers</th>
                            <th>Parcours</th>
                            <th>Distance</th>
                            <th>Départ aéroport</th>
                            <th>Arrivée hôtel</th>
                            <th>Disponible à nouveau</th>
                            <th>Véhicule</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% if (!assignations.isEmpty()) {
                           for (TransferAssignment a : assignations) {
                               boolean mutualisee = a.getNbReservationsTrajet() != null && a.getNbReservationsTrajet() > 1;
                    %>
                        <tr class="<%= mutualisee ? "row-mutualized" : "" %>">
                            <td>
                                <span class="cell-main">#<%= a.getReservationId() %></span>
                                <span class="cell-sub">Client <%= a.getIdClient() %></span>
                            </td>
                            <td>
                                <span class="badge badge--info">T<%= a.getTrajetId() != null ? a.getTrajetId() : "-" %></span>
                                <span class="cell-sub">dépôt <%= a.getOrdreDepot() != null ? a.getOrdreDepot() : "-" %> / <%= a.getNbReservationsTrajet() != null ? a.getNbReservationsTrajet() : "-" %></span>
                            </td>
                            <td>
                                <span class="badge badge--accent"><%= a.getNbPassager() %> pax</span>
                            </td>
                            <td>
                                <span class="cell-main"><%= a.getAeroportCode() != null ? a.getAeroportCode() : "-" %> → <%= a.getHotelNom() != null ? a.getHotelNom() : ("Hotel #" + a.getIdHotel()) %></span>
                                <span class="cell-sub"><%= a.getHotelCode() != null ? a.getHotelCode() : "-" %></span>
                                <% if (mutualisee) { %>
                                    <span class="cell-sub">Mutualisé: <%= a.getNbReservationsTrajet() %> réservations / <%= a.getPassagersTrajet() %> passagers</span>
                                <% } %>
                            </td>
                            <td>
                                <span class="cell-main"><%= String.format(Locale.US, "%.1f", a.getDistanceKm()) %> km</span>
                                <span class="cell-sub"><%= a.getDureeTrajetMinutes() %> min</span>
                            </td>
                            <td><span class="cell-main"><%= a.getHeureDepartAeroport() != null ? a.getHeureDepartAeroport().format(dtf) : "-" %></span></td>
                            <td><span class="cell-main"><%= a.getHeureArriveeHotel() != null ? a.getHeureArriveeHotel().format(dtf) : "-" %></span></td>
                            <td><span class="cell-main"><%= a.getHeureDisponibleVehicule() != null ? a.getHeureDisponibleVehicule().format(dtf) : "-" %></span></td>
                            <td>
                                <span class="badge badge--success">#<%= a.getVehiculeId() %> <%= a.getVehiculeReference() %></span>
                                <span class="cell-sub"><%= a.getVehiculeNbPlace() %> places - <%= a.getVehiculeTypeCarburant() %></span>
                            </td>
                        </tr>
                    <%     }
                       } else { %>
                        <tr>
                            <td colspan="9">
                                <div class="empty-message">
                                    <span class="empty-icon">-</span>
                                    <div class="empty-title">Aucune réservation assignée</div>
                                    <div class="muted">Aucun véhicule n'a pu être affecté pour cette date.</div>
                                </div>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="section-card">
            <div class="section-header">
                <h2>Réservations non assignées</h2>
                <span class="section-summary"><%= unassignedCount %> réservation(s) en attente</span>
            </div>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>Réservation</th>
                            <th>Passagers</th>
                            <th>Hôtel</th>
                            <th>Arrivée avion</th>
                            <th>Motif</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% if (!nonAssignees.isEmpty()) {
                           for (TransferAssignment a : nonAssignees) { %>
                        <tr>
                            <td>
                                <span class="cell-main">#<%= a.getReservationId() %></span>
                                <span class="cell-sub">Client <%= a.getIdClient() %></span>
                            </td>
                            <td><span class="badge badge--warning"><%= a.getNbPassager() %> pax</span></td>
                            <td>
                                <span class="cell-main"><%= a.getHotelNom() != null ? a.getHotelNom() : ("Hotel #" + a.getIdHotel()) %></span>
                                <span class="cell-sub"><%= a.getHotelCode() != null ? a.getHotelCode() : "-" %></span>
                            </td>
                            <td><span class="cell-main"><%= a.getHeureArriveeHotel() != null ? a.getHeureArriveeHotel().format(dtf) : "-" %></span></td>
                            <td><span class="badge badge--danger"><%= a.getMotif() != null ? a.getMotif() : "-" %></span></td>
                        </tr>
                    <%     }
                       } else { %>
                        <tr>
                            <td colspan="5">
                                <div class="empty-message">
                                    <span class="empty-icon">OK</span>
                                    <div class="empty-title">Aucune réservation non assignée</div>
                                    <div class="muted">Toutes les réservations de la date sont affectées.</div>
                                </div>
                            </td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        </section>
    </main>
</body>
</html>
