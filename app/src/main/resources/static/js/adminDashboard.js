// app/src/main/resources/static/js/adminDashboard.js

import { openModal } from "./components/modals.js";
import { getDoctors, filterDoctors, saveDoctor } from "./services/doctorServices.js";
import { createDoctorCard } from "./components/doctorCard.js";

// Open "Add Doctor" modal on button click
document.getElementById('addDocBtn').addEventListener('click', () => {
  openModal('addDoctor');
});

// Load all doctors and render cards on page load
window.addEventListener('DOMContentLoaded', loadDoctorCards);

/**
 * Fetch all doctors and render them as cards
 */
async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
  } catch (error) {
    console.error("Error loading doctors:", error);
  }
}

/**
 * Render a list of doctors as cards inside #content div
 * @param {Array} doctors
 */
function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";

  if (!doctors.length) {
    contentDiv.innerHTML = `<p>No doctors found.</p>`;
    return;
  }

  doctors.forEach(doctor => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });
}

/**
 * Handle filtering doctors on input or select change
 */
async function filterDoctorsOnChange() {
  try {
    const nameInput = document.getElementById("searchBar").value.trim();
    const timeFilter = document.getElementById("filterTime").value;
    const specialtyFilter = document.getElementById("filterSpecialty").value;

    // Normalize empty strings to null
    const name = nameInput || null;
    const time = timeFilter || null;
    const specialty = specialtyFilter || null;

    const result = await filterDoctors(name, time, specialty);

    if (result.doctors && result.doctors.length > 0) {
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

// Attach event listeners for search and filters
document.getElementById("searchBar").addEventListener("input", filterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", filterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", filterDoctorsOnChange);

/**
 * Collect data from add doctor modal form and save new doctor
 */
export async function adminAddDoctor() {
  try {
    const name = document.getElementById("doctorName").value.trim();
    const email = document.getElementById("doctorEmail").value.trim();
    const phone = document.getElementById("doctorPhone").value.trim();
    const password = document.getElementById("doctorPassword").value.trim();
    const specialty = document.getElementById("doctorSpecialty").value.trim();

    // Collect availability from checkboxes (assuming checkboxes have class 'availability-checkbox')
    const availabilityCheckboxes = document.querySelectorAll(".availability-checkbox");
    const availability = [];
    availabilityCheckboxes.forEach(box => {
      if (box.checked) availability.push(box.value);
    });

    // Simple validation
    if (!name || !email || !password || !specialty) {
      alert("Please fill in all required fields.");
      return;
    }

    const token = localStorage.getItem("token");
    if (!token) {
      alert("You must be logged in as admin to add a doctor.");
      return;
    }

    const doctor = {
      name,
      email,
      phone,
      password,
      specialty,
      availability
    };

    const response = await saveDoctor(doctor, token);
    if (response.success) {
      alert("Doctor added successfully.");
      // Close the modal - assuming a function closeModal(id) exists in modals.js
      // If not, just reload the doctor list here
      openModal.closeModal && openModal.closeModal('addDoctor'); // call closeModal if available
      await loadDoctorCards();
    } else {
      alert("Failed to add doctor: " + response.message);
    }
  } catch (error) {
    console.error("Error adding doctor:", error);
    alert("An unexpected error occurred. Please try again.");
  }
}


/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/
