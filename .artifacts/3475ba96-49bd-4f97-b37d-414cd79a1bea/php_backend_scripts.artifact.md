# Scripts PHP para el Servidor AWS (Recuperación, Verificación y Actualización de Contraseña)

Para cumplir con el **Ítem 2 de la rúbrica** (envío de código aleatorio mediante PHPMailer, vigencia de 1 minuto, verificación del código ingresado, bloqueo y reactivación de cuenta al guardar la nueva clave), debes tener estos 3 scripts en tu servidor AWS (`/var/www/html/`).

---

## 1. `recuperar_password.php`
Genera el código, lo guarda con expiración a 1 minuto, bloquea la cuenta temporalmente (`status = 'blocked'`) y envía el correo con PHPMailer.

```php
<?php
header('Content-Type: application/json; charset=utf-8');

if (file_exists('vendor/autoload.php')) {
    require 'vendor/autoload.php';
} else {
    echo json_encode(["status" => "error", "message" => "Falta instalar PHPMailer"]);
    exit();
}

use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;

$host = "127.0.0.1";
$user = "root";
$pass = "Ventana$123";
$db   = "iotpnk";

$conn = mysqli_init();
$conn->options(MYSQLI_OPT_CONNECT_TIMEOUT, 3);

if (!@$conn->real_connect($host, $user, $pass, $db)) {
    echo json_encode(["status" => "error", "message" => "Conexion MySQL fallida"]);
    exit();
}

$email = $_POST['email'] ?? '';
if (empty($email)) {
    echo json_encode(["status" => "error", "message" => "Email requerido"]);
    exit();
}

$email = mysqli_real_escape_string($conn, $email);
$sql = "SELECT * FROM users WHERE email = '$email'";
$resultado = mysqli_query($conn, $sql);

if ($resultado && mysqli_num_rows($resultado) > 0) {
    $codigo = rand(10000, 99999);
    $updateSql = "UPDATE users SET recovery_code = '$codigo', code_expiration = DATE_ADD(NOW(), INTERVAL 1 MINUTE), status = 'blocked' WHERE email = '$email'";

    if (mysqli_query($conn, $updateSql)) {
        $mail = new PHPMailer(true);
        try {
            $mail->isSMTP();
            $mail->Host = 'smtp.gmail.com';
            $mail->SMTPAuth = true;
            $mail->Username = 'tu_correo_real@gmail.com';
            $mail->Password = 'tu_contraseña_aplicacion';
            $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
            $mail->Port = 587;

            $mail->setFrom('tu_correo_real@gmail.com', 'IoT App Penka');
            $mail->addAddress($email);

            $mail->isHTML(true);
            $mail->Subject = 'Codigo de recuperacion de contraseña';
            $mail->Body    = 'Su codigo de recuperacion es: <b>' . $codigo . '</b>. Vigencia de 1 minuto.';

            $mail->send();
            echo json_encode(["status" => "success", "message" => "Codigo enviado correctamente"]);
        } catch (Exception $e) {
            echo json_encode(["status" => "error", "message" => "Error PHPMailer: " . $mail->ErrorInfo]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "Error SQL"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "El correo no esta registrado"]);
}
mysqli_close($conn);
?>
```

---

## 2. `verificar_codigo.php`
Verifica si el código ingresado coincide con el de la base de datos y si no ha vencido la vigencia de 1 minuto.

```php
<?php
header('Content-Type: application/json; charset=utf-8');

$host = "127.0.0.1";
$user = "root";
$pass = "Ventana$123";
$db   = "iotpnk";

$conn = mysqli_init();
$conn->options(MYSQLI_OPT_CONNECT_TIMEOUT, 3);

if (!@$conn->real_connect($host, $user, $pass, $db)) {
    echo json_encode(["status" => "error", "message" => "Conexion MySQL fallida"]);
    exit();
}

$email = $_POST['email'] ?? '';
$codigo = $_POST['codigo'] ?? '';

if (empty($email) || empty($codigo)) {
    echo json_encode(["status" => "error", "message" => "Complete todos los campos"]);
    exit();
}

$email = mysqli_real_escape_string($conn, $email);
$codigo = mysqli_real_escape_string($conn, $codigo);

$sql = "SELECT * FROM users WHERE email = '$email'";
$resultado = mysqli_query($conn, $sql);

if ($resultado && mysqli_num_rows($resultado) > 0) {
    $row = mysqli_fetch_assoc($resultado);
    $dbCode = $row['recovery_code'] ?? '';
    $expiration = $row['code_expiration'] ?? '';

    if ($dbCode === $codigo) {
        if (strtotime($expiration) >= time()) {
            echo json_encode(["status" => "success", "message" => "Codigo verificado"]);
        } else {
            echo json_encode(["status" => "error", "message" => "El codigo ha expirado (1 minuto vencido)"]);
        }
    } else {
        echo json_encode(["status" => "error", "message" => "Codigo incorrecto"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Usuario no encontrado"]);
}

mysqli_close($conn);
?>
```

---

## 3. `actualizar_password.php`
Actualiza la nueva contraseña en MySQL, **elimina el código utilizado (`recovery_code = NULL`)** para que no se vuelva a usar, y **reactiva la cuenta (`status = 'active'`)** para que el usuario pueda volver a iniciar sesión con sus nuevas credenciales.

```php
<?php
header('Content-Type: application/json; charset=utf-8');

$host = "127.0.0.1";
$user = "root";
$pass = "Ventana$123";
$db   = "iotpnk";

$conn = mysqli_init();
$conn->options(MYSQLI_OPT_CONNECT_TIMEOUT, 3);

if (!@$conn->real_connect($host, $user, $pass, $db)) {
    echo json_encode(["status" => "error", "message" => "Conexion MySQL fallida"]);
    exit();
}

$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Complete todos los campos"]);
    exit();
}

$email = mysqli_real_escape_string($conn, $email);
$password = mysqli_real_escape_string($conn, $password);

// Actualizar la contraseña en phpMyAdmin, borrar el código usado y reactivar la cuenta
$sql = "UPDATE users SET password = '$password', status = 'active', recovery_code = NULL, code_expiration = NULL WHERE email = '$email'";

if (mysqli_query($conn, $sql)) {
    echo json_encode(["status" => "success", "message" => "Contraseña actualizada y cuenta reactivada"]);
} else {
    echo json_encode(["status" => "error", "message" => "Error SQL: " . mysqli_error($conn)]);
}

mysqli_close($conn);
?>
```
