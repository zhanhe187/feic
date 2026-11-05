package com.zhanhe.servicedesk.web.controller.it;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zhanhe.servicedesk.common.core.controller.BaseController;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.ai.AiIntakeService;
import com.zhanhe.servicedesk.system.ai.AiQuota;
import com.zhanhe.servicedesk.system.ai.IntakeResult;
import com.zhanhe.servicedesk.system.ai.KbRetriever;
import com.zhanhe.servicedesk.system.ai.TicketDoc;
import com.zhanhe.servicedesk.system.domain.ItAiLog;
import com.zhanhe.servicedesk.system.domain.ItTicket;
import com.zhanhe.servicedesk.system.mapper.ItAiLogMapper;
import com.zhanhe.servicedesk.system.service.IItTicketService;
import com.zhanhe.servicedesk.web.controller.it.dto.AiIntakeRequest;
import com.zhanhe.servicedesk.web.controller.it.dto.AiSimilarRequest;

/**
 * 工单 AI 辅助接口。
 *
 * <p>边界：AI 只提供「信息结构化」与「历史工单检索」，不参与状态流转，也不返回他人工单数据。</p>
 */
@RestController
@RequestMapping("/it/ai")
public class ItAiController extends BaseController
{
    private final AiIntakeService intakeService;

    private final KbRetriever kbRetriever;

    private final AiQuota aiQuota;

    private final IItTicketService ticketService;

    private final ItAiLogMapper aiLogMapper;

    public ItAiController(AiIntakeService intakeService, KbRetriever kbRetriever, AiQuota aiQuota,
            IItTicketService ticketService, ItAiLogMapper aiLogMapper)
    {
        this.intakeService = intakeService;
        this.kbRetriever = kbRetriever;
        this.aiQuota = aiQuota;
        this.ticketService = ticketService;
        this.aiLogMapper = aiLogMapper;
    }

    /**
     * 建单前：把自由描述解析成结构化槽位（员工确认后再提交，本接口不写库）。
     */
    @PreAuthorize("@ss.hasPermi('it:ai:intake')")
    @PostMapping("/ticket/intake")
    public AjaxResult intake(@RequestBody AiIntakeRequest request)
    {
        String title = request == null ? null : request.getTitle();
        String description = request == null ? null : request.getDescription();
        if (StringUtils.isEmpty(title) && StringUtils.isEmpty(description))
        {
            return AjaxResult.warn("请先填写标题或问题描述");
        }
        IntakeResult result = intakeService.intake(title, description);
        AjaxResult ajax = AjaxResult.success(result);
        ajax.put("quotaRemaining", aiQuota.remaining(currentUserName()));
        return ajax;
    }

    /**
     * 处理中：检索历史同类工单（真实工单 + 真实处理说明）。
     *
     * <p>仅授 it:ai:similar 的角色（管理员 / IT）可调用，普通员工无法通过该接口读到他人工单内容。</p>
     */
    @PreAuthorize("@ss.hasPermi('it:ai:similar')")
    @PostMapping("/kb/similar")
    public AjaxResult similar(@RequestBody AiSimilarRequest request)
    {
        int topK = request == null || request.getTopK() == null ? 3 : Math.min(Math.max(request.getTopK(), 1), 10);
        Long excludeId = request == null ? null : request.getTicketId();
        String query = request == null ? null : request.getQuery();

        if (StringUtils.isEmpty(query) && excludeId != null)
        {
            ItTicket ticket = ticketService.selectItTicketById(excludeId);
            if (ticket == null)
            {
                return AjaxResult.error("工单不存在");
            }
            query = String.join(" ",
                    nullToEmpty(ticket.getTitle()),
                    nullToEmpty(ticket.getSymptom()),
                    nullToEmpty(ticket.getDescription()),
                    nullToEmpty(ticket.getDeviceInfo()));
        }
        if (StringUtils.isEmpty(query))
        {
            return AjaxResult.warn("请提供 ticketId 或 query");
        }
        List<TicketDoc> docs = kbRetriever.search(query, excludeId, topK);
        AjaxResult ajax = AjaxResult.success(docs);
        ajax.put("total", docs.size());
        return ajax;
    }

    /** 当前用户今日剩余调用额度（-1 表示不限） */
    @GetMapping("/quota")
    public AjaxResult quota()
    {
        return AjaxResult.success(aiQuota.remaining(currentUserName()));
    }

    /** 某工单的 AI 调用记录（详情页展示"AI 已辅助"的凭证） */
    @PreAuthorize("@ss.hasPermi('it:ticket:query')")
    @GetMapping("/log/{ticketId}")
    public AjaxResult logs(@PathVariable Long ticketId, @RequestParam(defaultValue = "5") int limit)
    {
        List<ItAiLog> logs = aiLogMapper.selectRecentLogs(null, ticketId, Math.min(Math.max(limit, 1), 20));
        return AjaxResult.success(logs);
    }

    private String currentUserName()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return "anonymous";
        }
    }

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }
}
