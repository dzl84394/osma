package com.xxl.job.admin.dao;

import com.xxl.job.admin.core.model.OperateLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Mapper
public interface OperateLogDao {

    // 根据ID查询操作日志
    OperateLog selectById(@Param("id") Long id);

    // 查询某个任务ID的所有操作日志，按操作时间倒序
    List<OperateLog> selectByJobId(@Param("jobId") Long jobId);

    // 查询某个时间段内的操作日志
    List<OperateLog> selectByOperateTimeBetween(@Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    // 分页查询所有操作日志，按操作时间倒序
    List<OperateLog> selectPage(@Param("offset") int offset,
                                @Param("limit") int limit,
                                @Param("jobGroup") int jobGroup,
                                @Param("jobId") int jobId,
                                @Param("triggerTimeStart") Date triggerTimeStart,
                                @Param("triggerTimeEnd") Date triggerTimeEnd);

    List<OperateLog> selectList(
                                @Param("jobGroup") int jobGroup,
                                @Param("jobId") int jobId,
                                @Param("triggerTimeStart") Date triggerTimeStart,
                                @Param("triggerTimeEnd") Date triggerTimeEnd);

    // 分页查询某任务ID的操作日志
    List<OperateLog> selectPageByJobId(@Param("jobId") Long jobId,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    // 统计总记录数（分页用）
    int pageListCount(@Param("offset") int offset,
                 @Param("limit") int limit,
                 @Param("jobGroup") int jobGroup,
                 @Param("jobId") int jobId,
                 @Param("triggerTimeStart") Date triggerTimeStart,
                 @Param("triggerTimeEnd") Date triggerTimeEnd);

    // 统计某任务ID的记录数
    int countByJobId(@Param("jobId") Long jobId);
    public int save(OperateLog operateLog);
}
