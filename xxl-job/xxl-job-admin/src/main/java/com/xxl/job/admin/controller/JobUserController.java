package com.xxl.job.admin.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.google.common.base.Strings;
import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.controller.interceptor.PermissionInterceptor;
import com.xxl.job.admin.core.model.OperateLog;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.OperateLogDao;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobUserDao;
import com.xxl.job.admin.service.impl.LoginService;
import com.xxl.job.core.biz.model.ReturnT;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xuxueli 2019-05-04 16:39:50
 */
@Controller
@RequestMapping("/user")
public class JobUserController {

    @Resource
    private XxlJobUserDao xxlJobUserDao;
    @Resource
    private XxlJobGroupDao xxlJobGroupDao;

    @Resource
    private LoginService loginService;

    @Resource
    OperateLogDao operateLogDao;


    @RequestMapping
    @PermissionLimit(adminuser = true)
    public String index(Model model,
                        HttpServletRequest request,
                        HttpServletResponse response) {
        XxlJobUser user = loginService.ifLogin(request, response);
        // 执行器列表
        List<XxlJobGroup> groupList = xxlJobGroupDao.findAll2(user.getDept());
        model.addAttribute("groupList", groupList);
        List<String> depts = xxlJobUserDao.selectDistinctDept();
        model.addAttribute("deptList", depts);
        model.addAttribute("dept", user.getDept());
        return "user/user.index";
    }

    @RequestMapping("/pageList")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public Map<String, Object> pageList(@RequestParam(value = "start", required = false, defaultValue = "0") int start,
                                        @RequestParam(value = "length", required = false, defaultValue = "10") int length,
                                        @RequestParam("username") String username,
                                        @RequestParam("role") int role,
                                        @RequestParam(value = "dept", required = false, defaultValue = "") String dept,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {

        XxlJobUser user = loginService.ifLogin(request, response);
        //如果自己是管理员，但是只有
        if (!Strings.isNullOrEmpty(user.getDept())){
            dept = user.getDept();
        }
        // page list
        List<XxlJobUser> list = xxlJobUserDao.pageList(start, length, username, role,dept);
        int list_count = xxlJobUserDao.pageListCount(start, length, username, role,user.getDept());

        // filter
        if (list!=null && list.size()>0) {
            for (XxlJobUser item: list) {
                item.setPassword(null);
            }
        }

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表

        return maps;
    }

    @RequestMapping("/add")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> add(XxlJobUser xxlJobUser,HttpServletRequest request) {

        // valid username
        if (!StringUtils.hasText(xxlJobUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_username") );
        }
        xxlJobUser.setUsername(xxlJobUser.getUsername().trim());
        if (!(xxlJobUser.getUsername().length()>=4 && xxlJobUser.getUsername().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }
        // valid password
        if (!StringUtils.hasText(xxlJobUser.getPassword())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_please_input")+I18nUtil.getString("user_password") );
        }
        xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
        if (!(xxlJobUser.getPassword().length()>=4 && xxlJobUser.getPassword().length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }
        // md5 password
        xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));

        // check repeat
        XxlJobUser existUser = xxlJobUserDao.loadByUserName(xxlJobUser.getUsername());
        if (existUser != null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("user_username_repeat") );
        }

        // write
        xxlJobUserDao.save(xxlJobUser);
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        OperateLog log = new OperateLog(xxlJobUser,"新增用户",loginUser.getUsername());
        operateLogDao.save(log);

        return ReturnT.SUCCESS;
    }

    @RequestMapping("/update")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> update(HttpServletRequest request, XxlJobUser xxlJobUser) {

        // avoid opt login seft
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        if (loginUser.getUsername().equals(xxlJobUser.getUsername())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit")+",不能编辑自己");
        }

        // valid password
        if (StringUtils.hasText(xxlJobUser.getPassword())) {
            xxlJobUser.setPassword(xxlJobUser.getPassword().trim());
            if (!(xxlJobUser.getPassword().length()>=4 && xxlJobUser.getPassword().length()<=20)) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
            }
            // md5 password
            xxlJobUser.setPassword(DigestUtils.md5DigestAsHex(xxlJobUser.getPassword().getBytes()));
        } else {
            xxlJobUser.setPassword(null);
        }

        // write
        xxlJobUserDao.update(xxlJobUser);

        OperateLog log = new OperateLog(xxlJobUser,"编辑用户",loginUser.getUsername());
        operateLogDao.save(log);

        return ReturnT.SUCCESS;
    }

    @RequestMapping("/remove")
    @ResponseBody
    @PermissionLimit(adminuser = true)
    public ReturnT<String> remove(HttpServletRequest request, @RequestParam("id") int id) {

        // avoid opt login seft
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        if (loginUser.getId() == id) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("user_update_loginuser_limit"));
        }
        XxlJobUser old = xxlJobUserDao.load(id);

        xxlJobUserDao.delete(id);

        OperateLog log = new OperateLog(old,"删除用户",loginUser.getUsername());
        operateLogDao.save(log);

        return ReturnT.SUCCESS;
    }

    @RequestMapping("/updatePwd")
    @ResponseBody
    public ReturnT<String> updatePwd(HttpServletRequest request,
                                     @RequestParam("password") String password,
                                     @RequestParam("oldPassword") String oldPassword){

        // valid
        if (oldPassword==null || oldPassword.trim().length()==0){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        if (password==null || password.trim().length()==0){
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("system_please_input") + I18nUtil.getString("change_pwd_field_oldpwd"));
        }
        password = password.trim();
        if (!(password.length()>=4 && password.length()<=20)) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, I18nUtil.getString("system_lengh_limit")+"[4-20]" );
        }

        // md5 password
        String md5OldPassword = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        String md5Password = DigestUtils.md5DigestAsHex(password.getBytes());

        // valid old pwd
        XxlJobUser loginUser = PermissionInterceptor.getLoginUser(request);
        XxlJobUser existUser = xxlJobUserDao.loadByUserName(loginUser.getUsername());
        if (!md5OldPassword.equals(existUser.getPassword())) {
            return new ReturnT<String>(ReturnT.FAIL.getCode(), I18nUtil.getString("change_pwd_field_oldpwd") + I18nUtil.getString("system_unvalid"));
        }

        // write new
        existUser.setPassword(md5Password);
        xxlJobUserDao.update(existUser);


        OperateLog log = new OperateLog(existUser,"修改密码",loginUser.getUsername());
        operateLogDao.save(log);
        return ReturnT.SUCCESS;
    }

    @RequestMapping("/export")
    @ResponseBody
    public void exportUserExcel(@RequestParam("username") String username,
                                @RequestParam("role") int role,
                                @RequestParam(value = "dept", required = false, defaultValue = "") String dept,
                                HttpServletResponse response) {
        List<XxlJobUser> list = xxlJobUserDao.findList(username, role, dept);
        try {
            exportUserExcelWithHutool(response, list);
        } catch (Exception e) {
            // 这里可以根据需要记录日志，或者返回错误信息
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    public void exportUserExcelWithHutool(HttpServletResponse response, List<XxlJobUser> userList) throws IOException {

        // 设置响应头，告诉浏览器这是个 Excel 文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("XXL-JOB用户列表.xlsx", "UTF-8");
        // Content-Disposition 格式更规范，filename*=UTF-8'' + 编码后的文件名
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        try (ServletOutputStream out = response.getOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) { // true 表示 xlsx 格式

            // 自定义表头
            writer.addHeaderAlias("id", "用户ID");
            writer.addHeaderAlias("username", "用户名");
            writer.addHeaderAlias("dept", "部门");
            writer.addHeaderAlias("role", "角色");
            writer.addHeaderAlias("permission", "权限");

            // 只导出你设置了别名的字段，排除 passwd
            writer.setOnlyAlias(true);

            // 写入数据，自动使用别名作为表头
            writer.write(userList, true);

            // 将 Excel 写入响应输出流，第二个参数 true 表示写完后关闭流
            writer.flush(out, true);
        }
    }

}
