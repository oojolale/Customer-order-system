package com.pcnx.submitform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcnx.submitform.entity.FormSubmission;
import com.pcnx.submitform.service.FormSubmissionService;
import com.pcnx.submitform.service.MailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 表单提交控制器
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;
    private final MailService mailService;

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    /**
     * 表单提交页 - URL: /
     */
    @GetMapping("/")
    public String indexPage(Model model) {
        model.addAttribute("form", new FormSubmission());
        model.addAttribute("pageTitle", "项目需求提交");
        return "index";
    }

    /**
     * 提交表单处理
     */
    @PostMapping("/submit")
    public String submitForm(
            @RequestParam(value = "categories", required = false) String[] categories,
            @RequestParam("productType") String productType,
            @RequestParam(value = "needPlan", required = false) String needPlanStr,
            @RequestParam("projectScale") String projectScale,
            @RequestParam(value = "projectInfo", required = false) String projectInfo,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam("clientEmail") String clientEmail,
            @RequestParam("message") String message,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            // 构建实体
            FormSubmission form = new FormSubmission();

            // 处理多选分类
            if (categories != null && categories.length > 0) {
                form.setCategory(String.join(",", Arrays.asList(categories)));
            } else {
                redirectAttributes.addFlashAttribute("error", "请至少选择一个分类！");
                return "redirect:/";
            }

            form.setProductType(productType);
            form.setNeedPlan("1".equals(needPlanStr) ? 1 : 0);
            form.setProjectScale(projectScale);
            form.setProjectInfo(projectInfo);
            form.setMessage(message);
            form.setClientEmail(clientEmail);

            // 获取客户端IPf
            String ip = getClientIp(request);
            form.setSubmitIp(ip);

            formSubmissionService.submitForm(form, files);

            // 异步发送确认邮件（不阻塞响应）
            mailService.sendSubmitConfirm(form);

            redirectAttributes.addFlashAttribute("success", "提交成功！我会尽快查看并回复您，请耐心等待。");
            return "redirect:/";

        } catch (Exception e) {
            log.error("表单提交失败", e);
            redirectAttributes.addFlashAttribute("error", "提交失败，请稍后重试：" + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * 管理记录页 - URL: /manage
     */
    @GetMapping("/manage")
    public String managePage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<FormSubmission> pageResult = formSubmissionService.pageList(page, size);

        model.addAttribute("pageData", pageResult);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageResult.getPages());
        model.addAttribute("total", pageResult.getTotal());
        model.addAttribute("pageTitle", "提交记录管理");
        model.addAttribute("formSubmissionService", formSubmissionService);
        return "manage";
    }

    /**
     * 导出所有数据到 Excel
     */
    @GetMapping("/export")
    public void exportExcel(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<FormSubmission> list = formSubmissionService.list();
        ObjectMapper objectMapper = new ObjectMapper();

        // 获取服务器地址用于生成下载链接
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }

        // 创建工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("提交记录");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // 创建超链接样式（蓝色下划线）
            CellStyle linkStyle = workbook.createCellStyle();
            Font linkFont = workbook.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());
            linkStyle.setFont(linkFont);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "分类", "产品类型", "计划书", "项目规模", "项目资料", "留言服务", "附件下载链接", "客户邮箱", "提交IP", "状态", "备注", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 4000);
            }
            sheet.setColumnWidth(5, 8000); // 项目资料列更宽
            sheet.setColumnWidth(6, 10000); // 留言服务列更宽
            sheet.setColumnWidth(7, 12000); // 附件下载链接列更宽

            // 日期格式化
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // 填充数据
            int rowNum = 1;
            for (FormSubmission form : list) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(form.getId());
                row.createCell(1).setCellValue(form.getCategory());
                row.createCell(2).setCellValue(form.getProductType());
                row.createCell(3).setCellValue(form.getNeedPlan() == 1 ? "需要" : "不需要");
                row.createCell(4).setCellValue(form.getProjectScale());
                row.createCell(5).setCellValue(form.getProjectInfo() != null ? form.getProjectInfo() : "");
                row.createCell(6).setCellValue(form.getMessage());

                // 附件下载链接（格式：datePath/newName||originalName，多个用逗号分隔）
                String attachmentLinks = "";
                if (form.getAttachments() != null && !form.getAttachments().isEmpty()) {
                    try {
                        String[] fileParts = form.getAttachments().split(",");
                        StringBuilder links = new StringBuilder();
                        for (int i = 0; i < fileParts.length; i++) {
                            String[] info = fileParts[i].split("\\|\\|");
                            if (info.length == 2) {
                                String path = info[0];  // datePath/newName
                                String name = info[1];  // originalName
                                String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8);
                                String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8);
                                String downloadUrl = baseUrl + "/download?path=" + encodedPath + "&name=" + encodedName;
                                if (i > 0) links.append("\n");
                                links.append(name).append(": ").append(downloadUrl);
                            }
                        }
                        attachmentLinks = links.toString();
                    } catch (Exception e) {
                        log.warn("解析附件失败: {}", form.getAttachments(), e);
                    }
                }
                row.createCell(7).setCellValue(attachmentLinks);

                row.createCell(8).setCellValue(form.getClientEmail());
                row.createCell(9).setCellValue(form.getSubmitIp() != null ? form.getSubmitIp() : "");
                row.createCell(10).setCellValue(formSubmissionService.getStatusText(form.getStatus()));
                row.createCell(11).setCellValue(form.getRemark() != null ? form.getRemark() : "");
                row.createCell(12).setCellValue(form.getCreateTime() != null ? form.getCreateTime().format(dtf) : "");
            }

            // 设置响应头
            String fileName = "提交记录_" + java.time.LocalDate.now() + ".xlsx";
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);

            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }
            log.info("导出 Excel 成功，共 {} 条记录", list.size());
        }
    }

    /**
     * 查看详情
     */
    @GetMapping("/detail/{id}")
    public String detailPage(@PathVariable Long id, Model model) {
        FormSubmission form = formSubmissionService.getById(id);
        if (form == null) {
            return "redirect:/manage";
        }

        // 更新状态为已查看
        if (form.getStatus() == 0) {
            form.setStatus(1);
            formSubmissionService.updateById(form);
        }

        model.addAttribute("form", form);
        model.addAttribute("categoryList", formSubmissionService.parseCategoryList(form.getCategory()));
        model.addAttribute("attachmentList", formSubmissionService.parseAttachmentsWithPath(form.getAttachments()));
        model.addAttribute("statusText", formSubmissionService.getStatusText(form.getStatus()));
        model.addAttribute("pageTitle", "提交详情 #" + id);
        return "detail";
    }

    /**
     * 文件下载接口
     * path 参数为存储时的相对路径，如 2026/04/19/abc123.docx
     */
    @GetMapping("/download")
    public void downloadFile(
            @RequestParam("path") String filePath,
            @RequestParam(value = "name", required = false) String originalName,
            HttpServletResponse response) throws IOException {

        // 安全校验：禁止路径穿越
        if (filePath.contains("..") || filePath.contains("\\")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "非法路径");
            return;
        }

        // 构造绝对路径
        Path basePath = Paths.get(uploadPath).isAbsolute()
                ? Paths.get(uploadPath)
                : Paths.get(System.getProperty("user.dir"), uploadPath);
        Path target = basePath.resolve(filePath).normalize();

        // 二次校验：确保目标在 uploads 目录内
        if (!target.startsWith(basePath.normalize())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "拒绝访问");
            return;
        }

        if (!Files.exists(target)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        // 推断 Content-Type
        String contentType = Files.probeContentType(target);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        // 下载文件名：优先用原始文件名，否则取路径最后一段
        String downloadName = (originalName != null && !originalName.isBlank())
                ? originalName
                : target.getFileName().toString();
        String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");

        response.setContentType(contentType);
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(Files.size(target));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(target, out);
            out.flush();
        }
        log.info("文件下载: {}", target);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
