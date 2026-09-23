<?php
header("Content-Type: application/json; charset=UTF-8");
include 'configj.php';

$data = json_decode(file_get_contents("php://input"), true);

if (!$data) {
    echo json_encode(["status" => "error", "message" => "Invalid JSON data"]);
    exit;
}

$username    = mysqli_real_escape_string($conn, $data['username'] ?? '');
$token       = mysqli_real_escape_string($conn, $data['token'] ?? '');
$date        = mysqli_real_escape_string($conn, $data['date'] ?? '');
$item_name   = mysqli_real_escape_string($conn, $data['item_name'] ?? '');
$description = mysqli_real_escape_string($conn, $data['description'] ?? '');
$party_name  = mysqli_real_escape_string($conn, $data['party_name'] ?? '');
$stock_type  = mysqli_real_escape_string($conn, $data['stock_type'] ?? '');
$quantity    = mysqli_real_escape_string($conn, $data['quantity'] ?? '');
$unit_price  = mysqli_real_escape_string($conn, $data['unit_price'] ?? '');
$total_value = mysqli_real_escape_string($conn, $data['total_value'] ?? '');

// Validate fields
if (empty($username) || empty($token) || empty($item_name) || empty($stock_type)) {
    echo json_encode(["status" => "error", "message" => "Missing required fields"]);
    exit;
}

// -----------------------------------------------
// ? AUTO INVOICE NUMBER GENERATOR
// Format: YYYYXXXXX (e.g., 202500001)
// -----------------------------------------------

$currentYear = date("Y");

// Get latest invoice of this year
$invoiceQuery = mysqli_query($conn, "
    SELECT invoice_no 
    FROM stock_table 
    WHERE invoice_no LIKE '$currentYear%' 
    ORDER BY invoice_no DESC 
    LIMIT 1
");

if ($invoiceQuery && mysqli_num_rows($invoiceQuery) > 0) {
    $row = mysqli_fetch_assoc($invoiceQuery);
    $lastInvoice = intval(substr($row['invoice_no'], 4)); 
    $nextNumber = str_pad($lastInvoice + 1, 5, '0', STR_PAD_LEFT);
} else {
    $nextNumber = "00001";
}

$invoice_no = $currentYear . $nextNumber;

// -----------------------------------------------


// INSERT with auto invoice number
$query = "
INSERT INTO stock_table 
(date, item_name, description, party_name, stock_type, quantity, unit_price, total_value, username, invoice_no)
VALUES 
('$date', '$item_name', '$description', '$party_name', '$stock_type', '$quantity', '$unit_price', '$total_value', '$username', '$invoice_no')
";

if (mysqli_query($conn, $query)) {
    echo json_encode([
        "status" => "success",
        "message" => "Stock entry saved successfully",
        "invoice_no" => $invoice_no  // Optional to send back
    ]);
} else {
    echo json_encode(["status" => "error", "message" => "DB Error: " . mysqli_error($conn)]);
}

?>