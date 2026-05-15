import { ArrowRight, CheckCircle2, HeartPulse, LogIn } from "lucide-react";

export function LandingPage({ onAuth }: { onAuth: (mode: "login" | "signup") => void }) {
  return (
    <div className="landing-page" data-section="landing">
      <header className="landing-nav">
        <div className="brand-row">
          <div className="brand-mark"><HeartPulse size={24} /></div>
          <div><strong>PMS Health</strong><span>Assessment system</span></div>
        </div>
        <div className="hero-actions">
          <button className="ghost-button" onClick={() => onAuth("login")}><LogIn size={18} />Login</button>
          <button className="primary-button" onClick={() => onAuth("signup")}>Sign up<ArrowRight size={18} /></button>
        </div>
      </header>
      <section className="landing-hero">
        <div>
          <p className="eyebrow">Health awareness, not diagnosis</p>
          <h1>Understand symptoms, reports, and risk signals before your doctor visit.</h1>
          <p>
            A capstone-ready patient assessment system with structured health intake,
            rule-based risk scoring, follow-up questions, report summaries, and tracking.
          </p>
          <div className="hero-actions">
            <button className="primary-button" onClick={() => onAuth("signup")}>Create account<ArrowRight size={18} /></button>
            <button className="ghost-button" onClick={() => onAuth("login")}><LogIn size={18} />Login</button>
          </div>
        </div>
        <div className="landing-monitor">
          <div className="vital-card">
            <HeartPulse size={30} />
            <span>Safe prototype</span>
            <strong>AI + Rules</strong>
            <p>Low / Medium / High awareness output with reasons.</p>
          </div>
          <div className="mini-feature-grid">
            {["Dynamic questions", "PDF text summary", "Trend tracking", "Admin rules"].map((item) => (
              <span key={item}><CheckCircle2 size={16} />{item}</span>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
