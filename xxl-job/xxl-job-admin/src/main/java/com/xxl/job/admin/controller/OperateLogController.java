package com.xxl.job.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.google.common.base.Strings;
import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.complete.XxlJobCompleter;
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.model.*;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.OperateLogDao;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.service.impl.LoginService;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.KillParam;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/operateLog")
public class OperateLogController {
	private static Logger logger = LoggerFactory.getLogger(OperateLogController.class);

	@Resource
	private XxlJobGroupDao xxlJobGroupDao;
	@Resource
	public XxlJobInfoDao xxlJobInfoDao;
	@Resource
	public XxlJobLogDao xxlJobLogDao;
	@Resource
	private LoginService loginService;

	@Resource
	private OperateLogDao operateLogDao;

	@RequestMapping
	public String index(HttpServletRequest request
			, HttpServletResponse response
			, Model model
			, @RequestParam(value = "jobId", required = false, defaultValue = "0") Integer jobId) {
		XxlJobUser user = loginService.ifLogin(request, response);
		String dept = "";
		if (!Strings.isNullOrEmpty(user.getDept())){
			dept = user.getDept();
		}
		// 执行器列表
		List<XxlJobGroup> jobGroupList_all =  xxlJobGroupDao.findAll2(dept);

		// filter group
		List<XxlJobGroup> jobGroupList = PermissionInterceptor.filterJobGroupByRole(request, jobGroupList_all);
		if (jobGroupList==null || jobGroupList.size()==0) {
			throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
		}

		model.addAttribute("JobGroupList", jobGroupList);

		// 任务
		if (jobId > 0) {
			XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
			if (jobInfo == null) {
				throw new RuntimeException(I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
			}

			model.addAttribute("jobInfo", jobInfo);

			// valid permission
			PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());
		}

		return "operateLog/joblog.index";
	}

	@RequestMapping("/getJobsByGroup")
	@ResponseBody
	public ReturnT<List<XxlJobInfo>> getJobsByGroup(@RequestParam("jobGroup") int jobGroup){
		List<XxlJobInfo> list = xxlJobInfoDao.getJobsByGroup(jobGroup);
		return new ReturnT<List<XxlJobInfo>>(list);
	}
	
	@RequestMapping("/pageList")
	@ResponseBody
	public Map<String, Object> pageList(HttpServletRequest request,
										@RequestParam(value = "start", required = false, defaultValue = "0") int start,
										@RequestParam(value = "length", required = false, defaultValue = "10") int length,
										@RequestParam("jobGroup") int jobGroup,
										@RequestParam("jobId") int jobId,
										@RequestParam("filterTime") String filterTime) {

		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobGroup);	// 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup
		
		// parse param
		Date triggerTimeStart = null;
		Date triggerTimeEnd = null;
		if (filterTime!=null && filterTime.trim().length()>0) {
			String[] temp = filterTime.split(" - ");
			if (temp.length == 2) {
				triggerTimeStart = DateUtil.parseDateTime(temp[0]);
				triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
			}
		}
		
		// page query
		List<OperateLog> list = operateLogDao.selectPage(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd);
		int list_count = operateLogDao.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd);
		
		// package result
		Map<String, Object> maps = new HashMap<String, Object>();
	    maps.put("recordsTotal", list_count);		// 总记录数
	    maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
	    maps.put("data", list);  					// 分页列表
		return maps;
	}

	@RequestMapping("/logDetailPage")
	public String logDetailPage(HttpServletRequest request, @RequestParam("id") int id, Model model){

		// base check
		XxlJobLog jobLog = xxlJobLogDao.load(id);
		if (jobLog == null) {
            throw new RuntimeException(I18nUtil.getString("joblog_logid_unvalid"));
		}

		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobLog.getJobGroup());

		// data
        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("logId", jobLog.getId());
		return "joblog/joblog.detail";
	}

	@RequestMapping("/logDetailCat")
	@ResponseBody
	public ReturnT<LogResult> logDetailCat(@RequestParam("logId") long logId, @RequestParam("fromLineNum") int fromLineNum){
		try {
			// valid
			XxlJobLog jobLog = xxlJobLogDao.load(logId);	// todo, need to improve performance
			if (jobLog == null) {
				return new ReturnT<LogResult>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_logid_unvalid"));
			}

			// log cat
			ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(jobLog.getExecutorAddress());
			ReturnT<LogResult> logResult = executorBiz.log(new LogParam(jobLog.getTriggerTime().getTime(), logId, fromLineNum));

			// is end
            if (logResult.getContent()!=null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

			// fix xss
			if (logResult.getContent()!=null && StringUtils.hasText(logResult.getContent().getLogContent())) {
				String newLogContent = filter(logResult.getContent().getLogContent());
				logResult.getContent().setLogContent(newLogContent);
			}

			return logResult;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new ReturnT<LogResult>(ReturnT.FAIL_CODE, e.getMessage());
		}
	}

	/**
	 * filter xss tag
	 *
	 * @param originData
	 * @return
	 */
	private String filter(String originData){

		// exclude tag
		Map<String, String> excludeTagMap = new HashMap<String, String>();
		excludeTagMap.put("<br>", "###TAG_BR###");
		excludeTagMap.put("<b>", "###TAG_BOLD###");
		excludeTagMap.put("</b>", "###TAG_BOLD_END###");

		// replace
		for (String key : excludeTagMap.keySet()) {
			String value = excludeTagMap.get(key);
			originData = originData.replaceAll(key, value);
		}

		// htmlEscape
		originData = HtmlUtils.htmlEscape(originData, "UTF-8");

		// replace back
		for (String key : excludeTagMap.keySet()) {
			String value = excludeTagMap.get(key);
			originData = originData.replaceAll(value, key);
		}

		return originData;
	}

	@RequestMapping("/logKill")
	@ResponseBody
	public ReturnT<String> logKill(@RequestParam("id") int id){
		// base check
		XxlJobLog log = xxlJobLogDao.load(id);
		XxlJobInfo jobInfo = xxlJobInfoDao.loadById(log.getJobId());
		if (jobInfo==null) {
			return new ReturnT<String>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
		}
		if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
			return new ReturnT<String>(500, I18nUtil.getString("joblog_kill_log_limit"));
		}

		// request of kill
		ReturnT<String> runResult = null;
		try {
			ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(log.getExecutorAddress());
			runResult = executorBiz.kill(new KillParam(jobInfo.getId()));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			runResult = new ReturnT<String>(500, e.getMessage());
		}

		if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
			log.setHandleCode(ReturnT.FAIL_CODE);
			log.setHandleMsg( I18nUtil.getString("joblog_kill_log_byman")+":" + (runResult.getMsg()!=null?runResult.getMsg():""));
			log.setHandleTime(new Date());
			XxlJobCompleter.updateHandleInfoAndFinish(log);
			return new ReturnT<String>(runResult.getMsg());
		} else {
			return new ReturnT<String>(500, runResult.getMsg());
		}
	}

	@RequestMapping("/clearLog")
	@ResponseBody
	public ReturnT<String> clearLog(HttpServletRequest request,
									@RequestParam("jobGroup") int jobGroup,
									@RequestParam("jobId") int jobId,
									@RequestParam("type") int type){
		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobGroup);

		// opt
		Date clearBeforeTime = null;
		int clearBeforeNum = 0;
		if (type == 1) {
			clearBeforeTime = DateUtil.addMonths(new Date(), -1);	// 清理一个月之前日志数据
		} else if (type == 2) {
			clearBeforeTime = DateUtil.addMonths(new Date(), -3);	// 清理三个月之前日志数据
		} else if (type == 3) {
			clearBeforeTime = DateUtil.addMonths(new Date(), -6);	// 清理六个月之前日志数据
		} else if (type == 4) {
			clearBeforeTime = DateUtil.addYears(new Date(), -1);	// 清理一年之前日志数据
		} else if (type == 5) {
			clearBeforeNum = 1000;		// 清理一千条以前日志数据
		} else if (type == 6) {
			clearBeforeNum = 10000;		// 清理一万条以前日志数据
		} else if (type == 7) {
			clearBeforeNum = 30000;		// 清理三万条以前日志数据
		} else if (type == 8) {
			clearBeforeNum = 100000;	// 清理十万条以前日志数据
		} else if (type == 9) {
			clearBeforeNum = 0;			// 清理所有日志数据
		} else {
			return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
		}

		List<Long> logIds = null;
		do {
			logIds = xxlJobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
			if (logIds!=null && logIds.size()>0) {
				xxlJobLogDao.clearLog(logIds);
			}
		} while (logIds!=null && logIds.size()>0);

		return ReturnT.SUCCESS;
	}



	@RequestMapping("/export")
	@ResponseBody
	public void exportUserExcel(HttpServletRequest request,
								@RequestParam("jobGroup") int jobGroup,
								@RequestParam("jobId") int jobId,
								@RequestParam("filterTime") String filterTime,
								HttpServletResponse response) {
		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobGroup);	// 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup

		// parse param
		Date triggerTimeStart = null;
		Date triggerTimeEnd = null;
		if (filterTime!=null && filterTime.trim().length()>0) {
			String[] temp = filterTime.split(" - ");
			if (temp.length == 2) {
				triggerTimeStart = DateUtil.parseDateTime(temp[0]);
				triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
			}
		}

		// page query
		List<OperateLog> list = operateLogDao.selectList(jobGroup, jobId, triggerTimeStart, triggerTimeEnd);


		try {
			exportUserExcelWithHutool(response, list);
		} catch (Exception e) {
			// 这里可以根据需要记录日志，或者返回错误信息
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public void exportUserExcelWithHutool(HttpServletResponse response, List<OperateLog> userList) throws IOException {

		// 设置响应头，告诉浏览器这是个 Excel 文件
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		String fileName = URLEncoder.encode("XXL-JOB操作日志.xlsx", "UTF-8");
		// Content-Disposition 格式更规范，filename*=UTF-8'' + 编码后的文件名
		response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

		try (ServletOutputStream out = response.getOutputStream();
			 ExcelWriter writer = ExcelUtil.getWriter(true)) { // true 表示 xlsx 格式

			// 自定义表头
			writer.addHeaderAlias("id", "id");
			writer.addHeaderAlias("userId", "用户Id");
			writer.addHeaderAlias("appId", "执行器Id");
			writer.addHeaderAlias("jobId", "任务id");
			writer.addHeaderAlias("operateUm", "操作人");
			writer.addHeaderAlias("operationType", "操作类型");
			writer.addHeaderAlias("operateTime", "操作时间");
			writer.addHeaderAlias("record", "操作内容");

			// 只导出你设置了别名的字段，排除 passwd
			writer.setOnlyAlias(true);

			// 写入数据，自动使用别名作为表头
			writer.write(userList, true);

			// 将 Excel 写入响应输出流，第二个参数 true 表示写完后关闭流
			writer.flush(out, true);
		}
	}

}
