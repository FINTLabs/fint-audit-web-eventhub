package no.fint.audit.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import no.fint.audit.web.repository.EventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.time.Duration;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

@CrossOrigin
@RestController
@RequestMapping(value = "/events/api", produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class ApiController {

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping({"{orgid:.+}", "{orgid}/{source:.+}", "{orgid}/{source}/{action:.+}", "{orgid}/{source}/{action}/{status:.+}"})
    public void getEventsJson(
            @PathVariable String orgid,
            @PathVariable(required = false) String source,
            @PathVariable(required = false) String action,
            @PathVariable(required = false) String status,
            @RequestParam(required = false, defaultValue = "PT20m") Duration period,
            @RequestParam(required = false, defaultValue = "1000") long limit,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        long timestamp = eventsRepository.getTimestamp(period);
        Predicate<AuditEvent> predicate = eventsRepository.getQuery(orgid, source, action, status);
        Stream<AuditEvent> events = eventsRepository.findEvents(timestamp, predicate, limit);
        final Writer writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(response.getOutputStream())));
        objectMapper.writerFor(Iterator.class).writeValue(writer, events.iterator());
    }
}
