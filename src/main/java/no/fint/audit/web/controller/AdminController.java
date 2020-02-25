package no.fint.audit.web.controller;

import com.azure.messaging.eventhubs.EventProcessorClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

@CrossOrigin
@RestController
@RequestMapping(value = "/events/admin")
@Slf4j
public class AdminController {
    @Autowired
    private EventProcessorClient eventProcessorClient;

    @PostConstruct
    public void init() {
        eventProcessorClient.start();
    }

    @PostMapping
    public void start() {
        eventProcessorClient.start();
    }

    @DeleteMapping
    public void stop() {
        eventProcessorClient.stop();
    }

    @GetMapping
    public boolean isRunning() {
        return eventProcessorClient.isRunning();
    }
}
