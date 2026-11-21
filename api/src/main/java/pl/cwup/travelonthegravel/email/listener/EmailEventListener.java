package pl.cwup.travelonthegravel.email.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pl.cwup.travelonthegravel.auth.event.PasswordResetRequestedEvent;
import pl.cwup.travelonthegravel.email.service.EmailService;

@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        emailService.sendPasswordResetEmail(event.email(), event.token());
    }
}
