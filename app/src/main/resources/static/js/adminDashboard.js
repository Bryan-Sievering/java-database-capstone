// JavaScript
// Adjust import paths relative to /js/services/adminDashboard.js
import { openModal } from "../components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./doctorServices.js";
import { createDoctorCard } from "../components/doctorCard.js";

// Guard missing button to avoid runtime error
const addBtn = document.getElementById("addDocBtn");
if (addBtn) {
  addBtn.addEventListener("click", () => {
    openModal("addDoctor");
  });
}

// Load all doctors and render cards on page load
window.addEventListener("DOMContentLoaded", loadDoctorCards);

async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Error loading doctors:", error);
  }
}

function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";

  if (!doctors || !doctors.length) {
    contentDiv.innerHTML = `<p>No doctors found.</p>`;
    return;
  }

  doctors.forEach((doctor) => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

async function filterDoctorsOnChange() {
  try {
    const nameInput = document.getElementById("searchBar")?.value.trim() || "";
    // Match the IDs used in the HTML
    const timeFilter = document.getElementById("timeFilter")?.value || "";
    const specialtyFilter = document.getElementById("specialtyFilter")?.value || "";

    const name = nameInput || null;
    const time = timeFilter || null;
    const specialty = specialtyFilter || null;

    const result = await filterDoctors(name, time, specialty);

    if (result?.doctors?.length) {
      renderDoctorCards(result.doctors);
    } else {
      const contentDiv = document.getElementById("content");
      contentDiv.innerHTML = `<p>No doctors found with the given filters.</p>`;
    }
  } catch (error) {
    alert("Failed to filter doctors, please try again.");
    console.error("Filter error:", error);
  }
}

// Attach event listeners for search and filters (match IDs in HTML)
document.getElementById("searchBar")?.addEventListener("input", filterDoctorsOnChange);
document.getElementById("timeFilter")?.addEventListener("change", filterDoctorsOnChange);
document.getElementById("specialtyFilter")?.addEventListener("change", filterDoctorsOnChange);

// Exported function used by modal form to add a doctor
export async function adminAddDoctor() {
  try {
    const name = document.getElementById("doctorName").value.trim();
    const email = document.getElementById("doctorEmail").value.trim();
    const phone = document.getElementById("doctorPhone").value.trim();
    const password = document.getElementById("doctorPassword").value.trim();
    const specialty = document.getElementById("doctorSpecialty").value.trim();

    const availabilityCheckboxes = document.querySelectorAll(".availability-checkbox");
    const availability = [];
    availabilityCheckboxes.forEach((box) => {
      if (box.checked) availability.push(box.value);
    });

    if (!name || !email || !password || !specialty) {
      alert("Please fill in all required fields.");
      return;
    }

    const token = localStorage.getItem("token");
    if (!token) {
      alert("You must be logged in as admin to add a doctor.");
      return;
    }

    const doctor = { name, email, phone, password, specialty, availability };

    const response = await saveDoctor(doctor, token);
    if (response.success) {
      alert("Doctor added successfully.");
      if (openModal.closeModal) openModal.closeModal("addDoctor");
      await loadDoctorCards();
    } else {
      alert("Failed to add doctor: " + response.message);
    }
  } catch (error) {
    console.error("Error adding doctor:", error);
    alert("An unexpected error occurred. Please try again.");
  }
}