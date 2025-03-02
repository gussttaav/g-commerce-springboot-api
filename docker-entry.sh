#!/bin/sh

# Ensure log directories exist with proper permissions
mkdir -p ${LOG_PATH}
mkdir -p ${LOG_ARCHIVE}
chmod -R 755 ${LOG_PATH}

# Set the correct profile dynamically
if [ "$SPRING_PROFILES_ACTIVE" = "https" ] && [ -f "/certs/keystore.p12" ]; then
  echo "SSL certificate found. Running in HTTPS mode..."
  java -jar app.jar \
    --spring.profiles.active=https \
    --server.ssl.key-store=file:/certs/keystore.p12 \
    --server.ssl.key-store-password=$SSL_PASSWORD \
    --logging.file.path=${LOG_PATH} \
    --logging.file.name=${LOG_PATH}/g-commerce-api.log
else
  echo "No SSL certificate found. Running in HTTP mode..."
  java -jar app.jar \
    --spring.profiles.active=http \
    --logging.file.path=${LOG_PATH} \
    --logging.file.name=${LOG_PATH}/g-commerce-api.log
fi