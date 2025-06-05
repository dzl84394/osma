package com.xxl.job.admin.core.model;

import com.xxl.job.admin.core.util.JacksonUtil;

import java.time.LocalDateTime;
import java.util.Date;


public class OperateLog {
    private Long id;                      // 主键ID
    private Long appId;      // 应用名称
    private String username;             // 操作用户
    private String operationType;        // 操作类型（编辑、删除等）
    private Long jobId;                  // 关联任务ID
    private String record;               // 操作描述或备注
    private String oldValue;             // 修改前的值（JSON或字符串）
    private String newValue;             // 修改后的值（可选）
    private Date operateTime;  // 操作时间

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Long getAppId() {
        return appId;
    }

    public void setAppId(Long appId) {
        this.appId = appId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Date getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(Date operateTime) {
        this.operateTime = operateTime;
    }

    public OperateLog() {
    }

    public OperateLog(XxlJobGroup xxlJobGroup,String operationType,String username) {
        this.appId = (long) xxlJobGroup.getId();
        this.username = username;
        this.operationType =  operationType;
        this.record = JacksonUtil.writeValueAsString(xxlJobGroup);
        this.operateTime = new Date();
    }

    public OperateLog(XxlJobInfo jobInfo,String operationType,String username) {
        this.appId = (long) jobInfo.getJobGroup();
        this.username = username;
        this.operationType =  operationType;
        this.record = JacksonUtil.writeValueAsString(jobInfo);
        this.jobId = (long) jobInfo.getId();
        this.operateTime = new Date();
    }

}
