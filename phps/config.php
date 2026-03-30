<?php
// Database configuration
 $host = 'localhost';
 $dbname = 'fitnesstracker';
 $username = 'root';
 $password = '';  // Empty by default in Laragon

// Create connection
 $conn = new mysqli($host, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    die(json_encode([
        'success' => false,
        'message' => 'Connection failed: ' . $conn->connect_error
    ]));
}

// Set charset to utf8
 $conn->set_charset('utf8mb4');
?>