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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AssessmentService {
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
        assessment.setFollowUpQuestions(mergeFollowUps(result.followUps(), aiInsightService.assessmentFollowUps(user, assessment, result).questions()));
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
        assessment.setFollowUpQuestions(reportFollowUps(reportName));
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
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(56, 740);
                content.showText("PMS Health Care-Preparation Summary");
                content.setFont(PDType1Font.HELVETICA, 10);
                int y = 716;
                for (String line : exportLines(assessment)) {
                    if (y < 70) {
                        break;
                    }
                    content.newLineAtOffset(0, -18);
                    content.showText(safePdf(line));
                    y -= 18;
                }
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Assessment export could not be created.");
        }
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

    private List<String> mergeFollowUps(List<String> required, List<String> aiCandidates) {
        List<String> merged = new ArrayList<>(new LinkedHashSet<>(required == null ? List.of() : required));
        for (String candidate : aiCandidates == null ? List.<String>of() : aiCandidates) {
            String value = candidate == null ? "" : candidate.trim();
            if (!value.isBlank() && merged.size() < 7 && !merged.contains(value)) {
                merged.add(value);
            }
        }
        return merged.stream().limit(7).toList();
    }

    private List<String> reportFollowUps(String reportName) {
        List<String> questions = mergeFollowUps(List.of(
                "Do you currently have symptoms related to this report?",
                "Has a clinician already reviewed this report with you?",
                "Are any values marked high, low, abnormal, or critical?",
                "Do you want to discuss lifestyle, medication, or follow-up testing questions with your clinician?"
        ), aiInsightService.reportFollowUps(reportName).questions());
        return questions.stream().limit(7).toList();
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

    private List<String> exportLines(Assessment assessment) {
        List<String> lines = new ArrayList<>();
        lines.add("Patient: " + assessment.getUser().getFullName());
        lines.add("Source: " + (assessment.getSourceType() == null ? "SYMPTOM" : assessment.getSourceType()));
        lines.add("Risk: " + assessment.getRiskLevel() + " (" + assessment.getRiskScore() + ")");
        if (assessment.getReportName() != null) {
            lines.add("Report: " + assessment.getReportName());
        }
        if (assessment.getConnectedHealthSummary() != null) {
            lines.add("Connected health: " + assessment.getConnectedHealthSummary());
        }
        if (assessment.getUrgentWarning() != null) {
            lines.add("Urgent guidance: " + assessment.getUrgentWarning());
        }
        lines.add("Summary: " + nullSafe(assessment.getCareSummary()));
        lines.add("Why this matters: " + nullSafe(assessment.getExplanation()));
        lines.add("Possible directions: " + String.join("; ", nullList(assessment.getPossibleDirections())));
        lines.add("Care tips: " + String.join("; ", nullList(assessment.getCareTips())));
        lines.add("Monitoring plan: " + String.join("; ", nullList(assessment.getMonitoringPlan())));
        lines.add("Doctor questions: " + String.join("; ", nullList(assessment.getDoctorPrepQuestions())));
        if (assessment.getExtractedObservations() != null && !assessment.getExtractedObservations().isEmpty()) {
            lines.add("Extracted report values:");
            assessment.getExtractedObservations().stream().limit(8).forEach(observation ->
                    lines.add(observation.getTestName() + ": " + observation.getValueText()
                            + (observation.getUnit() == null ? "" : " " + observation.getUnit())
                            + (observation.getFlag() == null ? "" : " (" + observation.getFlag() + ")")));
        }
        return lines;
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
