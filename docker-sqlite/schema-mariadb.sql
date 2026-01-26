-- MariaDB schema for data centralization

CREATE TABLE IF NOT EXISTS sources (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_sources_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    alias VARCHAR(128) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_source_map (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_id BIGINT NOT NULL,
    source_user_id VARCHAR(128) DEFAULT NULL,
    device_id VARCHAR(128) DEFAULT NULL,
    user_profile_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_source_user_device (source_id, source_user_id, device_id),
    KEY idx_user_source_map_profile (user_profile_id),
    CONSTRAINT fk_user_source_map_source
        FOREIGN KEY (source_id) REFERENCES sources(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_user_source_map_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS workouts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_profile_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    source_uid VARCHAR(128) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME DEFAULT NULL,
    duration_minutes INT DEFAULT NULL,
    program VARCHAR(128) DEFAULT NULL,
    distance_km DECIMAL(8,3) DEFAULT NULL,
    avg_speed_kmh DECIMAL(6,2) DEFAULT NULL,
    calories INT DEFAULT NULL,
    calories_per_km DECIMAL(6,2) DEFAULT NULL,
    avg_heart_rate INT DEFAULT NULL,
    min_heart_rate INT DEFAULT NULL,
    max_heart_rate INT DEFAULT NULL,
    sleep_heart_rate_avg INT DEFAULT NULL,
    vo2_max DECIMAL(6,2) DEFAULT NULL,
    soundtrack TEXT DEFAULT NULL,
    notes TEXT DEFAULT NULL,
    raw_json JSON DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_workouts_source_uid (source_id, source_uid),
    KEY idx_workouts_start_time (start_time),
    KEY idx_workouts_profile (user_profile_id),
    CONSTRAINT fk_workouts_source
        FOREIGN KEY (source_id) REFERENCES sources(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_workouts_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS weight_measurements (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_profile_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    source_uid VARCHAR(128) NOT NULL,
    measured_at DATETIME NOT NULL,
    weight_kg DECIMAL(5,2) DEFAULT NULL,
    fat_mass_kg DECIMAL(5,2) DEFAULT NULL,
    fat_percentage DECIMAL(5,2) DEFAULT NULL,
    waist_cm DECIMAL(5,2) DEFAULT NULL,
    notes TEXT DEFAULT NULL,
    raw_json JSON DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_weight_source_uid (source_id, source_uid),
    KEY idx_weight_measured_at (measured_at),
    KEY idx_weight_profile (user_profile_id),
    CONSTRAINT fk_weight_source
        FOREIGN KEY (source_id) REFERENCES sources(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_weight_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS gadgetbridge_samples (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_profile_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    sample_time DATETIME NOT NULL,
    heart_rate INT DEFAULT NULL,
    steps INT DEFAULT NULL,
    raw_json JSON DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uniq_gb_sample (source_id, device_id, sample_time),
    KEY idx_gb_sample_time (sample_time),
    KEY idx_gb_device (device_id),
    CONSTRAINT fk_gb_source
        FOREIGN KEY (source_id) REFERENCES sources(id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_gb_profile
        FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
