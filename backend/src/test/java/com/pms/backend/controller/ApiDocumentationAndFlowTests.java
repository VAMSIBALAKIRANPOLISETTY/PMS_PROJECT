package com.pms.backend.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiDocumentationAndFlowTests {
    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void openApiDocumentIncludesMajorPathsAndBearerScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("PMS Health API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.paths['/api/health']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/staff-login']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/me']").exists())
                .andExpect(jsonPath("$.paths['/api/assessments']").exists())
                .andExpect(jsonPath("$.paths['/api/assessments/{assessmentId}/follow-ups']").exists())
                .andExpect(jsonPath("$.paths['/api/reports/follow-ups']").exists())
                .andExpect(jsonPath("$.paths['/api/reports/insight']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/analytics']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/rules']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/questions']").exists());

        mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk());
    }

    @Test
    void privateEndpointsRejectMissingToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/admin/analytics"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void underageRegistrationIsRejectedThroughApi() throws Exception {
        Map<String, Object> request = registrationBody("underage", 17);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("age: must be greater than or equal to 18"));
    }

    @Test
    void patientAssessmentFlowCreatesDraftThenCompletedCareGuide() throws Exception {
        String token = registerPatient("flow", 31);

        MvcResult draftResult = mockMvc.perform(post("/api/assessments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assessmentBody(List.of("Fever", "Weakness"), 6, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_FOLLOW_UP"))
                .andExpect(jsonPath("$.careSummary").value(nullValue()))
                .andExpect(jsonPath("$.followUpQuestions", hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(4))))
                .andReturn();

        JsonNode draft = jsonTree(draftResult);
        long assessmentId = draft.path("id").asLong();
        List<String> answers = StreamSupport.stream(draft.path("followUpQuestions").spliterator(), false)
                .map(question -> "No")
                .toList();

        mockMvc.perform(post("/api/assessments/" + assessmentId + "/follow-ups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("answers", answers))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.careSummary", notNullValue()))
                .andExpect(jsonPath("$.possibleDirections[0]", notNullValue()))
                .andExpect(jsonPath("$.doctorPrepQuestions[0]", notNullValue()))
                .andExpect(jsonPath("$.trustedSourceLinks[0]", notNullValue()));

        MvcResult listResult = mockMvc.perform(get("/api/assessments")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode history = jsonTree(listResult);
        assertTrue(StreamSupport.stream(history.spliterator(), false).anyMatch(item -> item.path("id").asLong() == assessmentId));
    }

    @Test
    void invalidFollowUpAnswersAreRejectedThroughApi() throws Exception {
        String token = registerPatient("followup", 34);
        MvcResult draftResult = mockMvc.perform(post("/api/assessments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assessmentBody(List.of("Headache"), 3, 1))))
                .andExpect(status().isOk())
                .andReturn();
        long assessmentId = jsonTree(draftResult).path("id").asLong();

        mockMvc.perform(post("/api/assessments/" + assessmentId + "/follow-ups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("answers", List.of("Maybe")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Answer every follow-up question before preparing your care guide."));
    }

    @Test
    void staffAndPatientAuthorizationBoundariesAreEnforced() throws Exception {
        String patientToken = registerPatient("boundary", 42);
        String adminToken = login("/api/auth/staff-login", "admin@example.com", "password123");

        mockMvc.perform(get("/api/admin/analytics")
                        .header("Authorization", bearer(patientToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/analytics")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/assessments")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assessmentBody(List.of("Headache"), 3, 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Patient access is required to create an assessment."));
    }

    @Test
    void staffCanCreateAndPauseOperationalRulesAndQuestions() throws Exception {
        String adminToken = login("/api/auth/staff-login", "admin@example.com", "password123");
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        MvcResult questionResult = mockMvc.perform(post("/api/admin/questions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "symptomKey", "General",
                                "prompt", "Have your symptoms changed since they started? " + suffix,
                                "active", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        long questionId = jsonTree(questionResult).path("id").asLong();

        mockMvc.perform(patch("/api/admin/questions/" + questionId + "/active")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("conditionLabel", "Swagger test rule " + suffix);
        rule.put("primarySymptom", "Headache");
        rule.put("minSeverity", 4);
        rule.put("score", 45);
        rule.put("urgent", false);
        rule.put("active", true);
        rule.put("explanation", "Raises awareness when headache severity needs review.");

        MvcResult ruleResult = mockMvc.perform(post("/api/admin/rules")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(rule)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.score").value(45))
                .andReturn();
        long ruleId = jsonTree(ruleResult).path("id").asLong();

        mockMvc.perform(patch("/api/admin/rules/" + ruleId + "/active")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void patientCanDiscardOwnPendingDraftThroughApi() throws Exception {
        String token = registerPatient("discard", 28);
        MvcResult draftResult = mockMvc.perform(post("/api/assessments")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assessmentBody(List.of("Cough"), 4, 2))))
                .andExpect(status().isOk())
                .andReturn();
        long assessmentId = jsonTree(draftResult).path("id").asLong();

        mockMvc.perform(delete("/api/assessments/" + assessmentId + "/draft")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        MvcResult pendingResult = mockMvc.perform(get("/api/assessments/pending")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals("", pendingResult.getResponse().getContentAsString());
    }

    private String registerPatient(String prefix, int age) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(registrationBody(prefix, age))))
                .andExpect(status().isOk())
                .andReturn();
        return jsonTree(result).path("token").asText();
    }

    private String login(String endpoint, String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post(endpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identifier", identifier, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return jsonTree(result).path("token").asText();
    }

    private Map<String, Object> registrationBody(String prefix, int age) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", prefix + "-" + suffix + "@example.com");
        body.put("username", prefix + suffix);
        body.put("fullName", "API Test Patient");
        body.put("password", "password123");
        body.put("age", age);
        body.put("heightCm", 170.0);
        body.put("weightKg", 68.0);
        body.put("sex", "Prefer not to say");
        body.put("privacyNoticeAccepted", true);
        body.put("termsAccepted", true);
        return body;
    }

    private Map<String, Object> assessmentBody(List<String> symptoms, int severity, int durationDays) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("symptoms", symptoms);
        body.put("severity", severity);
        body.put("durationDays", durationDays);
        body.put("temperatureAvailable", false);
        body.put("chronicCondition", "None");
        return body;
    }

    private JsonNode jsonTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
