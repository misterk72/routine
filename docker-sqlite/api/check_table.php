<?php
header('Content-Type: application/json');

// Configuration de la base de données
$host = 'db'; // Nom du service dans docker-compose
$dbname = 'healthtracker';
$username = 'healthuser';
$password = 'healthpassword';

try {
    // Connexion à la base de données
    $pdo = new PDO("mysql:host=$host;dbname=$dbname", $username, $password);
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    // Vérifier la structure de la table health_entries
    $stmt = $pdo->query("DESCRIBE health_entries");
    $columns = $stmt->fetchAll(PDO::FETCH_ASSOC);
    
    // Vérifier si la colonne deleted existe
    $deletedColumnExists = false;
    foreach ($columns as $column) {
        if ($column['Field'] === 'deleted') {
            $deletedColumnExists = true;
            break;
        }
    }
    
    // Si la colonne deleted n'existe pas, l'ajouter
    if (!$deletedColumnExists) {
        $pdo->exec("ALTER TABLE health_entries ADD COLUMN deleted BOOLEAN DEFAULT 0");
        echo json_encode([
            'status' => 'success',
            'message' => 'La colonne deleted a été ajoutée à la table health_entries'
        ]);
    } else {
        echo json_encode([
            'status' => 'success',
            'message' => 'La colonne deleted existe déjà dans la table health_entries'
        ]);
    }
    
} catch (PDOException $e) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Erreur de connexion à la base de données: ' . $e->getMessage()
    ]);
}
?>
