import { ArrowRight, FileText, HeartPulse, LogIn, Mail, MessageSquareText, ShieldCheck, Stethoscope } from "lucide-react";

const navItems = ["Home", "How it works", "Safety", "Contact"];
const trustItems = ["Awareness only", "No diagnosis", "Doctor-ready summary"];
const landingMetrics = [
  { label: "Guided steps", value: "3" },
  { label: "Follow-up range", value: "4-7" },
  { label: "Medical claim", value: "None" },
];
const serviceCards = [
  { title: "Describe symptoms", text: "Visitors learn how PMS helps organize symptoms before a consultation." },
  { title: "Prepare reports", text: "Report simplification is explained without exposing patient details." },
  { title: "Answer follow-ups", text: "Short guided questions help users prepare better notes for a doctor." },
  { title: "Track safely", text: "History is presented as a benefit, not as account data on the landing page." },
];
const storySteps = [
  "Create a private account",
  "Complete basic health history",
  "Generate a safe awareness summary",
];
const safetyCards = [
  { title: "Not an emergency service", text: "PMS cannot replace urgent care, ambulance services, or emergency medical advice." },
  { title: "No diagnosis or prescription", text: "The system explains awareness signals only. It does not name diseases or suggest medicines." },
  { title: "Doctor-first guidance", text: "Every result encourages users to consult a qualified professional for real medical decisions." },
];
const contactCards = [
  { title: "Project support", text: "Use this section for demo questions, setup help, and capstone review conversations." },
  { title: "Technical setup", text: "For local development, run PostgreSQL, the Spring Boot backend, and the Vite frontend." },
  { title: "Feedback", text: "Collect feedback about landing copy, usability, follow-up questions, and safe explanation wording." },
];

export function LandingPage({ onAuth }: { onAuth: (mode: "login" | "signup") => void }) {
  function scrollToSection(sectionId: string) {
    if (sectionId === "top") {
      window.scrollTo({ top: 0, behavior: "smooth" });
      return;
    }
    document.getElementById(sectionId)?.scrollIntoView({ behavior: "smooth" });
  }

  return (
    <div className="landing-page" data-section="landing">
      <div className="dynamic-ui-background" aria-hidden="true">
        <div className="floating-ui-card card-a"><HeartPulse size={22} /><strong>Health awareness</strong><span>Before care</span></div>
        <div className="floating-ui-card card-b"><FileText size={22} /><strong>Safe summary</strong><span>No diagnosis</span></div>
        <div className="floating-ui-card card-c"><Stethoscope size={22} /><strong>Doctor-ready</strong><span>Better notes</span></div>
        <div className="data-ribbon ribbon-one" />
        <div className="data-ribbon ribbon-two" />
      </div>

      <header className="landing-nav">
        <div className="brand-row">
          <div className="brand-mark"><HeartPulse size={24} /></div>
          <div><strong>PMS Health</strong><span>Assessment system</span></div>
        </div>
        <nav className="landing-links" aria-label="Landing navigation">
          {navItems.map((item) => {
            const target = item === "Home" ? "top" : item === "How it works" ? "landing-details" : item === "Safety" ? "landing-safety" : "landing-contact";
            return <button key={item} onClick={() => scrollToSection(target)}>{item}</button>;
          })}
        </nav>
        <div className="hero-actions">
          <button className="ghost-button" onClick={() => onAuth("login")}><LogIn size={18} />Login</button>
          <button className="primary-button" onClick={() => onAuth("signup")}>Sign up<ArrowRight size={18} /></button>
        </div>
      </header>

      <section className="landing-hero">
        <div className="landing-copy">
          <p className="eyebrow">Health awareness before care</p>
          <h1>Know what to ask before you visit a doctor.</h1>
          <p>
            PMS Health is a patient-preparation website that helps people organize
            symptoms, report notes, and safe follow-up questions before speaking
            with a qualified medical professional.
          </p>
          <div className="hero-actions">
            <button className="primary-button" onClick={() => onAuth("signup")}>Create account<ArrowRight size={18} /></button>
            <button className="ghost-button" onClick={() => scrollToSection("landing-details")}>Learn how it works</button>
          </div>
          <div className="landing-proof-row">
            {trustItems.map((item) => <span key={item}><ShieldCheck size={17} />{item}</span>)}
          </div>
        </div>

        <div className="landing-public-preview">
          <span className="eyebrow">Public website content</span>
          <h2>Prepare notes, understand context, stay safe.</h2>
          <p>
            The landing page explains the product clearly without exposing user,
            profile, assessment, or role-specific information.
          </p>
          <div className="public-preview-grid">
            <span>Symptoms</span>
            <span>Reports</span>
            <span>Follow-ups</span>
            <span>Safety</span>
          </div>
        </div>
      </section>

      <section className="landing-details-section" id="landing-details">
        <div className="public-story-card">
          <p className="eyebrow">Public landing content</p>
          <h2>Prepare better, decide safely.</h2>
          <p>
            PMS does not diagnose. It gives visitors a clear explanation of what
            the system does, what it does not do, and why a doctor should remain
            the final source of medical guidance.
          </p>
        </div>

        <div className="landing-metric-grid">
          {landingMetrics.map((metric) => (
            <div key={metric.label}><strong>{metric.value}</strong><span>{metric.label}</span></div>
          ))}
        </div>

        <div className="landing-service-grid">
          {serviceCards.map((card) => (
            <article key={card.title}><strong>{card.title}</strong><span>{card.text}</span></article>
          ))}
        </div>

        <div className="landing-step-list">
          {storySteps.map((step, index) => (
            <div key={step}><span>{index + 1}</span><strong>{step}</strong></div>
          ))}
        </div>
      </section>

      <section className="landing-safety-section" id="landing-safety">
        <div className="section-title">
          <div>
            <p className="eyebrow">Safety page</p>
            <h2>Built for awareness, not medical decision-making.</h2>
          </div>
          <ShieldCheck size={28} />
        </div>
        <div className="landing-safety-grid">
          {safetyCards.map((card) => (
            <article key={card.title}><strong>{card.title}</strong><span>{card.text}</span></article>
          ))}
        </div>
        <div className="safety-disclaimer">
          <strong>Important:</strong>
          <span>For severe, sudden, or worsening symptoms, users should contact local emergency services or a qualified medical professional immediately.</span>
        </div>
      </section>

      <section className="landing-contact-section" id="landing-contact">
        <div className="contact-copy">
          <p className="eyebrow">Contact page</p>
          <h2>Questions about the PMS project?</h2>
          <p>
            Keep contact content public and simple: project support, technical setup,
            and feedback collection. Personal health records stay inside authenticated
            user screens.
          </p>
          <div className="hero-actions">
            <button className="primary-button" onClick={() => onAuth("signup")}>Create account<ArrowRight size={18} /></button>
            <button className="ghost-button" onClick={() => onAuth("login")}><LogIn size={18} />Login</button>
          </div>
        </div>
        <div className="landing-contact-grid">
          {contactCards.map((card, index) => (
            <article key={card.title}>
              {index === 0 ? <Mail size={20} /> : index === 1 ? <Stethoscope size={20} /> : <MessageSquareText size={20} />}
              <strong>{card.title}</strong>
              <span>{card.text}</span>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
