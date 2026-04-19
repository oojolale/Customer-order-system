package com.pcnx.submitform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.pcnx.submitform.entity.FormSubmission;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 表单提交业务接口
 */
public interface FormSubmissionService extends IService<FormSubmission> {

    /**
     * 提交表单
     */
    void submitForm(FormSubmission form, MultipartFile[] files) throws Exception;

    /**
     * 分页查询记录
     */
    Page<FormSubmission> pageList(int pageNum, int pageSize);

    /**
     * 获取状态文字
     */
    String getStatusText(Integer status);

    /**
     * 解析分类标签
     */
    List<String> parseCategoryList(String category);

    /**
     * 解析附件列表（仅文件名，兼容旧用法）
     */
    List<String> parseAttachments(String attachments);

    /**
     * 解析附件列表，返回包含 path（存储路径）和 name（原始文件名）的 Map 列表
     */
    List<Map<String, String>> parseAttachmentsWithPath(String attachments);
}
