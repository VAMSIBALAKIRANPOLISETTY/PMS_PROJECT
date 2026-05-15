import { Upload } from "lucide-react";

export function Reports() {
  return (
    <div className="split-layout" data-section="reports">
      <section className="panel upload-panel">
        <div className="upload-zone">
          <Upload size={38} />
          <h2>Upload text-based PDF report</h2>
          <p>PDF upload UI is ready. The Spring Boot endpoint can be expanded for full text extraction in the next step.</p>
          <input type="file" accept="application/pdf" />
        </div>
      </section>
      <section className="panel">
        <p className="eyebrow">Mock AI summary</p>
        <h2>Report simplification</h2>
        <p className="summary-box">Upload support is intentionally safe in this branch. Scanned image PDFs remain out of scope.</p>
        <div className="disclaimer-box">This application does not provide medical diagnosis, treatment, or prescription.</div>
      </section>
    </div>
  );
}
