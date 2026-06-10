package com.pms.backend.service;

import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AssessmentDtos.AssessmentResponse;
import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.model.AppUser;
import com.pms.backend.model.Assessment;
import com.pms.backend.model.AssessmentSourceType;
import com.pms.backend.model.AssessmentStatus;
import com.pms.backend.model.HealthTimelineRecord;
import com.pms.backend.model.RiskLevel;
import com.pms.backend.model.Role;
import com.pms.backend.repository.AssessmentRepository;
import com.pms.backend.repository.HealthTimelineRecordRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssessmentService {
    private static final float PDF_MARGIN = 52f;
    private static final float PDF_WIDTH = PDRectangle.LETTER.getWidth() - (PDF_MARGIN * 2);
    private static final float PDF_TOP = 742f;
    private static final float PDF_BOTTOM = 64f;
    private static final DateTimeFormatter PDF_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final PDFont PDF_TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont PDF_BODY_FONT = PDType1Font.HELVETICA;
    private static final PDFont PDF_LABEL_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDFont PDF_MONO_FONT = PDType1Font.COURIER;

    private final AssessmentRepository assessmentRepository;
    private final RiskEngineService riskEngineService;
    private final AiInsightService aiInsightService;
    private final ReportParserService reportParserService;
    private final HealthTimelineRecordRepository timelineRepository;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            RiskEngineService riskEngineService,
            AiInsightService aiInsightService,
            ReportParserService reportParserService,
            HealthTimelineRecordRepository timelineRepository
    ) {
        this.assessmentRepository = assessmentRepository;
        this.riskEngineService = riskEngineService;
        this.aiInsightService = aiInsightService;
        this.reportParserService = reportParserService;
        this.timelineRepository = timelineRepository;
    }

    public AssessmentResponse create(AppUser user, AssessmentRequest request) {
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Patient access is required to create an assessment.");
        }
        if (assessmentRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.PENDING_FOLLOW_UP).isPresent()) {
            throw new IllegalArgumentException("Finish or discard your current assessment draft before starting another.");
        }
        List<String> symptoms = cleanSymptoms(request.symptoms());
        AssessmentRequest normalizedRequest = new AssessmentRequest(
                symptoms,
                request.severity(),
                request.durationDays(),
                request.temperatureAvailable(),
                request.temperatureF(),
                request.chronicCondition(),
                request.includeConnectedHealth(),
                request.connectedHealthRecordIds()
        );
        RiskEngineService.RiskResult result = riskEngineService.calculate(normalizedRequest);
        Assessment assessment = new Assessment();
        assessment.setUser(user);
        assessment.setSourceType(AssessmentSourceType.SYMPTOM);
        assessment.setSourceName("Guided symptom intake");
        assessment.setSymptoms(symptoms);
        assessment.setMainSymptom(String.join(", ", symptoms));
        assessment.setSeverity(request.severity());
        assessment.setDurationDays(request.durationDays());
        assessment.setTemperatureAvailable(Boolean.TRUE.equals(request.temperatureAvailable()));
        assessment.setTemperatureF(Boolean.TRUE.equals(request.temperatureAvailable()) ? request.temperatureF() : null);
        assessment.setChronicCondition(request.chronicCondition());
        assessment.setConnectedHealthSummary(connectedHealthSummary(user, request.includeConnectedHealth(), request.connectedHealthRecordIds()));
        assessment.setRiskScore(result.score());
        assessment.setRiskLevel(result.level());
        assessment.setReasons(result.reasons());
        List<String> connectedFollowUps = connectedHealthFollowUps(assessment.getConnectedHealthSummary());
        assessment.setFollowUpQuestions(mergeFollowUps(
                connectedFollowUps.isEmpty() ? result.followUps() : connectedFollowUps,
                connectedFollowUps.isEmpty() ? List.of() : result.followUps(),
                aiInsightService.assessmentFollowUps(user, assessment, result).questions()
        ));
        assessment.setSuggestions(result.suggestions());
        assessment.setStatus(AssessmentStatus.PENDING_FOLLOW_UP);
        assessment.setUrgentWarning(result.urgentWarning());
        return toResponse(assessmentRepository.save(assessment));
    }

    public AssessmentResponse answerFollowUps(AppUser user, Long assessmentId, FollowUpAnswerRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found."));
        if (user.getRole() != Role.USER || !assessment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Assessment does not belong to this user.");
        }
        if (completed(assessment)) {
            throw new IllegalArgumentException("This assessment has already been completed.");
        }
        List<String> answers = request.answers().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(7)
                .toList();
        if (answers.size() != assessment.getFollowUpQuestions().size()) {
            throw new IllegalArgumentException("Answer every follow-up question before preparing your care guide.");
        }
        if (answers.stream().anyMatch(answer -> !answer.matches("^(Yes|No|Not sure)( \\| Note: .+)?$"))) {
            throw new IllegalArgumentException("Choose Yes, No, or Not sure for every follow-up question.");
        }
        RiskEngineService.RiskResult result = riskEngineService.refineWithFollowUps(assessment, answers);
        assessment.setFollowUpAnswers(answers);
        assessment.setRiskScore(result.score());
        assessment.setRiskLevel(result.level());
        assessment.setReasons(result.reasons());
        assessment.setSuggestions(result.suggestions());
        applyCarePrep(assessment, aiInsightService.forAssessment(user, assessment, result), result);
        assessment.setStatus(AssessmentStatus.COMPLETED);
        return toResponse(assessmentRepository.save(assessment));
    }

    public AssessmentResponse uploadReport(AppUser user, MultipartFile file, String reportText) {
        return uploadReport(user, file, reportText, false, List.of());
    }

    public AssessmentResponse uploadReport(AppUser user, MultipartFile file, String reportText, Boolean includeConnectedHealth, List<Long> connectedHealthRecordIds) {
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Patient access is required to upload a report.");
        }
        if (assessmentRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.PENDING_FOLLOW_UP).isPresent()) {
            throw new IllegalArgumentException("Finish or discard your current assessment draft before starting another.");
        }
        ReportParserService.ParsedReport parsed = reportParserService.parse(file, reportText);
        if (parsed.text() == null || parsed.text().isBlank()) {
            throw new IllegalArgumentException("Upload a readable report file or paste report text.");
        }
        String reportName = cleanReportName(file == null ? null : file.getOriginalFilename());
        Assessment assessment = new Assessment();
        assessment.setUser(user);
        assessment.setSourceType(AssessmentSourceType.REPORT);
        assessment.setSourceName("Report-based assessment");
        assessment.setSourceRecordId(UUID.randomUUID().toString());
        assessment.setReportName(reportName);
        assessment.setReportDate(LocalDateTime.now());
        assessment.setReportProvider(parsed.providerName());
        assessment.setConnectedHealthSummary(connectedHealthSummary(user, includeConnectedHealth, connectedHealthRecordIds));
        assessment.setReportText(truncate(parsed.text(), 8000));
        assessment.setExtractedObservations(parsed.observations());
        assessment.setMainSymptom("Report review");
        assessment.setSymptoms(List.of("Report review"));
        assessment.setSeverity(parsed.criticalLanguage() ? 8 : 3);
        assessment.setDurationDays(0);
        assessment.setTemperatureAvailable(false);
        assessment.setChronicCondition("Report context");
        assessment.setRiskScore(parsed.criticalLanguage() ? 85 : 20);
        assessment.setRiskLevel(parsed.criticalLanguage() ? RiskLevel.HIGH : RiskLevel.LOW);
        assessment.setReasons(parsed.criticalLanguage()
                ? List.of("The report text includes urgent or critical wording that needs timely medical review.")
                : List.of("The report was saved for care-preparation review and clinician discussion."));
        assessment.setSuggestions(List.of(
                "Review notable values with a qualified clinician.",
                "Keep the original report available for your appointment."
        ));
        assessment.setUrgentWarning(parsed.criticalLanguage()
                ? "The uploaded report contains urgent or critical wording. Contact your clinician or local urgent care service promptly if this matches your current condition."
                : null);
        assessment.setFollowUpQuestions(reportFollowUps(reportName, parsed.text(), assessment.getConnectedHealthSummary()));
        assessment.setStatus(AssessmentStatus.PENDING_FOLLOW_UP);
        return toResponse(assessmentRepository.save(assessment));
    }

    public AssessmentResponse answerReportFollowUps(AppUser user, Long assessmentId, FollowUpAnswerRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Report assessment not found."));
        if (user.getRole() != Role.USER || !assessment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Report assessment does not belong to this user.");
        }
        if (assessment.getSourceType() != AssessmentSourceType.REPORT) {
            throw new IllegalArgumentException("This assessment was not created from a report.");
        }
        if (completed(assessment)) {
            throw new IllegalArgumentException("This report assessment has already been completed.");
        }
        List<String> answers = request.answers().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(7)
                .toList();
        if (answers.size() != assessment.getFollowUpQuestions().size()) {
            throw new IllegalArgumentException("Answer every follow-up question before preparing the report guide.");
        }
        if (answers.stream().anyMatch(answer -> !answer.matches("^(Yes|No|Not sure)( \\| Note: .+)?$"))) {
            throw new IllegalArgumentException("Choose Yes, No, or Not sure for every report follow-up question.");
        }
        assessment.setFollowUpAnswers(answers);
        RiskEngineService.RiskResult result = new RiskEngineService.RiskResult(
                assessment.getRiskScore() == null ? 20 : assessment.getRiskScore(),
                assessment.getRiskLevel() == null ? RiskLevel.LOW : assessment.getRiskLevel(),
                assessment.getReasons(),
                assessment.getFollowUpQuestions(),
                assessment.getSuggestions(),
                assessment.getUrgentWarning()
        );
        applyCarePrep(
                assessment,
                aiInsightService.forReport(user, assessment.getReportName(), reportPromptText(assessment), answers),
                result
        );
        assessment.setStatus(AssessmentStatus.COMPLETED);
        return toResponse(assessmentRepository.save(assessment));
    }

    public List<AssessmentResponse> listFor(AppUser user) {
        List<Assessment> rows = user.getRole() == Role.ADMIN
                ? assessmentRepository.findAllByOrderByCreatedAtDesc()
                : assessmentRepository.findByUserOrderByCreatedAtDesc(user);
        return rows.stream().filter(this::completed).map(this::toResponse).toList();
    }

    public List<AssessmentResponse> reportHistoryFor(AppUser user) {
        return listFor(user).stream()
                .filter(assessment -> assessment.sourceType() == AssessmentSourceType.REPORT)
                .toList();
    }

    public AssessmentResponse pendingFor(AppUser user) {
        if (user.getRole() != Role.USER) {
            return null;
        }
        return assessmentRepository.findFirstByUserAndStatusOrderByCreatedAtDesc(user, AssessmentStatus.PENDING_FOLLOW_UP)
                .map(this::toResponse)
                .orElse(null);
    }

    public void discardDraft(AppUser user, Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment draft not found."));
        if (user.getRole() != Role.USER || !assessment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Assessment draft does not belong to this user.");
        }
        if (completed(assessment)) {
            throw new IllegalArgumentException("Completed assessments cannot be discarded.");
        }
        assessmentRepository.delete(assessment);
    }

    public byte[] exportAssessment(AppUser user, Long assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found."));
        if (user.getRole() != Role.ADMIN && !assessment.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Assessment does not belong to this user.");
        }
        if (!completed(assessment)) {
            throw new IllegalArgumentException("Only completed assessments can be exported.");
        }
        return renderPdf(user, List.of(assessment), false);
    }

    public byte[] exportHistory(AppUser user) {
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Patient access is required to export assessment history.");
        }
        List<Assessment> assessments = assessmentRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .filter(this::completed)
                .toList();
        if (assessments.isEmpty()) {
            throw new IllegalArgumentException("No completed assessments are available to export.");
        }
        return renderPdf(user, assessments, true);
    }

    public AssessmentResponse toResponse(Assessment assessment) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getUser().getId(),
                assessment.getUser().getFullName(),
                assessment.getMainSymptom(),
                assessment.getSymptoms().isEmpty() ? List.of(assessment.getMainSymptom()) : assessment.getSymptoms(),
                assessment.getSeverity(),
                assessment.getDurationDays(),
                assessment.getTemperatureAvailable() == null ? assessment.getTemperatureF() != null : assessment.getTemperatureAvailable(),
                assessment.getTemperatureF(),
                assessment.getChronicCondition(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getStatus() == null ? AssessmentStatus.COMPLETED : assessment.getStatus(),
                assessment.getReasons(),
                assessment.getSuggestions(),
                assessment.getFollowUpQuestions(),
                assessment.getFollowUpAnswers(),
                assessment.getCareSummary(),
                assessment.getExplanation(),
                assessment.getPossibleDirections(),
                assessment.getUrgentWarning(),
                assessment.getMonitoringPlan(),
                assessment.getCareTips(),
                assessment.getDoctorPrepQuestions(),
                assessment.getTrustedSourceLinks(),
                assessment.getAiMode(),
                assessment.getSourceType() == null ? AssessmentSourceType.SYMPTOM : assessment.getSourceType(),
                assessment.getSourceName(),
                assessment.getSourceRecordId(),
                assessment.getReportName(),
                assessment.getReportDate(),
                assessment.getReportProvider(),
                assessment.getConnectedHealthSummary(),
                assessment.getUser().getProfilePhotoDataUrl(),
                assessment.getExtractedObservations(),
                completed(assessment) ? "/api/assessments/" + assessment.getId() + "/export" : null,
                assessment.getCreatedAt()
        );
    }

    private void applyCarePrep(
            Assessment assessment,
            AiInsightService.CarePrepInsight insight,
            RiskEngineService.RiskResult result
    ) {
        assessment.setCareSummary(insight.careSummary());
        assessment.setExplanation(insight.explanation());
        assessment.setPossibleDirections(insight.possibleDirections());
        assessment.setUrgentWarning(result.urgentWarning() != null ? result.urgentWarning() : insight.urgentWarning());
        assessment.setMonitoringPlan(insight.monitoringPlan());
        assessment.setCareTips(insight.careTips());
        assessment.setDoctorPrepQuestions(insight.doctorPrepQuestions());
        assessment.setTrustedSourceLinks(insight.trustedSourceLinks());
        assessment.setAiMode(insight.aiMode());
    }

    private List<String> cleanSymptoms(List<String> symptoms) {
        return new ArrayList<>(new LinkedHashSet<>(symptoms.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .limit(5)
                .toList()));
    }

    @SafeVarargs
    private List<String> mergeFollowUps(List<String> required, List<String>... candidateGroups) {
        List<String> merged = new ArrayList<>(new LinkedHashSet<>(required == null ? List.of() : required));
        if (candidateGroups == null) {
            return merged.stream().limit(7).toList();
        }
        for (List<String> candidates : candidateGroups) {
            for (String candidate : candidates == null ? List.<String>of() : candidates) {
                String value = candidate == null ? "" : candidate.trim();
                if (!value.isBlank() && merged.size() < 7 && !merged.contains(value)) {
                    merged.add(value);
                }
            }
        }
        return merged.stream().limit(7).toList();
    }

    private List<String> reportFollowUps(String reportName, String reportText, String connectedHealthSummary) {
        List<String> questions = mergeFollowUps(List.of(
                "Do you currently have symptoms related to this report?",
                "Has a clinician already reviewed this report with you?",
                "Are any values marked high, low, abnormal, or critical?",
                "Do you want to discuss lifestyle, medication, or follow-up testing questions with your clinician?"
        ), reportValueFollowUps(reportText), connectedHealthFollowUps(connectedHealthSummary),
                aiInsightService.reportFollowUps(reportName, reportText, connectedHealthSummary).questions());
        return questions.stream().limit(7).toList();
    }

    private List<String> reportValueFollowUps(String reportText) {
        String value = normalizeQuestionContext(reportText);
        List<String> questions = new ArrayList<>();
        if (value.matches(".*(hemoglobin|hb |anemia|iron|ferritin).*")) {
            questions.add("Have you had tiredness, dizziness, breathlessness, pale skin, or unusual weakness around this report?");
        }
        if (value.matches(".*(glucose|hba1c|sugar|diabetes).*")) {
            questions.add("Was this sugar-related result fasting, after food, or part of diabetes monitoring?");
        }
        if (value.matches(".*(cholesterol|ldl|hdl|triglyceride).*")) {
            questions.add("Has a clinician discussed heart-risk factors, diet, activity, or medicines related to these lipid values?");
        }
        if (value.matches(".*(vitamin d|b12|calcium).*")) {
            questions.add("Do you have fatigue, muscle aches, bone pain, numbness, or diet changes connected to these nutrition values?");
        }
        if (value.matches(".*(tsh|thyroid).*")) {
            questions.add("Have you noticed weight, sleep, heat or cold tolerance, mood, or heart-rate changes?");
        }
        if (value.matches(".*(creatinine|kidney|egfr|urea).*")) {
            questions.add("Have hydration, urine changes, swelling, blood pressure, or kidney history been discussed with your clinician?");
        }
        return questions;
    }

    private List<String> connectedHealthFollowUps(String connectedHealthSummary) {
        String value = normalizeQuestionContext(connectedHealthSummary);
        List<String> questions = new ArrayList<>();
        if (value.matches(".*(heart rate|resting heart|pulse|bpm).*")) {
            questions.add("Did the connected heart-rate change happen with palpitations, chest discomfort, dizziness, or breathlessness?");
        }
        if (value.matches(".*(sleep|hours).*")) {
            questions.add("Did reduced or disrupted sleep happen before the symptoms or report concern?");
        }
        if (value.matches(".*(steps|activity|workout|movement).*")) {
            questions.add("Did your symptoms start during activity or make normal walking and movement harder?");
        }
        if (value.matches(".*(stress|high stress).*")) {
            questions.add("Did high stress appear before the symptoms, appetite change, or report concern?");
        }
        if (value.matches(".*(oxygen|spo2|blood oxygen).*")) {
            questions.add("Have you had breathing difficulty, blue lips, or worsening shortness of breath with oxygen changes?");
        }
        return questions;
    }

    private String normalizeQuestionContext(String value) {
        return (value == null ? "" : value).toLowerCase().replaceAll("\\s+", " ");
    }

    private String cleanReportName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Uploaded health report";
        }
        return fileName.trim();
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private List<String> nullList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private String nullSafe(String value) {
        return value == null ? "Not recorded" : value;
    }

    private String safePdf(String value) {
        return nullSafe(value).replaceAll("[^\\x20-\\x7E]", " ");
    }

    private byte[] renderPdf(AppUser user, List<Assessment> assessments, boolean includeCover) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(document, LocalDateTime.now());
            try {
                if (includeCover) {
                    writeHistoryCover(writer, user, assessments);
                }
                for (int index = 0; index < assessments.size(); index += 1) {
                    if (includeCover || index > 0) {
                        writer.newPage();
                    }
                    writeAssessmentRecord(writer, assessments.get(index), index + 1, assessments.size());
                }
            } finally {
                writer.close();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException(includeCover
                    ? "Assessment history export could not be created."
                    : "Assessment export could not be created.");
        }
    }

    private void writeHistoryCover(PdfWriter writer, AppUser user, List<Assessment> assessments) throws IOException {
        writer.writeTitle("PMS Health Assessment History");
        writer.writeSubtitle("Patient care-preparation record");
        writer.writeDivider();
        writer.writeFactRows(List.of(
                "Patient: " + safePdf(user.getFullName()),
                "Generated: " + writer.generatedAt().format(PDF_DATE_FORMAT),
                "Completed records: " + assessments.size(),
                "Report assessments: " + assessments.stream().filter(item -> item.getSourceType() == AssessmentSourceType.REPORT).count(),
                "Symptom assessments: " + assessments.stream().filter(item -> item.getSourceType() != AssessmentSourceType.REPORT).count(),
                "High awareness records: " + assessments.stream().filter(item -> item.getRiskLevel() == RiskLevel.HIGH).count()
        ));
        writer.writeSection("Summary");
        writer.writeParagraph("This document combines completed symptom and report assessments from the patient's care-preparation history.");
        writer.writeSection("Included records");
        for (Assessment assessment : assessments) {
            writer.writeBullet(safePdf(formatDateForPdf(assessment.getCreatedAt()))
                    + " | "
                    + safePdf(assessment.getSourceType() == AssessmentSourceType.REPORT ? "Report assessment" : "Symptom assessment")
                    + " | "
                    + safePdf(primaryRecordTitle(assessment))
                    + " | Risk "
                    + safePdf(assessment.getRiskLevel() + " (" + assessment.getRiskScore() + ")"));
        }
    }

    private void writeAssessmentRecord(PdfWriter writer, Assessment assessment, int recordNumber, int totalRecords) throws IOException {
        writer.writeTitle("PMS Health Care-Preparation Record");
        writer.writeSubtitle("Record " + recordNumber + " of " + totalRecords);
        writer.writeDivider();
        writer.writeFactRows(List.of(
                "Record ID: ASM-" + assessment.getId(),
                "Patient: " + safePdf(assessment.getUser().getFullName()),
                "Recorded: " + safePdf(formatDateForPdf(assessment.getCreatedAt())),
                "Source type: " + safePdf(sourceLabel(assessment)),
                "Primary focus: " + safePdf(primaryRecordTitle(assessment)),
                "Risk: " + safePdf(assessment.getRiskLevel() + " (" + assessment.getRiskScore() + ")"),
                "Severity: " + assessment.getSeverity() + "/10",
                "Duration: " + assessment.getDurationDays() + " day" + (assessment.getDurationDays() == 1 ? "" : "s"),
                "Temperature: " + safePdf(assessment.getTemperatureAvailable() && assessment.getTemperatureF() != null ? assessment.getTemperatureF() + " F" : "Not available"),
                "Chronic-condition context: " + safePdf(nullSafe(assessment.getChronicCondition()))
        ));
        writer.writeSection("Patient context");
        writer.writeFactRows(patientContextRows(assessment.getUser()));
        if (assessment.getUrgentWarning() != null) {
            writer.writeSection("Urgent guidance");
            writer.writeParagraph(assessment.getUrgentWarning());
        }
        if (assessment.getConnectedHealthSummary() != null) {
            writer.writeSection("Connected health context");
            writer.writeParagraph(assessment.getConnectedHealthSummary());
        }
        if (assessment.getSourceType() == AssessmentSourceType.REPORT) {
            writer.writeSection("Report summary");
            writer.writeFactRows(List.of(
                    "Report name: " + safePdf(nullSafe(assessment.getReportName())),
                    "Report provider: " + safePdf(nullSafe(assessment.getReportProvider()))
            ));
            writer.writeObservationTable(assessment);
        }
        writer.writeSection("Care summary");
        writer.writeParagraph(nullSafe(assessment.getCareSummary()));
        writer.writeSection("Why this matters");
        writer.writeParagraph(nullSafe(assessment.getExplanation()));
        writer.writeSection("Possible directions");
        writer.writeBullets(nullList(assessment.getPossibleDirections()));
        writer.writeSection("Monitoring and care tips");
        writer.writeBullets(mergePdfLists(assessment.getMonitoringPlan(), assessment.getCareTips()));
        writer.writeSection("Follow-up questions and answers");
        List<String> followUps = new ArrayList<>();
        for (int index = 0; index < assessment.getFollowUpQuestions().size(); index += 1) {
            String question = assessment.getFollowUpQuestions().get(index);
            String answer = index < assessment.getFollowUpAnswers().size() ? assessment.getFollowUpAnswers().get(index) : "No answer recorded";
            followUps.add(question + " | " + answer);
        }
        writer.writeBullets(followUps);
        writer.writeSection("Doctor-preparation questions");
        writer.writeBullets(nullList(assessment.getDoctorPrepQuestions()));
        writer.writeSection("Trusted sources");
        writer.writeBullets(nullList(assessment.getTrustedSourceLinks()));
    }

    private List<String> patientContextRows(AppUser user) {
        List<String> rows = new ArrayList<>();
        rows.add("Full name: " + safePdf(user.getFullName()));
        rows.add("Age: " + (user.getAge() == null ? "Not recorded" : user.getAge() + " years"));
        rows.add("Sex: " + safePdf(nullSafe(user.getGender())));
        rows.add("Height: " + safePdf(formatHeightForPdf(user.getHeightCm())));
        rows.add("Weight: " + safePdf(user.getWeightKg() == null ? "Not recorded" : user.getWeightKg() + " kg"));
        rows.add("Blood type: " + safePdf(nullSafe(user.getBloodType())));
        rows.add("Allergies: " + safePdf(nullSafe(user.getAllergies())));
        rows.add("Chronic conditions: " + safePdf(nullSafe(user.getChronicConditions())));
        rows.add("Emergency contact: " + safePdf(nullSafe(user.getEmergencyContactName())));
        rows.add("Preferred hospital: " + safePdf(nullSafe(user.getPreferredHospital())));
        return rows;
    }

    private List<String> mergePdfLists(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>();
        merged.addAll(nullList(first));
        merged.addAll(nullList(second));
        return merged;
    }

    private String sourceLabel(Assessment assessment) {
        return assessment.getSourceType() == AssessmentSourceType.REPORT ? "Report assessment" : "Symptom assessment";
    }

    private String primaryRecordTitle(Assessment assessment) {
        return assessment.getSourceType() == AssessmentSourceType.REPORT
                ? nullSafe(assessment.getReportName())
                : nullSafe(assessment.getMainSymptom());
    }

    private String formatDateForPdf(LocalDateTime value) {
        return value == null ? "Not recorded" : value.format(PDF_DATE_FORMAT);
    }

    private String formatHeightForPdf(Double value) {
        if (value == null || !Double.isFinite(value)) {
            return "Not recorded";
        }
        int roundedCm = (int) Math.round(value);
        int totalInches = (int) Math.round(value / 2.54d);
        int feet = totalInches / 12;
        int inches = totalInches % 12;
        return roundedCm + " cm (" + feet + " ft " + inches + " in)";
    }

    private final class PdfWriter implements AutoCloseable {
        private final PDDocument document;
        private final LocalDateTime generatedAt;
        private PDPage page;
        private PDPageContentStream content;
        private float y;
        private int pageNumber;

        private PdfWriter(PDDocument document, LocalDateTime generatedAt) throws IOException {
            this.document = document;
            this.generatedAt = generatedAt;
            newPage();
        }

        private LocalDateTime generatedAt() {
            return generatedAt;
        }

        private void newPage() throws IOException {
            closeContent();
            page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            pageNumber += 1;
            y = PDF_TOP;
            writeFooter();
        }

        private void writeTitle(String text) throws IOException {
            ensureSpace(28);
            writeLine(text, PDF_TITLE_FONT, 18, PDF_MARGIN, y);
            y -= 24;
        }

        private void writeSubtitle(String text) throws IOException {
            ensureSpace(18);
            writeLine(text, PDF_BODY_FONT, 10, PDF_MARGIN, y);
            y -= 18;
        }

        private void writeDivider() throws IOException {
            ensureSpace(16);
            content.moveTo(PDF_MARGIN, y);
            content.lineTo(PDF_MARGIN + PDF_WIDTH, y);
            content.stroke();
            y -= 16;
        }

        private void writeSection(String title) throws IOException {
            ensureSpace(20);
            writeLine(title, PDF_LABEL_FONT, 11, PDF_MARGIN, y);
            y -= 16;
        }

        private void writeParagraph(String text) throws IOException {
            List<String> lines = wrap(safePdf(text), PDF_BODY_FONT, 10, PDF_WIDTH);
            ensureSpace((lines.size() * 13f) + 6f);
            for (String line : lines) {
                writeLine(line, PDF_BODY_FONT, 10, PDF_MARGIN, y);
                y -= 13;
            }
            y -= 6;
        }

        private void writeFactRows(List<String> rows) throws IOException {
            for (String row : rows) {
                List<String> lines = wrap(safePdf(row), PDF_BODY_FONT, 10, PDF_WIDTH);
                ensureSpace((lines.size() * 12f) + 4f);
                for (String line : lines) {
                    writeLine(line, PDF_BODY_FONT, 10, PDF_MARGIN, y);
                    y -= 12;
                }
                y -= 4;
            }
            y -= 4;
        }

        private void writeBullet(String text) throws IOException {
            writeBullets(List.of(text));
        }

        private void writeBullets(List<String> items) throws IOException {
            if (items == null || items.isEmpty()) {
                writeParagraph("Not recorded.");
                return;
            }
            for (String item : items) {
                List<String> lines = wrap(safePdf(item), PDF_BODY_FONT, 10, PDF_WIDTH - 14f);
                ensureSpace((lines.size() * 12f) + 5f);
                writeLine("-", PDF_BODY_FONT, 10, PDF_MARGIN, y);
                for (int index = 0; index < lines.size(); index += 1) {
                    writeLine(lines.get(index), PDF_BODY_FONT, 10, PDF_MARGIN + 12f, y);
                    y -= 12;
                }
                y -= 5;
            }
        }

        private void writeObservationTable(Assessment assessment) throws IOException {
            List<com.pms.backend.model.LabObservation> observations = assessment.getExtractedObservations();
            if (observations == null || observations.isEmpty()) {
                writeSection("Extracted report values");
                writeParagraph("No structured values were extracted from this report.");
                return;
            }
            writeSection("Extracted report values");
            drawObservationHeader();
            for (com.pms.backend.model.LabObservation observation : observations.stream().limit(12).toList()) {
                writeObservationRow(observation);
            }
            y -= 6;
        }

        private void drawObservationHeader() throws IOException {
            ensureSpace(22);
            float testX = PDF_MARGIN;
            float valueX = PDF_MARGIN + 220f;
            float rangeX = PDF_MARGIN + 330f;
            float flagX = PDF_MARGIN + 455f;
            writeLine("Test", PDF_LABEL_FONT, 9, testX, y);
            writeLine("Value", PDF_LABEL_FONT, 9, valueX, y);
            writeLine("Range", PDF_LABEL_FONT, 9, rangeX, y);
            writeLine("Flag", PDF_LABEL_FONT, 9, flagX, y);
            y -= 10;
            content.moveTo(PDF_MARGIN, y);
            content.lineTo(PDF_MARGIN + PDF_WIDTH, y);
            content.stroke();
            y -= 8;
        }

        private void writeObservationRow(com.pms.backend.model.LabObservation observation) throws IOException {
            float testX = PDF_MARGIN;
            float valueX = PDF_MARGIN + 220f;
            float rangeX = PDF_MARGIN + 330f;
            float flagX = PDF_MARGIN + 455f;
            List<String> testLines = wrap(safePdf(nullSafe(observation.getTestName())), PDF_MONO_FONT, 9, 205f);
            List<String> valueLines = wrap(safePdf(nullSafe(observation.getValueText()) + (observation.getUnit() == null ? "" : " " + observation.getUnit())), PDF_MONO_FONT, 9, 100f);
            List<String> rangeLines = wrap(safePdf(nullSafe(observation.getReferenceRange())), PDF_MONO_FONT, 9, 115f);
            List<String> flagLines = wrap(safePdf(nullSafe(observation.getFlag())), PDF_MONO_FONT, 9, 45f);
            int rowLines = Math.max(Math.max(testLines.size(), valueLines.size()), Math.max(rangeLines.size(), flagLines.size()));
            ensureSpace((rowLines * 12f) + 10f);
            for (int index = 0; index < rowLines; index += 1) {
                if (index < testLines.size()) writeLine(testLines.get(index), PDF_MONO_FONT, 9, testX, y);
                if (index < valueLines.size()) writeLine(valueLines.get(index), PDF_MONO_FONT, 9, valueX, y);
                if (index < rangeLines.size()) writeLine(rangeLines.get(index), PDF_MONO_FONT, 9, rangeX, y);
                if (index < flagLines.size()) writeLine(flagLines.get(index), PDF_MONO_FONT, 9, flagX, y);
                y -= 12;
            }
            content.moveTo(PDF_MARGIN, y + 4);
            content.lineTo(PDF_MARGIN + PDF_WIDTH, y + 4);
            content.stroke();
            y -= 6;
        }

        private void ensureSpace(float needed) throws IOException {
            if (y - needed < PDF_BOTTOM) {
                newPage();
            }
        }

        private void writeLine(String text, PDFont font, float size, float x, float yPosition) throws IOException {
            content.beginText();
            content.setFont(font, size);
            content.newLineAtOffset(x, yPosition);
            content.showText(safePdf(text));
            content.endText();
        }

        private List<String> wrap(String text, PDFont font, float size, float width) throws IOException {
            List<String> lines = new ArrayList<>();
            String normalized = safePdf(text).trim();
            if (normalized.isEmpty()) {
                lines.add("Not recorded");
                return lines;
            }
            StringBuilder current = new StringBuilder();
            for (String word : normalized.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (font.getStringWidth(candidate) / 1000 * size <= width) {
                    current = new StringBuilder(candidate);
                } else if (current.isEmpty()) {
                    lines.add(word);
                } else {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private void writeFooter() throws IOException {
            writeLine("Generated " + generatedAt.format(PDF_DATE_FORMAT), PDF_BODY_FONT, 8, PDF_MARGIN, 34);
            writeLine("Page " + pageNumber, PDF_BODY_FONT, 8, PDF_MARGIN + PDF_WIDTH - 36f, 34);
        }

        private void closeContent() throws IOException {
            if (content != null) {
                content.close();
                content = null;
            }
        }

        @Override
        public void close() throws IOException {
            closeContent();
        }
    }

    private String connectedHealthSummary(AppUser user, Boolean includeConnectedHealth, List<Long> recordIds) {
        if (!Boolean.TRUE.equals(includeConnectedHealth)) {
            return null;
        }
        List<HealthTimelineRecord> records = selectConnectedRecords(user, recordIds);
        if (records.isEmpty()) {
            return null;
        }
        String summary = records.stream()
                .limit(8)
                .map(record -> record.getLabel()
                        + (record.getValueText() == null ? "" : " " + record.getValueText())
                        + (record.getUnit() == null ? "" : " " + record.getUnit())
                        + (record.getSourceName() == null ? "" : " from " + record.getSourceName()))
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
        return summary.isBlank() ? null : "Recent connected health context: " + summary + ".";
    }

    private List<HealthTimelineRecord> selectConnectedRecords(AppUser user, List<Long> recordIds) {
        if (recordIds != null && !recordIds.isEmpty()) {
            return timelineRepository.findAllById(recordIds).stream()
                    .filter(record -> record.getUser().getId().equals(user.getId()))
                    .toList();
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        return timelineRepository.findByUserOrderByObservedAtDescCreatedAtDesc(user).stream()
                .filter(record -> record.getObservedAt() == null || !record.getObservedAt().isBefore(cutoff))
                .limit(20)
                .toList();
    }

    private String reportPromptText(Assessment assessment) {
        String reportText = assessment.getReportText() == null ? "" : assessment.getReportText();
        return assessment.getConnectedHealthSummary() == null
                ? reportText
                : reportText + "\n\n" + assessment.getConnectedHealthSummary();
    }

    private boolean completed(Assessment assessment) {
        return assessment.getStatus() == null || assessment.getStatus() == AssessmentStatus.COMPLETED;
    }
}
