package pl.cwup.travelonthegravel.email.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.IContext;
import pl.cwup.travelonthegravel.config.EmailProperties;

final class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final ITemplateEngine templateEngine = mock(ITemplateEngine.class);
    private final MessageSource messageSource = mock(MessageSource.class);
    private final EmailService emailService = new EmailService(
            mailSender,
            templateEngine,
            new EmailProperties("no-reply@example.com", "http://localhost:3000/reset-password"),
            messageSource);

    @Test
    void smtpFailureDoesNotThrow() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("treść");
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("temat");
        doThrow(new MailSendException("SMTP nie działa")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.sendPasswordResetEmail("jan@example.com", "abc123"))
                .doesNotThrowAnyException();
    }
}
