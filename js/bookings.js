// ======= CONFIG =======
const API_BASE = "https://booking-reservation-system-umuy.onrender.com";

// ======= STATE =======
const appState = {
    bookings: [],
    resources: [],
};

// ======= DOM ELEMENTS =======
const bookingsBody = document.getElementById("bookingsBody");
const resourceDropdown = document.getElementById("resourceId");
const addBookingFormContainer = document.getElementById("addBookingForm");
const bookingForm = document.getElementById("bookingForm");
const availabilityStatus = document.getElementById("availabilityStatus");

// ======= API FUNCTIONS =======
async function fetchResources() {
    try {
        const res = await fetch(`${API_BASE}/api/resources`);
        if (!res.ok) throw new Error("Failed to load resources");
        return await res.json();
    } catch (err) {
        console.error(err);
        showError("Failed to load resources");
        return [];
    }
}

async function fetchBookings() {
    try {
        const res = await fetch(`${API_BASE}/api/bookings`);
        if (!res.ok) throw new Error("Failed to load bookings");
        return await res.json();
    } catch (err) {
        console.error(err);
        showError("Failed to load bookings");
        return [];
    }
}

async function createBookingAPI(data) {
    try {
        const res = await fetch(`${API_BASE}/api/bookings`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        });
        if (!res.ok) {
            const error = await res.json();
            throw new Error(error.error || "Failed to create booking");
        }
        showSuccess("Booking created successfully!");
    } catch (err) {
        console.error(err);
        showError(err.message);
    }
}

async function updateBookingStatusAPI(bookingId, status) {
    try {
        const res = await fetch(`${API_BASE}/api/bookings/${bookingId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ status }),
        });
        if (!res.ok) throw new Error("Failed to update booking status");
        showSuccess(status === "CANCELLED" ? "Booking cancelled!" : "Booking updated!");
    } catch (err) {
        console.error(err);
        showError(err.message);
    }
}

async function deleteBookingAPI(bookingId) {
    try {
        const res = await fetch(`${API_BASE}/api/bookings/${bookingId}`, { method: "DELETE" });
        if (!res.ok) throw new Error("Failed to delete booking");
        showSuccess("Booking deleted successfully!");
    } catch (err) {
        console.error(err);
        showError(err.message);
    }
}

async function checkAvailabilityAPI(resourceId, startTime, endTime) {
    try {
        const res = await fetch(`${API_BASE}/api/availability`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ resourceId, startTime, endTime }),
        });
        return res.ok ? await res.json() : { error: "Failed to check availability" };
    } catch (err) {
        console.error(err);
        return { error: "Failed to check availability" };
    }
}

// ======= UI FUNCTIONS =======
function populateResourceDropdown() {
    if (!resourceDropdown) return;
    resourceDropdown.innerHTML = `
        <option value="">Select Resource</option>
        ${appState.resources.map(r =>
            `<option value="${r.id}">${r.name} (${r.type}) - ${r.location || ""}</option>`
        ).join("")}
    `;
}

function displayBookings() {
    if (!bookingsBody) return;

    if (!appState.bookings.length) {
        bookingsBody.innerHTML = `
            <tr>
                <td colspan="7" class="empty-state">
                    <i class="fas fa-calendar-times"></i>
                    <h3>No bookings found</h3>
                    <p>Create your first booking using the "Create New Booking" button above!</p>
                </td>
            </tr>
        `;
        return;
    }

    // Sort by start time, newest first
    const sortedBookings = [...appState.bookings].sort(
        (a, b) => new Date(b.startTime) - new Date(a.startTime)
    );

    bookingsBody.innerHTML = sortedBookings.map(b => {
        const resource = appState.resources.find(r => r.id === b.resourceId);
        const resourceName = resource ? resource.name : `Resource #${b.resourceId}`;
        return `
            <tr>
                <td><strong>#${b.id}</strong></td>
                <td>${resourceName}</td>
                <td><code>${b.userId}</code></td>
                <td>${formatDateTime(b.startTime)}</td>
                <td>${formatDateTime(b.endTime)}</td>
                <td><span class="status-badge ${b.status.toLowerCase()}">${b.status}</span></td>
                <td class="actions">
                    ${b.status === "CONFIRMED" ? `<button class="btn btn-sm btn-cancel">Cancel</button>` : ""}
                    <button class="btn btn-sm btn-delete">Delete</button>
                </td>
            </tr>
        `;
    }).join("");

    // Add event listeners for buttons after rendering
    bookingsBody.querySelectorAll(".btn-cancel").forEach((btn, idx) => {
        btn.addEventListener("click", () => cancelBooking(appState.bookings[idx].id));
    });
    bookingsBody.querySelectorAll(".btn-delete").forEach((btn, idx) => {
        btn.addEventListener("click", () => deleteBooking(appState.bookings[idx].id));
    });
}

function showAddBookingForm() {
    addBookingFormContainer.style.display = "block";
    bookingForm.reset();
    availabilityStatus.style.display = "none";
    addBookingFormContainer.scrollIntoView({ behavior: "smooth", block: "start" });

    // Default times
    const now = new Date();
    const start = new Date(now.getTime() + 60 * 60 * 1000); // 1 hour from now
    const end = new Date(start.getTime() + 60 * 60 * 1000);   // 2 hours from now
    document.getElementById("startTime").value = formatDateForInput(start);
    document.getElementById("endTime").value = formatDateForInput(end);
}

function hideAddBookingForm() {
    addBookingFormContainer.style.display = "none";
}

function formatDateForInput(date) {
    const pad = num => String(num).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth()+1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return "N/A";
    const date = dateTimeStr.includes("T") ? new Date(dateTimeStr) : new Date(dateTimeStr.replace(" ", "T"));
    if (isNaN(date.getTime())) return dateTimeStr;
    return date.toLocaleString("en-US", { year:"numeric", month:"short", day:"numeric", hour:"2-digit", minute:"2-digit" });
}

function showError(msg) {
    alert(`❌ ${msg}`);
}

function showSuccess(msg) {
    alert(`✅ ${msg}`);
}

// ======= EVENT HANDLERS =======
async function handleBookingForm(event) {
    event.preventDefault();
    const resourceId = parseInt(document.getElementById("resourceId").value);
    const userId = document.getElementById("userId").value;
    const startTime = document.getElementById("startTime").value;
    const endTime = document.getElementById("endTime").value;

    if (!resourceId || !userId || !startTime || !endTime) {
        showError("Please fill in all required fields");
        return;
    }

    if (new Date(endTime) <= new Date(startTime)) {
        showError("End time must be after start time");
        return;
    }

    const data = {
        resourceId,
        userId,
        startTime: startTime.replace("T", " "),
        endTime: endTime.replace("T", " "),
        status: "CONFIRMED",
    };

    await createBookingAPI(data);
    hideAddBookingForm();
    await loadBookings();
}

async function cancelBooking(bookingId) {
    if (!confirm("Are you sure you want to cancel this booking?")) return;
    await updateBookingStatusAPI(bookingId, "CANCELLED");
    await loadBookings();
}

async function deleteBooking(bookingId) {
    if (!confirm("Are you sure you want to delete this booking? This action cannot be undone.")) return;
    await deleteBookingAPI(bookingId);
    await loadBookings();
}

async function checkAvailability() {
    const resourceId = parseInt(document.getElementById("resourceId").value);
    const startTime = document.getElementById("startTime").value;
    const endTime = document.getElementById("endTime").value;
    if (!resourceId || !startTime || !endTime) return;

    if (new Date(endTime) <= new Date(startTime)) {
        availabilityStatus.innerHTML = `<div class="availability-unavailable">
            <i class="fas fa-exclamation-triangle"></i> End time must be after start time!
        </div>`;
        availabilityStatus.style.display = "block";
        return;
    }

    const result = await checkAvailabilityAPI(resourceId, startTime.replace("T"," "), endTime.replace("T"," "));
    if (result.error) {
        availabilityStatus.innerHTML = `<div class="availability-unavailable"><i class="fas fa-exclamation-circle"></i> ${result.error}</div>`;
    } else if (result.availableResources?.some(r => r.id === resourceId && r.isAvailable)) {
        availabilityStatus.innerHTML = `<div class="availability-available"><i class="fas fa-check-circle"></i> Resource is available!</div>`;
    } else {
        availabilityStatus.innerHTML = `<div class="availability-unavailable"><i class="fas fa-times-circle"></i> Resource is NOT available</div>`;
    }
    availabilityStatus.style.display = "block";
}

// ======= INITIALIZATION =======
async function loadResources() {
    appState.resources = await fetchResources();
    populateResourceDropdown();
}

async function loadBookings() {
    appState.bookings = await fetchBookings();
    displayBookings();
}

// DOMContentLoaded
document.addEventListener("DOMContentLoaded", async () => {
    await loadResources();
    await loadBookings();
    bookingForm.addEventListener("submit", handleBookingForm);
    document.getElementById("resourceId").addEventListener("change", checkAvailability);
    document.getElementById("startTime").addEventListener("change", checkAvailability);
    document.getElementById("endTime").addEventListener("change", checkAvailability);

    // Check URL params for pre-filled booking
    const params = new URLSearchParams(window.location.search);
    const resourceId = params.get("resourceId");
    const startTime = params.get("startTime");
    const endTime = params.get("endTime");
    if (resourceId) {
        showAddBookingForm();
        document.getElementById("resourceId").value = resourceId;
        if (startTime) document.getElementById("startTime").value = decodeURIComponent(startTime);
        if (endTime) document.getElementById("endTime").value = decodeURIComponent(endTime);
        if (startTime && endTime) setTimeout(checkAvailability, 300);
    }
});
