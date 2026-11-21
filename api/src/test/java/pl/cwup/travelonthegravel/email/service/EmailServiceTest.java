package pl.cwup.travelonthegravel.email.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import pl.cwup.travelonthegravel.auth.config.PasswordResetProperties;
import pl.cwup.travelonthegravel.email.config.EmailProperties;

final class EmailServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final ITemplateEngine templateEngine = mock(ITemplateEngine.class);
    private final MessageSource messageSource = mock(MessageSource.class);
    private final EmailService emailService = new EmailService(
            mailSender,
            templateEngine,
            new EmailProperties("no-reply@example.com", "http://localhost:3000/reset-password"),
            messageSource,
            new PasswordResetProperties(Duration.ofHours(1)));

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(anyString(), any(IContext.class))).thenReturn("treść");
        when(messageSource.getMessage(eq("email.password-reset.subject"), any(), any(Locale.class)))
                .thenReturn("Reset hasła");
    }

    @Test
    void sendsEmailWithSubjectRecipientAndSender() throws Exception {
        emailService.sendPasswordResetEmail("jan@example.com", "abc123");

        final var sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo("Reset hasła");
        assertThat(sent.getValue().getRecipients(Message.RecipientType.TO)[0]).hasToString("jan@example.com");
        assertThat(sent.getValue().getFrom()[0]).hasToString("no-reply@example.com");
    }

    @Test
    void templateGetsResetLinkWithTokenAndExpiryMinutes() {
        emailService.sendPasswordResetEmail("jan@example.com", "abc123");

        final var context = ArgumentCaptor.forClass(IContext.class);
        verify(templateEngine).process(eq("email/password-reset"), context.capture());
        final var variables = (Context) context.getValue();
        assertThat(variables.getVariable("resetUrl")).isEqualTo("http://localhost:3000/reset-password?token=abc123");
        assertThat(variables.getVariable("expiryMinutes")).isEqualTo(60L);
    }

    @Test
    void smtpFailureDoesNotThrow() {
        doThrow(new MailSendException("SMTP nie działa")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.sendPasswordResetEmail("jan@example.com", "abc123"))
                .doesNotThrowAnyException();
    }
}
