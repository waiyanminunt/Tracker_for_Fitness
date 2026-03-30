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
if (!isset($data['email']) || !isset($data['password'])) {
    echo json_encode([
        'success' => false,
        'message' => 'Email and password are required'
    ]);
    exit;
}

 $email = $conn->real_escape_string($data['email']);
 $password = $data['password'];

// Find user by email
 $sql = "SELECT id, name, email, password FROM users WHERE email = '$email'";
 $result = $conn->query($sql);

if ($result->num_rows === 0) {
    echo json_encode([
        'success' => false,
        'message' => 'User not found'
    ]);
    exit;
}

 $user = $result->fetch_assoc();

// Verify password
if (password_verify($password, $user['password'])) {
    echo json_encode([
        'success' => true,
        'message' => 'Login successful',
        'user' => [
            'id' => $user['id'],
            'name' => $user['name'],
            'email' => $user['email']
        ]
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid password'
    ]);
}

 $conn->close();
?>