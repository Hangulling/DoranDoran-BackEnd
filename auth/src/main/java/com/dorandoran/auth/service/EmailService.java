package com.dorandoran.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import jakarta.mail.MessagingException;


@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public void sendVerificationEmail(String toEmail, String verifyLink) {
        String subject = "[DoranDoran] 이메일 인증을 완료하세요";
        String htmlBody = buildVerificationHtml(verifyLink);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("이메일 인증 메일 전송 성공: to={}", toEmail);
        } catch (MailException e) {
            // Gmail 일일 500통 제한 포함 모든 전송 실패를 포착
            log.error("이메일 전송 실패 (제한/네트워크/설정): to={}, error={}, exception={}", 
                    toEmail, e.getMessage(), e.getClass().getName(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        } catch (MessagingException e) {
            log.error("이메일 메시지 생성 실패: to={}, error={}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }
    
    /**
     * 비밀번호 재설정 코드 발송
     */
    public void sendPasswordResetCode(String toEmail, String code) {
        String subject = "[DoranDoran] 비밀번호 재설정 인증 코드";
        String htmlBody = buildPasswordResetCodeHtml(code);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(toEmail);
            if (fromAddress != null && !fromAddress.isBlank()) {
                helper.setFrom(fromAddress);
            }
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            
            mailSender.send(message);
            log.info("비밀번호 재설정 코드 메일 전송 성공: to={}", toEmail);
        } catch (MailException e) {
            log.error("이메일 전송 실패 (제한/네트워크/설정): to={}, error={}, exception={}", 
                    toEmail, e.getMessage(), e.getClass().getName(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        } catch (MessagingException e) {
            log.error("이메일 메시지 생성 실패: to={}, error={}", toEmail, e.getMessage(), e);
            throw new RuntimeException("이메일 전송에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private String buildVerificationHtml(String link) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f5f5f5;\">\n" +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5;\">\n" +
                "        <tr>\n" +
                "            <td align=\"center\" style=\"padding: 40px 20px;\">\n" +
                "                <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 8px; overflow: hidden;\">\n" +
                "                    <tr>\n" +
                "                        <td align=\"center\" style=\"padding: 40px 20px;\">\n" +
                "                            <h1 style=\"margin: 0 0 24px 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 20px; line-height: 1.5; color: #333333; text-align: center;\">DoranDoran 이메일 인증 안내</h1>\n" +
                "                            <p style=\"margin: 0 0 32px 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 14px; line-height: 1.5; color: #666666; text-align: center;\">아래 링크를 클릭하여 이메일 인증을 완료하세요. (5분 내 유효)</p>\n" +
                "                            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "                                <tr>\n" +
                "                                    <td align=\"center\" style=\"padding: 0;\">\n" +
                "                                        <a href=\"" + link + "\" target=\"_self\" style=\"display: inline-block; padding: 14px 16px; background-color: rgb(84, 189, 180); color: rgb(255, 255, 255); text-decoration: none; border-radius: 8px; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 14px; line-height: 1.5; text-align: center;\">\n" +
                "                                            <span style=\"display: inline-block; vertical-align: middle; margin-right: 8px;\">✓</span>\n" +
                "                                            <span style=\"display: inline-block; vertical-align: middle;\">이메일 인증하기</span>\n" +
                "                                        </a>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </table>\n" +
                // "                            <p style=\"margin: 32px 0 0 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 14px; line-height: 1.5; color: #999999; text-align: center;\">링크가 클릭되지 않으면 브라우저 주소창에 복사해서 여세요.</p>\n" +
                // "                            <p style=\"margin: 16px 0 0 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 12px; line-height: 1.5; color: #999999; text-align: center; word-break: break-all;\">" + link + "</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";
    }
    
    private String buildPasswordResetCodeHtml(String code) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f5f5f5;\">\n" +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color: #f5f5f5;\">\n" +
                "        <tr>\n" +
                "            <td align=\"center\" style=\"padding: 40px 20px;\">\n" +
                "                <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 8px; overflow: hidden;\">\n" +
                "                    <tr>\n" +
                "                        <td align=\"center\" style=\"padding: 40px 20px;\">\n" +
                "                            <h1 style=\"margin: 0 0 24px 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 20px; line-height: 1.5; color: #333333; text-align: center;\">DoranDoran 비밀번호 재설정</h1>\n" +
                "                            <p style=\"margin: 0 0 32px 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 14px; line-height: 1.5; color: #666666; text-align: center;\">아래 인증 코드를 입력하여 비밀번호를 재설정하세요. (5분 내 유효)</p>\n" +
                "                            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                "                                <tr>\n" +
                "                                    <td align=\"center\" style=\"padding: 0 0 24px 0;\">\n" +
                "                                        <div style=\"display: inline-block; padding: 20px 32px; background-color: #f5f5f5; border-radius: 8px; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 600; font-size: 32px; line-height: 1.5; color: #333333; letter-spacing: 8px;\">" + code + "</div>\n" +
                "                                    </td>\n" +
                "                                </tr>\n" +
                "                            </table>\n" +
                "                            <p style=\"margin: 32px 0 0 0; font-family: 'Noto Sans', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-weight: 500; font-size: 14px; line-height: 1.5; color: #999999; text-align: center;\">본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";
    }
}


