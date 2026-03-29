package org.eci.ZwingBackend.auth.infraestructure.adapters.out;

import org.eci.ZwingBackend.auth.application.port.out.GoogleAuthPort;
import org.eci.ZwingBackend.auth.domain.model.GoogleUserData;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class GoogleAuthAdapter implements GoogleAuthPort {
    private final GoogleIdTokenVerifier verifier;

    public GoogleAuthAdapter(@Value("${google.client.id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleUserData verifyToken(String googleToken) {
        try {
            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                return new GoogleUserData(payload.getSubject(), payload.getEmail(), (String) payload.get("name"), (String) payload.get("picture"));
            } else {
                throw new RuntimeException("Invalid Google ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error verifying token", e);
        }
    }
}
