import { CheckCircle2, Database } from "lucide-react";

export function Datasets() {
  return (
    <section className="panel wide" data-section="admin-datasets">
      <div className="section-title"><div><p className="eyebrow">Synthetic only</p><h2>Testing dataset plan</h2></div><Database size={24} /></div>
      <div className="dataset-grid">{["Seeded demo users", "Symptom mapping rules", "Validation test cases", "PostgreSQL-backed records"].map((item) => <div key={item}><CheckCircle2 size={20} /><strong>{item}</strong><p>No real patient data is used in this prototype.</p></div>)}</div>
    </section>
  );
}
