package com.zhanhe.servicedesk.web.controller.it;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zhanhe.servicedesk.common.core.controller.BaseController;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.system.domain.ItMessage;
import com.zhanhe.servicedesk.system.service.IItMessageService;

/**
 * 消息中心：按人投递的站内通知。
 *
 * <p>只返回当前登录人自己的消息，接收人由服务端从会话取，前端传什么都无效。</p>
 */
@RestController
@RequestMapping("/it/message")
public class ItMessageController extends BaseController
{
    private final IItMessageService messageService;

    public ItMessageController(IItMessageService messageService)
    {
        this.messageService = messageService;
    }

    /** 我的消息列表 */
    @PreAuthorize("@ss.hasPermi('it:message:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String readFlag,
            @RequestParam(required = false) String msgType,
            @RequestParam(required = false, defaultValue = "50") Integer limit)
    {
        List<ItMessage> list = messageService.selectByReceiver(SecurityUtils.getUsername(), readFlag, msgType, limit);
        return AjaxResult.success(list);
    }

    /** 未读数，供菜单红点或页面顶部使用 */
    @GetMapping("/unread")
    public AjaxResult unread()
    {
        return AjaxResult.success(messageService.countUnread(SecurityUtils.getUsername()));
    }

    /** 标记单条已读 */
    @PutMapping("/read/{messageId}")
    public AjaxResult read(@PathVariable Long messageId)
    {
        return AjaxResult.success(messageService.markRead(messageId));
    }

    /** 全部已读 */
    @PutMapping("/readAll")
    public AjaxResult readAll()
    {
        return AjaxResult.success(messageService.markAllRead());
    }
}
