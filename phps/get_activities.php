<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

// Get user_id from URL parameter
 $user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;

if ($user_id === 0) {
    echo json_encode([
        'success' => false,
        'message' => 'User ID is required'
    ]);
    exit;
}

// Get activities for user using Prepared Statements
 $stmt = $conn->prepare("SELECT id, activity_type, duration, distance, calories, notes, created_at FROM activities WHERE user_id = ? ORDER BY created_at DESC");
 $stmt->bind_param("i", $user_id);
 $stmt->execute();
 $result = $stmt->get_result();

 $activities = [];

if ($result->num_rows > 0) {
    while ($row = $result->fetch_assoc()) {
        $activities[] = [
            'id' => (int)$row['id'],
            'activity_type' => $row['activity_type'],
            'duration' => (int)$row['duration'],
            'distance' => (float)$row['distance'],
            'calories' => (int)$row['calories'],
            'notes' => $row['notes'],
            'created_at' => $row['created_at']
        ];
    }
}

echo json_encode([
    'success' => true,
    'activities' => $activities
]);

 $stmt->close();
 $conn->close();
?>