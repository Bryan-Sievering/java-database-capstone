// app/src/main/resources/static/js/services/patientServices.js

import { API_BASE_URL } from "../config/config.js";

const PATIENT_API = API_BASE_URL + '/patient';

/**
 * Register a new patient
 * @param {Object} data - Patient details (name, email, password, etc.)
 * @returns {Promise<{success: boolean, message: string}>}
 */
export async function patientSignup(data) {
  try {
    const response = await fetch(`${PATIENT_API}/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    const result = await response.json();
    return {
      success: response.ok,
      message: result.message || (response.ok ? "Signup successful" : "Signup failed")
    };
  } catch (error) {
    console.error("Patient signup error:", error);
    return { success: false, message: "Error during signup" };
  }
}

/**
 * Login patient with credentials
 * @param {Object} data - Credentials (email, password)
 * @returns {Promise<Response>} Full fetch response (handle status, token extraction in UI)
 */
export async function patientLogin(data) {
  try {
    const response = await fetch(`${PATIENT_API}/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data)
    });
    return response;
  } catch (error) {
    console.error("Patient login error:", error);
    throw error;
  }
}

/**
 * Get patient details using auth token
 * @param {string} token - Authentication token
 * @returns {Promise<Object|null>} Patient data or null if failed
 */
export async function getPatientData(token) {
  try {
    const response = await fetch(`${PATIENT_API}/details`, {
      method: "GET",
      headers: { "Authorization": `Bearer ${token}` }
    });
    if (response.ok) {
      return await response.json();
    } else {
      return null;
    }
  } catch (error) {
    console.error("Failed to get patient data:", error);
    return null;
  }
}

/**
 * Fetch patient appointments for patient or doctor dashboard
 * @param {string} id - Patient ID
 * @param {string} token - Auth token
 * @param {string} user - User role ("patient" or "doctor")
 * @returns {Promise<Array|null>} Array of appointments or null if failed
 */
export async function getPatientAppointments(id, token, user) {
  try {
    const url = `${PATIENT_API}/appointments/${id}/${user}`;
    const response = await fetch(url, {
      method: "GET",
      headers: { "Authorization": `Bearer ${token}` }
    });
    if (response.ok) {
      const data = await response.json();
      return data.appointments || [];
    } else {
      console.error("Failed to fetch appointments:", response.status);
      return null;
    }
  } catch (error) {
    console.error("Error fetching appointments:", error);
    return null;
  }
}

/**
 * Filter appointments by condition and name
 * @param {string} condition - Status filter (e.g., pending, consulted)
 * @param {string} name - Name filter
 * @param {string} token - Auth token
 * @returns {Promise<Array>} Filtered appointments or empty array on failure
 */
export async function filterAppointments(condition, name, token) {
  try {
    const url = `${PATIENT_API}/appointments/filter/${encodeURIComponent(condition)}/${encodeURIComponent(name)}`;
    const response = await fetch(url, {
      method: "GET",
      headers: { "Authorization": `Bearer ${token}` }
    });
    if (response.ok) {
      const data = await response.json();
      return data.appointments || [];
    } else {
      console.error("Failed to filter appointments:", response.status);
      return [];
    }
  } catch (error) {
    alert("Error filtering appointments, please try again.");
    console.error("Filter appointments error:", error);
    return [];
  }
}


/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/
