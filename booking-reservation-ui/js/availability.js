// ================= CONFIG =================
const API_BASE = "https://booking-backend.onrender.com";

// ================= GLOBAL VARIABLES =================
let allResources = [];
let availabilityResults = [];
let isLoading = false;

// DOM Elements
const availabilityList = document.getElementById("availability-list");
const resultsSection = document.getElementById("resultsSection");
const tableBody = document.getElementById("availabilityBody");
const resourceFilter = document.getElementById("resourceFilter");
const checkStartTime = document.getElementById("checkStartTime");
const checkEndTime = document.getElementById("checkEndTime");
const notificationContainer = document.getElementById("notificationContainer");

// ================= INIT =================
document.addEventListener('DOMContentLoaded', () => {
    setupDefaultTimes();
    loadAllResources();
    setupKeyboardShortcuts();
});

// ================= SETUP FUNCTIONS =================
function setupDefaultTimes() {
    const now = new Date();
    const startTime = new Date(now.getTime() + 60 * 60 * 1000); // 1 hour from now
    const endTime = new Date(startTime.getTime() + 2 * 60 * 60 * 1000); // 3 hours total

    checkStartTime.value = formatDateForInput(startTime);
    checkEndTime.value = formatDateForInput(endTime);
}

function formatDateForInput(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2,'0');
    const day = String(date.getDate()).padStart(2,'0');
    const hours = String(date.getHours()).padStart(2,'0');
    const minutes = String(date.getMinutes()).padStart(2,'0');
    return `${year}-${month}-${day}T${hours}:${minutes}`;
}

// ================= RESOURCE FUNCTIONS =================
async function loadAllResources() {
    try {
        const res = await fetch(`${API_BASE}/api/resources`);
        if (!res.ok) throw new Error("Failed to fetch resources");
        allResources = await res.json();
        populateFilterOptions();
    } catch (error) {
        console.error(error);
        showNotification('Failed to load resources. Please refresh.', 'error');
    }
}

function populateFilterOptions() {
    if (!resourceFilter) return;
    const types = [...new Set(allResources.map(r => r.type))];
    resourceFilter.innerHTML = `<option value="">All Types</option>` + 
        types.map(t => `<option value="${t}">${t}</option>`).join('');
}

// ================= AVAILABILITY CHECK =================
async function checkAvailability(event) {
    event.preventDefault();
    if (isLoading) return;

    const start = checkStartTime.value;
    const end = checkEndTime.value;

    if (!start || !end) return showNotification('Select both start and end times', 'error');
    if (new Date(end) <= new Date(start)) return showNotification('End time must be after start', 'error');

    isLoading = true;
    showLoading(true);

    try {
        const res = await fetch(`${API_BASE}/api/availability?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`);
        if (!res.ok) throw new Error('Failed to fetch availability');

        const data = await res.json();
        availabilityResults = mapAvailabilityToResources(data);
        displayResults(availabilityResults);

        resultsSection.style.display = 'block';
        resultsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });

        showNotification('Availability check completed!', 'success');
    } catch (error) {
        console.error(error);
        showNotification('Failed to check availability. Try again.', 'error');
    } finally {
        isLoading = false;
        showLoading(false);
    }
}

// Map availability data to resources
function mapAvailabilityToResources(availData) {
    return allResources.map(resource => {
        const isAvailable = availData.some(a => a.resourceId === resource.id && a.isAvailable);
        return { ...resource, isAvailable };
    });
}

// ================= DISPLAY FUNCTIONS =================
function displayResults(results) {
    if (!tableBody) return;

    if (!results.length) {
        tableBody.innerHTML = `
        <tr>
            <td colspan="6" class="empty-state">
                <i class="fas fa-inbox"></i>
                <h3>No resources found</h3>
                <p>Try adjusting your time period or filters.</p>
            </td>
        </tr>`;
        updateSummaryCounts(results);
        return;
    }

    tableBody.innerHTML = results.map(r => {
        const statusClass = r.isAvailable ? 'available' : 'unavailable';
        const statusText = r.isAvailable ? 'Available' : 'Unavailable';
        const statusIcon = r.isAvailable ? 'fa-check-circle' : 'fa-times-circle';
        const typeClass = r.type?.toLowerCase() || 'other';

        return `
        <tr class="${statusClass}">
            <td><strong>${r.name}</strong></td>
            <td><span class="resource-type ${typeClass}">${r.type || 'Other'}</span></td>
            <td>${r.capacity || 'N/A'}</td>
            <td>${r.location || 'Not specified'}</td>
            <td>
                <span class="status-badge ${statusClass}">
                    <i class="fas ${statusIcon}"></i> ${statusText}
                </span>
            </td>
            <td class="actions">
                ${r.isAvailable ? `
                <button class="btn btn-sm btn-primary" onclick="bookResource(${r.id})">
                    <i class="fas fa-calendar-plus"></i> Book Now
                </button>` : `
                <button class="btn btn-sm btn-secondary" onclick="viewConflicts(${r.id})">
                    <i class="fas fa-info-circle"></i> View Details
                </button>`}
            </td>
        </tr>`;
    }).join('');

    updateSummaryCounts(results);
}

function updateSummaryCounts(results) {
    const availableCount = results.filter(r => r.isAvailable).length;
    const unavailableCount = results.length - availableCount;

    document.getElementById('availableCount').textContent = availableCount;
    document.getElementById('unavailableCount').textContent = unavailableCount;
    document.getElementById('totalCount').textContent = results.length;
}

// ================= FILTER =================
function filterResults() {
    const type = resourceFilter.value;
    const filtered = type ? availabilityResults.filter(r => r.type === type) : availabilityResults;
    displayResults(filtered);
}

// ================= CLEAR =================
function clearResults() {
    if (!resultsSection || !tableBody) return;

    resultsSection.style.display = 'none';
    tableBody.innerHTML = '';
    resourceFilter.value = '';
    setupDefaultTimes();

    document.getElementById('availableCount').textContent = '0';
    document.getElementById('unavailableCount').textContent = '0';
    document.getElementById('totalCount').textContent = '0';

    showNotification('Results cleared', 'info');
}

// ================= BOOKING =================
function bookResource(resourceId) {
    const resource = availabilityResults.find(r => r.id === resourceId);
    if (!resource) return;

    const start = checkStartTime.value;
    const end = checkEndTime.value;

    sessionStorage.setItem('prefilledBooking', JSON.stringify({
        resourceId: resource.id,
        resourceName: resource.name,
        startTime: start,
        endTime: end
    }));

    showNotification(`Redirecting to book "${resource.name}"...`, 'success');

    setTimeout(() => {
        window.location.href = `bookings.html?resourceId=${resourceId}&startTime=${encodeURIComponent(start)}&endTime=${encodeURIComponent(end)}`;
    }, 1000);
}

function viewConflicts(resourceId) {
    const resource = availabilityResults.find(r => r.id === resourceId);
    if (!resource) return;

    const start = formatDisplayTime(checkStartTime.value);
    const end = formatDisplayTime(checkEndTime.value);

    showNotification(`Resource "${resource.name}" is unavailable during ${start} - ${end}`, 'error');
}

function formatDisplayTime(dateTimeString) {
    if (!dateTimeString) return 'N/A';
    const date = new Date(dateTimeString);
    return date.toLocaleString('en-US', {
        weekday: 'short',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// ================= NOTIFICATIONS =================
function showNotification(message, type='info') {
    if (!notificationContainer) return;

    const id = 'notif-' + Date.now();
    const notif = document.createElement('div');
    notif.id = id;
    notif.className = `notification ${type}`;
    notif.innerHTML = `
        <i class="fas fa-${type==='success' ? 'check-circle' : type==='error' ? 'exclamation-circle' : 'info-circle'}"></i>
        <span>${message}</span>
    `;
    notificationContainer.appendChild(notif);

    setTimeout(() => notif.classList.add('show'), 10);
    setTimeout(() => {
        notif.classList.remove('show');
        setTimeout(() => notif.remove(), 400);
    }, 5000);
}

// ================= LOADING =================
function showLoading(show) {
    const checkBtn = document.querySelector('.btn-primary');
    const clearBtn = document.querySelector('.btn-secondary');

    if (!checkBtn || !clearBtn) return;

    if (show) {
        checkBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Checking...';
        checkBtn.disabled = true;
        clearBtn.disabled = true;
    } else {
        checkBtn.innerHTML = '<i class="fas fa-search"></i> Check Availability';
        checkBtn.disabled = false;
        clearBtn.disabled = false;
    }
}

// ================= KEYBOARD SHORTCUTS =================
function setupKeyboardShortcuts() {
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            const form = document.getElementById('availabilityForm');
            form?.requestSubmit();
        }
        if (e.key === 'Escape' && resultsSection.style.display !== 'none') {
            clearResults();
        }
        if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
            e.preventDefault();
            resourceFilter.focus();
        }
    });
}

// ================= GLOBAL EXPORT =================
window.checkAvailability = checkAvailability;
window.clearResults = clearResults;
window.bookResource = bookResource;
window.viewConflicts = viewConflicts;
window.filterResults = filterResults;
