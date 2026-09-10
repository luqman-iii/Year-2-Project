const RESET_API_BASE = "http://localhost:8080";

const resetForm = document.getElementById("reset-password-form");
const resetIdentifier = document.getElementById("reset-email-or-phone");
const resetPassword = document.getElementById("reset-password");
const resetConfirmPassword = document.getElementById("reset-confirm-password");
const resetMessage = document.getElementById("reset-message");

function setResetMessage(message, isError = false) {
  if (!resetMessage) return;
  resetMessage.textContent = message;
  resetMessage.style.color = isError ? "#b42318" : "#475467";
}

resetForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  if (resetPassword.value !== resetConfirmPassword.value) {
    setResetMessage("Passwords do not match.", true);
    return;
  }

  setResetMessage("Resetting password...");

  try {
    const response = await fetch(`${RESET_API_BASE}/auth/reset-password`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        emailOrPhone: resetIdentifier.value.trim(),
        newPassword: resetPassword.value,
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || "Password reset failed.");
    }

    setResetMessage("Password reset successful. Redirecting to sign in...");
    setTimeout(() => {
      window.location.href = "SignIn.html";
    }, 1200);
  } catch (error) {
    console.error(error);
    setResetMessage(error.message || "Unable to reset password.", true);
  }
});
