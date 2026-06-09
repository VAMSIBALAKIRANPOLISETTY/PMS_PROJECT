package com.pms.backend.controller;

import com.pms.backend.config.OpenApiConfig;
import com.pms.backend.dto.ConnectedHealthDtos.ConnectionCallbackRequest;
import com.pms.backend.dto.ConnectedHealthDtos.ConnectionResponse;
import com.pms.backend.dto.ConnectedHealthDtos.ConnectionStartResponse;
import com.pms.backend.dto.ConnectedHealthDtos.TimelineRecordResponse;
import com.pms.backend.dto.ConnectedHealthDtos.TimelineSyncRequest;
import com.pms.backend.model.AppUser;
import com.pms.backend.service.AuthService;
import com.pms.backend.service.ConnectedHealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Connected Health", description = "Patient-owned connections and normalized health timeline records.")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ConnectedHealthController {
    private final AuthService authService;
    private final ConnectedHealthService connectedHealthService;

    public ConnectedHealthController(AuthService authService, ConnectedHealthService connectedHealthService) {
        this.authService = authService;
        this.connectedHealthService = connectedHealthService;
    }

    @Operation(summary = "List connections", description = "Returns patient-owned connected health sources and their sync status.")
    @GetMapping("/connections")
    public List<ConnectionResponse> connections(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        AppUser user = authService.requireUser(authHeader);
        return connectedHealthService.listConnections(user);
    }

    @Operation(summary = "Start connection", description = "Returns the provider authorization destination and permission summary for a health source.")
    @PostMapping("/connections/{provider}/start")
    public ConnectionStartResponse start(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable String provider
    ) {
        AppUser user = authService.requireUser(authHeader);
        return connectedHealthService.start(user, provider);
    }

    @Operation(summary = "Complete connection", description = "Stores connection metadata after a provider/native OAuth callback.")
    @PostMapping("/connections/{provider}/callback")
    public ConnectionResponse callback(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable String provider,
            @Valid @RequestBody ConnectionCallbackRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return connectedHealthService.callback(user, provider, request);
    }

    @Operation(summary = "Complete Google Health OAuth callback", description = "Handles the provider redirect, stores backend-only connection credentials, and imports recent Google Health records.")
    @GetMapping(value = "/connections/google-health/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> googleHealthCallback(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        ConnectedHealthService.CallbackPage page = connectedHealthService.completeGoogleHealthAuthorization(state, code, error);
        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>%s</title>
                  <style>
                    body { font-family: Inter, Segoe UI, sans-serif; margin: 0; min-height: 100vh; display: grid; place-items: center; background: #f5fbfa; color: #173237; }
                    main { width: min(560px, calc(100%% - 32px)); padding: 32px; border: 1px solid #dbe9e6; border-radius: 12px; background: #ffffff; box-shadow: 0 18px 32px rgba(18, 61, 66, 0.08); }
                    h1 { margin: 0 0 10px; font-size: 28px; }
                    p { margin: 0 0 12px; line-height: 1.6; color: #5f7478; }
                    a { display: inline-flex; margin-top: 10px; padding: 10px 14px; border-radius: 8px; background: #0f9f8f; color: #fff; text-decoration: none; font-weight: 700; }
                  </style>
                </head>
                <body>
                  <main>
                    <h1>%s</h1>
                    <p>%s</p>
                    <a href="%s">Return to PMS Health</a>
                  </main>
                </body>
                </html>
                """.formatted(page.title(), page.title(), page.detail(), page.returnUrl());
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @Operation(summary = "Sync normalized records", description = "Stores normalized health timeline records for a connected source.")
    @PostMapping("/connections/{connectionId}/sync")
    public List<TimelineRecordResponse> sync(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long connectionId,
            @Valid @RequestBody TimelineSyncRequest request
    ) {
        AppUser user = authService.requireUser(authHeader);
        return connectedHealthService.sync(user, connectionId, request.records());
    }

    @Operation(summary = "Revoke connection", description = "Stops future syncs from the selected health connection.")
    @DeleteMapping("/connections/{connectionId}")
    public void revoke(
            @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
            @PathVariable Long connectionId
    ) {
        AppUser user = authService.requireUser(authHeader);
        connectedHealthService.revoke(user, connectionId);
    }

    @Operation(summary = "Health timeline", description = "Returns normalized health events imported from reports, devices, apps, or hospital connections.")
    @GetMapping("/health-timeline")
    public List<TimelineRecordResponse> timeline(@Parameter(hidden = true) @RequestHeader("Authorization") String authHeader) {
        AppUser user = authService.requireUser(authHeader);
        return connectedHealthService.timeline(user);
    }
}
