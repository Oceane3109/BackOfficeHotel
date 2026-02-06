<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>Nouvelle réservation</title>
</head>
<body>
<h1>Insertion de réservation</h1>

<% if (request.getAttribute("error") != null) { %>
    <p style="color:red;"><%= request.getAttribute("error") %></p>
<% } %>
<% if (request.getAttribute("success") != null) { %>
    <p style="color:green;"><%= request.getAttribute("success") %></p>
<% } %>

<form method="post" action="/reservations/new">
    <label>id_client (4 caractères)</label><br/>
    <input type="text" name="id_client" maxlength="4" required /><br/><br/>

    <label>nb_passager</label><br/>
    <input type="number" name="nb_passager" min="1" required /><br/><br/>

    <label>date_heure_arrive (format: 2026-02-06T15:30)</label><br/>
    <input type="text" name="date_heure_arrive" required /><br/><br/>

    <label>id_hotel</label><br/>
    <input type="number" name="id_hotel" min="1" required /><br/><br/>

    <button type="submit">Enregistrer</button>
</form>
</body>
</html>
