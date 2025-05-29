package com.xxl.job.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.xxl.job.admin.controller.annotation.PermissionLimit;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.util.JacksonUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.GsonTool;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Created by xuxueli on 17/5/10.
 */
@Controller
@RequestMapping("/api")
public class JobApiController {

    @Resource
    private AdminBiz adminBiz;

    @Resource
    public XxlJobGroupDao xxlJobGroupDao;

    /**
     * api
     *
     * @param uri
     * @param data
     * @return
     */
    @RequestMapping("/{uri}")
    @ResponseBody
    @PermissionLimit(limit=false)
    public ReturnT<String> api(HttpServletRequest request, @PathVariable("uri") String uri, @RequestBody(required = false) String data) {

        // valid
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
        }
        if (uri==null || uri.trim().length()==0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
        }
        String accessToken = request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN);

        JsonNode node = JacksonUtil.stringToJsonObject(data);
//      {"registryGroup":"EXECUTOR","registryKey":"xxl-job-executor-sample","registryValue":"http://192.168.31.184:9999/"}
        String appnanme = node.get("registryKey").asText();

        if (Strings.isNullOrEmpty(accessToken)||Strings.isNullOrEmpty(appnanme)){
            return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is not founnd.");
        }
//        if (XxlJobAdminConfig.getAdminConfig().getAccessToken()!=null
//                && XxlJobAdminConfig.getAdminConfig().getAccessToken().trim().length()>0
//                && !XxlJobAdminConfig.getAdminConfig().getAccessToken().equals(request.getHeader(XxlJobRemotingUtil.XXL_JOB_ACCESS_TOKEN))) {
//            return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
//        }
        List<XxlJobGroup> groups = xxlJobGroupDao.findByAppname(appnanme);
        if (groups==null||groups.isEmpty()){
            if (!appnanme.equals(groups.get(0).getAccessToken())){
                return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
            }
        }

        // services mapping
        if ("callback".equals(uri)) {
            List<HandleCallbackParam> callbackParamList = GsonTool.fromJson(data, List.class, HandleCallbackParam.class);
            return adminBiz.callback(callbackParamList);
        } else if ("registry".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registry(registryParam);
        } else if ("registryRemove".equals(uri)) {
            RegistryParam registryParam = GsonTool.fromJson(data, RegistryParam.class);
            return adminBiz.registryRemove(registryParam);
        } else {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping("+ uri +") not found.");
        }

    }

}
