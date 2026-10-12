import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../components/Toast";
import {
  FaGoogle,
  FaUserShield,
  FaUser,
  FaLock,
  FaEnvelope,
  FaEye,
  FaEyeSlash,
} from "react-icons/fa";
import "../styles/Login.css";
import api from "../services/api";

function Login() {
  const navigate = useNavigate();
  const { login, setUser } = useAuth();
  const { addToast } = useToast();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("USER"); // Default role: USER or ADMIN
  const [rememberMe, setRememberMe] = useState(false);
  const [showForgot, setShowForgot] = useState(false);
  const [forgotEmail, setForgotEmail] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [forgotLoading, setForgotLoading] = useState(false);

  // Restore remembered email on mount
  useEffect(() => {
    const savedEmail = localStorage.getItem("decisionhub_remember_email");
    if (savedEmail) {
      setEmail(savedEmail);
      setRememberMe(true);
    }
  }, []);

  const handleLogin = async (e) => {
    e.preventDefault();

    if (!email || !password) {
      addToast("Please fill in all fields.", "error");
      return;
    }

    setLoading(true);

    try {
      // 1. Post to Login Endpoint
      const response = await api.post("/auth/login", {
        email: email.trim(),
        password,
        role,
      });

      const token = response.data?.token;

      if (!token) {
        throw new Error("No authentication token received from server");
      }

      // Store JWT token
      localStorage.setItem("token", token);

      if (rememberMe) {
        localStorage.setItem("decisionhub_remember_email", email.trim());
      } else {
        localStorage.removeItem("decisionhub_remember_email");
      }

      // 2. Fetch logged-in user profile
      let user;
      try {
        const profileResponse = await api.get("/auth/profile", {
          headers: { Authorization: `Bearer ${token}` },
        });
        user = profileResponse.data;
      } catch (profileError) {
        console.warn("Could not fetch profile endpoint, falling back to JWT claims:", profileError);
        // Fallback: parse token payload
        try {
          const payload = JSON.parse(atob(token.split(".")[1]));
          user = {
            id: payload.id,
            email: payload.sub,
            username: payload.sub?.split("@")[0] || "User",
            role: payload.role || role,
            status: "ACTIVE",
          };
        } catch {
          user = { email: email.trim(), role, status: "ACTIVE" };
        }
      }

      // 3. Save session in AuthContext & localStorage
      if (typeof login === "function") {
        login(token, user);
      } else if (typeof setUser === "function") {
        localStorage.setItem("decisionhub-session", JSON.stringify(user));
        setUser(user);
      }

      addToast("Login Successful!", "success");

      // 4. Redirect to dashboard
      navigate("/dashboard");
    } catch (error) {
      console.error("Login attempt failed:", error);
      const errorMessage =
        error.response?.data?.error ||
        error.response?.data?.message ||
        (error.message === "Network Error"
          ? "Unable to reach server. Please ensure the backend is running at http://localhost:8080."
          : error.message || "Invalid email or password");

      addToast(errorMessage, "error");
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async (e) => {
    e.preventDefault();
    if (!forgotEmail) {
      addToast("Please enter your email.", "error");
      return;
    }

    setForgotLoading(true);
    try {
      const res = await api.post("/auth/forgot-password", {
        email: forgotEmail.trim(),
      });

      addToast(
        res.data?.message || `Password reset link sent to ${forgotEmail}`,
        "success"
      );
      setShowForgot(false);
      setForgotEmail("");
    } catch (error) {
      console.error("Forgot password request failed:", error);
      const errMsg =
        error.response?.data?.error ||
        error.response?.data?.message ||
        "Failed to send reset link. Please check your email.";
      addToast(errMsg, "error");
    } finally {
      setForgotLoading(false);
    }
  };

  return (
    <div className="login-container">
      <div className="glow-sphere main-glow"></div>
      <div className="glow-sphere sub-glow"></div>

      <div className="login-card glass-panel animate-fade-in">
        <div className="brand-header">
          <div className="brand-logo">DH</div>
          <h1>DecisionHub</h1>
          <p>Collaborative Polling & Decision Making Platform</p>
        </div>

        <form onSubmit={handleLogin} className="login-form">
          {/* Role Selection */}
          <div className="role-selection">
            <button
              type="button"
              className={`role-btn ${role === "USER" ? "active-user" : ""}`}
              onClick={() => setRole("USER")}
            >
              <FaUser /> User
            </button>
            <button
              type="button"
              className={`role-btn ${role === "ADMIN" ? "active-admin" : ""}`}
              onClick={() => setRole("ADMIN")}
            >
              <FaUserShield /> Admin
            </button>
          </div>

          <div className="input-group">
            <FaEnvelope className="input-icon" />
            <input
              type="email"
              placeholder="Email Address"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="input-group">
            <FaLock className="input-icon" />
            <input
              type={showPassword ? "text" : "password"}
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
            <button
              type="button"
              className="password-toggle-btn"
              onClick={() => setShowPassword(!showPassword)}
              tabIndex={-1}
            >
              {showPassword ? <FaEyeSlash /> : <FaEye />}
            </button>
          </div>

          <div className="login-options">
            <label className="checkbox-container">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                disabled={loading}
              />
              <span className="checkmark"></span>
              Remember Me
            </label>
            <button
              type="button"
              className="forgot-link"
              onClick={() => setShowForgot(true)}
              disabled={loading}
            >
              Forgot Password?
            </button>
          </div>

          <button
            type="submit"
            className="btn-primary login-btn"
            disabled={loading}
          >
            {loading ? "Logging in..." : `Login as ${role === "ADMIN" ? "Admin" : "User"}`}
          </button>
        </form>

        <div className="separator">
          <span>or continue with</span>
        </div>

        <button
          type="button"
          className="btn-secondary google-btn"
          onClick={() =>
            addToast("Google authentication is UI-only in mock mode.", "info")
          }
        >
          <FaGoogle /> Google Login
        </button>

        <p className="auth-footer">
          Don't have an account?
          <Link to="/register"> Register Now</Link>
        </p>
      </div>

      {showForgot && (
        <div className="forgot-modal-overlay">
          <div className="forgot-modal glass-panel animate-pop-in">
            <h2>Reset Password</h2>
            <p>Enter your email to receive a password reset link.</p>
            <form onSubmit={handleForgotPassword}>
              <div className="input-group">
                <FaEnvelope className="input-icon" />
                <input
                  type="email"
                  placeholder="name@domain.com"
                  value={forgotEmail}
                  onChange={(e) => setForgotEmail(e.target.value)}
                  required
                  disabled={forgotLoading}
                />
              </div>
              <div className="modal-actions">
                <button
                  type="button"
                  className="btn-secondary"
                  onClick={() => setShowForgot(false)}
                  disabled={forgotLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn-primary"
                  disabled={forgotLoading}
                >
                  {forgotLoading ? "Sending..." : "Send Link"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Login;