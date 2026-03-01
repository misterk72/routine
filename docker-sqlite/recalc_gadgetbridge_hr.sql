-- Recalculate Gadgetbridge workout HR stats from gadgetbridge_samples
-- Excludes invalid values (0, 255) and only updates workouts with matching samples.

UPDATE workouts w
JOIN (
  SELECT
    w.id AS workout_id,
    CAST(AVG(s.heart_rate) AS UNSIGNED) AS avg_hr,
    MIN(s.heart_rate) AS min_hr,
    MAX(s.heart_rate) AS max_hr
  FROM workouts w
  JOIN gadgetbridge_samples s
    ON s.user_id = w.user_id
   AND s.sample_time >= w.start_time
   AND s.sample_time <= COALESCE(w.end_time, DATE_ADD(w.start_time, INTERVAL w.duration_minutes MINUTE))
  WHERE w.source_id = 3
    AND s.heart_rate BETWEEN 1 AND 254
  GROUP BY w.id
) stats ON stats.workout_id = w.id
SET
  w.avg_heart_rate = stats.avg_hr,
  w.min_heart_rate = stats.min_hr,
  w.max_heart_rate = stats.max_hr;
