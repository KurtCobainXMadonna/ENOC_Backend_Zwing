package org.eci.ZwingBackend.sound.infrastructure.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.ParallelTransferOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.sound.application.port.out.AudioStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class AzureBlobAudioStorageAdapter implements AudioStoragePort {
    private final String connectionString;
    private final String containerName;
    private BlobContainerClient containerClient;

    public AzureBlobAudioStorageAdapter(
            @Value("${azure.storage.connection-string}") String connectionString,
            @Value("${azure.storage.uploads-container}") String containerName) {
        this.connectionString = connectionString;
        this.containerName = containerName;
    }

    @PostConstruct
    void init() {
        if (connectionString == null || connectionString.isBlank()) {
            log.warn("[AudioStorage] AZURE_STORAGE_CONNECTION_STRING is not set — uploads will fail until configured.");
            return;
        }
        BlobServiceClient service = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();
        this.containerClient = service.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
            log.info("[AudioStorage] Created Azure container '{}'.", containerName);
        }
    }

    @Override
    public String upload(String blobPath, InputStream data, long contentLen, String contentType) {
        requireConfigured();
        BlobClient blob = containerClient.getBlobClient(blobPath);
        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);
        blob.uploadWithResponse(
                new com.azure.storage.blob.options.BlobParallelUploadOptions(data, contentLen)
                        .setHeaders(headers)
                        .setParallelTransferOptions(new ParallelTransferOptions()),
                null,
                null
        );
        return blob.getBlobUrl();
    }

    @Override
    public void deleteByUrl(String blobUrl) {
        requireConfigured();
        String blobName = extractBlobName(blobUrl);
        if (blobName == null) {
            log.warn("[AudioStorage] Could not parse blob name from URL '{}'. Skipping delete.", blobUrl);
            return;
        }
        BlobClient blob = containerClient.getBlobClient(blobName);
        boolean deleted = blob.deleteIfExists();
        if (!deleted) {
            log.warn("[AudioStorage] Blob '{}' was already absent at delete time.", blobName);
        }
    }

    private void requireConfigured() {
        if (containerClient == null) {
            throw new IllegalStateException("Azure Blob Storage is not configured. Set AZURE_STORAGE_CONNECTION_STRING.");
        }
    }

    /** Extract the blob name (path inside the container) from a full URL. */
    private String extractBlobName(String blobUrl) {
        try {
            URI uri = new URI(blobUrl);
            String path = uri.getPath();
            String prefix = "/" + containerName + "/";
            if (path == null || !path.startsWith(prefix)) {
                return null;
            }
            return java.net.URLDecoder.decode(path.substring(prefix.length()), java.nio.charset.StandardCharsets.UTF_8);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
