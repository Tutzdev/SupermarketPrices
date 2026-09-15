package br.com.supermercados.prices.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
class SmtpAccountEmailSender implements AccountEmailSender {

    private final JavaMailSender mailSender;
    private final String sender;

    SmtpAccountEmailSender(JavaMailSender mailSender, @Value("${app.mail.from}") String sender) {
        this.mailSender = mailSender;
        this.sender = sender;
    }

    @Override
    public void sendEmailVerification(String recipient, String token) {
        send(recipient, "Confirme seu e-mail",
                "Use este token em POST /api/v1/auth/email-verifications/confirm:\n\n" + token);
    }

    @Override
    public void sendPasswordReset(String recipient, String token) {
        send(recipient, "Redefinição de senha",
                "Use este token em POST /api/v1/auth/password-resets/confirm:\n\n" + token);
    }

    private void send(String recipient, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(sender);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
