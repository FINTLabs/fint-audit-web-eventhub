package no.fint.audit.web.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.audit.model.AuditEvent;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

@Service
public class Wrapper {
    private final ObjectMapper mapper;

    public Wrapper(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public AuditEvent unwrap(AuditEntry entry) {
        try (InputStream in = new InflaterInputStream(new ByteArrayInputStream(entry.getData()))) {
            return mapper.readValue(in, AuditEvent.class);
        } catch (IOException e) {
            return null;
        }
    }

    public AuditEntry wrap(byte[] body, AuditEvent event) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DeflaterOutputStream cout = new DeflaterOutputStream(out)) {
            cout.write(body);
        } catch (IOException e) {
            return null;
        }
        return new AuditEntry(out.toByteArray(), StringUtils.lowerCase(event.getOrgId()).hashCode());
    }
}
