package no.fint.audit.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import no.fint.audit.web.repository.EventsRepository;
import no.fint.event.model.HeaderConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@CrossOrigin
@RestController
@RequestMapping(value = "/api/events/{env}", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class ApiController {

    @Value("${fint.audit.environment}")
    private String environment;

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping({"/{source:.+}", "/{source}/{action:.+}", "/{source}/{action}/{status:.+}"})
    public void getEventsJson(
            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
            @PathVariable String env,
            @PathVariable(required = false) String source,
            @PathVariable(required = false) String action,
            @PathVariable(required = false) String status,
            @RequestParam(required = false, defaultValue = "PT20m") Duration period,
            @RequestParam(required = false, defaultValue = "1000") long limit,
            HttpServletResponse response
    ) throws IOException {
        if (!environment.equals(env)) throw new IllegalArgumentException(env);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        long timestamp = eventsRepository.getTimestamp(period);
        Predicate<AuditEvent> predicate = eventsRepository.getQuery(orgId, source, action, status);
        Stream<AuditEvent> events = eventsRepository.findEvents(timestamp, orgId, predicate, limit);
        final Writer writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(response.getOutputStream())));
        objectMapper.writerFor(Iterator.class).writeValue(writer, events.iterator());
    }

    @GetMapping("/id/{id}")
    public List<AuditEvent> getEventById(
            @RequestHeader(HeaderConstants.ORG_ID) String orgId,
            @PathVariable String env,
            @PathVariable String id
    ) {
        if (!environment.equals(env)) throw new IllegalArgumentException(env);
        return eventsRepository
                .getEventsByCorrId(id, orgId)
                .stream()
                .sorted(Comparator.comparingLong(AuditEvent::getTimestamp))
                .collect(Collectors.toList());
    }
}
