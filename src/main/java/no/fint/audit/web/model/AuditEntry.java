package no.fint.audit.web.model;

import lombok.Data;

@Data
public class AuditEntry {
    private final byte[] data;
    private final int orgId;
}
