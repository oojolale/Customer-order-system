package com.pcnx.submitform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pcnx.submitform.entity.FormSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 表单提交 Mapper
 */
@Mapper
public interface FormSubmissionMapper extends BaseMapper<FormSubmission> {
}
