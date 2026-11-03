import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useToast } from "../components/Toast";
import { FaUser, FaEnvelope, FaLock, FaUserShield, FaGoogle } from "react-icons/fa";
import "../styles/Register.css";
import api from "../services/api";

function Register() {
  const navigate = useNavigate();
  const { addToast } = useToast();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("USER"); // Option to select Admin/User on register
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleRegister = async (e) => {
    e.preventDefault();

    if (!username || !email || !password || !confirmPassword) {
      addToast("Please fill in all fields.", "error");
      return;
    }

    if (password !== confirmPassword) {
      addToast("Passwords do not match.", "error");
      return;
    }

    if (password.length < 6) {
      addToast("Password must be at least 6 characters.", "error");
      return;
    }

    setLoading(true);

    try {
      await api.post("/auth/register", {
        username: username.trim(),
        email: email.trim().toLowerCase(),
        password,
      });

      addToast("Account created successfully! Please login.", "success");
      navigate("/login");
    } catch (error) {
      console.error("Registration failed:", error);
      addToast(
        error.response?.data?.error ||
        error.response?.data?.message ||
        (error.message === "Network Error"
          ? "Unable to reach server. Please ensure the backend is running."
          : error.message || "Registration failed."),
        "error"
      );
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = () => {
    try {
      const state = crypto.randomUUID
        ? crypto.randomUUID()
        : Math.random().toString(36).substring(2) + Date.now().toString(36);
      sessionStorage.setItem("dh_oauth_state", state);

      const baseUrl = api.defaults.baseURL || "http://localhost:8080/api";
      window.location.href = `${baseUrl}/auth/oauth2/google?state=${encodeURIComponent(state)}`;
    } catch (err) {
      console.error("Failed to initiate Google OAuth:", err);
      addToast("Failed to initiate Google login. Please try again.", "error");
    }
  };

  return (
    <div className="login-container">
      <div className="glow-sphere main-glow"></div>
      <div className="glow-sphere sub-glow"></div>

      <div className="login-card glass-panel animate-fade-in">
        <div className="brand-header">
          <div className="brand-logo">DH</div>
          <h1>Create Account</h1>
          <p>Join DecisionHub and start polling today</p>
        </div>

        <form onSubmit={handleRegister} className="login-form">
          {/* Role Selection */}
          <div className="role-selection">
            <button
              type="button"
              className={`role-btn ${role === "USER" ? "active-user" : ""}`}
              onClick={() => setRole("USER")}
              disabled={loading}
            >
              <FaUser /> User
            </button>
            <button
              type="button"
              className={`role-btn ${role === "ADMIN" ? "active-admin" : ""}`}
              onClick={() => setRole("ADMIN")}
              disabled={loading}
            >
              <FaUserShield /> Admin
            </button>
          </div>

          <div className="input-group">
            <FaUser className="input-icon" />
            <input
              type="text"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={loading}
            />
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
              type="password"
              placeholder="Password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <div className="input-group">
            <FaLock className="input-icon" />
            <input
              type="password"
              placeholder="Confirm Password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              disabled={loading}
            />
          </div>

          <button type="submit" className="btn-primary login-btn" disabled={loading}>
            {loading ? "Registering..." : `Register as ${role === "ADMIN" ? "Admin" : "User"}`}
          </button>
        </form>

        <div className="separator">
          <span>or continue with</span>
        </div>

        <button
          type="button"
          className="btn-secondary google-btn"
          onClick={handleGoogleLogin}
          disabled={loading}
        >
          <FaGoogle /> Continue with Google
        </button>

        <p className="auth-footer">
          Already have an account?
          <Link to="/login"> Login Here</Link>
        </p>
      </div>
    </div>
  );
}

export default Register;
