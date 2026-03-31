package org.eci.ZwingBackend.project.application.port.out;

public interface EmailPort {
    void sendInviteEmail(String toEmail, String projectName, String inviteLink);
}