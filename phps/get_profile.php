<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

// Validate user_id query parameter
$user_id = isset($_GET['user_id']) ? (int)$_GET['user_id'] : 0;

if ($user_id <= 0) {
    echo json_encode([
        'success' => false,
        'message' => 'A valid User ID is required'
    ]);
    exit;
}

// Fetch user profile using a Prepared Statement
$stmt = $conn->prepare("SELECT id, name, email FROM users WHERE id = ?");
$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    echo json_encode([
        'success' => false,
        'message' => 'User not found'
    ]);
    $stmt->close();
    $conn->close();
    exit;
}

$user = $result->fetch_assoc();

echo json_encode([
    'success' => true,
    'message' => 'Profile retrieved successfully',
    'user' => [
        'id'    => (int)$user['id'],
        'name'  => $user['name'],
        'email' => $user['email']
    ]
]);

$stmt->close();
$conn->close();
?>
