package com.dorandoran.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@Slf4j
public class SupportEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public SupportEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 문의 답변 이메일 발송
     *
     * @return 발송 성공 여부 (실패 시 저장은 유지)
     */
    public boolean sendReplyEmail(String toEmail, String requesterName, String category, String answerContent) {
        String subject = "[Koach] 문의하신 내용에 대한 답변입니다";
        String htmlBody = buildReplyHtml(requesterName, category, answerContent);

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
            log.info("문의 답변 이메일 발송 성공: to={}", toEmail);
            return true;
        } catch (MailException e) {
            log.error("문의 답변 이메일 발송 실패: to={}, error={}", toEmail, e.getMessage(), e);
            return false;
        } catch (MessagingException e) {
            log.error("문의 답변 이메일 메시지 생성 실패: to={}, error={}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    private String buildReplyHtml(String requesterName, String category, String answerContent) {
        String displayName = (requesterName != null && !requesterName.isBlank()) ? requesterName : "고객";
        String displayCategory = (category != null && !category.isBlank()) ? category : "문의";
        String escapedAnswer = escapeHtml(answerContent);

        return "<!DOCTYPE html>\n" +
                "<html lang=\"ko\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "</head>\n" +
                "<body style=\"margin:0;padding:0;font-family:'Noto Sans KR',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#F5F3FF;\">\n" +
                "    <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#F5F3FF;\">\n" +
                "        <tr><td align=\"center\" style=\"padding:40px 20px;\">\n" +
                "            <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 4px 24px rgba(108,81,240,0.12);\">\n" +
                "                <tr><td style=\"height:4px;background:linear-gradient(90deg,#6C51F0,#8B7CF7);\"></td></tr>\n" +
                "                <tr><td style=\"background:#6C51F0;padding:20px;text-align:center;\">" +
                "<div style=\"color:#fff;font-size:22px;font-weight:700;letter-spacing:-0.5px;\">Koach</div>" +
                "<div style=\"color:rgba(255,255,255,0.9);font-size:12px;margin-top:4px;\">Coach</div>" +
                "</td></tr>\n" +
                "                <tr><td style=\"padding:40px 28px;\">\n" +
                "                    <h1 style=\"margin:0 0 16px 0;font-size:20px;font-weight:600;color:#1f1f2e;\">" +
                displayName + " 고객님, 문의 답변이 도착했습니다.</h1>\n" +
                "                    <p style=\"margin:0 0 8px 0;font-size:14px;color:#666;\">" +
                "문의 유형: <strong style=\"color:#6C51F0;\">" + displayCategory + "</strong></p>\n" +
                "                    <p style=\"margin:0 0 24px 0;font-size:14px;color:#666;\">아래 내용을 확인해 주세요.</p>\n" +
                "                    <div style=\"background:#F5F3FF;border-left:4px solid #6C51F0;border-radius:4px;padding:16px 20px;font-size:14px;line-height:1.7;color:#333;white-space:pre-line;\">" +
                escapedAnswer +
                "</div>\n" +
                "                    <p style=\"margin:24px 0 0 0;font-size:13px;color:#999;text-align:center;\">추가 문의는 앱 내 고객센터를 이용해 주세요.</p>\n" +
                "                </td></tr>\n" +
                "            </table>\n" +
                "        </td></tr>\n" +
                "    </table>\n" +
                "</body>\n" +
                "</html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
