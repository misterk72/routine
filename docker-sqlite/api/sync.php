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
        
        // Traiter les entrées de santé
        if (isset($data['entries']) && is_array($data['entries'])) {
            // Log pour débogage
            error_log("DEBUG - Nombre d'entrées reçues: " . count($data['entries']));
            error_log("DEBUG - Données reçues: " . json_encode($data));
            
            $result = processEntries($pdo, $data['entries']);
            error_log("DEBUG - Résultat du traitement: " . json_encode($result));
            echo json_encode($result);
        } else {
            error_log("DEBUG - Format de données invalide");
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
    
    // Vérifier si la colonne 'deleted' existe dans la table health_entries
    $tableInfo = $pdo->query("DESCRIBE health_entries");
    $columns = $tableInfo->fetchAll(PDO::FETCH_COLUMN);
    
    // Si la colonne 'deleted' n'existe pas, l'ajouter
    if (!in_array('deleted', $columns)) {
        error_log("Ajout de la colonne 'deleted' à la table health_entries");
        $pdo->exec("ALTER TABLE health_entries ADD COLUMN deleted BOOLEAN DEFAULT 0");
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
?>
