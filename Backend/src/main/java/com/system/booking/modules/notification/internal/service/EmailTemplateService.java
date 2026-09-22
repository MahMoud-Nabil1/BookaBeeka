package com.system.booking.modules.notification.internal.service;

import com.system.booking.modules.notification.api.model.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Service responsible for rendering HTML email bodies using the Thymeleaf template engine.
 *
 * <p><b>Architecture Rationale:</b>
 * Decouples email visual presentation and styling from Java code. Templates are stored as
 * standard HTML files under {@code src/main/resources/templates/email/} with full support
 * for dynamic variable interpolation, conditionals, and brand theming.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final ITemplateEngine templateEngine;

    /**
     * Renders an HTML email body for a specific {@link NotificationType}.
     *
     * @param type      The notification type to resolve the appropriate template.
     * @param variables Map of template model variables.
     * @return Rendered HTML string, or falls back to plain text if rendering fails.
     */
    public String renderEmail(NotificationType type, Map<String, Object> variables) {
        String templateName = resolveTemplateName(type);
        return renderTemplate(templateName, variables);
    }

    /**
     * Renders any Thymeleaf template by name.
     *
     * @param templateName Relative path under templates directory (e.g., "email/booking-confirmed").
     * @param variables    Map of template model variables.
     * @return Rendered HTML string.
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context(Locale.ENGLISH);
            if (variables != null) {
                context.setVariables(new HashMap<>(variables));
            }
            return templateEngine.process(templateName, context);
        } catch (Exception ex) {
            log.warn("Failed to render Thymeleaf template [{}]: {}. Falling back to default body.",
                    templateName, ex.getMessage());
            Object body = (variables != null) ? variables.get("body") : null;
            return (body != null) ? body.toString() : "";
        }
    }

    /**
     * Maps {@link NotificationType} to its corresponding Thymeleaf template file path.
     *
     * @param type The notification type.
     * @return Template path relative to templates root.
     */
    public String resolveTemplateName(NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMED -> "email/booking-confirmed";
            case OTP_REQUESTED -> "email/otp-requested";
            case PASSWORD_RESET -> "email/password-reset";
        };
    }
}
