// doctorDashboard.js
import { getAllAppointments } from "./services/appointmentRecordService.js";
import { createPatientRow } from "./components/patientRows.js";

const tableBody = document.getElementById("patientTableBody");
const searchBar = document.getElementById("searchBar");
const todayBtn = document.getElementById("todayButton");
const datePicker = document.getElementById("datePicker");

let selectedDate = new Date().toISOString().slice(0, 10);
let patientName = "null";  // Backend expects "null" string when no filter
const token = localStorage.getItem("token");

// Set date picker initial value
datePicker.value = selectedDate;

// Search input event
searchBar.addEventListener("input", () => {
  const input = searchBar.value.trim();
  patientName = input.length > 0 ? input : "null";
  loadAppointments();
});

// Today button click event
todayBtn.addEventListener("click", () => {
  selectedDate = new Date().toISOString().slice(0, 10);
  datePicker.value = selectedDate;
  loadAppointments();
});

// Date picker change event
datePicker.addEventListener("change", () => {
  selectedDate = datePicker.value;
  loadAppointments();
});

// Load and render appointments
async function loadAppointments() {
  try {
    const appointments = await getAllAppointments(selectedDate, patientName, token);
    tableBody.innerHTML = "";

    if (!appointments || appointments.length === 0) {
      const noDataRow = document.createElement("tr");
      noDataRow.innerHTML = `<td colspan="5" class="text-center">No Appointments found for today.</td>`;
      tableBody.appendChild(noDataRow);
      return;
    }

    appointments.forEach(app => {
      // Construct patient info and pass required params
      const patient = {
        id: app.patientId || app.patient?.id || null,
        name: app.patientName || app.patient?.name || "Unknown",
        phone: app.patientPhone || app.patient?.phone || "",
        email: app.patientEmail || app.patient?.email || ""
      };

      // Pass patient object and appointmentId, doctorId to match createPatientRow signature
      const row = createPatientRow(patient, app.appointmentId || app.id, app.doctorId || null);
      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error loading appointments:", error);
    tableBody.innerHTML = `<tr><td colspan="5" class="text-center text-danger">Error loading appointments. Try again later.</td></tr>`;
  }
}

window.addEventListener("DOMContentLoaded", () => {
  if (typeof renderContent === "function") {
    renderContent();
  }
  loadAppointments();
});


/*
  Import getAllAppointments to fetch appointments from the backend
  Import createPatientRow to generate a table row for each patient appointment


  Get the table body where patient rows will be added
  Initialize selectedDate with today's date in 'YYYY-MM-DD' format
  Get the saved token from localStorage (used for authenticated API calls)
  Initialize patientName to null (used for filtering by name)


  Add an 'input' event listener to the search bar
  On each keystroke:
    - Trim and check the input value
    - If not empty, use it as the patientName for filtering
    - Else, reset patientName to "null" (as expected by backend)
    - Reload the appointments list with the updated filter


  Add a click listener to the "Today" button
  When clicked:
    - Set selectedDate to today's date
    - Update the date picker UI to match
    - Reload the appointments for today


  Add a change event listener to the date picker
  When the date changes:
    - Update selectedDate with the new value
    - Reload the appointments for that specific date


  Function: loadAppointments
  Purpose: Fetch and display appointments based on selected date and optional patient name

  Step 1: Call getAllAppointments with selectedDate, patientName, and token
  Step 2: Clear the table body content before rendering new rows

  Step 3: If no appointments are returned:
    - Display a message row: "No Appointments found for today."

  Step 4: If appointments exist:
    - Loop through each appointment and construct a 'patient' object with id, name, phone, and email
    - Call createPatientRow to generate a table row for the appointment
    - Append each row to the table body

  Step 5: Catch and handle any errors during fetch:
    - Show a message row: "Error loading appointments. Try again later."


  When the page is fully loaded (DOMContentLoaded):
    - Call renderContent() (assumes it sets up the UI layout)
    - Call loadAppointments() to display today's appointments by default
*/
