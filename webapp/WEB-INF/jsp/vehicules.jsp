<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.time.LocalTime" %>
<%@ page import="com.hotel.backoffice.model.Vehicule" %>
<!DOCTYPE html>
<html>
<head>
    <title>Gestion des véhicules</title>
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
            <a href="${pageContext.request.contextPath}/reservations">Réservations</a>
            <a href="${pageContext.request.contextPath}/reservations/new">Nouvelle réservation</a>
            <a href="${pageContext.request.contextPath}/vehicules" class="active">Véhicules</a>
        </nav>
    </header>

    <main class="container">
        <% List<Vehicule> vehicules = (List<Vehicule>) request.getAttribute("vehicules");
           if (vehicules == null) vehicules = new ArrayList<>();

           int totalPlaces = 0;
           int nbDiesel = 0;
           int nbEssence = 0;
           int nbHybride = 0;
           int nbEs = 0;

           for (Vehicule v : vehicules) {
               totalPlaces += v.getNbPlace();
               String type = v.getTypeCarburant() != null ? v.getTypeCarburant().trim().toLowerCase() : "";
               if ("diesel".equals(type)) nbDiesel++;
               else if ("essence".equals(type)) nbEssence++;
               else if ("hybride".equals(type)) nbHybride++;
               else if ("es".equals(type) || "électrique".equals(type) || "electrique".equals(type)) nbEs++;
           }
        %>

        <section class="page-head">
            <div class="page-eyebrow">Flotte</div>
            <h1>Gestion des véhicules</h1>
            <p class="page-subtitle">Administration de la capacité, des carburants et des actions de maintenance des véhicules de transfert.</p>
        </section>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error"><%= request.getAttribute("error") %></div>
        <% } %>
        <% if (request.getAttribute("success") != null) { %>
            <div class="alert alert-success"><%= request.getAttribute("success") %></div>
        <% } %>

        <section class="toolbar">
            <div>
                <span class="badge badge--info">Total véhicules: <%= vehicules.size() %></span>
                <span class="badge badge--muted">Capacité cumulée: <%= totalPlaces %> places</span>
            </div>
            <div class="actions">
                <a href="${pageContext.request.contextPath}/vehicules/new" class="btn btn-primary">Ajouter un véhicule</a>
                <a href="${pageContext.request.contextPath}/reservations" class="btn btn-secondary">Retour aux assignations</a>
            </div>
        </section>

        <section class="kpi-grid">
            <article class="kpi-card kpi-card--accent">
                <div class="kpi-label">Diesel</div>
                <div class="kpi-value"><%= nbDiesel %></div>
                <div class="kpi-note">Véhicules prioritaires en cas d'égalité</div>
            </article>
            <article class="kpi-card kpi-card--accent">
                <div class="kpi-label">Essence</div>
                <div class="kpi-value"><%= nbEssence %></div>
                <div class="kpi-note">Flotte essence disponible</div>
            </article>
            <article class="kpi-card kpi-card--accent">
                <div class="kpi-label">Hybride</div>
                <div class="kpi-value"><%= nbHybride %></div>
                <div class="kpi-note">Option faible consommation</div>
            </article>
            <article class="kpi-card kpi-card--accent">
                <div class="kpi-label">ES / Électrique</div>
                <div class="kpi-value"><%= nbEs %></div>
                <div class="kpi-note">Type ES dans la base</div>
            </article>
        </section>

        <section class="section-card">
            <div class="section-header">
                <h2>Inventaire de la flotte</h2>
                <span class="section-summary"><%= vehicules.size() %> véhicule(s)</span>
            </div>
            <div class="table-wrapper">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Référence</th>
                            <th>Capacité</th>
                            <th>Carburant</th>
                            <th>Disponibilité défaut</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% if (!vehicules.isEmpty()) {
                           for (Vehicule v : vehicules) {
                               String typeRaw = v.getTypeCarburant();
                               String type = typeRaw != null ? typeRaw.trim() : "-";
                               String lower = type.toLowerCase();
                               String badgeClass = "badge--muted";
                               String typeLabel = type;
                               if ("diesel".equals(lower)) {
                                   badgeClass = "badge--muted";
                                   typeLabel = "Diesel";
                               } else if ("essence".equals(lower)) {
                                   badgeClass = "badge--warning";
                                   typeLabel = "Essence";
                               } else if ("hybride".equals(lower)) {
                                   badgeClass = "badge--success";
                                   typeLabel = "Hybride";
                               } else if ("es".equals(lower) || "électrique".equals(lower) || "electrique".equals(lower)) {
                                   badgeClass = "badge--info";
                                   typeLabel = "ES";
                               } else {
                                   badgeClass = "badge--danger";
                               }
                               LocalTime availabilityTime = v.getHeureDisponibiliteDefaut() != null
                                   ? v.getHeureDisponibiliteDefaut()
                                   : LocalTime.MIDNIGHT;
                    %>
                        <tr>
                            <td><span class="cell-main">#<%= v.getId() %></span></td>
                            <td>
                                <span class="cell-main"><%= v.getReference() %></span>
                            </td>
                            <td>
                                <span class="badge badge--accent"><%= v.getNbPlace() %> places</span>
                            </td>
                            <td><span class="badge <%= badgeClass %>"><%= typeLabel %></span></td>
                            <td>
                                <span class="badge badge--info"><%= String.format("%02d:%02d", availabilityTime.getHour(), availabilityTime.getMinute()) %></span>
                            </td>
                            <td>
                                <div class="actions">
                                    <a href="${pageContext.request.contextPath}/vehicules/edit?id=<%= v.getId() %>" class="btn btn-secondary">Modifier</a>
                                    <form method="post" action="${pageContext.request.contextPath}/vehicules/delete" style="display:inline;">
                                        <input type="hidden" name="id" value="<%= v.getId() %>">
                                        <button type="submit" class="btn btn-danger" onclick="return confirm('Supprimer ce véhicule ?');">Supprimer</button>
                                    </form>
                                </div>
                            </td>
                        </tr>
                    <%     }
                       } else { %>
                        <tr>
                            <td colspan="6">
                                <div class="empty-message">
                                    <span class="empty-icon">-</span>
                                    <div class="empty-title">Aucun véhicule enregistré</div>
                                    <div class="muted">Ajoutez un véhicule pour démarrer la planification des transferts.</div>
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
