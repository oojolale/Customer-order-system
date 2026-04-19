package com.pcnx.submitform.service;

import com.pcnx.submitform.entity.FormSubmission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 邮件发送服务
 */
@Slf4j
@Service
public class MailService {

    @Autowired
    private JavaMailSenderImpl mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String mailFrom;

    @Value("${mail.subject:【PCNX】项目需求提交确认}")
    private String mailSubject;

    @Value("${mail.templatePath:submit-email}")
    private String templatePath;

    /**
     * 提交成功后，发送确认邮件给客户
     *
     * @param form  表单实体（含所有填写信息）
     */
    @Async
    public void sendSubmitConfirm(FormSubmission form) {
        String clientEmail = form.getClientEmail();
        if (clientEmail == null || clientEmail.isBlank()) {
            log.info("客户未填写邮箱，跳过发送确认邮件");
            return;
        }

        try {
            log.info("开始发送确认邮件到: {}", clientEmail);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(clientEmail);
            helper.setSubject(mailSubject + " - " + DateTimeFormatter.ofPattern("yyyy-MM-dd").format(LocalDateTime.now()));
            helper.setSentDate(new Date());

            // 构建模板变量
            Context ctx = new Context();
            ctx.setVariable("formId", form.getId());
            ctx.setVariable("productType", form.getProductType());
            ctx.setVariable("projectScale", form.getProjectScale());
            ctx.setVariable("message", form.getMessage());
            ctx.setVariable("needPlan", form.getNeedPlan() == 1 ? "需要" : "不需要");
            ctx.setVariable("submitTime", form.getCreateTime() != null
                    ? form.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            ctx.setVariable("categories", form.getCategory());

            String emailHtml = templateEngine.process(templatePath, ctx);
            helper.setText(emailHtml, true);

            mailSender.send(mimeMessage);
            log.info("确认邮件发送成功: {}", clientEmail);

        } catch (MessagingException e) {
            log.error("邮件发送失败: {}", e.getMessage());
        }
    }
}
