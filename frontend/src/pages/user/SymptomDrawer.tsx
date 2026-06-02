import { useState } from "react";
import { Search } from "lucide-react";
import { symptomGroups } from "../../data";

interface SymptomDrawerProps {
  selected: string[];
  onSelect: (symptom: string) => void;
}

export function SymptomDrawer({ selected, onSelect }: SymptomDrawerProps) {
  const [query, setQuery] = useState("");
  const filteredGroups = symptomGroups
    .map((group) => ({
      ...group,
      symptoms: group.symptoms.filter((symptom) => query.length < 1 || symptom.toLowerCase().includes(query.toLowerCase())),
    }))
    .filter((group) => group.symptoms.length > 0);
  return (
    <aside className="symptom-drawer" data-section="symptom-drawer">
      <div className="section-title"><div><p className="eyebrow">Symptom library</p><h2>Add symptoms</h2></div></div>
      <div className="search-box drawer-search"><Search size={17} /><input placeholder="Search symptoms" value={query} onChange={(event) => setQuery(event.target.value)} /></div>
      <div className="symptom-list">
        {filteredGroups.map((group) => (
          <section className="symptom-group" key={group.category}>
            <h3>{group.category}</h3>
            {group.symptoms.map((symptom) => (
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
          </section>
        ))}
      </div>
    </aside>
  );
}
