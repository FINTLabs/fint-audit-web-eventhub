package no.fint.audit.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import no.fint.audit.web.repository.EventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@CrossOrigin
@Controller
@RequestMapping(value = "/events")
@Slf4j
public class EventsController {

    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping({"{orgid:.+}", "{orgid}/{source:.+}", "{orgid}/{source}/{action:.+}", "{orgid}/{source}/{action}/{status:.+}"})
    public String getEvents(
            Model model,
            @PathVariable String orgid,
            @PathVariable(required = false) String source,
            @PathVariable(required = false) String action,
            @PathVariable(required = false) String status,
            @RequestParam(required = false, defaultValue = "20m") String period,
            @RequestParam(required = false, defaultValue = "1000") long limit
    ) {

        long timestamp = eventsRepository.getTimestamp(period);
        Predicate<AuditEvent> predicate = eventsRepository.getQuery(orgid, source, action, status);
        model.addAttribute("orgid", orgid);
        model.addAttribute("source", source);
        model.addAttribute("events",
                eventsRepository
                        .findEvents(timestamp, orgid, predicate, limit)
                        .collect(Collectors.toList()));
        return "events";
    }

    @GetMapping("id/{id}")
    public String getEventById(
            Model model,
            @PathVariable String id
    ) {
        Collection<AuditEvent> eventsByCorrId = eventsRepository.getEventsByCorrId(id);
        if (eventsByCorrId == null) {
            throw new EventsNotFoundException();
        }
        model.addAttribute("mapper", objectMapper);
        model.addAttribute("events",
                eventsByCorrId.stream()
                        .sorted(Comparator.comparingLong(AuditEvent::getTimestamp))
                        .collect(Collectors.toList()));
        return "event";
    }


    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Events not found")
    public static class EventsNotFoundException extends RuntimeException {
    }

}
