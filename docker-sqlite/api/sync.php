<?php
// Supprimer les avertissements PHP pour éviter de corrompre la réponse JSON
error_reporting(0);
header('Content-Type: application/json');

// Configuration de la base de données
$host = 'db'; // Nom du service dans docker-compose
$dbname = 'healthtracker';
$username = 'healthuser';
$password = 'healthpassword';

// Fonction pour se connecter à la base de données
function connectDB() {
    global $host, $dbname, $username, $password;
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        return $pdo;
    } catch (PDOException $e) {
        die(json_encode(['error' => 'Erreur de connexion: ' . $e->getMessage()]));
    }
}

// Récupérer la méthode HTTP
$method = $_SERVER['REQUEST_METHOD'];

// Traiter la requête en fonction de la méthode
switch ($method) {
    case 'POST':
        // Récupérer les données JSON envoyées
        $json = file_get_contents('php://input');
        $data = json_decode($json, true);
        
        if (!$data) {
            echo json_encode(['error' => 'Données JSON invalides']);
            exit;
        }
        
        // Se connecter à la base de données
        $pdo = connectDB();
        
        // Vérifier si les tables existent, sinon les créer
        createTablesIfNotExist($pdo);
        
        $response = [];

        // Traiter les entrées de santé
        if (isset($data['entries']) && is_array($data['entries'])) {
            error_log("DEBUG - Nombre d'entrées reçues: " . count($data['entries']));
            $result = processEntries($pdo, $data['entries']);
            $response['entries'] = $result;
        }

        // Traiter les séances
        if (isset($data['workouts']) && is_array($data['workouts'])) {
            error_log("DEBUG - Nombre de séances reçues: " . count($data['workouts']));
            $result = processWorkouts($pdo, $data['workouts']);
            $response['workouts'] = $result;
        }

        if (empty($response)) {
            error_log("DEBUG - Format de données invalide");
            echo json_encode(['error' => 'Format de données invalide']);
        } else {
            error_log("DEBUG - Résultat du traitement: " . json_encode($response));
            echo json_encode($response);
        }
        break;
        
    case 'GET':
        // Récupérer les entrées plus récentes qu'un timestamp donné
        $timestamp = isset($_GET['since']) ? $_GET['since'] : 0;
        
        // Se connecter à la base de données
        $pdo = connectDB();
        
        // Vérifier si les tables existent, sinon les créer
        createTablesIfNotExist($pdo);
        
        // Récupérer les entrées
        $entries = getEntriesSince($pdo, $timestamp);
        $workouts = getWorkoutsSince($pdo, $timestamp);
        echo json_encode(['entries' => $entries, 'workouts' => $workouts]);
        break;
        
    default:
        echo json_encode(['error' => 'Méthode non supportée']);
        break;
}

// Fonction pour créer les tables si elles n'existent pas
function createTablesIfNotExist($pdo) {
    // Créer la table des utilisateurs si elle n'existe pas
    $pdo->exec("CREATE TABLE IF NOT EXISTS users (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(255) NOT NULL,
        is_default BOOLEAN DEFAULT 0,
        last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )");
    
    // Créer la table des entrées de santé si elle n'existe pas
    $pdo->exec("CREATE TABLE IF NOT EXISTS health_entries (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        timestamp DATETIME NOT NULL,
        weight FLOAT,
        waist_measurement FLOAT,
        body_fat FLOAT,
        notes TEXT,
        client_id BIGINT,
        deleted BOOLEAN DEFAULT 0,
        last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        UNIQUE KEY (client_id)
    )");

    // Créer la table des séances si elle n'existe pas
    $pdo->exec("CREATE TABLE IF NOT EXISTS workout_entries (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        start_time DATETIME NOT NULL,
        duration_minutes INTEGER,
        distance_km FLOAT,
        avg_speed_kmh FLOAT,
        calories INTEGER,
        calories_per_km FLOAT,
        avg_heart_rate INTEGER,
        min_heart_rate INTEGER,
        max_heart_rate INTEGER,
        sleep_heart_rate_avg INTEGER,
        vo2_max FLOAT,
        program TEXT,
        notes TEXT,
        client_id BIGINT,
        deleted BOOLEAN DEFAULT 0,
        last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
        UNIQUE KEY (client_id)
    )");
    
    // Vérifier si la colonne 'deleted' existe dans la table health_entries
    $tableInfo = $pdo->query("DESCRIBE health_entries");
    $columns = $tableInfo->fetchAll(PDO::FETCH_COLUMN);
    
    // Si la colonne 'deleted' n'existe pas, l'ajouter
    if (!in_array('deleted', $columns)) {
        error_log("Ajout de la colonne 'deleted' à la table health_entries");
        $pdo->exec("ALTER TABLE health_entries ADD COLUMN deleted BOOLEAN DEFAULT 0");
    }

    $workoutInfo = $pdo->query("DESCRIBE workout_entries");
    $workoutColumns = $workoutInfo->fetchAll(PDO::FETCH_COLUMN);
    if (!in_array('deleted', $workoutColumns)) {
        error_log("Ajout de la colonne 'deleted' à la table workout_entries");
        $pdo->exec("ALTER TABLE workout_entries ADD COLUMN deleted BOOLEAN DEFAULT 0");
    }
    $workoutColumnDefs = [
        'avg_speed_kmh' => 'FLOAT',
        'calories_per_km' => 'FLOAT',
        'avg_heart_rate' => 'INTEGER',
        'min_heart_rate' => 'INTEGER',
        'max_heart_rate' => 'INTEGER',
        'sleep_heart_rate_avg' => 'INTEGER',
        'vo2_max' => 'FLOAT',
    ];
    foreach ($workoutColumnDefs as $column => $definition) {
        if (!in_array($column, $workoutColumns)) {
            error_log("Ajout de la colonne '$column' a la table workout_entries");
            $pdo->exec("ALTER TABLE workout_entries ADD COLUMN $column $definition");
        }
    }
    
    // Vérifier s'il y a un utilisateur par défaut, sinon le créer
    $stmt = $pdo->query("SELECT COUNT(*) FROM users WHERE is_default = 1");
    if ($stmt->fetchColumn() == 0) {
        $pdo->exec("INSERT INTO users (name, is_default) VALUES ('Utilisateur par défaut', 1)");
    }
}

// Fonction pour traiter les entrées de santé
function processEntries($pdo, $entries) {
    $processed = 0;
    $errors = [];
    
    error_log("DEBUG - processEntries: Traitement de " . count($entries) . " entrées");
    
    // Traiter toutes les entrées (normales et supprimées)
    foreach ($entries as $entry) {
        try {
            error_log("DEBUG - Traitement de l'entrée: " . json_encode($entry));
            
            // Vérifier si l'utilisateur existe
            $userId = ensureUserExists($pdo, $entry['userId']);
            error_log("DEBUG - Utilisateur ID: $userId");
            
            // Vérifier si l'entrée est marquée comme supprimée
            $deleted = isset($entry['deleted']) && $entry['deleted'] ? 1 : 0;
            error_log("DEBUG - Entrée supprimée: $deleted");
            
            // Vérifier d'abord si une entrée avec ce client_id existe déjà
            $checkStmt = $pdo->prepare("SELECT id FROM health_entries WHERE client_id = ?");
            $checkStmt->execute([$entry['id']]);
            $existingEntry = $checkStmt->fetch(PDO::FETCH_ASSOC);
            
            if ($existingEntry) {
                // Mise à jour d'une entrée existante
                error_log("DEBUG - Mise à jour de l'entrée existante avec client_id = {$entry['id']}");
                $stmt = $pdo->prepare("UPDATE health_entries 
                    SET user_id = ?, timestamp = ?, weight = ?, waist_measurement = ?, 
                    body_fat = ?, notes = ?, deleted = ?
                    WHERE client_id = ?");
                
                $stmt->execute([
                    $userId,
                    $entry['timestamp'],
                    $entry['weight'],
                    $entry['waistMeasurement'],
                    $entry['bodyFat'],
                    $entry['notes'],
                    $deleted,
                    $entry['id']
                ]);
            } else {
                // Insertion d'une nouvelle entrée
                error_log("DEBUG - Insertion d'une nouvelle entrée avec client_id = {$entry['id']}");
                $stmt = $pdo->prepare("INSERT INTO health_entries 
                    (user_id, timestamp, weight, waist_measurement, body_fat, notes, client_id, deleted)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                
                $stmt->execute([
                    $userId,
                    $entry['timestamp'],
                    $entry['weight'],
                    $entry['waistMeasurement'],
                    $entry['bodyFat'],
                    $entry['notes'],
                    $entry['id'],
                    $deleted
                ]);
            }
            
            $processed++;
        } catch (Exception $e) {
            $errors[] = [
                'entry' => $entry,
                'error' => $e->getMessage()
            ];
        }
    }
    
    return [
        'success' => true,
        'processed' => $processed,
        'errors' => $errors
    ];
}

// Fonction pour traiter les séances
function processWorkouts($pdo, $workouts) {
    $processed = 0;
    $errors = [];

    foreach ($workouts as $workout) {
        try {
            $userId = ensureUserExists($pdo, $workout['userId']);
            $deleted = isset($workout['deleted']) && $workout['deleted'] ? 1 : 0;

            $checkStmt = $pdo->prepare("SELECT id FROM workout_entries WHERE client_id = ?");
            $checkStmt->execute([$workout['id']]);
            $existingEntry = $checkStmt->fetch(PDO::FETCH_ASSOC);

            if ($existingEntry) {
                $stmt = $pdo->prepare("UPDATE workout_entries
                    SET user_id = ?, start_time = ?, duration_minutes = ?, distance_km = ?, avg_speed_kmh = ?,
                    calories = ?, calories_per_km = ?, avg_heart_rate = ?, min_heart_rate = ?,
                    max_heart_rate = ?, sleep_heart_rate_avg = ?, vo2_max = ?, program = ?, notes = ?, deleted = ?
                    WHERE client_id = ?");
                $stmt->execute([
                    $userId,
                    $workout['startTime'],
                    $workout['durationMinutes'],
                    $workout['distanceKm'],
                    $workout['avgSpeedKmh'] ?? null,
                    $workout['calories'],
                    $workout['caloriesPerKm'] ?? null,
                    $workout['avgHeartRate'] ?? null,
                    $workout['minHeartRate'] ?? null,
                    $workout['maxHeartRate'] ?? null,
                    $workout['sleepHeartRateAvg'] ?? null,
                    $workout['vo2Max'] ?? null,
                    $workout['program'],
                    $workout['notes'],
                    $deleted,
                    $workout['id']
                ]);
            } else {
                $stmt = $pdo->prepare("INSERT INTO workout_entries
                    (user_id, start_time, duration_minutes, distance_km, avg_speed_kmh, calories,
                    calories_per_km, avg_heart_rate, min_heart_rate, max_heart_rate,
                    sleep_heart_rate_avg, vo2_max, program, notes, client_id, deleted)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                $stmt->execute([
                    $userId,
                    $workout['startTime'],
                    $workout['durationMinutes'],
                    $workout['distanceKm'],
                    $workout['avgSpeedKmh'] ?? null,
                    $workout['calories'],
                    $workout['caloriesPerKm'] ?? null,
                    $workout['avgHeartRate'] ?? null,
                    $workout['minHeartRate'] ?? null,
                    $workout['maxHeartRate'] ?? null,
                    $workout['sleepHeartRateAvg'] ?? null,
                    $workout['vo2Max'] ?? null,
                    $workout['program'],
                    $workout['notes'],
                    $workout['id'],
                    $deleted
                ]);
            }

            $processed++;
        } catch (Exception $e) {
            $errors[] = [
                'workout' => $workout,
                'error' => $e->getMessage()
            ];
        }
    }

    return [
        'success' => true,
        'processed' => $processed,
        'errors' => $errors
    ];
}

// Fonction pour s'assurer qu'un utilisateur existe
function ensureUserExists($pdo, $userId) {
    // Vérifier si l'utilisateur existe
    $stmt = $pdo->prepare("SELECT id FROM users WHERE client_id = ?");
    $stmt->execute([$userId]);
    $result = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if ($result) {
        return $result['id'];
    }
    
    // Si l'utilisateur n'existe pas, utiliser l'utilisateur par défaut
    $stmt = $pdo->query("SELECT id FROM users WHERE is_default = 1");
    return $stmt->fetchColumn();
}

// Fonction pour récupérer les entrées plus récentes qu'un timestamp donné
function getEntriesSince($pdo, $timestamp) {
    $date = date('Y-m-d H:i:s', $timestamp / 1000);
    
    // Récupérer toutes les entrées qui ont été modifiées après le timestamp
    // et qui ne sont pas marquées comme supprimées
    // NOTE: On récupère maintenant toutes les entrées, qu'elles aient un client_id ou non
    $stmt = $pdo->prepare("SELECT 
        e.id, e.user_id, e.timestamp, e.weight, e.waist_measurement, e.body_fat, e.notes, e.client_id, e.deleted,
        u.name as user_name
        FROM health_entries e
        JOIN users u ON e.user_id = u.id
        WHERE e.last_modified > ? AND (e.deleted = 0)");
    $stmt->execute([$date]);
    
    $entries = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $entries[] = [
            'id' => (int)$row['id'],
            'userId' => (int)$row['user_id'],
            'userName' => $row['user_name'],
            'timestamp' => $row['timestamp'],
            'weight' => $row['weight'] ? (float)$row['weight'] : null,
            'waistMeasurement' => $row['waist_measurement'] ? (float)$row['waist_measurement'] : null,
            'bodyFat' => $row['body_fat'] ? (float)$row['body_fat'] : null,
            'notes' => $row['notes'],
            'clientId' => $row['client_id'] ? (int)$row['client_id'] : null
        ];
    }
    
    return $entries;
}

// Fonction pour récupérer les séances plus récentes qu'un timestamp donné
function getWorkoutsSince($pdo, $timestamp) {
    $date = date('Y-m-d H:i:s', $timestamp / 1000);
    $stmt = $pdo->prepare("SELECT
        w.id, w.user_id, w.start_time, w.duration_minutes, w.distance_km, w.avg_speed_kmh,
        w.calories, w.calories_per_km, w.avg_heart_rate, w.min_heart_rate, w.max_heart_rate,
        w.sleep_heart_rate_avg, w.vo2_max, w.program, w.notes,
        w.client_id, w.deleted,
        u.name as user_name
        FROM workout_entries w
        JOIN users u ON w.user_id = u.id
        WHERE w.last_modified > ? AND (w.deleted = 0)");
    $stmt->execute([$date]);

    $workouts = [];
    while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
        $workouts[] = [
            'id' => (int)$row['id'],
            'userId' => (int)$row['user_id'],
            'userName' => $row['user_name'],
            'startTime' => $row['start_time'],
            'durationMinutes' => $row['duration_minutes'] !== null ? (int)$row['duration_minutes'] : null,
            'distanceKm' => $row['distance_km'] !== null ? (float)$row['distance_km'] : null,
            'avgSpeedKmh' => $row['avg_speed_kmh'] !== null ? (float)$row['avg_speed_kmh'] : null,
            'calories' => $row['calories'] !== null ? (int)$row['calories'] : null,
            'caloriesPerKm' => $row['calories_per_km'] !== null ? (float)$row['calories_per_km'] : null,
            'avgHeartRate' => $row['avg_heart_rate'] !== null ? (int)$row['avg_heart_rate'] : null,
            'minHeartRate' => $row['min_heart_rate'] !== null ? (int)$row['min_heart_rate'] : null,
            'maxHeartRate' => $row['max_heart_rate'] !== null ? (int)$row['max_heart_rate'] : null,
            'sleepHeartRateAvg' => $row['sleep_heart_rate_avg'] !== null ? (int)$row['sleep_heart_rate_avg'] : null,
            'vo2Max' => $row['vo2_max'] !== null ? (float)$row['vo2_max'] : null,
            'program' => $row['program'],
            'notes' => $row['notes'],
            'clientId' => $row['client_id'] ? (int)$row['client_id'] : null
        ];
    }

    return $workouts;
}
?>
