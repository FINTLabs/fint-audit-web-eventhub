package no.fint.audit.web.worker;

import com.azure.messaging.eventhubs.models.EventContext;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.web.repository.EventsRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class EventsWorker {
    private final BlockingQueue<EventContext> queue;
    private final EventsRepository eventsRepository;
    private final Executor executor;
    private final AtomicInteger threadCount;

    public EventsWorker(EventsRepository eventsRepository) {
        this.eventsRepository = eventsRepository;
        queue = new LinkedBlockingQueue<>();
        executor = Executors.newCachedThreadPool();
        threadCount = new AtomicInteger();
    }

    public void add(EventContext eventContext) {
        if (!queue.offer(eventContext)) {
            eventsRepository.add(eventContext);
        } else if (queue.size() > 10) {
            start();
        }
    }

    public void start() {
        executor.execute(this::work);
    }

    public void work() {
        try {
            threadCount.incrementAndGet();
            EventContext eventContext = queue.poll();
            while (eventContext != null) {
                eventsRepository.add(eventContext);
                eventContext = queue.poll(2500, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            log.warn(e.getMessage());
        } finally {
            threadCount.decrementAndGet();
        }
    }
}
