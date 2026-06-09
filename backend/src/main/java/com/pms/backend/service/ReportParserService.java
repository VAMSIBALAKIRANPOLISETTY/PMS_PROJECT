package com.pms.backend.service;

import com.pms.backend.model.LabObservation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportParserService {
    private static final Pattern LAB_LINE = Pattern.compile(
            "^\\s*([A-Za-z][A-Za-z0-9 /()\\-]{2,60})\\s+([<>]?\\d+(?:\\.\\d+)?)\\s*([A-Za-z/%\\u00b5]+)?\\s*(?:\\(?([0-9.\\-\\s<>]+)\\)?)?\\s*(High|Low|Normal|Critical|Abnormal|H|L)?\\s*$",
            Pattern.CASE_INSENSITIVE
    );

    public ParsedReport parse(MultipartFile file, String pastedText) {
        String fileText = extractText(file);
        String text = firstUseful(fileText, pastedText);
        List<LabObservation> observations = extractObservations(text);
        return new ParsedReport(
                text,
                observations,
                detectProvider(text),
                containsCriticalLanguage(text)
        );
    }

    public List<LabObservation> extractObservations(String text) {
        List<LabObservation> observations = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return observations;
        }
        for (String rawLine : text.split("\\R")) {
            String line = rawLine.replace(":", " ").trim();
            if (line.length() < 5 || line.length() > 140) {
                continue;
            }
            Matcher matcher = LAB_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            String name = clean(matcher.group(1));
            String value = clean(matcher.group(2));
            String unit = clean(matcher.group(3));
            String range = clean(matcher.group(4));
            String flag = normalizeFlag(matcher.group(5));
            if (name != null && value != null) {
                observations.add(new LabObservation(name, value, unit, range, flag));
            }
            if (observations.size() >= 40) {
                break;
            }
        }
        return observations;
    }

    private String extractText(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        try {
            if (name.endsWith(".pdf")) {
                try (PDDocument document = PDDocument.load(file.getInputStream())) {
                    return new PDFTextStripper().getText(document);
                }
            }
            if (isImage(name, contentType)) {
                return null;
            }
            if (!name.endsWith(".txt") && !contentType.startsWith("text/") && !contentType.isBlank()) {
                throw new IllegalArgumentException("Report upload supports PDF, text, JPG, PNG, or WebP files.");
            }
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Report text could not be extracted from the uploaded file.");
        }
    }

    private boolean isImage(String name, String contentType) {
        return contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp");
    }

    private String firstUseful(String fileText, String pastedText) {
        if (fileText != null && !fileText.isBlank()) {
            return fileText.trim();
        }
        return pastedText == null ? "" : pastedText.trim();
    }

    private String detectProvider(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.lines()
                .map(String::trim)
                .filter(line -> line.length() >= 4 && line.length() <= 120)
                .filter(line -> line.toLowerCase(Locale.ROOT).matches(".*(lab|laboratory|diagnostic|hospital|clinic|medical).*"))
                .findFirst()
                .orElse(null);
    }

    private boolean containsCriticalLanguage(String text) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("critical")
                || lower.contains("panic value")
                || lower.contains("urgent")
                || lower.contains("immediate attention")
                || lower.contains("severely abnormal");
    }

    private String normalizeFlag(String value) {
        String clean = clean(value);
        if (clean == null) {
            return null;
        }
        return switch (clean.toLowerCase(Locale.ROOT)) {
            case "h" -> "High";
            case "l" -> "Low";
            default -> clean;
        };
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record ParsedReport(
            String text,
            List<LabObservation> observations,
            String providerName,
            boolean criticalLanguage
    ) {}
}
