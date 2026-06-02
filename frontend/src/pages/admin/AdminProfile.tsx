import { ShieldCheck, UserRound } from "lucide-react";
import type { User } from "../../types";

export function AdminProfile({ user }: { user: User }) {
  return (
    <section className="panel profile-panel" data-section="admin-profile">
      <div className="profile-header">
        <div className="avatar"><UserRound size={30} /></div>
        <div><p className="eyebrow">Staff profile</p><h2>{user.fullName}</h2><p>Clinical operations access</p></div>
        <ShieldCheck size={24} />
      </div>
      <div className="profile-grid">
        <div className="profile-detail"><small>Display name</small><strong>{user.fullName}</strong></div>
        <div className="profile-detail"><small>Email</small><strong>{user.email}</strong></div>
        <div className="profile-detail"><small>Username</small><strong>@{user.username}</strong></div>
        <div className="profile-detail"><small>Role</small><strong>Administrator</strong></div>
        <div className="profile-detail"><small>Workspace</small><strong>Clinical operations</strong></div>
        <div className="profile-detail"><small>Access type</small><strong>Staff review</strong></div>
      </div>
    </section>
  );
}
