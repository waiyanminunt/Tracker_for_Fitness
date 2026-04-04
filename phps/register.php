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

 $name = $data['name'];
 $email = $data['email'];
 $password = $data['password'];

// Validate email
if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid email format'
    ]);
    exit;
}

// Check if email already exists using Prepared Statements
 $stmt = $conn->prepare("SELECT id FROM users WHERE email = ?");
 $stmt->bind_param("s", $email);
 $stmt->execute();
 $result = $stmt->get_result();

if ($result->num_rows > 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Email already registered'
    ]);
    $stmt->close();
    exit;
}
 $stmt->close();

// Hash the password
 $hashedPassword = password_hash($password, PASSWORD_DEFAULT);

// Insert new user using Prepared Statements
 $stmt = $conn->prepare("INSERT INTO users (name, email, password) VALUES (?, ?, ?)");
 $stmt->bind_param("sss", $name, $email, $hashedPassword);

if ($stmt->execute()) {
    echo json_encode([
        'success' => true,
        'message' => 'Registration successful',
        'user_id' => (int)$conn->insert_id
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Registration failed: ' . $stmt->error
    ]);
}

 $stmt->close();
 $conn->close();
?>