import { useState } from "react";
import type { FormEvent } from "react";
import axios from "axios";
import { ArrowRight } from "lucide-react";
import { api } from "../../api";
import type { Role, User } from "../../types";

interface AuthPageProps {
  initialMode: "login" | "signup";
  onSuccess: (token: string, user: User) => void;
  onBack: () => void;
}

export function AuthPage({ initialMode, onSuccess, onBack }: AuthPageProps) {
  const [authMode, setAuthMode] = useState(initialMode);
  const [message, setMessage] = useState("");
  const [form, setForm] = useState({
    identifier: "user@example.com",
    email: "",
    username: "",
    fullName: "",
    password: "password123",
    role: "USER" as Role,
  });

  async function submit(event: FormEvent) {
    event.preventDefault();
    setMessage("");
    try {
      const response = authMode === "login"
        ? await api.post("/auth/login", { identifier: form.identifier, password: form.password })
        : await api.post("/auth/register", {
          email: form.email,
          username: form.username,
          fullName: form.fullName,
          password: form.password,
          role: form.role,
        });
      onSuccess(response.data.token, response.data.user);
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Authentication failed." : "Authentication failed.");
    }
  }

  return (
    <div className="auth-page" data-section="auth">
      <section className="panel auth-panel">
        <button className="ghost-button" onClick={onBack}>Back</button>
        <div>
          <p className="eyebrow">{authMode === "login" ? "Welcome back" : "Create account"}</p>
          <h2>{authMode === "login" ? "Login to your health workspace" : "Sign up for your own health history"}</h2>
        </div>
        <form className="form-grid auth-form" onSubmit={submit}>
          {authMode === "login" ? (
            <label>Email or username<input value={form.identifier} onChange={(event) => setForm({ ...form, identifier: event.target.value })} required /></label>
          ) : (
            <>
              <label>Email<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></label>
              <label>Username<input value={form.username} minLength={3} onChange={(event) => setForm({ ...form, username: event.target.value })} required /></label>
              <label>Full name<input value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required /></label>
              <label>Role<select value={form.role} onChange={(event) => setForm({ ...form, role: event.target.value as Role })}><option value="USER">User</option><option value="ADMIN">Admin</option></select></label>
            </>
          )}
          <label>Password<input type="password" minLength={8} value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></label>
          {message && <div className="form-message">{message}</div>}
          <button className="primary-button full">{authMode === "login" ? "Login" : "Create account"}<ArrowRight size={18} /></button>
        </form>
        <button className="ghost-button" onClick={() => setAuthMode(authMode === "login" ? "signup" : "login")}>
          {authMode === "login" ? "Need an account? Sign up" : "Already have an account? Login"}
        </button>
        <button className="ghost-button" onClick={onBack}>Back</button>
       {/* <div className="disclaimer-box">Demo users: user@example.com / admin@example.com. Password: password123.</div> */}
      </section>
    </div>
  );
}
