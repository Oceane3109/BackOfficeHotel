<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Nouvelle réservation</title>
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
            <a href="${pageContext.request.contextPath}/reservations/new" class="active">Nouvelle réservation</a>
            <a href="${pageContext.request.contextPath}/vehicules">Véhicules</a>
        </nav>
    </header>

    <main class="container">
        <section class="page-head">
            <div class="page-eyebrow">Saisie opérationnelle</div>
            <h1>Créer une réservation</h1>
            <p class="page-subtitle">Enregistrement d'une arrivée client pour l'affectation automatique des véhicules.</p>
        </section>

        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error"><%= request.getAttribute("error") %></div>
        <% } %>
        <% if (request.getAttribute("success") != null) { %>
            <div class="alert alert-success"><%= request.getAttribute("success") %></div>
        <% } %>

        <section class="form-panel">
            <form method="post" action="${pageContext.request.contextPath}/reservations/new">
                <div class="form-grid">
                    <div class="form-group">
                        <label for="id_client">ID client</label>
                        <input type="text" id="id_client" name="id_client" maxlength="4" required placeholder="Ex: 1101" class="form-control" />
                        <div class="field-hint">Format attendu: 4 caractères.</div>
                    </div>

                    <div class="form-group">
                        <label for="nb_passager">Nombre de passagers</label>
                        <input type="number" id="nb_passager" name="nb_passager" min="1" required placeholder="Ex: 2" class="form-control" />
                        <div class="field-hint">Tous les passagers d'une même réservation restent ensemble.</div>
                    </div>

                    <div class="form-group">
                        <label for="date_heure_arrive">Date et heure d'arrivée avion</label>
                        <input type="datetime-local" id="date_heure_arrive" name="date_heure_arrive" required class="form-control" />
                        <div class="field-hint">Cette heure sert de base pour calculer le départ aéroport.</div>
                    </div>

                    <div class="form-group">
                        <label for="id_hotel">ID hôtel de destination</label>
                        <input type="number" id="id_hotel" name="id_hotel" min="1" required placeholder="Ex: 1" class="form-control" />
                        <div class="field-hint">Exemple dataset reset: 1 Colbert, 2 Novotel, 3 Ibis, 4 Lokanga, 5 Beach Resort.</div>
                    </div>
                </div>

                <div class="actions" style="margin-top:14px;">
                    <button type="submit" class="btn btn-primary">Enregistrer la réservation</button>
                    <a href="${pageContext.request.contextPath}/reservations" class="btn btn-secondary">Voir les assignations</a>
                    <a href="${pageContext.request.contextPath}/reservations/new" class="btn btn-secondary">Nouveau formulaire</a>
                </div>
            </form>
        </section>

        <div class="page-links">
            <a href="${pageContext.request.contextPath}/vehicules" class="btn btn-secondary">Ouvrir la gestion des véhicules</a>
        </div>
    </main>
</body>
</html>
