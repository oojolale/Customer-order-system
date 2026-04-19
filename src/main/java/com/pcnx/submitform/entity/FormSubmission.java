package com.pcnx.submitform.entity;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 表单提交记录实体
 */
@Data
@TableName("t_form_submission")
public class FormSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 第一项：分类（多选，逗号分隔：网站/app/小程序/原生app/游戏） */
    private String category;

    /** 第二项：产品类型（单选） */
    private String productType;

    /** 第三项：是否需要计划书 */
    private Integer needPlan;

    /** 第四项：项目规模（单选） */
    private String projectScale;

    /** 第五项：项目资料描述 */
    private String projectInfo;

    /** 第五项：附件文件路径（JSON数组） */
    private String attachments;

    /** 第六项：留言服务（项目功能描述） */
    private String message;

    /** 客户邮箱（必填，用于发送提交确认邮件） */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String clientEmail;

    /** 提交者IP */
    private String submitIp;

    /** 状态：0=待处理 1=已查看 2=已回复 */
    private Integer status;

    /** 管理员备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer isDeleted;
}
