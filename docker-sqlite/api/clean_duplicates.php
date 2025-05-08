<?php
// Supprimer les avertissements PHP pour éviter de corrompre la réponse JSON
error_reporting(0);
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
    
    // 1. Identifier les entrées en double basées sur client_id
    $stmt = $pdo->query("
        SELECT client_id, COUNT(*) as count
        FROM health_entries
        WHERE client_id IS NOT NULL
        GROUP BY client_id
        HAVING COUNT(*) > 1
    ");
    
    $duplicates = $stmt->fetchAll(PDO::FETCH_ASSOC);
    $duplicateCount = count($duplicates);
    
    // 2. Pour chaque groupe de doublons, conserver uniquement l'entrée la plus récente
    $removedCount = 0;
    foreach ($duplicates as $duplicate) {
        $clientId = $duplicate['client_id'];
        
        // Trouver l'ID de l'entrée la plus récente pour ce client_id
        $stmt = $pdo->prepare("
            SELECT id
            FROM health_entries
            WHERE client_id = ?
            ORDER BY last_modified DESC
            LIMIT 1
        ");
        $stmt->execute([$clientId]);
        $latestId = $stmt->fetchColumn();
        
        // Supprimer toutes les autres entrées avec ce client_id
        $stmt = $pdo->prepare("
            DELETE FROM health_entries
            WHERE client_id = ? AND id != ?
        ");
        $stmt->execute([$clientId, $latestId]);
        
        $removedCount += $stmt->rowCount();
    }
    
    echo json_encode([
        'status' => 'success',
        'message' => "Nettoyage terminé. $duplicateCount groupes de doublons trouvés, $removedCount entrées supprimées."
    ]);
    
} catch (PDOException $e) {
    echo json_encode([
        'status' => 'error',
        'message' => 'Erreur: ' . $e->getMessage()
    ]);
}
?>
