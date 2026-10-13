# Script PHP para el Servidor AWS (`login.php`)

Para que el inicio de sesión valide correctamente las credenciales contra la base de datos MySQL en AWS y rechace cuentas bloqueadas (según el Ítem 1 de la rúbrica), crea un archivo llamado **`login.php`** en la carpeta web de tu servidor AWS (`34.202.221.246`).

## Código para `login.php`:

```php
<?php
header("Content-Type: application/json");
include 'conexion.php'; // Asegúrate de que este archivo conecte correctamente a tu MySQL

$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Complete todos los campos"]);
    exit;
}

// Consultar usuario en la base de datos
$sql = "SELECT * FROM users WHERE email = '$email'";
$result = $conn->query($sql);

if ($result && $result->num_rows > 0) {
    $row = $result->fetch_assoc();

    // Verificar si la cuenta está bloqueada temporalmente por recuperación de contraseña
    if (isset($row['status']) && $row['status'] == 'blocked') {
        echo json_encode(["status" => "error", "message" => "Cuenta bloqueada temporalmente por recuperación de contraseña"]);
        exit;
    }

    // Verificar contraseña (soporta texto plano o hash)
    if ($row['password'] == $password || md5($password) == $row['password']) {
        echo json_encode(["status" => "success", "message" => "Login exitoso"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Contraseña incorrecta"]);
    }
} else {
    echo json_encode(["status" => "error", "message" => "Usuario no registrado"]);
}

$conn->close();
?>
```

## Pasos para subirlo a tu servidor AWS:
1. Conéctate a tu servidor por SSH o SFTP.
2. Crea el archivo `login.php` en la carpeta raíz de tu sitio web (por ejemplo, `/var/www/html/login.php`).
3. Pega el código anterior y guarda los cambios.
4. ¡Listo! Tu app Android ahora enviará las credenciales por POST a `http://34.202.221.246/login.php` y validará exactamente el usuario, la contraseña y si la cuenta está activa o bloqueada.
