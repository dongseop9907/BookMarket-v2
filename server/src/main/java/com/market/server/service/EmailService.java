package com.market.server.service;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    public void sendVerificationCode(String recipient, String code) throws Exception {
        String host = env("BOOKMARKET_SMTP_HOST", "smtp.gmail.com");
        String port = env("BOOKMARKET_SMTP_PORT", "587");
        String username = required("BOOKMARKET_SMTP_USERNAME");
        String password = required("BOOKMARKET_SMTP_PASSWORD");
        String from = env("BOOKMARKET_SMTP_FROM", username);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));
        message.setSubject("[BookMarket] 이메일 인증번호");
        message.setText("BookMarket 회원가입 인증번호는 " + code + " 입니다. 10분 이내에 입력해 주세요.");
        Transport.send(message);
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration: " + name);
        }
        return value.trim();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            value = System.getProperty(name);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
