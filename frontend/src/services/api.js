import axios from "axios";

// Determine base API URL with robust fallback
export const getApiBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_URL;
  if (envUrl && envUrl.trim() !== "") {
    return envUrl.endsWith("/api") ? envUrl : `${envUrl.replace(/\/$/, "")}/api`;
  }
  return "http://localhost:8080/api";
};

// Retrieve token isolated to the current browser tab
export const getAuthToken = () => {
  return (
    sessionStorage.getItem("token") ||
    sessionStorage.getItem("authToken") ||
    sessionStorage.getItem("jwt")
  );
};

const api = axios.create({
  baseURL: getApiBaseUrl(),
});

// Automatically send JWT token with every request (isolated per browser tab)
api.interceptors.request.use(
  (config) => {
    const token = getAuthToken();

    if (token) {
      if (config.headers && typeof config.headers.set === "function") {
        config.headers.set("Authorization", `Bearer ${token}`);
      } else {
        config.headers = config.headers || {};
        config.headers.Authorization = `Bearer ${token}`;
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle 401 unauthorized & 403 blocked user revocation
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const status = error.response.status;
      const isAuthEndpoint =
        error.config?.url?.includes("/auth/login") ||
        error.config?.url?.includes("/auth/register") ||
        error.config?.url?.includes("/auth/oauth2");

      if (status === 401 && !isAuthEndpoint) {
        sessionStorage.removeItem("token");
        sessionStorage.removeItem("authToken");
        sessionStorage.removeItem("jwt");
        sessionStorage.removeItem("decisionhub-session");
      } else if (
        status === 403 &&
        error.response.data?.error &&
        typeof error.response.data.error === "string" &&
        error.response.data.error.toLowerCase().includes("blocked")
      ) {
        // Immediate revocation: clear tab session when account is blocked
        sessionStorage.removeItem("token");
        sessionStorage.removeItem("authToken");
        sessionStorage.removeItem("jwt");
        sessionStorage.removeItem("decisionhub-session");
        if (window.location.pathname !== "/login") {
          window.location.href = "/login?blocked=true";
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;
