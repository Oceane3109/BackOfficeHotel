<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page import="com.hotel.backoffice.model.Vehicule" %>
<!DOCTYPE html>
<html>
<head>
    <title>Formulaire véhicule</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css?v=2026030402">
    <link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🏨</text></svg>">
</head>
<body>
    <% Vehicule vehicule = (Vehicule) request.getAttribute("vehicule");
       boolean editing = vehicule != null;
       String typeValue = editing && vehicule.getTypeCarburant() != null ? vehicule.getTypeCarburant().trim() : "";
       String typeLower = typeValue.toLowerCase();
    %>

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
        <section class="page-head">
            <div class="page-eyebrow">Flotte</div>
            <h1><%= editing ? "Modifier un véhicule" : "Ajouter un véhicule" %></h1>
            <p class="page-subtitle"><%= editing ? "Mise à jour des informations de capacité et de carburant." : "Création d'un nouvel élément de flotte pour les affectations." %></p>
        </section>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error"><%= request.getAttribute("error") %></div>
        <% } %>

        <section class="form-panel">
            <form method="post" action="${pageContext.request.contextPath}/vehicules/save">
                <% if (editing) { %>
                    <input type="hidden" name="id" value="<%= vehicule.getId() %>">
                <% } %>

                <div class="form-grid">
                    <div class="form-group">
                        <label for="reference">Référence véhicule</label>
                        <input
                            type="text"
                            id="reference"
                            name="reference"
                            value="<%= editing ? vehicule.getReference() : "" %>"
                            required
                            placeholder="Ex: CAR-006-D"
                            class="form-control" />
                        <div class="field-hint">Identifiant visible dans les rapports d'assignation.</div>
                    </div>

                    <div class="form-group">
                        <label for="nb_place">Nombre de places</label>
                        <input
                            type="number"
                            id="nb_place"
                            name="nb_place"
                            min="1"
                            value="<%= editing ? vehicule.getNbPlace() : "" %>"
                            required
                            placeholder="Ex: 6"
                            class="form-control" />
                        <div class="field-hint">Capacité maximale autorisée pour la mutualisation.</div>
                    </div>

                    <div class="form-group form-group--full">
                        <label for="type_carburant">Type carburant</label>
                        <select id="type_carburant" name="type_carburant" required class="form-control">
                            <option value="">Sélectionner un type</option>
                            <option value="Diesel" <%= "diesel".equals(typeLower) ? "selected" : "" %>>Diesel</option>
                            <option value="Essence" <%= "essence".equals(typeLower) ? "selected" : "" %>>Essence</option>
                            <option value="Hybride" <%= "hybride".equals(typeLower) ? "selected" : "" %>>Hybride</option>
                            <option value="ES" <%= ("es".equals(typeLower) || "électrique".equals(typeLower) || "electrique".equals(typeLower)) ? "selected" : "" %>>ES / Électrique</option>
                        </select>
                        <div class="field-hint">Conserver les valeurs autorisées par l'ENUM SQL.</div>
                    </div>
                </div>

                <div class="actions" style="margin-top:14px;">
                    <button type="submit" class="btn btn-primary"><%= editing ? "Enregistrer les modifications" : "Ajouter le véhicule" %></button>
                    <a href="${pageContext.request.contextPath}/vehicules" class="btn btn-secondary">Annuler</a>
                </div>
            </form>
        </section>
    </main>
</body>
</html>
