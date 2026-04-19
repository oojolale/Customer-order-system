package com.pcnx.submitform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pcnx.submitform.entity.FormSubmission;
import com.pcnx.submitform.mapper.FormSubmissionMapper;
import com.pcnx.submitform.service.FormSubmissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 表单提交业务实现
 */
@Slf4j
@Service
public class FormSubmissionServiceImpl extends ServiceImpl<FormSubmissionMapper, FormSubmission>
        implements FormSubmissionService {

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForm(FormSubmission form, MultipartFile[] files) throws Exception {
        // 处理文件上传
        if (files != null && files.length > 0) {
            List<String> fileNames = new ArrayList<>();
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

            // 将相对路径转为绝对路径，基于用户目录或当前工作目录
            Path basePath;
            if (Paths.get(uploadPath).isAbsolute()) {
                basePath = Paths.get(uploadPath);
            } else {
                // 以项目实际运行目录为基准（而非 Tomcat 临时目录）
                String userDir = System.getProperty("user.dir");
                basePath = Paths.get(userDir, uploadPath);
            }
            Path dirPath = basePath.resolve(datePath);
            Files.createDirectories(dirPath);
            log.info("文件上传目录: {}", dirPath.toAbsolutePath());

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalName = file.getOriginalFilename();
                    String ext = "";
                    if (originalName != null && originalName.contains(".")) {
                        ext = originalName.substring(originalName.lastIndexOf("."));
                    }
                    String newName = UUID.randomUUID().toString().replace("-", "") + ext;
                    Path filePath = dirPath.resolve(newName);
                    // 使用 Files.copy 替代 transferTo，避免 Tomcat 相对路径问题
                    try (var inputStream = file.getInputStream()) {
                        Files.copy(inputStream, filePath);
                    }
                    // 存储相对路径 + 原始文件名
                    fileNames.add(datePath + "/" + newName + "||" + originalName);
                    log.info("文件上传成功: {}", filePath.toAbsolutePath());
                }
            }
            if (!fileNames.isEmpty()) {
                form.setAttachments(String.join(",", fileNames));
            }
        }

        // 初始状态
        form.setStatus(0);
        form.setCreateTime(LocalDateTime.now());
        form.setUpdateTime(LocalDateTime.now());
        form.setIsDeleted(0);

        this.save(form);
        log.info("表单提交成功, ID: {}", form.getId());
    }

    @Override
    public Page<FormSubmission> pageList(int pageNum, int pageSize) {
        Page<FormSubmission> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<FormSubmission> wrapper = new LambdaQueryWrapper<FormSubmission>()
                .orderByDesc(FormSubmission::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public String getStatusText(Integer status) {
        if (status == null) return "待处理";
        return switch (status) {
            case 0 -> "待处理";
            case 1 -> "已查看";
            case 2 -> "已回复";
            default -> "未知";
        };
    }

    @Override
    public List<String> parseCategoryList(String category) {
        if (!StringUtils.hasText(category)) {
            return new ArrayList<>();
        }
        return Arrays.asList(category.split(","));
    }

    @Override
    public List<String> parseAttachments(String attachments) {
        if (!StringUtils.hasText(attachments)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String item : attachments.split(",")) {
            // 格式: path||originalName
            if (item.contains("||")) {
                result.add(item.split("\\|\\|")[1]); // 返回原始文件名
            } else {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, String>> parseAttachmentsWithPath(String attachments) {
        if (!StringUtils.hasText(attachments)) {
            return new ArrayList<>();
        }
        List<Map<String, String>> result = new ArrayList<>();
        for (String item : attachments.split(",")) {
            Map<String, String> entry = new HashMap<>();
            if (item.contains("||")) {
                String[] parts = item.split("\\|\\|", 2);
                entry.put("path", parts[0].trim());     // 存储的相对路径（用于下载）
                entry.put("name", parts[1].trim());     // 原始文件名（用于展示）
            } else {
                entry.put("path", item.trim());
                entry.put("name", item.trim());
            }
            result.add(entry);
        }
        return result;
    }
}
