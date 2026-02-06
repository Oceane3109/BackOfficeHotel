
#!/bin/bash
# Script de démarrage BackOffice

# 1. Variables d'environnement DB
export DB_URL="jdbc:mysql://localhost:3306/hotel"
export DB_USER="root"
export DB_PASSWORD=""

# 2. Build WAR
echo "📦 Building BackOffice..."
mvn clean package

# 3. Copier le WAR dans Tomcat
echo "🚀 Déploiement vers Tomcat..."
cp target/backofficehotel.war /Users/oceanechristodoulou/Desktop/S3/apache-tomcat-10.1.28/webapps/

echo "✅ WAR déployé. Tomcat va le déployer automatiquement."
echo "📍 URL: http://localhost:8080/backofficehotel"
