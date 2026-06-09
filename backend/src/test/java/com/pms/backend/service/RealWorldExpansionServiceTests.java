package com.pms.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pms.backend.dto.AssessmentDtos.FollowUpAnswerRequest;
import com.pms.backend.dto.AssessmentDtos.AssessmentRequest;
import com.pms.backend.dto.AuthDtos.RegisterRequest;
import com.pms.backend.dto.ConnectedHealthDtos.ConnectionCallbackRequest;
import com.pms.backend.dto.ConnectedHealthDtos.TimelineRecordRequest;
import com.pms.backend.model.AssessmentSourceType;
import com.pms.backend.model.AssessmentStatus;
import com.pms.backend.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RealWorldExpansionServiceTests {
    @Autowired
    private AuthService authService;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private ConnectedHealthService connectedHealthService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void reportUploadFinalizationHistoryAndExportWorkTogether() {
        var user = registerPatient("report.patient@example.com", "reportpatient");
        var draft = assessmentService.uploadReport(user, null,
                "City Diagnostic Laboratory\nHemoglobin 10.5 g/dL 12-16 Low\nGlucose 92 mg/dL 70-110 Normal");

        assertEquals(AssessmentSourceType.REPORT, draft.sourceType());
        assertEquals(AssessmentStatus.PENDING_FOLLOW_UP, draft.status());
        assertFalse(draft.extractedObservations().isEmpty());

        var answers = draft.followUpQuestions().stream().map(question -> "No").toList();
        var completed = assessmentService.answerReportFollowUps(user, draft.id(), new FollowUpAnswerRequest(answers));

        assertEquals(AssessmentStatus.COMPLETED, completed.status());
        assertEquals(AssessmentSourceType.REPORT, completed.sourceType());
        assertTrue(completed.careSummary() != null && !completed.careSummary().isBlank());
        assertTrue(assessmentService.reportHistoryFor(user).stream().anyMatch(item -> item.id().equals(completed.id())));
        assertTrue(assessmentService.exportAssessment(user, completed.id()).length > 100);
    }

    @Test
    void connectedHealthStoresPatientOwnedConnectionAndTimeline() {
        var user = registerPatient("connected.patient@example.com", "connectedpatient");

        var start = connectedHealthService.start(user, "APPLE_HEALTH");
        assertEquals("APPLE_HEALTH", start.provider());

        var connection = connectedHealthService.callback(user, "APPLE_HEALTH",
                new ConnectionCallbackRequest("apple-records", "Apple Health"));
        var timeline = connectedHealthService.sync(user, connection.id(), List.of(
                new TimelineRecordRequest("Vital reading", "Resting heart rate", "72", "bpm", "Apple Health", "Imported value")
        ));

        assertEquals(1, timeline.size());
        assertEquals("Resting heart rate", timeline.get(0).label());
        assertTrue(connectedHealthService.timeline(user).stream().anyMatch(record -> "Resting heart rate".equals(record.label())));

        connectedHealthService.revoke(user, connection.id());
        assertThrows(IllegalArgumentException.class, () -> connectedHealthService.sync(user, connection.id(), List.of()));
    }

    @Test
    void samsungSmartWatchSyncCreatesDemoRecordsForAssessmentsAndReports() {
        var user = registerPatient("samsung.context@example.com", "samsungcontext");

        var connection = connectedHealthService.callback(user, "SAMSUNG_HEALTH",
                new ConnectionCallbackRequest("samsung-watch", "Samsung Health"));
        var timeline = connectedHealthService.sync(user, connection.id(), List.of());

        assertTrue(timeline.stream().anyMatch(record -> "Resting heart rate".equals(record.label())));
        assertTrue(timeline.stream().anyMatch(record -> "Sleep duration".equals(record.label())));

        var symptomDraft = assessmentService.create(user, new AssessmentRequest(
                List.of("Fatigue"),
                5,
                3,
                false,
                null,
                "None",
                true,
                timeline.stream().map(record -> record.id()).toList()
        ));

        assertTrue(symptomDraft.connectedHealthSummary().contains("Samsung Galaxy Watch"));
        assertTrue(symptomDraft.followUpQuestions().stream().anyMatch(question -> question.toLowerCase().contains("heart-rate")));

        assessmentService.discardDraft(user, symptomDraft.id());

        var reportDraft = assessmentService.uploadReport(
                user,
                null,
                "City Diagnostic Laboratory\nHemoglobin 10.5 g/dL 12-16 Low\nGlucose 145 mg/dL 70-110 High",
                true,
                timeline.stream().map(record -> record.id()).toList()
        );

        assertTrue(reportDraft.followUpQuestions().stream().anyMatch(question -> question.toLowerCase().contains("sugar")));
        assertTrue(reportDraft.followUpQuestions().stream().anyMatch(question -> question.toLowerCase().contains("wearable")
                || question.toLowerCase().contains("heart-rate")
                || question.toLowerCase().contains("sleep")));
    }

    @Test
    void profilePhotoAcceptsSupportedImagesAndRejectsInvalidFiles() {
        var user = registerPatient("photo.patient@example.com", "photopatient");

        var response = authService.updateProfilePhoto(user, new MockMultipartFile(
                "file", "profile.png", "image/png", new byte[] {1, 2, 3, 4}
        ));

        assertTrue(response.profilePhotoDataUrl().startsWith("data:image/png;base64,"));
        assertThrows(IllegalArgumentException.class, () -> authService.updateProfilePhoto(user, new MockMultipartFile(
                "file", "profile.txt", "text/plain", "not an image".getBytes()
        )));
    }

    @Test
    void assessmentCanIncludeOnlyPatientOwnedConnectedHealthContext() {
        var user = registerPatient("context.patient@example.com", "contextpatient");
        var otherUser = registerPatient("other.context@example.com", "othercontext");

        var connection = connectedHealthService.callback(user, "APPLE_HEALTH",
                new ConnectionCallbackRequest("apple-context", "Apple Health"));
        var otherConnection = connectedHealthService.callback(otherUser, "APPLE_HEALTH",
                new ConnectionCallbackRequest("other-apple-context", "Other Apple Health"));
        var timeline = connectedHealthService.sync(user, connection.id(), List.of(
                new TimelineRecordRequest("Vital reading", "Resting heart rate", "72", "bpm", "Apple Health", "Imported value")
        ));
        var otherTimeline = connectedHealthService.sync(otherUser, otherConnection.id(), List.of(
                new TimelineRecordRequest("Vital reading", "Other heart rate", "88", "bpm", "Apple Health", "Other value")
        ));

        var draft = assessmentService.create(user, new AssessmentRequest(
                List.of("Fatigue"),
                4,
                2,
                false,
                null,
                "None",
                true,
                List.of(timeline.get(0).id(), otherTimeline.get(0).id())
        ));

        assertTrue(draft.connectedHealthSummary().contains("Resting heart rate"));
        assertFalse(draft.connectedHealthSummary().contains("Other heart rate"));
    }

    @Test
    void imageReportUploadRequiresReadablePastedTextUntilOcrIsAvailable() {
        var user = registerPatient("image.report@example.com", "imagereport");
        var image = new MockMultipartFile("file", "report.png", "image/png", new byte[] {1, 2, 3, 4});

        assertThrows(IllegalArgumentException.class, () -> assessmentService.uploadReport(user, image, ""));

        var draft = assessmentService.uploadReport(user, image, "City Diagnostic Laboratory\nVitamin D 18 ng/mL 30-100 Low");

        assertEquals(AssessmentSourceType.REPORT, draft.sourceType());
        assertFalse(draft.extractedObservations().isEmpty());
    }

    private com.pms.backend.model.AppUser registerPatient(String email, String username) {
        authService.register(new RegisterRequest(
                email,
                username,
                "Expansion Patient",
                "password123",
                31,
                170.0,
                68.0,
                "Prefer not to say",
                true,
                true
        ));
        return userRepository.findByEmailIgnoreCase(email).orElseThrow();
    }
}
