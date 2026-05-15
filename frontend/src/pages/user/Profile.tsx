import type { User } from "../../types";

export function Profile({ user }: { user: User }) {
  const initials = user.fullName.split(" ").map((word) => word[0]).join("").slice(0, 2).toUpperCase();
  return (
    <section className="panel profile-panel" data-section="profile">
      <div className="avatar">{initials}</div>
      <div><p className="eyebrow">Patient profile</p><h2>{user.fullName}</h2><p>Age {user.age ?? "not set"} | {user.gender ?? "not set"} | {user.heightCm ?? 0} cm | {user.weightKg ?? 0} kg</p></div>
      <div className="profile-grid">
        {[user.allergies ?? "No known allergies", user.chronicConditions ?? "No chronic condition", user.lifestyle ?? "Lifestyle not set", user.email, `Username: ${user.username}`, `Role: ${user.role}`].map((item) => <span key={item}>{item}</span>)}
      </div>
    </section>
  );
}
