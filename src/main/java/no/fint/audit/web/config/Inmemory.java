package no.fint.audit.web.config;

import com.azure.messaging.eventhubs.CheckpointStore;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.web.repository.InMemoryCheckpointStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "fint.audit.azure.checkpoint", havingValue = "memory", matchIfMissing = true)
@Slf4j
public class Inmemory {

    @Bean
    public CheckpointStore checkpointStore() {
        log.info("Using IN-MEMORY checkpoint store.");
        return new InMemoryCheckpointStore();
    }
}
