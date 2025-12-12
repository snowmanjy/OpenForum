package com.openforum.application.event;

public class TenantCreatedEvent {
    private final String tenantId;
    private final String externalOwnerId;
    private final String ownerEmail;
    private final String ownerName;

    public TenantCreatedEvent(String tenantId, String externalOwnerId, String ownerEmail, String ownerName) {
        this.tenantId = tenantId;
        this.externalOwnerId = externalOwnerId;
        this.ownerEmail = ownerEmail;
        this.ownerName = ownerName;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getExternalOwnerId() {
        return externalOwnerId;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getOwnerName() {
        return ownerName;
    }
}
