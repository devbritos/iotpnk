# Scripts PHP para el Servidor AWS (PHPMailer y Recuperación de Contraseña)

Para cumplir con el **Ítem 2 de la rúbrica** (envío de código aleatorio mediante PHPMailer, vigencia de 1 minuto, bloqueo temporal de cuenta y reactivación al guardar la nueva contraseña), debes subir estos scripts PHP a tu servidor AWS (`34.202.221.246`).

## 1. `recuperar_password.php`
Este script genera el código aleatorio, lo guarda en la base de datos con expiración a 1 minuto, bloquea temporalmente la cuenta y envía el correo usando PHPMailer.

```php
<?php
header("Content-Type: application/json");
include 'conexion.php'; // Tu archivo de conexión a MySQL

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

require 'vendor/autoload.php'; // Asegúrate de tener PHPMailer instalado vía Composer

$email = $_POST['email'] ?? '';

if (empty($email)) {
    echo json_encode(["status" => "error", "message" => "Email requerido"]);
    exit;
}

// Generar código aleatorio de 5 dígitos
$codigo = rand(10000, 99999);
// Expiración en 1 minuto (NOW() + INTERVAL 1 MINUTE)
// Bloquear cuenta temporalmente (status = 'blocked')
$sql = "UPDATE users SET recovery_code = '$codigo', code_expiration = DATE_ADD(NOW(), INTERVAL 1 MINUTE), status = 'blocked' WHERE email = '$email'";

if ($conn->query($sql) === TRUE) {
    if ($conn->affected_rows > 0) {
        // Enviar correo con PHPMailer
        $mail = new PHPMailer(true);
        try {
            // Configuración del servidor SMTP (ejemplo)
            $mail->isSMTP();
            $mail->Host = 'smtp.gmail.com';
            $mail->SMTPAuth = true;
            $mail->Username = 'tu_correo@gmail.com';
            $mail->Password = 'tu_contraseña_aplicacion';
            $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
            $mail->Port = 587;

            $mail->setFrom('tu_correo@gmail.com', 'IoT App Penka');
            $mail->addAddress($email);

            $mail->isHTML(true);
            $mail->Subject = 'Código de recuperación de contraseña';
            $mail->Body    = 'Su código de recuperación es: <b>' . $codigo . '</b>. Tiene una vigencia de 1 minuto.';

            $mail->send();
            echo json_encode(["status" => "success", "message" => "Código enviado"]);
        } catch (Exception $e) {
            echo json_encode(["status" => "error", "message" => "Error al enviar correo: {$mail->ErrorInfo}"]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "El correo no está registrado"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Error en base de datos"]);
}
$conn->close();
?>
```
