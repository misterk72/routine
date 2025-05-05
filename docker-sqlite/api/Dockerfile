FROM php:8.1-apache

# Installer les extensions PHP nécessaires
RUN docker-php-ext-install pdo pdo_mysql

# Activer le module de réécriture d'URL
RUN a2enmod rewrite

# Configurer les permissions
RUN chown -R www-data:www-data /var/www/html
