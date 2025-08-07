// doctorCard.js

// Import helper functions from service files
import { showBookingOverlay } from "../services/loggedPatient.js";
import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";

/**
 * Creates and returns a DOM element representing a doctor's card.
 * @param {Object} doctor - Object containing doctor information.
 * @returns {HTMLElement} - Fully constructed doctor card element.
 */
export function createDoctorCard(doctor) {
  // Create the main card container
  const card = document.createElement("div");
  card.classList.add("doctor-card");

  // Fetch the current user's role
  const role = localStorage.getItem("userRole");

  // Create the doctor info section
  const infoDiv = document.createElement("div");
  infoDiv.classList.add("doctor-info");

  const name = document.createElement("h3");
  name.textContent = doctor.name;

  const specialization = document.createElement("p");
  specialization.textContent = `Specialty: ${doctor.specialty}`;

  const email = document.createElement("p");
  email.textContent = `Email: ${doctor.email}`;

  const availability = document.createElement("p");
  availability.textContent = `Available Times: ${doctor.availableTimes.join(", ")}`;

  // Append doctor details to the info container
  infoDiv.appendChild(name);
  infoDiv.appendChild(specialization);
  infoDiv.appendChild(email);
  infoDiv.appendChild(availability);

  // Create button container
  const actionsDiv = document.createElement("div");
  actionsDiv.classList.add("card-actions");

  // === ADMIN ROLE ===
  if (role === "admin") {
    const removeBtn = document.createElement("button");
    removeBtn.textContent = "Delete";
    removeBtn.classList.add("btn-delete");

    removeBtn.addEventListener("click", async () => {
      const confirmed = confirm(`Are you sure you want to delete Dr. ${doctor.name}?`);
      if (!confirmed) return;

      const token = localStorage.getItem("token");
      if (!token) return alert("Unauthorized action.");

      try {
        const response = await deleteDoctor(doctor.id, token);
        alert(response.message || "Doctor deleted.");
        card.remove();
      } catch (error) {
        console.error("Delete error:", error);
        alert("Failed to delete doctor.");
      }
    });

    actionsDiv.appendChild(removeBtn);
  }

  // === PATIENT ROLE (not logged in) ===
  else if (role === "patient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.classList.add("btn-book");

    bookNow.addEventListener("click", () => {
      alert("Please log in as a patient to book an appointment.");
    });

    actionsDiv.appendChild(bookNow);
  }

  // === LOGGED-IN PATIENT ===
  else if (role === "loggedPatient") {
    const bookNow = document.createElement("button");
    bookNow.textContent = "Book Now";
    bookNow.classList.add("btn-book");

    bookNow.addEventListener("click", async (e) => {
      const token = localStorage.getItem("token");
      if (!token) return alert("Session expired. Please log in again.");

      try {
        const patientData = await getPatientData(token);
        showBookingOverlay(e, doctor, patientData);
      } catch (error) {
        console.error("Booking error:", error);
        alert("Unable to fetch patient data.");
      }
    });

    actionsDiv.appendChild(bookNow);
  }

  // Assemble the card
  card.appendChild(infoDiv);
  card.appendChild(actionsDiv);

  return card;
}



/*
Import the overlay function for booking appointments from loggedPatient.js

  Import the deleteDoctor API function to remove doctors (admin role) from docotrServices.js

  Import function to fetch patient details (used during booking) from patientServices.js

  Function to create and return a DOM element for a single doctor card
    Create the main container for the doctor card
    Retrieve the current user role from localStorage
    Create a div to hold doctor information
    Create and set the doctor’s name
    Create and set the doctor's specialization
    Create and set the doctor's email
    Create and list available appointment times
    Append all info elements to the doctor info container
    Create a container for card action buttons
    === ADMIN ROLE ACTIONS ===
      Create a delete button
      Add click handler for delete button
     Get the admin token from localStorage
        Call API to delete the doctor
        Show result and remove card if successful
      Add delete button to actions container
   
    === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
      Create a book now button
      Alert patient to log in before booking
      Add button to actions container
  
    === LOGGED-IN PATIENT ROLE ACTIONS === 
      Create a book now button
      Handle booking logic for logged-in patient   
        Redirect if token not available
        Fetch patient data with token
        Show booking overlay UI with doctor and patient info
      Add button to actions container
   
  Append doctor info and action buttons to the car
  Return the complete doctor card element
*/
