import { useState } from "react";
import { Search } from "lucide-react";
import { possibleSymptoms } from "../../data";

interface SymptomDrawerProps {
  selected: string[];
  onSelect: (symptom: string) => void;
}

export function SymptomDrawer({ selected, onSelect }: SymptomDrawerProps) {
  const [query, setQuery] = useState("");
  const filtered = possibleSymptoms.filter((symptom) => query.length < 1 || symptom.toLowerCase().includes(query.toLowerCase()));
  return (
    <aside className="symptom-drawer" data-section="symptom-drawer">
      <div className="section-title"><div><p className="eyebrow">Symptom drawer</p><h2>Possible symptoms</h2></div></div>
      <div className="search-box drawer-search"><Search size={17} /><input placeholder="Type 1 or 2 letters" value={query} onChange={(event) => setQuery(event.target.value)} /></div>
      <div className="symptom-list">
        {filtered.map((symptom) => (
          <button
            key={symptom}
            className={selected.includes(symptom) ? "active" : ""}
            draggable
            onDragStart={(event) => event.dataTransfer.setData("text/plain", symptom)}
            onClick={() => onSelect(symptom)}
          >
            {symptom}
          </button>
        ))}
      </div>
    </aside>
  );
}
