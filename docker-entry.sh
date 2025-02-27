#!/bin/sh

# Set the correct profile dynamically
if [ "$SPRING_PROFILES_ACTIVE" = "https" ] && [ -f "/certs/keystore.p12" ]; then
  echo "SSL certificate found. Running in HTTPS mode..."
  java -jar app.jar --spring.profiles.active=https --server.ssl.key-store=file:/certs/keystore.p12 --server.ssl.key-store-password=$SSL_PASSWORD
else
  echo "No SSL certificate found. Running in HTTP mode..."
  java -jar app.jar --spring.profiles.active=http
fi