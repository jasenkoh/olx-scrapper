package org.olxscrapper.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class MailService {
    private final String mailAccount;
    private final String emailPass;

    public MailService(Environment environment) {
        this.mailAccount = environment.getProperty("OLX_EMAIL");
        this.emailPass = environment.getProperty("EMAIL_PASS");
    }

    private Session getMailSession() {
        Properties properties = new Properties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");

        return Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailAccount, emailPass);
            }
        });
    }

    public void sendMail(String subject, String content) {
        try {
            Message message = new MimeMessage(getMailSession());
            message.setFrom(new InternetAddress(mailAccount));
            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(mailAccount)
            );

            message.setSubject(subject);
            message.setContent(content, "text/html");

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
