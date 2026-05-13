from fastapi import UploadFile


async def extract_pdf_text(file: UploadFile) -> str:
    content = await file.read()
    text = ""
    try:
        import fitz

        with fitz.open(stream=content, filetype="pdf") as document:
            text = "\n".join(page.get_text() for page in document)
    except Exception:
        text = ""

    return text.strip()


def summarize_report_text(raw_text: str) -> str:
    if not raw_text:
        return "No readable text was extracted. This prototype supports text-based PDFs only."

    lowered = raw_text.lower()
    highlights = []
    for keyword in ["hemoglobin", "glucose", "cholesterol", "blood pressure", "wbc", "platelet"]:
        if keyword in lowered:
            highlights.append(keyword)

    if highlights:
        return f"Readable report text extracted. Mentioned areas include: {', '.join(highlights)}. This is a simple mock summary and not a diagnosis."
    return "Readable report text extracted. No common lab keywords were detected by the mock summarizer."
