package com.market.mail;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.market.config.AppConfig;

public class EmailUtil {

    public static void send(String toEmail, String code) {
        String fromEmail = AppConfig.getRequired("mail.from");
        String appPassword = AppConfig.getRequired("mail.app.password");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("[BookMarket] 이메일 인증번호");
            message.setText("인증번호: " + code);

            Transport.send(message);
            System.out.println("이메일 전송 완료: " + toEmail);
        } catch (MessagingException e) {
            System.err.println("이메일 전송 실패: " + e.getMessage());
        }
    }
}
