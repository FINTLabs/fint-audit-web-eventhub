package no.fint.audit.web.repository;

import com.azure.messaging.eventhubs.models.EventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.model.AuditEvent;
import no.fint.audit.web.model.AuditEntry;
import no.fint.audit.web.model.Wrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.*;

@Repository
@Slf4j
public class EventsRepository {

    private final ConcurrentNavigableMap<Long, AuditEntry> auditEntries = new ConcurrentSkipListMap<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Cache<Integer, Collection<AuditEntry>> entryCache;
    private final Wrapper wrapper;

    public EventsRepository(
            @Value("${fint.audit.azure.eventhub.cache.duration:PT2H}") Duration duration,
            Wrapper wrapper) {
        entryCache = CacheBuilder.newBuilder().expireAfterWrite(duration).build();
        this.wrapper = wrapper;
    }

    public Stream<AuditEvent> findEvents(long sinceTimestamp, String orgId, Predicate<AuditEvent> predicate, long limit) {
        return auditEntries
                .tailMap(sinceTimestamp << 8)
                .values()
                .parallelStream()
                .filter(hasOrgId(orgId))
                .map(wrapper::unwrap)
                .filter(predicate)
                .limit(limit);
    }

    private Predicate<AuditEntry> hasOrgId(String orgId) {
        final int hashCode = lowerCase(orgId).hashCode();
        return entry -> entry.getOrgId() == hashCode;
    }

    public Collection<AuditEvent> getEventsByCorrId(String corrId) {
        Collection<AuditEntry> entries = entryCache.getIfPresent(corrId.hashCode());
        if (entries == null) {
            return null;
        }
        return entries
                .stream()
                .map(wrapper::unwrap)
                .filter(hasCorrId(corrId))
                .collect(Collectors.toList());
    }

    private Predicate<AuditEvent> hasCorrId(String corrId) {
        return event -> StringUtils.equals(corrId, event.getCorrId());
    }

    public long getTimestamp(String period) {
        Duration duration = Duration.parse("PT" + period.toUpperCase());
        return getTimestamp(duration);
    }

    public long getTimestamp(Duration duration) {
        return ZonedDateTime.now().minus(duration).toInstant().toEpochMilli();
    }

    public Predicate<AuditEvent> getQuery(String orgid, String source, String action, String status) {
        Predicate<AuditEvent> predicate = Objects::nonNull;
        predicate = predicate.and(event -> equalsIgnoreCase(event.getOrgId(), orgid));
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
            byte[] body = eventContext.getEventData().getBody();
            AuditEvent event = mapper.readValue(body, AuditEvent.class);
            AuditEntry entry = wrapper.wrap(body, event);
            long index = event.getTimestamp() << 8;
            while (auditEntries.putIfAbsent(index, entry) != null) {
                ++index;
            }
            entryCache.get(event.getCorrId().hashCode(), ConcurrentLinkedQueue::new).add(entry);
            eventContext.updateCheckpoint();
        } catch (IOException | ExecutionException e) {
            log.error("Error processing {}", eventContext, e);
        }
    }

    public void delete(long timestamp) {
        auditEntries.headMap(timestamp << 8).clear();
    }

    public int size() {
        return auditEntries.size();
    }

}
