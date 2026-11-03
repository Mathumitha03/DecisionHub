import { createContext, useContext, useState, useEffect } from "react";
import api, { getAuthToken } from "../services/api";

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Read session isolated to this browser tab
    const storedUser = sessionStorage.getItem("decisionhub-session");

    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (err) {
        console.error("Failed to parse stored session:", err);
        sessionStorage.removeItem("decisionhub-session");
      }
    }

    const activeTheme = localStorage.getItem("decisionhub-theme") || "dark";
    document.documentElement.setAttribute("data-theme", activeTheme);

    setLoading(false);
  }, []);

  const login = (token, userData) => {
    if (token) {
      sessionStorage.setItem("token", token);
    }
    if (userData) {
      sessionStorage.setItem("decisionhub-session", JSON.stringify(userData));
      setUser(userData);
    }
  };

  const refreshProfile = async () => {
    try {
      const t = getAuthToken();

      if (t) {
        const res = await api.get("/auth/profile", {
          headers: { Authorization: `Bearer ${t}` },
        });

        if (res.status === 200) {
          const freshUser = res.data;
          sessionStorage.setItem("decisionhub-session", JSON.stringify(freshUser));
          setUser(freshUser);
          return freshUser;
        }
      }
    } catch (err) {
      console.error("Failed to refresh user profile:", err);
    }
  };

  const logout = () => {
    sessionStorage.removeItem("token");
    sessionStorage.removeItem("authToken");
    sessionStorage.removeItem("jwt");
    sessionStorage.removeItem("decisionhub-session");
    sessionStorage.removeItem("dh_oauth_state");
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        setUser,
        login,
        logout,
        refreshProfile,
        loading,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }

  return context;
};

export default AuthContext;
