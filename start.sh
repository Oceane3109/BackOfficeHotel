
#!/bin/bash
# Script de démarrage BackOffice

# 1. Variables d'environnement DB
export DB_URL="jdbc:mysql://localhost:3306/hotel"
export DB_USER="root"
export DB_PASSWORD=""

# 2. Chemin Tomcat (modifiable via variable d'environnement TOMCAT_HOME)
TOMCAT_HOME="${TOMCAT_HOME:-/Users/oceanechristodoulou/Desktop/S3/apache-tomcat-10.1.28}"
TOMCAT_WEBAPPS="$TOMCAT_HOME/webapps"

if [ ! -f "$TOMCAT_HOME/bin/startup.sh" ]; then
  echo "❌ Tomcat introuvable: $TOMCAT_HOME"
  echo "👉 Définis TOMCAT_HOME puis relance, ex:"
  echo "   export TOMCAT_HOME=/chemin/vers/apache-tomcat-10.1.28"
  exit 1
fi

# 3. Build WAR
echo "📦 Building BackOffice..."
mvn clean package

# 4. Copier le WAR dans Tomcat
echo "🚀 Déploiement vers Tomcat..."
cp target/backoffice8.war "$TOMCAT_WEBAPPS/"

echo "✅ WAR déployé. Tomcat va le déployer automatiquement."
echo "📍 URL: http://localhost:8080/backoffice8"


-raha misy vehicule tafaverina ka mbola misy resa non assignées de:
	-1) fenoiny ny place-ny, raha feno tanteraka izy de tonga de mandeha
	amin'ilay lera nahatongavany
	-2) raha tsy feno tantareka izy de lasa micréer regroupement
	vaovao à partir anle lera nahatongavany iny de alainy daholo izay
	resa ao anatin'iny regroupement iny
exemple:
cas1:
	-vehicule tafaverina tam 14:00, 8 places
	-resa CL1 (4), CL2 (4) non assignées daholo
	-tonga de mandeha izy satria alaina daholo reo de feno izy
cas2:
	-io vehicule io ihany tam 16:00 fa resa CL3(2) de CL4 (4)
	-tsy feno ilay vehicule, manamboatra regroupement vaovao izy
	à partir de 16:00 + temps attente 
	-andramany atao daol ilay resa ao anatin'io regroupement io
	-de aveo mitohy ihany ny regle de gestion, ny heure départ dia heure
	d'arrivee anle resa farany parmi anle trajet tao anatin'io regroupement io etc.
 
 