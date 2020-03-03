package no.fint.audit.web.controller;

import com.azure.messaging.eventhubs.EventProcessorClient;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.web.repository.EventsRepository;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.ZonedDateTime;

@CrossOrigin
@RestController
@RequestMapping(value = "/events/admin")
@Slf4j
public class AdminController {
    private final EventProcessorClient eventProcessorClient;
    private final EventsRepository eventsRepository;

    public AdminController(EventProcessorClient eventProcessorClient, EventsRepository eventsRepository) {
        this.eventProcessorClient = eventProcessorClient;
        this.eventsRepository = eventsRepository;
    }

    @PostConstruct
    public void init() {
        eventProcessorClient.start();
    }

    @PostMapping("/processor")
    public void start() {
        eventProcessorClient.start();
    }

    @DeleteMapping("/processor")
    public void stop() {
        eventProcessorClient.stop();
    }

    @GetMapping("/processor")
    public boolean isRunning() {
        return eventProcessorClient.isRunning();
    }

    @DeleteMapping("/repository")
    public void deleteEvents(@RequestParam Duration since) {
        ZonedDateTime cutoff = ZonedDateTime.now().minus(since);
        log.info("Deleting events before {}", cutoff);
        eventsRepository.delete(cutoff.toInstant().toEpochMilli());
    }

    @GetMapping("/repository")
    public int eventCount() {
        return eventsRepository.size();
    }
}
