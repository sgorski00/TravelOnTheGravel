package pl.cwup.travelonthegravel.email.service;

import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import pl.cwup.travelonthegravel.config.EmailProperties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final EmailProperties emailProperties;
    private final MessageSource messageSource;

    public void sendPasswordResetEmail(String to, String token) {
        final var locale = Locale.getDefault();
        final var context = new Context(locale);
        context.setVariable("resetUrl", resetUrl(token));

        try {
            final var message = mailSender.createMimeMessage();
            final var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(emailProperties.from());
            helper.setTo(to);
            helper.setSubject(messageSource.getMessage("email.password-reset.subject", null, locale));
            helper.setText(
                    templateEngine.process("email/password-reset.txt", context),
                    templateEngine.process("email/password-reset", context));
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            log.error("Nie udało się wysłać maila resetu hasła do {}", to, ex);
        }
    }

    private String resetUrl(String token) {
        return UriComponentsBuilder.fromUriString(emailProperties.resetPasswordUrl())
                .queryParam("token", token)
                .toUriString();
    }
}
