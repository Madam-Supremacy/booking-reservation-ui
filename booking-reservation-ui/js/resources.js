// ======= CONFIG =======
const API_BASE = "https://booking-backend.onrender.com";

// ======= STATE =======
const appState = {
    resources: [],
    isEditMode: false,
    resourceToDelete: null,
    editingResourceId: null,
};

// ======= DOM ELEMENTS =======
const resourceList = document.getElementById("resource-list");
const resourcesBody = document.getElementById("resourcesBody");
const summaryCards = document.getElementById("summaryCards");
const addResourceForm = document.getElementById("addResourceForm");
const resourceIdInput = document.getElementById("resourceId");
const resourceNameInput = document.getElementById("resourceName");
const resourceTypeInput = document.getElementById("resourceType");
const capacityInput = document.getElementById("capacity");
const locationInput = document.getElementById("location");
const descriptionInput = document.getElementById("description");
const deleteModal = document.getElementById("deleteModal");
const deleteModalBody = document.getElementById("deleteModalBody");
const notificationContainer = document.getElementById("notificationContainer");

// ======= API FUNCTIONS =======
async function fetchResources() {
    try {
        const res = await fetch(`${API_BASE}/api/resources`);
        if (!res.ok) throw new Error("Failed to fetch resources");
        return await res.json();
    } catch (err) {
        showNotification(err.message, "error");
        console.error(err);
        return [];
    }
}

async function createResource(data) {
    try {
        await fetch(`${API_BASE}/api/resources`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        });
        showNotification("Resource created successfully", "success");
    } catch (err) {
        showNotification("Failed to create resource", "error");
        console.error(err);
    }
}

async function updateResource(id, data) {
    try {
        await fetch(`${API_BASE}/api/resources/${id}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(data),
        });
        showNotification("Resource updated successfully", "success");
    } catch (err) {
        showNotification("Failed to update resource", "error");
        console.error(err);
    }
}

async function deleteResource(id) {
    try {
        await fetch(`${API_BASE}/api/resources/${id}`, { method: "DELETE" });
        showNotification("Resource deleted", "success");
    } catch (err) {
        showNotification("Failed to delete resource", "error");
        console.error(err);
    }
}

// ======= UI FUNCTIONS =======
function displayResources() {
    resourcesBody.innerHTML = "";
    appState.resources.forEach(r => {
        const tr = document.createElement("tr");

        tr.innerHTML = `
            <td>#${r.id}</td>
            <td>${r.name}</td>
            <td><span class="resource-type ${r.type.toLowerCase()}">${r.type}</span></td>
            <td>${r.capacity}</td>
            <td>${r.location || '-'}</td>
            <td>${r.description || '-'}</td>
            <td>
                <button class="btn btn-sm btn-warning">Edit</button>
                <button class="btn btn-sm btn-danger">Delete</button>
            </td>
        `;

        // Edit button
        tr.querySelector(".btn-warning").addEventListener("click", () => editResource(r));

        // Delete button
        tr.querySelector(".btn-danger").addEventListener("click", () => showDeleteModal(r));

        resourcesBody.appendChild(tr);
    });
}

function updateSummaryCards() {
    summaryCards.innerHTML = `
        <div class="summary-card">
            <div class="summary-icon"><i class="fas fa-layer-group"></i></div>
            <div><strong>Total Resources</strong><p>${appState.resources.length}</p></div>
        </div>
    `;
}

function showAddResourceForm() {
    appState.isEditMode = false;
    addResourceForm.reset();
    addResourceForm.style.display = "block";
}

function hideAddResourceForm() {
    addResourceForm.style.display = "none";
}

function editResource(resource) {
    appState.isEditMode = true;
    appState.editingResourceId = resource.id;

    resourceIdInput.value = resource.id;
    resourceNameInput.value = resource.name;
    resourceTypeInput.value = resource.type;
    capacityInput.value = resource.capacity;
    locationInput.value = resource.location;
    descriptionInput.value = resource.description;

    addResourceForm.style.display = "block";
}

function showDeleteModal(resource) {
    appState.resourceToDelete = resource.id;
    deleteModalBody.innerHTML = `Delete <strong>${resource.name}</strong>?`;
    deleteModal.style.display = "flex";
}

function closeDeleteModal() {
    deleteModal.style.display = "none";
}

function showNotification(msg, type = "info") {
    const n = document.createElement("div");
    n.className = `notification ${type}`;
    n.textContent = msg;
    notificationContainer.appendChild(n);
    setTimeout(() => n.classList.add("show"), 50);
    setTimeout(() => n.remove(), 3000);
}

// ======= EVENT HANDLERS =======
async function handleResourceForm(e) {
    e.preventDefault();

    const data = {
        name: resourceNameInput.value,
        type: resourceTypeInput.value,
        capacity: parseInt(capacityInput.value, 10),
        location: locationInput.value,
        description: descriptionInput.value,
    };

    if (appState.isEditMode) {
        await updateResource(appState.editingResourceId, data);
    } else {
        await createResource(data);
    }

    hideAddResourceForm();
    await loadResources();
}

async function confirmDelete() {
    if (appState.resourceToDelete) {
        await deleteResource(appState.resourceToDelete);
        closeDeleteModal();
        await loadResources();
    }
}

// ======= INIT =======
async function loadResources() {
    appState.resources = await fetchResources();
    displayResources();
    updateSummaryCards();
}

// DOMContentLoaded
document.addEventListener("DOMContentLoaded", () => {
    loadResources();
    addResourceForm.addEventListener("submit", handleResourceForm);
});
