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
        String subject = "[Koach] 이메일 인증을 완료하세요";
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
        String subject = "[Koach] 비밀번호 재설정 인증 코드";
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
                "    <style>body{margin:0;padding:0}.brand-bar{background:#6C51F0;padding:20px;text-align:center}.brand-name{color:#fff;font-size:22px;font-weight:700;letter-spacing:-0.5px}.brand-sub{color:rgba(255,255,255,0.9);font-size:12px;margin-top:4px}.btn-primary{background:#6C51F0;color:#fff;padding:14px 24px;border-radius:8px;text-decoration:none;font-weight:600;display:inline-block}.btn-primary:hover{background:#5a45d4}.card{max-width:600px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(108,81,240,0.12)}.accent-line{height:4px;background:linear-gradient(90deg,#6C51F0,#8B7CF7)}</style>\n" +
                "</head>\n" +
                "<body style=\"margin:0;padding:0;font-family:'Noto Sans KR',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#F5F3FF;\">\n" +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#F5F3FF;\">\n" +
                "        <tr><td align=\"center\" style=\"padding:40px 20px;\">\n" +
                "            <table class=\"card\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(108,81,240,0.12);\">\n" +
                "                <tr><td class=\"accent-line\" style=\"height:4px;background:linear-gradient(90deg,#6C51F0,#8B7CF7);\"></td></tr>\n" +
                "                <tr><td class=\"brand-bar\" style=\"background:#6C51F0;padding:20px;text-align:center;\"><div class=\"brand-name\" style=\"color:#fff;font-size:22px;font-weight:700;letter-spacing:-0.5px;\">Koach</div><div class=\"brand-sub\" style=\"color:rgba(255,255,255,0.9);font-size:12px;margin-top:4px;\">코치</div></td></tr>\n" +
                "                <tr><td align=\"center\" style=\"padding:40px 28px;\">\n" +
                "                    <h1 style=\"margin:0 0 16px 0;font-size:20px;font-weight:600;color:#1f1f2e;\">이메일 인증을 완료하세요</h1>\n" +
                "                    <p style=\"margin:0 0 32px 0;font-size:14px;line-height:1.6;color:#666;\">아래 버튼을 클릭하여 이메일 인증을 완료해 주세요.<br><span style=\"color:#6C51F0;font-weight:500;\">(5분 내 유효)</span></p>\n" +
                "                    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\">\n" +
                "                        <a href=\"" + link + "\" target=\"_self\" class=\"btn-primary\" style=\"display:inline-block;padding:14px 24px;background:#6C51F0;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;font-size:14px;\"><span style=\"margin-right:8px;\">✓</span>이메일 인증하기</a>\n" +
                "                    </td></tr></table>\n" +
                "                </td></tr>\n" +
                "            </table>\n" +
                "        </td></tr>\n" +
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
                "    <style>.brand-bar{background:#6C51F0;padding:20px;text-align:center}.brand-name{color:#fff;font-size:22px;font-weight:700}.code-box{background:#F5F3FF;border:2px solid #6C51F0;border-radius:10px;padding:20px 32px;font-size:32px;font-weight:700;color:#6C51F0;letter-spacing:8px}</style>\n" +
                "</head>\n" +
                "<body style=\"margin:0;padding:0;font-family:'Noto Sans KR',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#F5F3FF;\">\n" +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#F5F3FF;\">\n" +
                "        <tr><td align=\"center\" style=\"padding:40px 20px;\">\n" +
                "            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(108,81,240,0.12);\">\n" +
                "                <tr><td style=\"height:4px;background:linear-gradient(90deg,#6C51F0,#8B7CF7);\"></td></tr>\n" +
                "                <tr><td class=\"brand-bar\" style=\"background:#6C51F0;padding:20px;text-align:center;\"><div class=\"brand-name\" style=\"color:#fff;font-size:22px;font-weight:700;letter-spacing:-0.5px;\">Koach</div><div style=\"color:rgba(255,255,255,0.9);font-size:12px;margin-top:4px;\">코치</div></td></tr>\n" +
                "                <tr><td align=\"center\" style=\"padding:40px 28px;\">\n" +
                "                    <h1 style=\"margin:0 0 16px 0;font-size:20px;font-weight:600;color:#1f1f2e;\">비밀번호 재설정 인증 코드</h1>\n" +
                "                    <p style=\"margin:0 0 28px 0;font-size:14px;line-height:1.6;color:#666;\">아래 인증 코드를 앱에 입력하여 비밀번호를 재설정해 주세요.<br><span style=\"color:#6C51F0;font-weight:500;\">(5분 내 유효)</span></p>\n" +
                "                    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:0 0 28px 0;\">\n" +
                "                        <div class=\"code-box\" style=\"display:inline-block;padding:20px 32px;background:#F5F3FF;border:2px solid #6C51F0;border-radius:10px;font-weight:700;font-size:32px;color:#6C51F0;letter-spacing:8px;\">" + code + "</div>\n" +
                "                    </td></tr></table>\n" +
                "                    <p style=\"margin:0;font-size:13px;color:#999;text-align:center;\">본인이 요청하지 않은 경우 이 메일을 무시하세요.</p>\n" +
                "                </td></tr>\n" +
                "            </table>\n" +
                "        </td></tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";
    }
}


