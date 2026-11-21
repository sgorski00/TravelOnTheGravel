package pl.cwup.travelonthegravel.email.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import pl.cwup.travelonthegravel.auth.event.PasswordResetRequestedEvent;
import pl.cwup.travelonthegravel.email.service.EmailService;

final class EmailEventListenerTest {

    private final EmailService emailService = mock(EmailService.class);
    private final EmailEventListener listener = new EmailEventListener(emailService);

    @Test
    void passwordResetEventSendsEmail() {
        listener.onPasswordResetRequested(new PasswordResetRequestedEvent("jan@example.com", "abc123"));

        verify(emailService).sendPasswordResetEmail("jan@example.com", "abc123");
    }
}
