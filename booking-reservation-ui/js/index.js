const API_BASE = "https://booking-backend.onrender.com";

const resourcesCountEl = document.getElementById("resourcesCount");
const bookingsCountEl = document.getElementById("bookingsCount");
const navLinks = document.querySelectorAll("header nav a");

// Highlight active nav link
navLinks.forEach(link => {
    if (link.href === window.location.href) link.classList.add("active");
    else link.classList.remove("active");
});

// Load dashboard stats
async function loadDashboardStats() {
    try {
        const [resourcesRes, bookingsRes] = await Promise.all([
            fetch(`${API_BASE}/api/resources`),
            fetch(`${API_BASE}/api/bookings`)
        ]);

        const resources = await resourcesRes.json();
        const bookings = await bookingsRes.json();

        resourcesCountEl.textContent = `${resources.length} resource(s) available`;
        bookingsCountEl.textContent = `${bookings.length} booking(s) made`;
    } catch (err) {
        console.error(err);
        resourcesCountEl.textContent = "Error loading resources";
        bookingsCountEl.textContent = "Error loading bookings";
    }
}

document.addEventListener("DOMContentLoaded", loadDashboardStats);
