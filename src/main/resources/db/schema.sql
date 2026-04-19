-- 创建表单提交记录表
DROP TABLE IF EXISTS t_form_submission;

CREATE TABLE t_form_submission (
    id             BIGINT       PRIMARY KEY AUTO_INCREMENT,
    -- 第一项：分类（多选，逗号分隔）
    category       VARCHAR(200) NOT NULL,
    -- 第二项：产品类型（单选）
    product_type   VARCHAR(100) NOT NULL,
    -- 第三项：计划书（是否需要）
    need_plan      TINYINT      NOT NULL DEFAULT 0,
    -- 第四项：项目规模（单选）
    project_scale  VARCHAR(50)  NOT NULL,
    -- 第五项：项目资料
    project_info   TEXT,
    -- 附件文件（多个文件，逗号分隔存储 path||原始名）
    attachments    TEXT,
    -- 第六项：留言服务
    message        TEXT         NOT NULL,
    -- 客户邮箱（用于发送确认邮件）
    client_email   VARCHAR(100),
    -- 提交人信息
    submit_ip      VARCHAR(50),
    -- 状态：0=待处理 1=已查看 2=已回复
    status         TINYINT      NOT NULL DEFAULT 0,
    -- 管理员备注
    remark         VARCHAR(500),
    -- 公共字段
    create_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted     TINYINT      NOT NULL DEFAULT 0
);
