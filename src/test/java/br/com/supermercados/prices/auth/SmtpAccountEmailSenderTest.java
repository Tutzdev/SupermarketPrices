package br.com.supermercados.prices.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class SmtpAccountEmailSenderTest {

    @Test
    void sendsVerificationThroughLocalSmtpWithoutCredentials() throws Exception {
        try (ServerSocket smtp = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
                var executor = Executors.newSingleThreadExecutor()) {
            smtp.setSoTimeout((int) Duration.ofSeconds(5).toMillis());
            var receivedMessage = executor.submit(() -> receiveMessage(smtp));

            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(InetAddress.getLoopbackAddress().getHostAddress());
            mailSender.setPort(smtp.getLocalPort());
            mailSender.getJavaMailProperties().put("mail.smtp.connectiontimeout", "5000");
            mailSender.getJavaMailProperties().put("mail.smtp.timeout", "5000");
            SmtpAccountEmailSender sender = new SmtpAccountEmailSender(mailSender, "no-reply@localhost");

            sender.sendEmailVerification("person@example.test", "local-verification-token");

            assertThat(receivedMessage.get())
                    .contains("To: person@example.test")
                    .contains("POST /api/v1/auth/email-verifications/confirm")
                    .contains("local-verification-token");
        }
    }

    private String receiveMessage(ServerSocket server) throws Exception {
        try (var socket = server.accept();
                var reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.US_ASCII));
                var writer = new BufferedWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.US_ASCII))) {
            socket.setSoTimeout((int) Duration.ofSeconds(5).toMillis());
            respond(writer, "220 localhost test SMTP");
            StringBuilder message = new StringBuilder();
            boolean receivingData = false;
            String line;
            while ((line = reader.readLine()) != null) {
                if (receivingData) {
                    if (line.equals(".")) {
                        respond(writer, "250 accepted");
                        receivingData = false;
                    } else {
                        message.append(line).append('\n');
                    }
                } else if (line.startsWith("EHLO") || line.startsWith("HELO")) {
                    respond(writer, "250-localhost\r\n250 8BITMIME");
                } else if (line.startsWith("MAIL FROM:") || line.startsWith("RCPT TO:")) {
                    respond(writer, "250 accepted");
                } else if (line.equals("DATA")) {
                    respond(writer, "354 end with <CRLF>.<CRLF>");
                    receivingData = true;
                } else if (line.equals("QUIT")) {
                    respond(writer, "221 closing");
                    return message.toString();
                } else {
                    respond(writer, "250 accepted");
                }
            }
            return message.toString();
        }
    }

    private void respond(BufferedWriter writer, String response) throws Exception {
        writer.write(response);
        writer.write("\r\n");
        writer.flush();
    }
}
