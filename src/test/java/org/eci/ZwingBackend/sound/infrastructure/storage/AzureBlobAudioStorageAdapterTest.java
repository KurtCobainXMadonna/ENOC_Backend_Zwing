package org.eci.ZwingBackend.sound.infrastructure.storage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobAudioStorageAdapterTest {

    @Mock
    private BlobContainerClient containerClient;

    @Mock
    private BlobClient blobClient;

    private AzureBlobAudioStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AzureBlobAudioStorageAdapter("", "uploads");
    }

    @Test
    void uploadAndDeleteUseContainerClient() {
        ReflectionTestUtils.setField(adapter, "containerClient", containerClient);
        when(containerClient.getBlobClient("tracks/beat.wav")).thenReturn(blobClient);
        when(blobClient.uploadWithResponse(any(BlobParallelUploadOptions.class), isNull(), isNull())).thenReturn(null);
        when(blobClient.getBlobUrl()).thenReturn("https://account.blob.core.windows.net/uploads/tracks/beat.wav");
        when(blobClient.deleteIfExists()).thenReturn(true);

        String url = adapter.upload("tracks/beat.wav", new ByteArrayInputStream(new byte[] {1, 2, 3}), 3, "audio/wav");
        adapter.deleteByUrl(url);

        assertThat(url).endsWith("/tracks/beat.wav");
    }

    @Test
    void uploadFailsWhenAdapterIsNotConfigured() {
        assertThatThrownBy(() -> adapter.upload("x.wav", new ByteArrayInputStream(new byte[] {1}), 1, "audio/wav"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Azure Blob Storage is not configured");
    }

    @Test
    void deleteByUrlSkipsInvalidUrl() {
        ReflectionTestUtils.setField(adapter, "containerClient", containerClient);
        adapter.deleteByUrl("not-a-valid-url");

        verifyNoInteractions(containerClient);
    }
}