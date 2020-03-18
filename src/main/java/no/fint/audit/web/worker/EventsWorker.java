package no.fint.audit.web.worker;

import com.azure.messaging.eventhubs.models.EventContext;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.web.repository.EventsRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class EventsWorker {
    private final EventsRepository eventsRepository;
    private final Executor executor;

    public EventsWorker(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
        executor = Executors.newWorkStealingPool();
    }

    public void add(EventContext eventContext) {
        executor.execute(() -> eventsRepository.add(eventContext));
    }

}
