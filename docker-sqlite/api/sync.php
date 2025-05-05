<?php
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
        
        // Traiter les entrées de santé
        if (isset($data['entries']) && is_array($data['entries'])) {
            $result = processEntries($pdo, $data['entries']);
            echo json_encode($result);
        } else {
            echo json_encode(['error' => 'Format de données invalide']);
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
        echo json_encode(['entries' => $entries]);
        break;
        
    default:
        echo json_encode(['error' => 'Méthode non supportée']);
        break;
}

// Fonction pour créer les tables si elles n'existent pas
function createTablesIfNotExist($pdo) {
    // Table des utilisateurs
    $pdo->exec("CREATE TABLE IF NOT EXISTS users (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        name VARCHAR(255) NOT NULL,
        is_default BOOLEAN NOT NULL DEFAULT FALSE,
        client_id BIGINT,
        last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    )");
    
    // Table des entrées de santé
    $pdo->exec("CREATE TABLE IF NOT EXISTS health_entries (
        id BIGINT PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT NOT NULL,
        timestamp DATETIME NOT NULL,
        weight FLOAT,
        waist_measurement FLOAT,
        body_fat FLOAT,
        notes TEXT,
        client_id BIGINT,
        last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    )");
    
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
    
    foreach ($entries as $entry) {
        try {
            // Vérifier si l'utilisateur existe
            $userId = ensureUserExists($pdo, $entry['userId']);
            
            // Préparer la requête d'insertion/mise à jour
            $stmt = $pdo->prepare("INSERT INTO health_entries 
                (user_id, timestamp, weight, waist_measurement, body_fat, notes, client_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                weight = VALUES(weight),
                waist_measurement = VALUES(waist_measurement),
                body_fat = VALUES(body_fat),
                notes = VALUES(notes)");
                
            // Exécuter la requête
            $stmt->execute([
                $userId,
                $entry['timestamp'],
                $entry['weight'],
                $entry['waistMeasurement'],
                $entry['bodyFat'],
                $entry['notes'],
                $entry['id']
            ]);
            
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
    
    $stmt = $pdo->prepare("SELECT 
        e.id, e.user_id, e.timestamp, e.weight, e.waist_measurement, e.body_fat, e.notes, e.client_id,
        u.name as user_name
        FROM health_entries e
        JOIN users u ON e.user_id = u.id
        WHERE e.last_modified > ?");
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
?>
