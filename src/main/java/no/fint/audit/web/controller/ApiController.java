package no.fint.audit.web.controller;

import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import no.fint.audit.web.repository.EventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@CrossOrigin
@RestController
@RequestMapping(value = "/events/api", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
@Slf4j
public class ApiController {

    @Autowired
    private EventsRepository eventsRepository;

    @GetMapping({"{orgid:.+}", "{orgid}/{source:.+}", "{orgid}/{source}/{action:.+}", "{orgid}/{source}/{action}/{status:.+}"})
    public List<AuditEvent> getEventsJson(
            @PathVariable String orgid,
            @PathVariable String source,
            @PathVariable(required = false) String action,
            @PathVariable(required = false) String status,
            @RequestParam(required = false, defaultValue = "20m") String period,
            @RequestParam(required = false, defaultValue = "1000") long limit
    ) {
        long timestamp = eventsRepository.getTimestamp(period);
        Predicate<AuditEvent> predicate = eventsRepository.getQuery(orgid, source, action, status);
        return new ArrayList<>(eventsRepository.findEvents(timestamp, predicate, limit));
    }
}
