package com.zhanhe.servicedesk.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhanhe.servicedesk.common.config.DeskConfig;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.common.core.domain.entity.SysUser;
import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.service.ISysUserService;

/**
 * 首页
 *
 * @author zhanhe
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置 */
    @Autowired
    private DeskConfig deskConfig;

    @Autowired
    private ISysUserService userService;

    /**
     * 访问首页，提示语
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", deskConfig.getName(), deskConfig.getVersion());
    }

    /**
     * 解锁屏幕
     */
    @PostMapping("/unlockscreen")
    public AjaxResult unlockScreen(@RequestBody Map<String, String> body)
    {
        String password = body.get("password");
        if (StringUtils.isEmpty(password))
        {
            return AjaxResult.error("密码不能为空");
        }
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        if (user == null)
        {
            return AjaxResult.error("服务器超时，请重新登录");
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            return AjaxResult.error("密码错误，请重新输入");
        }

        return AjaxResult.success("解锁成功");
    }
}
