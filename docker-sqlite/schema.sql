CREATE TABLE IF NOT EXISTS workouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date_time DATETIME NOT NULL,
    program TEXT,
    duration_minutes INTEGER,
    average_speed REAL,
    distance_km REAL,
    calories INTEGER,
    calories_per_km REAL,
    average_heart_rate INTEGER,
    max_heart_rate INTEGER,
    min_heart_rate INTEGER,
    weight_kg REAL,
    fat_mass_kg REAL,
    fat_percentage REAL,
    waist_circumference_cm REAL,
    background_music TEXT,
    observations TEXT,
    custom_data JSON,  -- For storing additional structured data
    tags TEXT,  -- For comma-separated tags
    notes TEXT,  -- For free-form notes
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER IF NOT EXISTS update_workouts_timestamp
AFTER UPDATE ON workouts
BEGIN
    UPDATE workouts SET updated_at = CURRENT_TIMESTAMP WHERE id = NEW.id;
END;

-- Create indexes for better query performance
CREATE INDEX idx_workouts_date_time ON workouts(date_time);
CREATE INDEX idx_workouts_program ON workouts(program);
CREATE INDEX idx_workouts_tags ON workouts(tags);
