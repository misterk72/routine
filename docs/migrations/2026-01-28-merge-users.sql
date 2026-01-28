-- Merge user_profiles into users (run manually; adjust constraint names if needed).
-- Backup DB before running.

START TRANSACTION;

-- 1) Ensure users table has required columns (skip if already present).
-- ALTER TABLE users ADD COLUMN alias VARCHAR(255) DEFAULT NULL;
-- ALTER TABLE users ADD COLUMN client_id BIGINT DEFAULT NULL;
-- ALTER TABLE users ADD COLUMN is_default BOOLEAN DEFAULT 0;

-- 2) Copy user_profiles into users (preserve IDs where possible).
INSERT INTO users (id, name, alias, is_default)
SELECT up.id, up.name, up.alias, 0
FROM user_profiles up
LEFT JOIN users u ON u.id = up.id
WHERE u.id IS NULL;

-- 3) Migrate foreign keys to users.
-- user_source_map
ALTER TABLE user_source_map ADD COLUMN user_id BIGINT;
UPDATE user_source_map SET user_id = user_profile_id;

-- workouts / weight_measurements / gadgetbridge_samples
ALTER TABLE workouts ADD COLUMN user_id BIGINT;
UPDATE workouts SET user_id = user_profile_id;

ALTER TABLE weight_measurements ADD COLUMN user_id BIGINT;
UPDATE weight_measurements SET user_id = user_profile_id;

ALTER TABLE gadgetbridge_samples ADD COLUMN user_id BIGINT;
UPDATE gadgetbridge_samples SET user_id = user_profile_id;

-- 4) Drop old FKs/columns (adjust constraint names).
-- ALTER TABLE user_source_map DROP FOREIGN KEY fk_user_source_map_profile;
ALTER TABLE user_source_map DROP COLUMN user_profile_id;
ALTER TABLE user_source_map ADD CONSTRAINT fk_user_source_map_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ALTER TABLE workouts DROP FOREIGN KEY fk_workouts_profile;
ALTER TABLE workouts DROP COLUMN user_profile_id;
ALTER TABLE workouts ADD CONSTRAINT fk_workouts_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ALTER TABLE weight_measurements DROP FOREIGN KEY fk_weight_profile;
ALTER TABLE weight_measurements DROP COLUMN user_profile_id;
ALTER TABLE weight_measurements ADD CONSTRAINT fk_weight_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- ALTER TABLE gadgetbridge_samples DROP FOREIGN KEY fk_gb_profile;
ALTER TABLE gadgetbridge_samples DROP COLUMN user_profile_id;
ALTER TABLE gadgetbridge_samples ADD CONSTRAINT fk_gb_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 5) Drop user_profiles table once all references are removed.
DROP TABLE user_profiles;

COMMIT;
