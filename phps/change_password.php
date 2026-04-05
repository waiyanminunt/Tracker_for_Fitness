<?php
error_reporting(0);
ini_set('display_errors', 0);
ob_start();

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST');
header('Access-Control-Allow-Headers: Content-Type');

require_once 'config.php';

// Read raw JSON body (Retrofit sends @Body as JSON, not $_POST)
$json = file_get_contents('php://input');
$data = json_decode($json, true);

// Validate required fields
if (!isset($data['user_id']) || !isset($data['current_password']) || !isset($data['new_password'])) {
    ob_clean();
    echo json_encode(['success' => false, 'message' => 'All fields are required.']);
    exit;
}

$userId          = intval($data['user_id']);
$currentPassword = $data['current_password'];
$newPassword     = $data['new_password'];

if ($userId <= 0) {
    ob_clean();
    echo json_encode(['success' => false, 'message' => 'Invalid user ID.']);
    exit;
}

if (strlen($newPassword) < 6) {
    ob_clean();
    echo json_encode(['success' => false, 'message' => 'New password must be at least 6 characters.']);
    exit;
}

// Fetch stored password hash for this user (mirrors login.php exactly)
$stmt = $conn->prepare("SELECT password FROM users WHERE id = ?");
$stmt->bind_param("i", $userId);
$stmt->execute();
$result = $stmt->get_result();

if ($result->num_rows === 0) {
    $stmt->close();
    $conn->close();
    ob_clean();
    echo json_encode(['success' => false, 'message' => 'User not found.']);
    exit;
}

$user = $result->fetch_assoc();
$stmt->close();

// Verify current password — same method as login.php line 43
if (!password_verify($currentPassword, $user['password'])) {
    $conn->close();
    ob_clean();
    echo json_encode(['success' => false, 'message' => 'Incorrect current password.']);
    exit;
}

// Hash the new password — same method used by register.php
$newHash = password_hash($newPassword, PASSWORD_DEFAULT);

$updateStmt = $conn->prepare("UPDATE users SET password = ? WHERE id = ?");
$updateStmt->bind_param("si", $newHash, $userId);
$updateStmt->execute();

if ($updateStmt->affected_rows > 0) {
    $updateStmt->close();
    $conn->close();
    ob_clean();
    echo json_encode(['success' => true, 'message' => 'Password updated successfully.']);
} else {
    $updateStmt->close();
    $conn->close();
    ob_clean();
    echo json_encode(['success' => false, 'message' => 'Failed to update. The new password may be the same as the current one.']);
}
