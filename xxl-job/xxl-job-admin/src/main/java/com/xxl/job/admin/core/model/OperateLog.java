package com.xxl.job.admin.core.model;

import java.time.LocalDateTime;
import java.util.Date;

public class OperateLog {
    private Long id;                      // 主键ID
    private String applicationName;      // 应用名称
    private String username;             // 操作用户
    private String operationType;        // 操作类型（编辑、删除等）
    private Long jobId;                  // 关联任务ID
    private String record;               // 操作描述或备注
    private String oldValue;             // 修改前的值（JSON或字符串）
    private String newValue;             // 修改后的值（可选）
    private Date operateTime;  // 操作时间
}
