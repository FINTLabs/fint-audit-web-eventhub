package no.fint.audit.web.config;

import com.azure.messaging.eventhubs.CheckpointStore;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "fint.audit.azure.checkpoint", havingValue = "blob")
@Slf4j
public class Blob {

    @Bean
    public CheckpointStore checkpointStore(BlobContainerAsyncClient blobContainerAsyncClient) {
        log.info("Using BLOB checkpoint store.");
        return new BlobCheckpointStore(blobContainerAsyncClient);
    }

    @Bean
    public BlobContainerAsyncClient blobContainerAsyncClient(
            @Value("${fint.audit.azure.blob.connection-string}") String connectionString,
            @Value("${fint.audit.azure.blob.container-name}") String containerName) {
        log.info("Using blob container {}", containerName);
        return new BlobContainerClientBuilder()
                .connectionString(connectionString)
                .containerName(containerName)
                .buildAsyncClient();
    }
}
