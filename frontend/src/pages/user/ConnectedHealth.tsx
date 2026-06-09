import { useEffect, useState } from "react";
import type { KeyboardEvent } from "react";
import axios from "axios";
import { Activity, ArrowRight, DatabaseZap, Link2, RefreshCw, ShieldCheck, Trash2 } from "lucide-react";
import { api, authHeaders } from "../../api";
import type { ConnectionStartResponse, HealthConnection, Notify, TimelineRecord } from "../../types";

interface ConnectedHealthProps {
  token: string;
  notify: Notify;
}

const providers = [
  { id: "APPLE_HEALTH", label: "Apple Health / Apple Watch", detail: "iOS companion import for activity, sleep, vitals, workouts, and supported Health Records." },
  { id: "ANDROID_HEALTH_CONNECT", label: "Android Health Connect", detail: "Android companion import for steps, heart rate, sleep, temperature, and supported records." },
  { id: "SAMSUNG_HEALTH", label: "Samsung Health", detail: "Samsung-supported wearable data through Health Connect or Samsung Health Data SDK." },
  { id: "HOSPITAL_PORTAL", label: "Hospital portal", detail: "Patient-authorized clinical record import through SMART on FHIR where available." },
  { id: "MANUAL_ENTRY", label: "Manual vitals", detail: "Manual entry remains available when direct device or hospital access is unavailable." },
];

export function ConnectedHealth({ token, notify }: ConnectedHealthProps) {
  const [connections, setConnections] = useState<HealthConnection[]>([]);
  const [timeline, setTimeline] = useState<TimelineRecord[]>([]);
  const [pendingStart, setPendingStart] = useState<ConnectionStartResponse | null>(null);
  const [credentialAccount, setCredentialAccount] = useState("");
  const [credentialSecret, setCredentialSecret] = useState("");
  const [connectionReady, setConnectionReady] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    void loadConnections();
    void loadTimeline();
  }, [token]);

  async function loadConnections(showError = true) {
    try {
      const response = await api.get<HealthConnection[]>("/connections", { headers: authHeaders(token) });
      setConnections(response.data);
      return true;
    } catch (error) {
      const fallback = apiMessage(error, "Connected sources could not be loaded.");
      setMessage(fallback);
      if (showError) notify("Connected sources could not be loaded.", "danger");
      return false;
    }
  }

  async function loadTimeline(showError = true) {
    try {
      const response = await api.get<TimelineRecord[]>("/health-timeline", { headers: authHeaders(token) });
      setTimeline(response.data);
      return true;
    } catch (error) {
      const fallback = apiMessage(error, "Connected health timeline could not be loaded.");
      setMessage(fallback);
      if (showError) notify("Connected health timeline could not be loaded.", "danger");
      return false;
    }
  }

  async function start(provider: string) {
    setMessage("");
    try {
      const response = await api.post<ConnectionStartResponse>(`/connections/${provider}/start`, {}, { headers: authHeaders(token) });
      setPendingStart(response.data);
      setCredentialAccount("");
      setCredentialSecret("");
      setConnectionReady(false);
    } catch (error) {
      const fallback = axios.isAxiosError(error) ? error.response?.data?.message ?? "Connection could not be prepared." : "Connection could not be prepared.";
      setMessage(fallback);
      notify("Connection setup failed.", "danger");
    }
  }

  async function confirmConnection(provider: string) {
    if (!connectionReady) {
      setMessage("Connect the API link before saving this source.");
      return;
    }
    try {
      await api.post(`/connections/${provider}/callback`, {
        displayName: providers.find((item) => item.id === provider)?.label ?? provider,
        externalAccountId: credentialAccount.trim(),
      }, { headers: authHeaders(token) });
      setPendingStart(null);
      setCredentialAccount("");
      setCredentialSecret("");
      setConnectionReady(false);
      notify("Connected health source saved.");
      setMessage("Connection saved. Checking for imported records...");
      const connectionsLoaded = await loadConnections(false);
      const timelineLoaded = await loadTimeline(false);
      if (connectionsLoaded && timelineLoaded) {
        setMessage("Connection saved. No provider password or API token was stored.");
      } else {
        setMessage("Connection saved, but the latest connected-health records could not be refreshed. Try opening Connected Health again or log in again if the session expired.");
      }
    } catch (error) {
      const fallback = apiMessage(error, "Connection could not be saved.");
      setMessage(fallback);
      notify("Connection save failed.", "danger");
    }
  }

  async function sync(connection: HealthConnection) {
    try {
      await api.post(`/connections/${connection.id}/sync`, {
        records: [],
      }, { headers: authHeaders(token) });
      await loadConnections(false);
      await loadTimeline(false);
      notify("Connection checked. No new records imported.");
    } catch (error) {
      const fallback = apiMessage(error, "Sync failed.");
      setMessage(fallback);
      notify("Health sync failed.", "danger");
    }
  }

  async function revoke(connection: HealthConnection) {
    await api.delete(`/connections/${connection.id}`, { headers: authHeaders(token) });
    await loadConnections(false);
    await loadTimeline(false);
    notify("Connection access stopped.");
  }

  function cancelPendingConnection() {
    setPendingStart(null);
    setCredentialAccount("");
    setCredentialSecret("");
    setConnectionReady(false);
    setConnecting(false);
    setMessage("");
  }

  async function connectApiLink() {
    if (!credentialAccount.trim() || !credentialSecret.trim()) {
      setMessage("Enter dummy account credentials before connecting the API link.");
      return;
    }
    setConnecting(true);
    setConnectionReady(false);
    setMessage("Connecting API link...");
    await new Promise((resolve) => window.setTimeout(resolve, 850));
    setConnecting(false);
    setConnectionReady(true);
    setMessage("Connection established. Review and save this source.");
  }

  function handleCredentialKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key !== "Enter") return;
    const target = event.target as HTMLElement;
    if (target.tagName === "TEXTAREA") return;
    event.preventDefault();
    if (connectionReady && pendingStart) {
      void confirmConnection(pendingStart.provider);
    } else {
      void connectApiLink();
    }
  }

  return (
    <div className="page-grid connected-health" data-section="connected-health">
      <section className="panel wide">
        <div className="section-title">
          <div>
            <p className="eyebrow">Connected health</p>
            <h2>Patient-owned health context</h2>
          </div>
          <ShieldCheck size={24} />
        </div>
        <p className="summary-box">
          Connectors normalize wearable, app, hospital, and manual records into one timeline before rules or AI summaries use the data.
        </p>
        {message && <div className="form-message">{message}</div>}
        <div className="connector-grid">
          {providers.map((provider) => (
            <article className="connector-card" key={provider.id}>
              <Link2 size={22} />
              <h3>{provider.label}</h3>
              <p>{provider.detail}</p>
              <button className="ghost-button" type="button" onClick={() => void start(provider.id)}>
                Review permissions <ArrowRight size={16} />
              </button>
            </article>
          ))}
        </div>
      </section>

      {pendingStart && (
        <section className="panel wide" onKeyDown={handleCredentialKeyDown}>
          <div className="section-title">
            <div>
              <p className="eyebrow">Permission review</p>
              <h2>{pendingStart.provider.replaceAll("_", " ")}</h2>
            </div>
          </div>
          <p>{pendingStart.permissionSummary}</p>
          <div className="connector-credential-grid">
            <label>
              Account ID or email
              <input placeholder="patient@example.com or portal ID" value={credentialAccount} onChange={(event) => { setCredentialAccount(event.target.value); setConnectionReady(false); }} />
            </label>
            <label>
              Password or API token
              <input type="password" placeholder="Dummy credential for connection check" value={credentialSecret} onChange={(event) => { setCredentialSecret(event.target.value); setConnectionReady(false); }} />
            </label>
          </div>
          <div className="profile-setup-actions">
            <button className="ghost-button" type="button" onClick={cancelPendingConnection}>Cancel</button>
            {!connectionReady ? (
              <button className="primary-button" type="button" disabled={connecting} onClick={() => void connectApiLink()}>
                {connecting ? "Connecting API link..." : "Connect API link"} <ArrowRight size={18} />
              </button>
            ) : (
              <button className="primary-button" type="button" onClick={() => void confirmConnection(pendingStart.provider)}>
                Save connection <ArrowRight size={18} />
              </button>
            )}
          </div>
        </section>
      )}

      <section className="panel">
        <p className="eyebrow">Sources</p>
        <h2>Active connections</h2>
        <div className="connection-list">
          {connections.length === 0 && <div className="empty-row">No connected sources yet.</div>}
          {connections.map((connection) => (
            <div className="connection-row" key={connection.id}>
              <DatabaseZap size={18} />
              <div>
                <strong>{connection.displayName}</strong>
                <span>{connection.status} {connection.lastSyncAt ? `| synced ${new Date(connection.lastSyncAt).toLocaleDateString()}` : ""}</span>
              </div>
              <button className="icon-button" type="button" aria-label={`Sync ${connection.displayName}`} onClick={() => void sync(connection)}><RefreshCw size={16} /></button>
              <button className="icon-button" type="button" aria-label={`Stop ${connection.displayName}`} onClick={() => void revoke(connection)}><Trash2 size={16} /></button>
            </div>
          ))}
        </div>
      </section>

      <section className="panel">
        <p className="eyebrow">Timeline</p>
        <h2>Normalized records</h2>
        <div className="timeline compact-timeline">
          {timeline.length === 0 && <div className="empty-row">No imported records yet.</div>}
          {timeline.map((record) => (
            <div className="timeline-item" key={record.id}>
              <span />
              <div>
                <strong>{record.label}</strong>
                <p>{record.recordType} | {record.valueText ?? "Recorded"} {record.unit ?? ""}</p>
                {record.notes && <small>{record.notes}</small>}
              </div>
              <Activity size={16} />
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

function apiMessage(error: unknown, fallback: string) {
  if (!axios.isAxiosError(error)) return fallback;
  if (typeof error.response?.data?.message === "string") return error.response.data.message;
  if (error.response?.status === 401 || error.response?.status === 403) return "Log in with a patient account before using connected health records.";
  if (error.response?.status) return `${fallback} Server returned ${error.response.status}.`;
  if (error.request) return `${fallback} Confirm the backend is running and the frontend proxy is connected.`;
  return fallback;
}
