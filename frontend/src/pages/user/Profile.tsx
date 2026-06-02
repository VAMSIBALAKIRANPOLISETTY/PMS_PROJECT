import { useState } from "react";
import axios from "axios";
import { Edit3, Save } from "lucide-react";
import { api, authHeaders } from "../../api";
import type { Notify, User } from "../../types";
import { cmToFeetInches, feetInchesToCm, formatHeight } from "../../utils";

interface ProfileProps {
  user: User;
  token: string;
  updateUser: (user: User) => void;
  notify: Notify;
}

export function Profile({ user, token, updateUser, notify }: ProfileProps) {
  const initials = user.fullName.split(" ").map((word) => word[0]).join("").slice(0, 2).toUpperCase();
  const [editing, setEditing] = useState(false);
  const [message, setMessage] = useState("");
  const initialImperial = cmToFeetInches(user.heightCm);
  const [heightUnit, setHeightUnit] = useState<"cm" | "imperial">("cm");
  const [heightFeet, setHeightFeet] = useState(initialImperial.feet);
  const [heightInches, setHeightInches] = useState(initialImperial.inches);
  const [form, setForm] = useState({
    fullName: user.fullName,
    age: user.age ?? 24,
    heightCm: user.heightCm ?? 170,
    weightKg: user.weightKg ?? 68,
    sex: user.sex ?? "Prefer not to say",
    allergies: user.allergies ?? "",
    chronicConditions: user.chronicConditions ?? "",
    lifestyle: user.lifestyle ?? "",
    medications: user.medications ?? "",
    familyHistory: user.familyHistory ?? "",
    mentalHealthHistory: user.mentalHealthHistory ?? "",
    sleepQuality: user.sleepQuality ?? "",
  });

  async function saveProfile() {
    setMessage("");
    try {
      const response = await api.put("/auth/profile", {
        ...form,
        heightCm: heightUnit === "cm" ? form.heightCm : feetInchesToCm(heightFeet, heightInches),
      }, { headers: authHeaders(token) });
      updateUser(response.data);
      setEditing(false);
      notify("Profile updated successfully.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Profile update failed." : "Profile update failed.");
      notify("Profile update failed.", "danger");
    }
  }

  return (
    <section className="panel profile-panel" data-section="profile">
      <div className="profile-header">
        <div className="avatar">{initials}</div>
        <div><p className="eyebrow">Patient profile</p><h2>{user.fullName}</h2><p>Age {user.age ?? "not set"} | {user.sex ?? "not set"} | {formatHeight(user.heightCm)} | {user.weightKg ?? 0} kg</p></div>
        <button className="ghost-button" onClick={() => setEditing(!editing)}>{editing ? "Cancel" : "Edit profile"}<Edit3 size={17} /></button>
      </div>
      {editing ? (
        <div className="form-grid profile-edit-form">
          <label>Full name<input value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} /></label>
          <label>Age<input type="number" min="1" max="120" value={form.age} onChange={(event) => setForm({ ...form, age: Number(event.target.value) })} /></label>
          <div className="height-field">
            <div className="field-heading"><span>Height</span><div className="segmented compact"><button type="button" className={heightUnit === "cm" ? "active" : ""} onClick={() => switchHeightUnit("cm")}>cm</button><button type="button" className={heightUnit === "imperial" ? "active" : ""} onClick={() => switchHeightUnit("imperial")}>ft + in</button></div></div>
            {heightUnit === "cm" ? (
              <input type="number" aria-label="Height cm" min="30" max="260" step="0.1" value={form.heightCm} onChange={(event) => setForm({ ...form, heightCm: Number(event.target.value) })} />
            ) : (
              <div className="imperial-height-grid"><input type="number" aria-label="Height feet" placeholder="Feet" min="1" max="8" value={heightFeet} onChange={(event) => setHeightFeet(event.target.value)} /><input type="number" aria-label="Height inches" placeholder="Inches" min="0" max="11" value={heightInches} onChange={(event) => setHeightInches(event.target.value)} /></div>
            )}
          </div>
          <label>Weight kg<input type="number" min="2" max="350" step="0.1" value={form.weightKg} onChange={(event) => setForm({ ...form, weightKg: Number(event.target.value) })} /></label>
          <label>Sex<select value={form.sex} onChange={(event) => setForm({ ...form, sex: event.target.value })}><option>Female</option><option>Male</option><option>Intersex</option><option>Prefer not to say</option></select></label>
          <label>Allergies<input value={form.allergies} onChange={(event) => setForm({ ...form, allergies: event.target.value })} /></label>
          <label>Chronic conditions<input value={form.chronicConditions} onChange={(event) => setForm({ ...form, chronicConditions: event.target.value })} /></label>
          <label>Medications<input value={form.medications} onChange={(event) => setForm({ ...form, medications: event.target.value })} /></label>
          <label>Family history<input value={form.familyHistory} onChange={(event) => setForm({ ...form, familyHistory: event.target.value })} /></label>
          <label>Mental health history<input value={form.mentalHealthHistory} onChange={(event) => setForm({ ...form, mentalHealthHistory: event.target.value })} /></label>
          <label>Sleep quality<input value={form.sleepQuality} onChange={(event) => setForm({ ...form, sleepQuality: event.target.value })} /></label>
          <label>Lifestyle<input value={form.lifestyle} onChange={(event) => setForm({ ...form, lifestyle: event.target.value })} /></label>
          {message && <div className="form-message">{message}</div>}
          <button className="primary-button full" onClick={saveProfile}>Save profile<Save size={18} /></button>
        </div>
      ) : (
        <div className="profile-grid">
          {[
            ["Allergies", user.allergies ?? "No known allergies"],
            ["Chronic conditions", user.chronicConditions ?? "No chronic condition"],
            ["Medications", user.medications ?? "No medicines set"],
            ["Family history", user.familyHistory ?? "Family history not set"],
            ["Mental health history", user.mentalHealthHistory ?? "Mental health history not set"],
            ["Sleep quality", user.sleepQuality ?? "Sleep quality not set"],
            ["Lifestyle", user.lifestyle ?? "Lifestyle not set"],
            ["Email", user.email],
            ["Username", `@${user.username}`],
            ["Profile completion", `${user.profileCompletion ?? 0}%`],
          ].map(([label, value]) => <div className="profile-detail" key={label}><small>{label}</small><strong>{value}</strong></div>)}
        </div>
      )}
    </section>
  );

  function switchHeightUnit(nextUnit: "cm" | "imperial") {
    if (nextUnit === heightUnit) return;
    if (nextUnit === "imperial") {
      const imperial = cmToFeetInches(form.heightCm);
      setHeightFeet(imperial.feet);
      setHeightInches(imperial.inches);
    } else {
      setForm({ ...form, heightCm: feetInchesToCm(heightFeet, heightInches) });
    }
    setHeightUnit(nextUnit);
  }
}
