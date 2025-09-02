import axios from "axios";

// const BACKEND_URI = import.meta.env.VITE_BACKEND_URI;

export default axios.create({
  baseURL: "/api",
});

export const authedCall = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});
