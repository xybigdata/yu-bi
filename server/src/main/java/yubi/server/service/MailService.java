package yubi.server.service;

import yubi.core.entity.Organization;
import yubi.core.entity.User;
import org.springframework.mail.SimpleMailMessage;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

public interface MailService {

    void sendSimpleMail(SimpleMailMessage simpleMailMessage);

    void sendMimeMessage(MimeMessage mimeMessage);

    MimeMessage createMimeMessage() throws MessagingException, UnsupportedEncodingException;

    void sendActiveMail(User user) throws MessagingException, UnsupportedEncodingException;

    void sendInviteMail(User user, Organization organization) throws UnsupportedEncodingException, MessagingException;

    void sendVerifyCode(User user) throws UnsupportedEncodingException, MessagingException;

}
