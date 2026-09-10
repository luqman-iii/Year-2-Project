const REQUESTS_API_BASE = "http://localhost:8080";

const requestCards = document.getElementById("request-cards");
const requestStatus = document.getElementById("request-status");

function setStatus(message) {
  requestStatus.textContent = message;
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

function renderRequests(requests) {
  if (!Array.isArray(requests) || requests.length === 0) {
    requestCards.innerHTML = `<div class="empty-state">No pending medication requests right now.</div>`;
    setStatus("No pending requests.");
    return;
  }

  requestCards.innerHTML = requests.map((request) => `
    <article class="request-card" data-request-id="${request.requestId}">
      <h2>${request.requester?.fullName || "Unknown requester"}</h2>
      <p><strong>Medication Requested:</strong><br>${request.medication?.medicineName || "Unknown medication"}</p>
      <div class="request-meta">
        <span><strong>Pharmacy:</strong> ${request.pharmacy?.pharmacyName || "Unknown pharmacy"}</span>
        <span><strong>City:</strong> ${request.pharmacy?.city || "Not set"}</span>
        <span><strong>Requested:</strong> ${formatDate(request.requestDate)}</span>
        <span><strong>Contact:</strong> ${request.requester?.phone || request.requester?.email || "No contact provided"}</span>
        <span><strong>Notes:</strong> ${request.notes || "No notes"}</span>
      </div>
      <div class="btn-row">
        <button class="accept-btn" type="button" data-action="accept">Accept</button>
        <button class="decline-btn" type="button" data-action="decline">Decline</button>
      </div>
    </article>
  `).join("");

  requestCards.querySelectorAll("[data-action]").forEach((button) => {
    button.addEventListener("click", async () => {
      const card = button.closest("[data-request-id]");
      const requestId = card?.dataset.requestId;
      const action = button.dataset.action;

      if (!requestId || !action) {
        return;
      }

      button.disabled = true;
      try {
        const response = await fetch(`${REQUESTS_API_BASE}/api/requests/${requestId}/${action}`, {
          method: "PUT",
        });

        if (!response.ok) {
          throw new Error(`Unable to ${action} request`);
        }

        card.remove();
        const remainingCards = requestCards.querySelectorAll("[data-request-id]").length;
        setStatus(`${action === "accept" ? "Accepted" : "Declined"} request #${requestId}.`);

        if (remainingCards === 0) {
          requestCards.innerHTML = `<div class="empty-state">No pending medication requests right now.</div>`;
        }
      } catch (error) {
        console.error(error);
        setStatus(`Unable to ${action} request #${requestId}.`);
        button.disabled = false;
      }
    });
  });

  setStatus(`Showing ${requests.length} pending request${requests.length === 1 ? "" : "s"}.`);
}

async function loadRequests() {
  setStatus("Loading pending requests...");

  try {
    const response = await fetch(`${REQUESTS_API_BASE}/api/requests/pending`);
    if (!response.ok) {
      throw new Error(`Unable to load requests (${response.status})`);
    }

    const data = await response.json();
    renderRequests(data);
  } catch (error) {
    console.error(error);
    requestCards.innerHTML = `<div class="empty-state">Unable to load medication requests.</div>`;
    setStatus("Medication request service is unavailable.");
  }
}

document.addEventListener("DOMContentLoaded", loadRequests);
