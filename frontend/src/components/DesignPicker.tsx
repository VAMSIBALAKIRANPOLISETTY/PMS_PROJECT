import type { DesignId } from "../types";
import { designOptions } from "../data";

interface DesignPickerProps {
  design: DesignId;
  setDesign: (design: DesignId) => void;
}

export function DesignPicker({ design, setDesign }: DesignPickerProps) {
  return (
    <section className="panel design-picker" data-section="design-picker">
      <div className="section-title"><div><p className="eyebrow">UI options</p><h2>Select a design direction</h2></div></div>
      <div className="design-grid">
        {designOptions.map((option) => (
          <button key={option.id} className={`design-option ${option.id} ${design === option.id ? "active" : ""}`} onClick={() => setDesign(option.id)}>
            <span className="swatches"><i /><i /><i /></span>
            <strong>{option.name}</strong>
            <small>{option.note}</small>
          </button>
        ))}
      </div>
    </section>
  );
}
