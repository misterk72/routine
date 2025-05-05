-- Schéma de base de données pour la synchronisation avec l'application HealthTracker

-- Utilisateurs
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    client_id BIGINT,  -- ID côté client
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Entrées de santé
CREATE TABLE health_entries (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    timestamp DATETIME NOT NULL,
    weight FLOAT,
    waist_measurement FLOAT,
    body_fat FLOAT,
    notes TEXT,
    client_id BIGINT,  -- ID côté client
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Types de métriques
CREATE TABLE metric_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    value_type VARCHAR(50) NOT NULL,
    client_id BIGINT,  -- ID côté client
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Valeurs de métriques
CREATE TABLE metric_values (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    metric_type_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    timestamp DATETIME NOT NULL,
    value_numeric FLOAT,
    value_text TEXT,
    client_id BIGINT,  -- ID côté client
    last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (metric_type_id) REFERENCES metric_types(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indices pour améliorer les performances
CREATE INDEX idx_health_entries_user_id ON health_entries(user_id);
CREATE INDEX idx_health_entries_timestamp ON health_entries(timestamp);
CREATE INDEX idx_health_entries_client_id ON health_entries(client_id);
CREATE INDEX idx_metric_values_metric_type_id ON metric_values(metric_type_id);
CREATE INDEX idx_metric_values_user_id ON metric_values(user_id);
CREATE INDEX idx_metric_values_timestamp ON metric_values(timestamp);
CREATE INDEX idx_metric_values_client_id ON metric_values(client_id);
CREATE INDEX idx_users_client_id ON users(client_id);
CREATE INDEX idx_metric_types_client_id ON metric_types(client_id);

-- Insérer un utilisateur par défaut
INSERT INTO users (name, is_default) VALUES ('Utilisateur par défaut', TRUE);
