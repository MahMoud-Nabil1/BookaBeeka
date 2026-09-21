package com.system.booking.modules.notification.internal.service;

import com.system.booking.modules.notification.internal.exception.EmailDispatchException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Service encapsulating email dispatching via {@link JavaMailSender}.
 *
 * <p><b>Architecture & Failure Isolation:</b>
 * This service abstracts low-level SMTP and MIME protocol details. Any mail transport errors,
 * timeout issues, or invalid recipient exceptions are caught, logged with contextual diagnostics,
 * and wrapped into {@link EmailDispatchException}. This allows the calling orchestration layer
 * ({@link NotificationService}) to transition the entity to {@code FAILED} without letting
 * unhandled exceptions terminate background worker threads silently.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailSenderService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@bookabeeka.com}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    /**
     * Dispatches an email to the specified recipient.
     * Automatically detects if the body content contains HTML tags.
     *
     * @param to      Recipient email address.
     * @param subject Subject line of the email.
     * @param body    Message body (HTML or plain text).
     * @throws EmailDispatchException if the email dispatch fails.
     */
    public void sendEmail(String to, String subject, String body) {
        boolean isHtml = body != null && (body.contains("<html") || body.contains("<div") || body.contains("<p>") || body.contains("<br"));
        sendEmail(to, subject, body, isHtml);
    }

    /**
     * Dispatches an email with an explicit HTML format flag.
     *
     * @param to      Recipient email address.
     * @param subject Subject line of the email.
     * @param body    Message body.
     * @param isHtml  True to render as HTML, false for plain text.
     * @throws EmailDispatchException if the email dispatch fails.
     */
    public void sendEmail(String to, String subject, String body, boolean isHtml) {
        if (!mailEnabled) {
            log.info("[MOCK_EMAIL] Email delivery is disabled. To: [{}], Subject: [{}]", to, subject);
            return;
        }

        log.info("Preparing email dispatch to: [{}], Subject: [{}], HTML: [{}]", to, subject, isHtml);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, isHtml);

            mailSender.send(message);
            log.info("Successfully dispatched email to: [{}], Subject: [{}]", to, subject);

        } catch (MessagingException e) {
            log.error("MIME creation failed for email to: [{}] with subject: [{}]. Reason: {}",
                    to, subject, e.getMessage(), e);
            throw new EmailDispatchException("Failed to construct MIME email message: " + e.getMessage(), e);

        } catch (MailException e) {
            log.error("SMTP transport failed when sending email to: [{}] with subject: [{}]. Reason: {}",
                    to, subject, e.getMessage(), e);
            throw new EmailDispatchException("SMTP delivery failed: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("Unexpected error during email dispatch to: [{}]. Reason: {}", to, e.getMessage(), e);
            throw new EmailDispatchException("Unexpected email dispatch failure: " + e.getMessage(), e);
        }
    }
}
