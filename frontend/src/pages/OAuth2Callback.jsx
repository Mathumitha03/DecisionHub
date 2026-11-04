import { useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/Toast";
import api from "../services/api";

export default function OAuth2Callback() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();
  const { addToast } = useToast();
  const processedRef = useRef(false);

  useEffect(() => {
    if (processedRef.current) return;
    processedRef.current = true;

    const processOAuth2 = async () => {
      const params = new URLSearchParams(location.search);
      const code = params.get("code");
      const state = params.get("state");
      const error = params.get("error");
      const errorDescription = params.get("error_description");

      if (error) {
        if (error === "access_denied") {
          addToast("Google sign-in was cancelled.", "info");
        } else {
          addToast(errorDescription || `Google sign-in error: ${error}`, "error");
        }
        navigate("/login", { replace: true });
        return;
      }

      if (!code) {
        addToast("No authorization code received from Google.", "error");
        navigate("/login", { replace: true });
        return;
      }

      const storedState = sessionStorage.getItem("dh_oauth_state");
      sessionStorage.removeItem("dh_oauth_state");

      if (storedState && state && storedState !== state) {
        console.warn("OAuth state mismatch:", { storedState, state });
        addToast("Security verification failed. Please try signing in again.", "error");
        navigate("/login", { replace: true });
        return;
      }

      try {
        const response = await api.post("/auth/oauth2/callback", {
          code,
          state,
          redirectUri: window.location.origin + "/oauth2/callback",
        });

        const { token, user } = response.data || {};

        if (!token) {
          throw new Error("No authentication token received from server");
        }

        // Store tab-isolated token
        sessionStorage.setItem("token", token);

        // Fetch complete profile if user payload is partial
        let finalUser = user;
        try {
          const profileRes = await api.get("/auth/profile", {
            headers: { Authorization: `Bearer ${token}` },
          });
          if (profileRes.status === 200 && profileRes.data) {
            finalUser = profileRes.data;
          }
        } catch (pErr) {
          console.warn("Could not fetch user profile, using response user:", pErr);
        }

        login(token, finalUser);
        addToast("Signed in with Google successfully!", "success");
        navigate("/dashboard", { replace: true });
      } catch (err) {
        console.error("OAuth2 callback failed:", err);
        const errMsg =
          err.response?.data?.error ||
          err.response?.data?.message ||
          (err.message === "Network Error"
            ? "Unable to reach server. Please ensure the backend is running."
            : "Google authentication failed. Please try again.");

        addToast(errMsg, "error");
        navigate("/login", { replace: true });
      }
    };

    processOAuth2();
  }, [location, login, navigate, addToast]);

  return (
    <div className="login-container" style={{ display: "flex", alignItems: "center", justifyContent: "center", minHeight: "100vh" }}>
      <div className="glow-sphere main-glow"></div>
      <div className="glow-sphere sub-glow"></div>

      <div
        className="glass-panel animate-fade-in"
        style={{
          padding: "3rem",
          maxWidth: "420px",
          width: "90%",
          textAlign: "center",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          gap: "1.25rem",
        }}
      >
        <div className="brand-logo" style={{ width: "56px", height: "56px", fontSize: "1.5rem" }}>
          DH
        </div>
        <h2 style={{ margin: 0, fontSize: "1.3rem", fontWeight: 700 }}>
          Authenticating with Google
        </h2>
        <p style={{ margin: 0, color: "var(--text-secondary)", fontSize: "0.95rem" }}>
          Please wait while we verify your Google credentials...
        </p>
        <div
          style={{
            marginTop: "0.5rem",
            width: "36px",
            height: "36px",
            border: "3px solid rgba(255, 255, 255, 0.15)",
            borderTopColor: "var(--accent-purple)",
            borderRadius: "50%",
            animation: "spin 1s linear infinite",
          }}
        />
      </div>
    </div>
  );
}
