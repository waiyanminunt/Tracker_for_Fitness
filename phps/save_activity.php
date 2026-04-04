<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

// Get JSON input
 $json = file_get_contents('php://input');
 $data = json_decode($json, true);

// Check if required fields exist
if (!isset($data['user_id']) || !isset($data['activity_type']) || !isset($data['duration'])) {
    echo json_encode([
        'success' => false,
        'message' => 'User ID, activity type, and duration are required'
    ]);
    exit;
}

 $user_id = (int)$data['user_id'];
 $activity_type = $data['activity_type'];
 $duration = (int)$data['duration'];
 $distance = isset($data['distance']) ? (float)$data['distance'] : 0.0;
 $calories = isset($data['calories']) ? (int)$data['calories'] : 0;
 $notes = isset($data['notes']) ? $data['notes'] : '';

// Insert activity using Prepared Statements
 $stmt = $conn->prepare("INSERT INTO activities (user_id, activity_type, duration, distance, calories, notes) VALUES (?, ?, ?, ?, ?, ?)");
 $stmt->bind_param("isddis", $user_id, $activity_type, $duration, $distance, $calories, $notes);

if ($stmt->execute()) {
    echo json_encode([
        'success' => true,
        'message' => 'Activity saved successfully',
        'activity_id' => (int)$conn->insert_id
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Failed to save activity: ' . $stmt->error
    ]);
}

 $stmt->close();
 $conn->close();
?>