package no.fint.audit.web.worker;

import com.azure.messaging.eventhubs.EventProcessorClient;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.web.repository.EventsRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Watchdog {
    private final EventProcessorClient eventProcessorClient;
    private final EventsRepository eventsRepository;

    public Watchdog(EventProcessorClient eventProcessorClient, EventsRepository eventsRepository) {
        this.eventProcessorClient = eventProcessorClient;
        this.eventsRepository = eventsRepository;
    }

    @Scheduled(cron = "${fint.audit.azure.watchdog:0 0 * * * *}")
    public void validate() {
        if (!eventsRepository.isHealthy()) {
            log.warn("No events received last hour!");
            eventProcessorClient.stop();
            eventProcessorClient.start();
            log.warn("Restarted event processor.");
        }
    }
}
