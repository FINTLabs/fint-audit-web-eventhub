package no.fint.audit.web.repository;

import com.azure.messaging.eventhubs.models.EventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.*;

@Repository
@Slf4j
public class EventsRepository {

    private final ConcurrentNavigableMap<Long, Collection<AuditEvent>> auditEvents = new ConcurrentSkipListMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Cache<String, Collection<AuditEvent>> eventCache =
            CacheBuilder.newBuilder().expireAfterWrite(Duration.ofHours(8)).build();

    public List<AuditEvent> findEvents(long sinceTimestamp, Predicate<AuditEvent> predicate, long limit) {
        return auditEvents
                .tailMap(sinceTimestamp)
                .values()
                .parallelStream()
                .flatMap(Collection::parallelStream)
                .filter(predicate)
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Collection<AuditEvent> getEventsByCorrId(String corrId) {
        return eventCache.getIfPresent(corrId);
    }

    public long getTimestamp(String period) {
        Duration duration = Duration.parse("PT" + period.toUpperCase());
        return ZonedDateTime.now().minus(duration).toInstant().toEpochMilli();
    }

    public Predicate<AuditEvent> getQuery(String orgid, String source, String action, String status) {
        Predicate<AuditEvent> predicate = event -> equalsIgnoreCase(event.getOrgId(), orgid);
        if (isNotBlank(source)) {
            predicate = predicate.and(event -> containsIgnoreCase(event.getSource(), source));
        }
        if (isNotBlank(action)) {
            Pattern pattern = Pattern.compile(action);
            predicate = predicate.and(event -> pattern.asPredicate().test(event.getEvent().getAction()));
        }
        if (isNotBlank(status)) {
            Pattern pattern = Pattern.compile(status);
            predicate = predicate.and(event -> pattern.asPredicate().test(event.getEvent().getStatus().name()));
        }
        return predicate;
    }

    public void add(EventContext eventContext) {
        try {
            AuditEvent event = mapper.readValue(eventContext.getEventData().getBody(), AuditEvent.class);
            log.debug("Event: {}", event);
            auditEvents.computeIfAbsent(event.getTimestamp(), k -> new ConcurrentLinkedQueue<>()).add(event);
            eventCache.get(event.getCorrId(), ConcurrentLinkedQueue::new).add(event);
            log.debug("Size: {}", auditEvents.size());
        } catch (IOException | ExecutionException e) {
            log.error("Error processing {}", eventContext, e);
        }
    }

    public void delete(long timestamp) {
        auditEvents.headMap(timestamp).clear();
    }

    public int size() {
        return auditEvents.values().parallelStream().mapToInt(Collection::size).sum();
    }
}
