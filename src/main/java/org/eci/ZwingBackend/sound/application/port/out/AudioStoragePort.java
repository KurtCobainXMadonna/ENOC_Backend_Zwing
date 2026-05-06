package org.eci.ZwingBackend.sound.application.port.out;

import java.io.InputStream;

public interface AudioStoragePort {
    String upload(String blobPath, InputStream data, long contentLen, String contentType);
    void deleteByUrl(String blobUrl);
}
