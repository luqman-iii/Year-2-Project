<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Reset Password</title>

    <!-- LINK CSS -->
    <link rel="stylesheet" href="ForgotPassword.css">
</head>

<body>

    <!-- NAVBAR -->
    <div class="navbar">
        <div class="navbar-inner">
            <img src="../design/Logo.svg" class="logo-img" alt="logo">
            <nav class="nav-links">
                <a class="nav-link" href="homepage.html">Home</a>
            </nav>
        </div>
    </div>

    <!-- PAGE TITLE -->
    <div class="container">
        <h1>Reset Password</h1>

        <!-- RESET PASSWORD CARD -->
        <form class="card" id="reset-password-form">

            <label>Email/phone number*</label>
            <input type="text" id="reset-email-or-phone" placeholder="Enter your email or phone" required>

            <label>New Password*</label>
            <input type="password" id="reset-password" placeholder="Enter new password" required>

            <label>Confirm Password*</label>
            <input type="password" id="reset-confirm-password" placeholder="Confirm new password" required>

            <button type="submit">Reset password</button>

            <p class="signup-text" id="reset-message" aria-live="polite"></p>
        </form>
    </div>

  <script src="navDriver.js?v=7"></script>
  <script src="ForgotPassword.js"></script>
</body>
</html>
