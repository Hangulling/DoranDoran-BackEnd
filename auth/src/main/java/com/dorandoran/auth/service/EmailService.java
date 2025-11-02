package com.dorandoran.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendVerificationEmail(String toEmail, String verifyLink) {
        String subject = "[DoranDoran] 이메일 인증을 완료하세요";
        String body = buildVerificationText(verifyLink);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("이메일 인증 메일 전송 성공: to={}", toEmail);
        } catch (MailException e) {
            // Gmail 일일 500통 제한 포함 모든 전송 실패를 포착
            log.error("이메일 전송 실패 (제한/네트워크/설정): to={}, error={}, exception={}", 
                    toEmail, e.getMessage(), e.getClass().getName(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private String buildVerificationText(String link) {
        return "DoranDoran 이메일 인증 안내\n\n"
                + "아래 링크를 클릭하여 이메일 인증을 완료하세요. (5분 내 유효)\n"
                + link + "\n\n"
                + "링크가 클릭되지 않으면 브라우저 주소창에 복사해서 여세요.";
    }
}


