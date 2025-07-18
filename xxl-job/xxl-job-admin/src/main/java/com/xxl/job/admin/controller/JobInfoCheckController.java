package com.xxl.job.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.google.common.base.Strings;
import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.exception.XxlJobException;
import com.xxl.job.admin.core.model.OperateLog;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.admin.service.impl.LoginService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * index controller
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobinfoCheck")
public class JobInfoCheckController {
	private static Logger logger = LoggerFactory.getLogger(JobInfoCheckController.class);

	@Resource
	private XxlJobGroupDao xxlJobGroupDao;
	@Resource
	private XxlJobService xxlJobService;
	@Resource
	private LoginService loginService;
	@RequestMapping
	public String index(HttpServletRequest request
			, HttpServletResponse response
			, Model model
			, @RequestParam(value = "jobGroup", required = false, defaultValue = "-1") int jobGroup) {

		// 枚举-字典
		model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());	    // 路由策略-列表
		model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());								// Glue类型-字典
		model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());	    // 阻塞处理策略-字典
		model.addAttribute("ScheduleTypeEnum", ScheduleTypeEnum.values());	    				// 调度类型
		model.addAttribute("MisfireStrategyEnum", MisfireStrategyEnum.values());	    			// 调度过期策略

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
		model.addAttribute("jobGroup", jobGroup);

		return "jobinfoCheck/jobinfoCheck.index";
	}

	@RequestMapping("/pageList")
	@ResponseBody
	public Map<String, Object> pageList(@RequestParam(value = "start", required = false, defaultValue = "0") int start,
										@RequestParam(value = "length", required = false, defaultValue = "10") int length,
										@RequestParam("jobGroup") int jobGroup,
										@RequestParam("triggerStatus") int triggerStatus,
										@RequestParam("jobDesc") String jobDesc,
										@RequestParam("executorHandler") String executorHandler,
										@RequestParam("author") String author) {
		
		return xxlJobService.pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
	}

	@RequestMapping("/checkList")
	@ResponseBody
	public Map<String, Object> checkList(@RequestParam(value = "start", required = false, defaultValue = "0") int start,
										 @RequestParam(value = "length", required = false, defaultValue = "10") int length,
										 @RequestParam("jobGroup") int jobGroup,
										 @RequestParam("triggerStatus") int triggerStatus,
										 @RequestParam("jobDesc") String jobDesc,
										 @RequestParam("executorHandler") String executorHandler,
										 @RequestParam("author") String author,
										 @RequestParam(value = "filterTime", required = false, defaultValue = "") String filterTime) {
		// parse param
		Date startTime = null;
		Date endTime = null;
		if (filterTime!=null && filterTime.trim().length()>0) {
			String[] temp = filterTime.split(" - ");
			if (temp.length == 2) {
				startTime = DateUtil.parseDateTime(temp[0]);
				endTime = DateUtil.parseDateTime(temp[1]);
			}
		}




		return xxlJobService.checkList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author,startTime,endTime);
	}

	@RequestMapping("/add")
	@ResponseBody
	public ReturnT<String> add(HttpServletRequest request, XxlJobInfo jobInfo) {
		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

		// opt
		XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
		return xxlJobService.add(jobInfo, loginUser);
	}
	
	@RequestMapping("/update")
	@ResponseBody
	public ReturnT<String> update(HttpServletRequest request, XxlJobInfo jobInfo) {
		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobInfo.getJobGroup());

		// opt
		XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
		return xxlJobService.update(jobInfo, loginUser);
	}
	
	@RequestMapping("/remove")
	@ResponseBody
	public ReturnT<String> remove(@RequestParam("id") int id) {
		return xxlJobService.remove(id);
	}
	
	@RequestMapping("/stop")
	@ResponseBody
	public ReturnT<String> pause(@RequestParam("id") int id) {
		return xxlJobService.stop(id);
	}
	
	@RequestMapping("/start")
	@ResponseBody
	public ReturnT<String> start(@RequestParam("id") int id) {
		return xxlJobService.start(id);
	}
	
	@RequestMapping("/trigger")
	@ResponseBody
	public ReturnT<String> triggerJob(HttpServletRequest request,
									  @RequestParam("id") int id,
									  @RequestParam("executorParam") String executorParam,
									  @RequestParam("addressList") String addressList) {

		// login user
		XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
		// trigger
		return xxlJobService.trigger(loginUser, id, executorParam, addressList);
	}

	@RequestMapping("/nextTriggerTime")
	@ResponseBody
	public ReturnT<List<String>> nextTriggerTime(@RequestParam("scheduleType") String scheduleType,
												 @RequestParam("scheduleConf") String scheduleConf) {

		XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
		paramXxlJobInfo.setScheduleType(scheduleType);
		paramXxlJobInfo.setScheduleConf(scheduleConf);

		List<String> result = new ArrayList<>();
		try {
			Date lastTime = new Date();
			for (int i = 0; i < 5; i++) {
				lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
				if (lastTime != null) {
					result.add(DateUtil.formatDateTime(lastTime));
				} else {
					break;
				}
			}
		} catch (Exception e) {
			logger.error("nextTriggerTime error. scheduleType = {}, scheduleConf= {}", scheduleType, scheduleConf, e);
			return new ReturnT<List<String>>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")+I18nUtil.getString("system_unvalid")) + e.getMessage());
		}
		return new ReturnT<List<String>>(result);

	}


	@RequestMapping("/export")
	@ResponseBody
	public void exportUserExcel(HttpServletRequest request,
								@RequestParam("jobGroup") int jobGroup,
								@RequestParam("triggerStatus") int triggerStatus,
								@RequestParam("jobDesc") String jobDesc,
								@RequestParam("executorHandler") String executorHandler,
								@RequestParam("author") String author,
								@RequestParam(value = "filterTime", required = false, defaultValue = "") String filterTime,
								HttpServletResponse response) {
		// valid permission
		PermissionInterceptor.validJobGroupPermission(request, jobGroup);	// 仅管理员支持查询全部；普通用户仅支持查询有权限的 jobGroup

		Date startTime = null;
		Date endTime = null;
		if (filterTime!=null && filterTime.trim().length()>0) {
			String[] temp = filterTime.split(" - ");
			if (temp.length == 2) {
				startTime = DateUtil.parseDateTime(temp[0]);
				endTime = DateUtil.parseDateTime(temp[1]);
			}
		}




		Map<String, Object> map = xxlJobService.checkList( 0,10000,jobGroup, triggerStatus, jobDesc, executorHandler, author,startTime,endTime);

		List<XxlJobInfo> list  = (List<XxlJobInfo>) map.get("data");

		try {
			exportUserExcelWithHutool(response, list);
		} catch (Exception e) {
			// 这里可以根据需要记录日志，或者返回错误信息
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	public void exportUserExcelWithHutool(HttpServletResponse response, List<XxlJobInfo> userList) throws IOException {

		// 设置响应头，告诉浏览器这是个 Excel 文件
		response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		response.setCharacterEncoding("utf-8");
		String fileName = URLEncoder.encode("XXL-JOB停服检查任务表.xlsx", "UTF-8");
		// Content-Disposition 格式更规范，filename*=UTF-8'' + 编码后的文件名
		response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

		try (ServletOutputStream out = response.getOutputStream();
			 ExcelWriter writer = ExcelUtil.getWriter(true)) { // true 表示 xlsx 格式

			// 自定义表头
			writer.addHeaderAlias("id", "id");
			writer.addHeaderAlias("jobDesc", "任务名称");

			writer.addHeaderAlias("jobGroup", "执行器id");
			writer.addHeaderAlias("scheduleType", "任务类型");
			writer.addHeaderAlias("scheduleConf", "任务配置");
			writer.addHeaderAlias("triggerStatus", "状态");
			writer.addHeaderAlias("triggerNextDate", "下次执行时间");

			// 只导出你设置了别名的字段，排除 passwd
			writer.setOnlyAlias(true);

			// 写入数据，自动使用别名作为表头
			writer.write(userList, true);

			// 将 Excel 写入响应输出流，第二个参数 true 表示写完后关闭流
			writer.flush(out, true);
		}
	}

}
