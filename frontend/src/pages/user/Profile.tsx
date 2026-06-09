import { useState } from "react";
import type { ReactNode } from "react";
import axios from "axios";
import { Camera, Edit3, Save } from "lucide-react";
import { api, authHeaders } from "../../api";
import { ProfileAvatar } from "../../components/ProfileAvatar";
import type { Notify, User } from "../../types";
import { cmToFeetInches, feetInchesToCm, formatHeight } from "../../utils";

interface ProfileProps {
  user: User;
  token: string;
  updateUser: (user: User) => void;
  notify: Notify;
}

type ProfileForm = {
  fullName: string;
  age: number | "";
  heightCm: number | "";
  weightKg: number | "";
  sex: string;
  allergies: string;
  chronicConditions: string;
  lifestyle: string;
  medications: string;
  familyHistory: string;
  mentalHealthHistory: string;
  sleepQuality: string;
  dateOfBirth: string;
  sexAtBirth: string;
  genderIdentity: string;
  preferredLanguage: string;
  phone: string;
  address: string;
  bloodType: string;
  pregnancyStatus: string;
  emergencyContactName: string;
  emergencyContactRelationship: string;
  emergencyContactPhone: string;
  preferredHospital: string;
  surgeries: string;
  immunizations: string;
  primaryDoctor: string;
  specialistNames: string;
  hospitalClinic: string;
  insuranceProvider: string;
  insuranceMemberId: string;
  baselineHeartRate: string;
  baselineBloodPressure: string;
  tobaccoAlcoholUse: string;
  dietNotes: string;
  connectedDataConsent: boolean;
  notificationPreference: string;
  exportFormatPreference: string;
  dataSharingPreference: string;
};

const bloodTypes = ["", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown"];

export function Profile({ user, token, updateUser, notify }: ProfileProps) {
  const [editing, setEditing] = useState(false);
  const [message, setMessage] = useState("");
  const [photoBusy, setPhotoBusy] = useState(false);
  const initialImperial = cmToFeetInches(user.heightCm);
  const [heightUnit, setHeightUnit] = useState<"cm" | "imperial">("cm");
  const [heightFeet, setHeightFeet] = useState(initialImperial.feet);
  const [heightInches, setHeightInches] = useState(initialImperial.inches);
  const [form, setForm] = useState<ProfileForm>(() => profileToForm(user));

  function setField<K extends keyof ProfileForm>(key: K, value: ProfileForm[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function saveProfile() {
    setMessage("");
    if (!form.fullName.trim() || !form.age || !form.heightCm || !form.weightKg || !form.sex.trim()) {
      setMessage("Full name, age, height, weight, and sex are required.");
      return;
    }
    try {
      const response = await api.put("/auth/profile", {
        ...form,
        age: Number(form.age),
        heightCm: heightUnit === "cm" ? Number(form.heightCm) : feetInchesToCm(heightFeet, heightInches),
        weightKg: Number(form.weightKg),
      }, { headers: authHeaders(token) });
      updateUser(response.data);
      setEditing(false);
      notify("Profile updated successfully.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Profile update failed." : "Profile update failed.");
      notify("Profile update failed.", "danger");
    }
  }

  async function uploadProfilePhoto(file: File | null) {
    if (!file) return;
    const body = new FormData();
    body.append("file", file);
    setPhotoBusy(true);
    setMessage("");
    try {
      const response = await api.post<User>("/auth/profile-photo", body, {
        headers: { ...authHeaders(token), "Content-Type": "multipart/form-data" },
      });
      updateUser(response.data);
      notify("Profile photo updated.");
    } catch (error) {
      setMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "Profile photo update failed." : "Profile photo update failed.");
      notify("Profile photo update failed.", "danger");
    } finally {
      setPhotoBusy(false);
    }
  }

  return (
    <section className="panel profile-panel" data-section="profile">
      <div className="profile-header">
        <ProfileAvatar name={user.fullName} photo={user.profilePhotoDataUrl} size="lg" />
        <div>
          <p className="eyebrow">Patient profile</p>
          <h2>{user.fullName}</h2>
          <p>Age {user.age ?? "not set"} | {user.sex ?? "not set"} | {formatHeight(user.heightCm)} | {user.weightKg ?? "not set"} kg</p>
        </div>
        <div className="profile-header-actions">
          <label className="ghost-button profile-photo-upload">
            <Camera size={17} /> {photoBusy ? "Uploading..." : "Profile photo"}
            <input type="file" accept="image/jpeg,image/png,image/webp" disabled={photoBusy} onChange={(event) => void uploadProfilePhoto(event.target.files?.[0] ?? null)} />
          </label>
          <button className="ghost-button" type="button" onClick={() => setEditing(!editing)}>{editing ? "Cancel" : "Edit profile"}<Edit3 size={17} /></button>
        </div>
      </div>

      {editing ? (
        <div className="profile-edit-form">
          <ProfileGroup title="Identity">
            <label>Full name<input value={form.fullName} onChange={(event) => setField("fullName", event.target.value)} /></label>
            <label>Date of birth<input type="date" value={form.dateOfBirth} onChange={(event) => setField("dateOfBirth", event.target.value)} /></label>
            <label>Age<input type="number" min="1" max="120" value={form.age} onChange={(event) => setField("age", event.target.value ? Number(event.target.value) : "")} /></label>
            <label>Sex<select value={form.sex} onChange={(event) => setField("sex", event.target.value)}><option value="">Select sex</option><option>Female</option><option>Male</option><option>Intersex</option><option>Prefer not to say</option></select></label>
            <label>Sex at birth<input value={form.sexAtBirth} placeholder="Female, male, intersex, or prefer not to say" onChange={(event) => setField("sexAtBirth", event.target.value)} /></label>
            <label>Gender identity<input value={form.genderIdentity} placeholder="Optional" onChange={(event) => setField("genderIdentity", event.target.value)} /></label>
            <label>Preferred language<input value={form.preferredLanguage} placeholder="Example: English" onChange={(event) => setField("preferredLanguage", event.target.value)} /></label>
            <label>Phone<input value={form.phone} placeholder="Contact number" onChange={(event) => setField("phone", event.target.value)} /></label>
            <label className="span-2">Address<input value={form.address} placeholder="City or address for care coordination" onChange={(event) => setField("address", event.target.value)} /></label>
          </ProfileGroup>

          <ProfileGroup title="Body basics">
            <div className="height-field">
              <div className="field-heading"><span>Height</span><div className="segmented compact"><button type="button" className={heightUnit === "cm" ? "active" : ""} onClick={() => switchHeightUnit("cm")}>cm</button><button type="button" className={heightUnit === "imperial" ? "active" : ""} onClick={() => switchHeightUnit("imperial")}>ft + in</button></div></div>
              {heightUnit === "cm" ? (
                <input type="number" aria-label="Height cm" min="30" max="260" step="0.1" value={form.heightCm} onChange={(event) => setField("heightCm", event.target.value ? Number(event.target.value) : "")} />
              ) : (
                <div className="imperial-height-grid"><input type="number" aria-label="Height feet" placeholder="Feet" min="1" max="8" value={heightFeet} onChange={(event) => setHeightFeet(event.target.value)} /><input type="number" aria-label="Height inches" placeholder="Inches" min="0" max="11" value={heightInches} onChange={(event) => setHeightInches(event.target.value)} /></div>
              )}
            </div>
            <label>Weight kg<input type="number" min="2" max="350" step="0.1" value={form.weightKg} onChange={(event) => setField("weightKg", event.target.value ? Number(event.target.value) : "")} /></label>
            <label>Blood type<select value={form.bloodType} onChange={(event) => setField("bloodType", event.target.value)}>{bloodTypes.map((type) => <option key={type || "blank"} value={type}>{type || "Select blood type"}</option>)}</select></label>
            <label>Pregnancy or postpartum status<input value={form.pregnancyStatus} placeholder="Not applicable, pregnant, postpartum, or clinician-guided detail" onChange={(event) => setField("pregnancyStatus", event.target.value)} /></label>
          </ProfileGroup>

          <ProfileGroup title="Emergency and care team">
            <label>Emergency contact<input value={form.emergencyContactName} onChange={(event) => setField("emergencyContactName", event.target.value)} /></label>
            <label>Relationship<input value={form.emergencyContactRelationship} onChange={(event) => setField("emergencyContactRelationship", event.target.value)} /></label>
            <label>Emergency phone<input value={form.emergencyContactPhone} onChange={(event) => setField("emergencyContactPhone", event.target.value)} /></label>
            <label>Preferred hospital<input value={form.preferredHospital} onChange={(event) => setField("preferredHospital", event.target.value)} /></label>
            <label>Primary doctor<input value={form.primaryDoctor} onChange={(event) => setField("primaryDoctor", event.target.value)} /></label>
            <label>Specialists<input value={form.specialistNames} onChange={(event) => setField("specialistNames", event.target.value)} /></label>
            <label>Hospital or clinic<input value={form.hospitalClinic} onChange={(event) => setField("hospitalClinic", event.target.value)} /></label>
            <label>Insurance provider<input value={form.insuranceProvider} onChange={(event) => setField("insuranceProvider", event.target.value)} /></label>
            <label>Insurance member ID<input value={form.insuranceMemberId} onChange={(event) => setField("insuranceMemberId", event.target.value)} /></label>
          </ProfileGroup>

          <ProfileGroup title="Clinical background">
            <label>Allergies<input value={form.allergies} onChange={(event) => setField("allergies", event.target.value)} /></label>
            <label>Chronic conditions<input value={form.chronicConditions} onChange={(event) => setField("chronicConditions", event.target.value)} /></label>
            <label>Medications<input value={form.medications} onChange={(event) => setField("medications", event.target.value)} /></label>
            <label>Surgeries<input value={form.surgeries} onChange={(event) => setField("surgeries", event.target.value)} /></label>
            <label>Immunizations<input value={form.immunizations} onChange={(event) => setField("immunizations", event.target.value)} /></label>
            <label>Family history<input value={form.familyHistory} onChange={(event) => setField("familyHistory", event.target.value)} /></label>
            <label>Mental health history<input value={form.mentalHealthHistory} onChange={(event) => setField("mentalHealthHistory", event.target.value)} /></label>
          </ProfileGroup>

          <ProfileGroup title="Lifestyle and preferences">
            <label>Sleep quality<input value={form.sleepQuality} onChange={(event) => setField("sleepQuality", event.target.value)} /></label>
            <label>Lifestyle<input value={form.lifestyle} onChange={(event) => setField("lifestyle", event.target.value)} /></label>
            <label>Tobacco or alcohol use<input value={form.tobaccoAlcoholUse} onChange={(event) => setField("tobaccoAlcoholUse", event.target.value)} /></label>
            <label>Diet notes<input value={form.dietNotes} onChange={(event) => setField("dietNotes", event.target.value)} /></label>
            <label>Baseline heart rate<input value={form.baselineHeartRate} placeholder="Example: 72 bpm" onChange={(event) => setField("baselineHeartRate", event.target.value)} /></label>
            <label>Baseline blood pressure<input value={form.baselineBloodPressure} placeholder="Example: 120/80" onChange={(event) => setField("baselineBloodPressure", event.target.value)} /></label>
            <label>Notification preference<input value={form.notificationPreference} placeholder="Email, SMS, app notification" onChange={(event) => setField("notificationPreference", event.target.value)} /></label>
            <label>Export format preference<input value={form.exportFormatPreference} placeholder="PDF or printable summary" onChange={(event) => setField("exportFormatPreference", event.target.value)} /></label>
            <label>Data-sharing preference<input value={form.dataSharingPreference} placeholder="Share only with consent" onChange={(event) => setField("dataSharingPreference", event.target.value)} /></label>
            <label className="checkbox-row"><input type="checkbox" checked={form.connectedDataConsent} onChange={(event) => setField("connectedDataConsent", event.target.checked)} /> Allow PMS to use connected health records in care-preparation summaries</label>
          </ProfileGroup>

          {message && <div className="form-message">{message}</div>}
          <div className="profile-setup-actions">
            <button className="primary-button" type="button" onClick={saveProfile}>Save profile<Save size={18} /></button>
          </div>
        </div>
      ) : (
        <div className="profile-grid expanded">
          {profileDetails(user).map(([label, value]) => <div className="profile-detail" key={label}><small>{label}</small><strong>{value || "Not set"}</strong></div>)}
        </div>
      )}
    </section>
  );

  function switchHeightUnit(nextUnit: "cm" | "imperial") {
    if (nextUnit === heightUnit) return;
    if (nextUnit === "imperial") {
      const imperial = cmToFeetInches(Number(form.heightCm) || user.heightCm);
      setHeightFeet(imperial.feet);
      setHeightInches(imperial.inches);
    } else {
      setForm({ ...form, heightCm: feetInchesToCm(heightFeet, heightInches) });
    }
    setHeightUnit(nextUnit);
  }
}

function ProfileGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <fieldset className="profile-edit-group">
      <legend>{title}</legend>
      <div className="form-grid">{children}</div>
    </fieldset>
  );
}

function profileToForm(user: User): ProfileForm {
  return {
    fullName: user.fullName,
    age: user.age ?? "",
    heightCm: user.heightCm ?? "",
    weightKg: user.weightKg ?? "",
    sex: user.sex ?? "",
    allergies: user.allergies ?? "",
    chronicConditions: user.chronicConditions ?? "",
    lifestyle: user.lifestyle ?? "",
    medications: user.medications ?? "",
    familyHistory: user.familyHistory ?? "",
    mentalHealthHistory: user.mentalHealthHistory ?? "",
    sleepQuality: user.sleepQuality ?? "",
    dateOfBirth: user.dateOfBirth ?? "",
    sexAtBirth: user.sexAtBirth ?? "",
    genderIdentity: user.genderIdentity ?? "",
    preferredLanguage: user.preferredLanguage ?? "",
    phone: user.phone ?? "",
    address: user.address ?? "",
    bloodType: user.bloodType ?? "",
    pregnancyStatus: user.pregnancyStatus ?? "",
    emergencyContactName: user.emergencyContactName ?? "",
    emergencyContactRelationship: user.emergencyContactRelationship ?? "",
    emergencyContactPhone: user.emergencyContactPhone ?? "",
    preferredHospital: user.preferredHospital ?? "",
    surgeries: user.surgeries ?? "",
    immunizations: user.immunizations ?? "",
    primaryDoctor: user.primaryDoctor ?? "",
    specialistNames: user.specialistNames ?? "",
    hospitalClinic: user.hospitalClinic ?? "",
    insuranceProvider: user.insuranceProvider ?? "",
    insuranceMemberId: user.insuranceMemberId ?? "",
    baselineHeartRate: user.baselineHeartRate ?? "",
    baselineBloodPressure: user.baselineBloodPressure ?? "",
    tobaccoAlcoholUse: user.tobaccoAlcoholUse ?? "",
    dietNotes: user.dietNotes ?? "",
    connectedDataConsent: Boolean(user.connectedDataConsent),
    notificationPreference: user.notificationPreference ?? "",
    exportFormatPreference: user.exportFormatPreference ?? "",
    dataSharingPreference: user.dataSharingPreference ?? "",
  };
}

function profileDetails(user: User): [string, string | number | undefined][] {
  return [
    ["Blood type", user.bloodType],
    ["Height", formatHeight(user.heightCm)],
    ["Weight", user.weightKg ? `${user.weightKg} kg` : undefined],
    ["Phone", user.phone],
    ["Preferred language", user.preferredLanguage],
    ["Emergency contact", joinValues(user.emergencyContactName, user.emergencyContactRelationship, user.emergencyContactPhone)],
    ["Preferred hospital", user.preferredHospital],
    ["Primary doctor", user.primaryDoctor],
    ["Hospital or clinic", user.hospitalClinic],
    ["Insurance", joinValues(user.insuranceProvider, user.insuranceMemberId)],
    ["Allergies", user.allergies],
    ["Chronic conditions", user.chronicConditions],
    ["Medications", user.medications],
    ["Surgeries", user.surgeries],
    ["Immunizations", user.immunizations],
    ["Family history", user.familyHistory],
    ["Mental health history", user.mentalHealthHistory],
    ["Sleep quality", user.sleepQuality],
    ["Lifestyle", user.lifestyle],
    ["Baseline heart rate", user.baselineHeartRate],
    ["Baseline blood pressure", user.baselineBloodPressure],
    ["Tobacco or alcohol use", user.tobaccoAlcoholUse],
    ["Diet notes", user.dietNotes],
    ["Connected data consent", user.connectedDataConsent ? "Allowed" : "Not allowed"],
    ["Notification preference", user.notificationPreference],
    ["Export preference", user.exportFormatPreference],
    ["Data sharing", user.dataSharingPreference],
    ["Email", user.email],
    ["Username", `@${user.username}`],
    ["Profile completion", `${user.profileCompletion ?? 0}%`],
  ];
}

function joinValues(...values: Array<string | undefined>) {
  return values.filter(Boolean).join(" | ");
}
