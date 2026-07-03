package com.creatorspace.module.email.service;

import com.creatorspace.common.exception.BusinessException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender,
                        org.springframework.core.env.Environment env) {
        this.mailSender = mailSender;
        this.from = env.getProperty("spring.mail.username", "noreply@qq.com");
    }

    public void sendVerificationCode(String to, String code, String purpose) {
        if (code == null || !code.matches("\\d{6}")) {
            throw BusinessException.badRequest("验证码格式不合法");
        }
        String subject = purposeTitle(purpose);
        String text = """
                您好！
                
                您的验证码为：%s
                
                验证码有效期为 10 分钟，请勿泄露给他人。
                如果不是您本人操作，请忽略此邮件。
                
                CreatorSpace 团队
                """.formatted(code);
        send(to, subject, text, buildVerificationCodeHtml(code, purpose));
    }

    String buildVerificationCodeHtml(String code, String purpose) {
        String action = switch (purpose) {
            case "REGISTER" -> "完成账号注册";
            case "RESET_PASSWORD" -> "重置账号密码";
            default -> "完成身份验证";
        };
        return """
                <!doctype html>
                <html lang="zh-CN">
                  <body style="margin:0;padding:0;background:#f6f8fb;font-family:'Segoe UI','Microsoft YaHei',Arial,sans-serif;color:#111827;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f6f8fb;padding:32px 12px;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:560px;background:#ffffff;border:1px solid #e5e7eb;border-radius:18px;overflow:hidden;box-shadow:0 18px 50px rgba(15,23,42,.08);">
                            <tr>
                              <td style="padding:30px 34px;background:linear-gradient(135deg,#2563eb,#0f766e);color:#ffffff;">
                                <div style="font-size:13px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;opacity:.84;">CreatorSpace</div>
                                <h1 style="margin:10px 0 0;font-size:24px;line-height:1.3;font-weight:800;">%s</h1>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:34px;">
                                <p style="margin:0 0 18px;font-size:16px;line-height:1.8;color:#374151;">你正在%s，请在 10 分钟内输入下面的验证码。</p>
                                <div style="margin:24px 0;padding:20px 22px;border-radius:16px;background:#eff6ff;border:1px solid #bfdbfe;text-align:center;">
                                  <div style="font-size:13px;font-weight:700;color:#1d4ed8;letter-spacing:.12em;text-transform:uppercase;">Verification Code</div>
                                  <div style="margin-top:10px;font-size:36px;line-height:1;font-weight:900;letter-spacing:.24em;color:#111827;">%s</div>
                                </div>
                                <p style="margin:0;font-size:14px;line-height:1.8;color:#6b7280;">请勿把验证码告诉他人。如果不是你本人操作，可以安全忽略这封邮件。</p>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:18px 34px;background:#f9fafb;border-top:1px solid #e5e7eb;color:#9ca3af;font-size:12px;line-height:1.7;">
                                CreatorSpace 团队<br/>
                                这是一封系统邮件，请不要直接回复。
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(purposeTitle(purpose), action, code);
    }

    private String purposeTitle(String purpose) {
        return switch (purpose) {
            case "REGISTER" -> "CreatorSpace 注册验证码";
            case "RESET_PASSWORD" -> "CreatorSpace 重置密码验证码";
            default -> "CreatorSpace 验证码";
        };
    }

    private void send(String to, String subject, String text, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, html);
            mailSender.send(message);
        } catch (Exception e) {
            throw BusinessException.badRequest("邮件发送失败: " + e.getMessage());
        }
    }
}
