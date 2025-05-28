package com.team.updevic001.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl {

    private final JavaMailSender mailSender;

    @Async("asyncTaskExecutor")
    public void sendEmail(String receiver, EmailTemplate template, Map<String, String> placeholders) {
        try {
            String subject = parseSubject(template, placeholders);
            String body = parseBody(template, placeholders);

            MimeMessage message = prepareMessage(receiver, subject, body);
            mailSender.send(message);

            log.info("Simple email sent to {}", receiver);
        } catch (MessagingException e) {
            log.error("Failed to sen    d simple email: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendFileEmail(String receiver, EmailTemplate template, Map<String, String> placeholders, File attachment) {
        try {

            String subject = parseSubject(template, placeholders);
            String body = parseBody(template, placeholders);

            MimeMessage message = prepareMessage(receiver, subject, body);
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED, "UTF-8");

            if (attachment != null && attachment.exists()) {
                helper.addAttachment(attachment.getName(), attachment);
            }
            mailSender.send(message);
            log.info("Email with attachment sent to {}", receiver);
        } catch (Exception e) {
            log.error("Failed to send email with attachment: {}", e.getMessage(), e);
        }
    }


    protected MimeMessage prepareMessage(String to, String subject, String body) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true);
        return message;
    }

    protected String parseSubject(EmailTemplate template, Map<String, String> placeholders) {
        return processPlaceholders(template.getSubject(), placeholders);
    }

    protected String parseBody(EmailTemplate template, Map<String, String> placeholders) {
        return processPlaceholders(template.getBody(), placeholders);
    }

    public static String processPlaceholders(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            text = text.replace(placeholder, entry.getValue());
        }
        return text;
    }

}