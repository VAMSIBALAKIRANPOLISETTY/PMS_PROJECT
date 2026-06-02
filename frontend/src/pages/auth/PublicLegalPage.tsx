import { ArrowLeft, HeartPulse, ShieldCheck } from "lucide-react";

interface PublicLegalPageProps {
  page: "privacy" | "terms";
  onBack: () => void;
}

export function PublicLegalPage({ page, onBack }: PublicLegalPageProps) {
  const isPrivacy = page === "privacy";
  return (
    <div className="legal-page">
      <header className="legal-nav">
        <div className="brand-row">
          <div className="brand-mark"><HeartPulse size={24} /></div>
          <div><strong>PMS Health</strong><span>Care preparation</span></div>
        </div>
        <button className="ghost-button" onClick={onBack}><ArrowLeft size={18} />Back to home</button>
      </header>
      <main className="legal-content">
        <p className="eyebrow">{isPrivacy ? "Privacy and consent" : "Terms of use"}</p>
        <h1>{isPrivacy ? "Understand how your information supports care preparation." : "Use PMS Health as a preparation tool."}</h1>
        <p className="legal-intro">
          PMS Health is a care-preparation service. It is not a hospital portal, emergency service, diagnostic tool, or HIPAA authorization form.
        </p>
        <div className="legal-section-grid">
          {isPrivacy ? (
            <>
              <article><ShieldCheck size={20} /><h2>Information you provide</h2><p>Your account may include profile details, symptoms, health-history answers, assessment responses, and report notes.</p></article>
              <article><ShieldCheck size={20} /><h2>How PMS uses information</h2><p>Your entries are used to organize a private care-preparation workspace and create non-diagnostic awareness summaries.</p></article>
              <article><ShieldCheck size={20} /><h2>Your responsibility</h2><p>Share only information you are comfortable entering and keep your account credentials private.</p></article>
              <article><ShieldCheck size={20} /><h2>Privacy questions</h2><p>For privacy guidance, contact privacy@pmshealth.example. This address is not for medical concerns.</p></article>
            </>
          ) : (
            <>
              <article><ShieldCheck size={20} /><h2>No medical advice</h2><p>PMS Health does not diagnose conditions, prescribe medicines, or replace a qualified medical professional.</p></article>
              <article><ShieldCheck size={20} /><h2>Not for emergencies</h2><p>For severe, sudden, or worsening symptoms, contact local emergency services or seek urgent professional care.</p></article>
              <article><ShieldCheck size={20} /><h2>Accurate information</h2><p>Use current, accurate details so your preparation notes reflect what you intend to discuss with a clinician.</p></article>
              <article><ShieldCheck size={20} /><h2>Account security</h2><p>Keep your password private and use your account only for lawful personal care-preparation purposes.</p></article>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
