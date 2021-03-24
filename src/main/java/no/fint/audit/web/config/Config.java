package no.fint.audit.web.config;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import no.fint.audit.web.worker.EventsWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            CheckpointStore checkpointStore,
            Consumer<EventContext> eventProcessor,
            Consumer<ErrorContext> errorProcessor) {
        return new EventProcessorClientBuilder()
                .connectionString(connectionString, eventHubName)
                .processEvent(eventProcessor)
                .processError(errorProcessor)
                .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
                .checkpointStore(checkpointStore)
                .buildEventProcessorClient();
    }

    @Bean
    public CheckpointStore checkpointStore(BlobContainerAsyncClient blobContainerAsyncClient) {
        return new BlobCheckpointStore(blobContainerAsyncClient);
    }

    @Bean
    public BlobContainerAsyncClient blobContainerAsyncClient(
            @Value("${fint.audit.azure.blob.connection-string}") String connectionString,
            @Value("${fint.audit.azure.blob.container-name}") String containerName,
            @Value("${fint.audit.azure.blob.sas-token}") String sasToken) {
        return new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .sasToken(sasToken)
                .buildAsyncClient();
    }

    @Bean
    public Consumer<EventContext> eventContextConsumer(EventsWorker worker) {
        return worker::add;
    }

    @Bean
    public Consumer<ErrorContext> errorContextConsumer() {
        Logger logger = LoggerFactory.getLogger("eventhub");
        return errorContext -> logger.warn("Error receiving events", errorContext.getThrowable());
    }
}
