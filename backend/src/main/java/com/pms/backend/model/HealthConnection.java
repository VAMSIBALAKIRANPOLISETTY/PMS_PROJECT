package com.pms.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class HealthConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionStatus status = ConnectionStatus.READY_TO_CONNECT;

    private String displayName;
    private String externalAccountId;
    private String authorizationState;
    private String providerSubjectId;
    @Column(length = 4000)
    private String encryptedRefreshToken;
    private LocalDateTime accessTokenExpiresAt;
    @Column(length = 1200)
    private String grantedScopes;
    private LocalDateTime connectedAt;
    private LocalDateTime lastSyncAt;
    private String lastSyncStatus;
    @Column(length = 1200)
    private String lastSyncMessage;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public ConnectionProvider getProvider() { return provider; }
    public void setProvider(ConnectionProvider provider) { this.provider = provider; }
    public ConnectionStatus getStatus() { return status; }
    public void setStatus(ConnectionStatus status) { this.status = status; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getExternalAccountId() { return externalAccountId; }
    public void setExternalAccountId(String externalAccountId) { this.externalAccountId = externalAccountId; }
    public String getAuthorizationState() { return authorizationState; }
    public void setAuthorizationState(String authorizationState) { this.authorizationState = authorizationState; }
    public String getProviderSubjectId() { return providerSubjectId; }
    public void setProviderSubjectId(String providerSubjectId) { this.providerSubjectId = providerSubjectId; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public void setEncryptedRefreshToken(String encryptedRefreshToken) { this.encryptedRefreshToken = encryptedRefreshToken; }
    public LocalDateTime getAccessTokenExpiresAt() { return accessTokenExpiresAt; }
    public void setAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) { this.accessTokenExpiresAt = accessTokenExpiresAt; }
    public String getGrantedScopes() { return grantedScopes; }
    public void setGrantedScopes(String grantedScopes) { this.grantedScopes = grantedScopes; }
    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
    public String getLastSyncMessage() { return lastSyncMessage; }
    public void setLastSyncMessage(String lastSyncMessage) { this.lastSyncMessage = lastSyncMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
