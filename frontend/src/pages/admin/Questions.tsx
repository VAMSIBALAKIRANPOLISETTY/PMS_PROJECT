import type { Question } from "../../types";

export function Questions({ questions }: { questions: Question[] }) {
  return (
    <section className="panel wide" data-section="admin-questions">
      <div className="section-title"><div><p className="eyebrow">Dynamic questioning</p><h2>Question bank</h2></div><button className="primary-button">Create question</button></div>
      <div className="question-list">{questions.map((question, index) => <div key={question.id}><span>Q{index + 1}</span><strong>{question.prompt}</strong><small>{question.active ? "Active" : "Inactive"} | {question.symptomKey}</small></div>)}</div>
    </section>
  );
}
