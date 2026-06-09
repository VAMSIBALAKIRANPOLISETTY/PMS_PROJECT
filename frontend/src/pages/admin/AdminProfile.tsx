import { useEffect, useState } from "react";
import axios from "axios";
import { Cpu, ShieldCheck, UserRound } from "lucide-react";
import { api, authHeaders } from "../../api";
import type { AiStatus, User } from "../../types";

export function AdminProfile({ user, token }: { user: User; token: string }) {
  const [aiStatus, setAiStatus] = useState<AiStatus | null>(null);
  const [statusMessage, setStatusMessage] = useState("");

  useEffect(() => {
    api.get<AiStatus>("/admin/ai/status", { headers: authHeaders(token) })
      .then((response) => {
        setAiStatus(response.data);
        setStatusMessage("");
      })
      .catch((error) => {
        setStatusMessage(axios.isAxiosError(error) ? error.response?.data?.message ?? "AI provider status could not be loaded." : "AI provider status could not be loaded.");
      });
  }, [token]);

  return (
    <div className="profile-panel" data-section="admin-profile">
      <section className="panel">
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

      <section className="panel">
        <div className="section-title">
          <div><p className="eyebrow">AI provider status</p><h2>Backend provider chain</h2></div>
          <Cpu size={24} />
        </div>
        {statusMessage && <div className="form-message">{statusMessage}</div>}
        {aiStatus ? (
          <div className="profile-grid expanded">
            <div className="profile-detail"><small>Mode</small><strong>{aiStatus.mode}</strong></div>
            <div className="profile-detail"><small>Provider chain</small><strong>{aiStatus.providerChain.join(" -> ")}</strong></div>
            <div className="profile-detail"><small>Last attempt</small><strong>{aiStatus.lastProviderAttempt}</strong></div>
            <div className="profile-detail"><small>Ollama model</small><strong>{aiStatus.ollamaModel}</strong></div>
            <div className="profile-detail"><small>Ollama key</small><strong>{aiStatus.ollamaApiKeyPresent ? "Configured" : "Missing"}</strong></div>
            <div className="profile-detail"><small>OpenAI key</small><strong>{aiStatus.openAiApiKeyPresent ? "Configured" : "Missing"}</strong></div>
            <div className="profile-detail span-2"><small>Ollama URL</small><strong>{aiStatus.ollamaBaseUrl}</strong></div>
            <div className="profile-detail"><small>OpenAI model</small><strong>{aiStatus.openAiModel}</strong></div>
            <div className="profile-detail span-2"><small>Latest fallback note</small><strong>{aiStatus.lastFallbackReason}</strong></div>
          </div>
        ) : !statusMessage ? (
          <div className="empty-row">Loading provider status...</div>
        ) : null}
      </section>
    </div>
  );
}
