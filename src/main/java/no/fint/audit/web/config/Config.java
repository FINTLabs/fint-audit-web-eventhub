package no.fint.audit.web.config;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import no.fint.audit.web.repository.EventsRepository;
import no.fint.audit.web.repository.InMemoryCheckpointStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class Config {

    @Bean
    public EventProcessorClient eventProcessorClient(
            @Value("${fint.audit.azure.eventhub.connection-string}") String connectionString,
            @Value("${fint.audit.azure.eventhub.name}") String eventHubName,
            Consumer<EventContext> eventProcessor,
            Consumer<ErrorContext> errorProcessor) {
        return new EventProcessorClientBuilder()
                .connectionString(connectionString, eventHubName)
                .processEvent(eventProcessor)
                .processError(errorProcessor)
                .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
                .checkpointStore(new InMemoryCheckpointStore())
                .buildEventProcessorClient();
    }

    @Bean
    public Consumer<EventContext> eventContextConsumer(EventsRepository repository) {
        return repository::add;
    }

    @Bean
    public Consumer<ErrorContext> errorContextConsumer() {
        return errorContext -> {};
    }
}
