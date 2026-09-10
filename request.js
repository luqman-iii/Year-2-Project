const API_BASE = "http://localhost:8080";
const ACCEPTED_REQUEST_POLL_MS = 15000;

const medicineInput = document.getElementById("medicine-input");
const cityInput = document.getElementById("city-input");
const searchButton = document.getElementById("search-button");
const searchMessage = document.getElementById("search-message");
const searchResults = document.getElementById("search-results");
const requestUpdatesSection = document.getElementById("request-updates");
const requestUpdatesStatus = document.getElementById("request-updates-status");
const requestUpdatesList = document.getElementById("request-updates-list");
const requestModal = document.getElementById("request-modal");
const selectedMedicationLabel = document.getElementById("selected-medication");
const requestForm = document.getElementById("request-form");
const requesterNameInput = document.getElementById("requester-name");
const requesterPhoneInput = document.getElementById("requester-phone");
const requesterEmailInput = document.getElementById("requester-email");
const requestNotesInput = document.getElementById("request-notes");
const closeRequestModalButton = document.getElementById("close-request-modal");
const cancelRequestButton = document.getElementById("cancel-request");

let selectedInventory = null;
let acceptedRequestsPollId = null;

const currentUserId = Number(localStorage.getItem("userId") || "");
const currentUserRole = (localStorage.getItem("userRole") || "").trim().toUpperCase();
const currentUserName = localStorage.getItem("userName") || "";
const currentUserPhone = localStorage.getItem("userPhone") || "";
const currentUserEmail = localStorage.getItem("userEmail") || "";

function setMessage(message) {
  searchMessage.textContent = message;
}

function setRequestUpdatesStatus(message) {
  if (!requestUpdatesStatus) {
    return;
  }
  requestUpdatesStatus.textContent = message;
}

function isMotherSession() {
  return currentUserRole === "MOTHER" && Number.isFinite(currentUserId) && currentUserId > 0;
}

function formatPharmacyLine(item) {
  const parts = [item.pharmacy?.addressLine, item.pharmacy?.city, item.pharmacy?.postcode].filter(Boolean);
  return parts.join(", ");
}

function formatStock(item) {
  const quantity = item.quantityInStock ?? 0;
  const status = item.availabilityStatus || "UNKNOWN";
  return `${quantity} in stock • ${status.replaceAll("_", " ")}`;
}

function formatDate(value) {
  if (!value) {
    return "Date not available";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function renderAcceptedRequestUpdates(requests) {
  if (!requestUpdatesSection || !requestUpdatesList) {
    return;
  }

  if (!Array.isArray(requests) || requests.length === 0) {
    requestUpdatesSection.classList.add("hidden");
    requestUpdatesList.innerHTML = "";
    setRequestUpdatesStatus("");
    return;
  }

  requestUpdatesSection.classList.remove("hidden");
  requestUpdatesList.innerHTML = requests.map((request) => `
    <article class="update-card" data-request-id="${request.requestId}">
      <h3>${request.pharmacy?.pharmacyName || "A pharmacy"} accepted your request</h3>
      <p><strong>Medication:</strong> ${request.medication?.medicineName || "Medication"}</p>
      <p><strong>Location:</strong> ${formatPharmacyLine(request) || "Location not available"}</p>
      <p><strong>Requested:</strong> ${formatDate(request.requestDate)}</p>
      <p><strong>Next step:</strong> Please confirm whether you collected it.</p>
      <div class="update-actions">
        <button class="update-btn" type="button" data-update-action="picked-up">I picked it up</button>
        <button class="dismiss-btn" type="button" data-update-action="not-going">I'm not going</button>
      </div>
    </article>
  `).join("");

  requestUpdatesList.querySelectorAll("[data-update-action]").forEach((button) => {
    button.addEventListener("click", async () => {
      const card = button.closest("[data-request-id]");
      const requestId = Number(card?.dataset.requestId);
      const action = button.dataset.updateAction;

      if (!requestId || !action) {
        return;
      }

      button.disabled = true;
      await submitAcceptedRequestAction(requestId, action);
    });
  });

  setRequestUpdatesStatus(`${requests.length} pharmacy update${requests.length === 1 ? "" : "s"} waiting for your response.`);
}

async function loadAcceptedRequestUpdates(options = {}) {
  if (!isMotherSession()) {
    requestUpdatesSection?.classList.add("hidden");
    requestUpdatesList && (requestUpdatesList.innerHTML = "");
    return;
  }

  if (!options.silent) {
    setRequestUpdatesStatus("Checking pharmacy updates...");
  }

  try {
    const response = await fetch(`${API_BASE}/api/requests/mother/${currentUserId}/accepted`);
    if (!response.ok) {
      throw new Error(`Unable to load request updates (${response.status})`);
    }

    const data = await response.json();
    renderAcceptedRequestUpdates(data);
  } catch (error) {
    console.error(error);
    requestUpdatesSection?.classList.remove("hidden");
    if (requestUpdatesList) {
      requestUpdatesList.innerHTML = "";
    }
    setRequestUpdatesStatus("Unable to load pharmacy updates right now.");
  }
}

async function submitAcceptedRequestAction(requestId, action) {
  const actionMessages = {
    "picked-up": "Marked as picked up.",
    "not-going": "Marked as not going.",
  };

  try {
    const response = await fetch(`${API_BASE}/api/requests/${requestId}/${action}`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        requesterUserId: currentUserId,
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `Unable to update request (${response.status})`);
    }

    setRequestUpdatesStatus(actionMessages[action] || "Request updated.");
    await loadAcceptedRequestUpdates({ silent: true });
  } catch (error) {
    console.error(error);
    setRequestUpdatesStatus("Unable to update your request right now.");
    await loadAcceptedRequestUpdates({ silent: true });
  }
}

function renderResults(results) {
  if (!Array.isArray(results) || results.length === 0) {
    searchResults.innerHTML = "";
    setMessage("No medications matched your search.");
    return;
  }

  searchResults.innerHTML = results.map((item) => {
    const medicine = item.medication || {};
    const pharmacy = item.pharmacy || {};
    return `
      <article class="result-card">
        <h2>${medicine.medicineName || "Medication"}</h2>
        <p><strong>Pharmacy:</strong> ${pharmacy.pharmacyName || "Unknown pharmacy"}</p>
        <p><strong>Location:</strong> ${formatPharmacyLine(item) || "No address available"}</p>
        <p><strong>Phone:</strong> ${pharmacy.phone || "No phone available"}</p>
        <div class="pill-row">
          <span class="pill">${medicine.category || "General"}</span>
          <span class="pill">${medicine.dosageForm || "Form not set"}</span>
          <span class="pill">${medicine.strength || "Strength not set"}</span>
        </div>
        <p><strong>Availability:</strong> ${formatStock(item)}</p>
        <div class="card-actions">
          <button class="request-btn" type="button" data-inventory-id="${item.inventoryId}">Request Medication</button>
        </div>
      </article>
    `;
  }).join("");

  searchResults.querySelectorAll("[data-inventory-id]").forEach((button) => {
    button.addEventListener("click", () => {
      const inventoryId = Number(button.dataset.inventoryId);
      selectedInventory = results.find((item) => item.inventoryId === inventoryId) || null;
      openRequestModal();
    });
  });

  setMessage(`Found ${results.length} result${results.length === 1 ? "" : "s"}.`);
}

async function runSearch() {
  const medicine = medicineInput.value.trim();
  const city = cityInput.value.trim();

  if (!medicine) {
    setMessage("Enter a medicine name before searching.");
    searchResults.innerHTML = "";
    return;
  }

  const params = new URLSearchParams({ medicine });
  if (city) {
    params.set("city", city);
  }

  setMessage("Searching...");

  try {
    const response = await fetch(`${API_BASE}/api/search?${params.toString()}`);
    if (!response.ok) {
      throw new Error(`Search failed with status ${response.status}`);
    }

    const data = await response.json();
    renderResults(data);
  } catch (error) {
    console.error(error);
    searchResults.innerHTML = "";
    setMessage("Unable to reach the medication search service.");
  }
}

function openRequestModal() {
  if (!selectedInventory) {
    return;
  }

  if (requesterNameInput && !requesterNameInput.value.trim()) {
    requesterNameInput.value = currentUserName;
  }
  if (requesterPhoneInput && !requesterPhoneInput.value.trim()) {
    requesterPhoneInput.value = currentUserPhone;
  }
  if (requesterEmailInput && !requesterEmailInput.value.trim()) {
    requesterEmailInput.value = currentUserEmail;
  }

  const medicineName = selectedInventory.medication?.medicineName || "medication";
  const pharmacyName = selectedInventory.pharmacy?.pharmacyName || "selected pharmacy";
  selectedMedicationLabel.textContent = `Requesting ${medicineName} from ${pharmacyName}`;
  requestModal.classList.remove("hidden");
  requestModal.setAttribute("aria-hidden", "false");
}

function closeRequestModal() {
  requestModal.classList.add("hidden");
  requestModal.setAttribute("aria-hidden", "true");
  requestForm.reset();
  selectedInventory = null;
}

async function submitRequest(event) {
  event.preventDefault();

  if (!selectedInventory) {
    return;
  }

  const payload = {
    requesterName: requesterNameInput.value.trim(),
    requesterPhone: requesterPhoneInput.value.trim(),
    requesterEmail: requesterEmailInput.value.trim(),
    requesterUserId: Number.isFinite(currentUserId) && currentUserId > 0 ? currentUserId : null,
    pharmacyId: selectedInventory.pharmacy?.pharmacyId,
    medicationId: selectedInventory.medication?.medicationId,
    notes: requestNotesInput.value.trim(),
  };

  if (!payload.requesterName || !payload.pharmacyId || !payload.medicationId) {
    setMessage("Requester name and medication details are required.");
    return;
  }

  try {
    const response = await fetch(`${API_BASE}/api/requests`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(errorText || `Request failed with status ${response.status}`);
    }

    closeRequestModal();
    setMessage("Medication request sent successfully.");
  } catch (error) {
    console.error(error);
    setMessage("Unable to send the medication request.");
  }
}

searchButton?.addEventListener("click", runSearch);
medicineInput?.addEventListener("keydown", (event) => {
  if (event.key === "Enter") {
    event.preventDefault();
    runSearch();
  }
});
cityInput?.addEventListener("keydown", (event) => {
  if (event.key === "Enter") {
    event.preventDefault();
    runSearch();
  }
});
requestForm?.addEventListener("submit", submitRequest);
closeRequestModalButton?.addEventListener("click", closeRequestModal);
cancelRequestButton?.addEventListener("click", closeRequestModal);
requestModal?.addEventListener("click", (event) => {
  if (event.target === requestModal) {
    closeRequestModal();
  }
});

loadAcceptedRequestUpdates();

if (isMotherSession()) {
  acceptedRequestsPollId = window.setInterval(() => {
    loadAcceptedRequestUpdates({ silent: true });
  }, ACCEPTED_REQUEST_POLL_MS);
}
