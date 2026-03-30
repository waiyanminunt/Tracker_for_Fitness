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
if (!isset($data['name']) || !isset($data['email']) || !isset($data['password'])) {
    echo json_encode([
        'success' => false,
        'message' => 'All fields are required'
    ]);
    exit;
}

 $name = $conn->real_escape_string($data['name']);
 $email = $conn->real_escape_string($data['email']);
 $password = $data['password'];

// Validate email
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid email format'
    ]);
    exit;
}

// Check if email already exists
 $checkEmail = "SELECT id FROM users WHERE email = '$email'";
 $result = $conn->query($checkEmail);

if ($result->num_rows > 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Email already registered'
    ]);
    exit;
}

// Hash the password
 $hashedPassword = password_hash($password, PASSWORD_DEFAULT);

// Insert new user
 $sql = "INSERT INTO users (name, email, password) VALUES ('$name', '$email', '$hashedPassword')";

if ($conn->query($sql)) {
    echo json_encode([
        'success' => true,
        'message' => 'Registration successful',
        'user_id' => $conn->insert_id
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Registration failed: ' . $conn->error
    ]);
}

 $conn->close();
?>