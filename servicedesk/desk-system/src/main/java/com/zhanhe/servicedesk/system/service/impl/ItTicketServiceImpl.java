package com.zhanhe.servicedesk.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.common.exception.ServiceException;
import com.zhanhe.servicedesk.common.utils.SecurityUtils;
import com.zhanhe.servicedesk.common.utils.StringUtils;
import com.zhanhe.servicedesk.system.ai.SlotSchema;
import com.zhanhe.servicedesk.system.domain.ItTicket;
import com.zhanhe.servicedesk.system.domain.ItTicketRecord;
import com.zhanhe.servicedesk.system.domain.SysNotice;
import com.zhanhe.servicedesk.system.mapper.ItTicketMapper;
import com.zhanhe.servicedesk.system.mapper.ItTicketRecordMapper;
import com.zhanhe.servicedesk.system.service.IItTicketService;
import com.zhanhe.servicedesk.system.service.ISysNoticeService;
import com.zhanhe.servicedesk.system.service.TicketStateMachine;

@Service
public class ItTicketServiceImpl implements IItTicketService
{
    @Autowired
    private ItTicketMapper ticketMapper;

    @Autowired
    private ItTicketRecordMapper recordMapper;

    @Autowired
    private ISysNoticeService noticeService;

    @Autowired
    private com.zhanhe.servicedesk.system.ai.AiAuditService aiAuditService;

    @Override
    public ItTicket selectItTicketById(Long ticketId)
    {
        return ticketMapper.selectItTicketById(ticketId);
    }

    @Override
    public List<ItTicket> selectItTicketList(ItTicket ticket)
    {
        if (!canHandleTicket())
        {
            ticket.setApplicant(SecurityUtils.getUsername());
        }
        return ticketMapper.selectItTicketList(ticket);
    }

    @Override
    public List<ItTicketRecord> selectRecordList(Long ticketId)
    {
        return recordMapper.selectItTicketRecordList(ticketId);
    }

    @Override
    @Transactional
    public int createTicket(ItTicket ticket, String username)
    {
        if (StringUtils.isEmpty(ticket.getTitle()))
        {
            throw new ServiceException("工单标题不能为空");
        }
        // 服务端兜底：不信任前端提交的 AI 字段（长度/枚举白名单），并强制清掉 status/handler/applicant，
        // 避免有人绕过状态机直接建一条"已分配"的工单。
        ticket.setTicketNo("IT" + System.currentTimeMillis());
        ticket.setApplicant(username);
        ticket.setCreateBy(username);
        ticket.setStatus(TicketStateMachine.STATUS_PENDING);
        ticket.setHandler(null);
        sanitizeAiSlots(ticket);
        int rows = ticketMapper.insertItTicket(ticket);
        insertRecord(ticket.getTicketId(), username, "CREATE", "创建工单", ticket.getStatus());
        linkAiAudit(ticket);
        return rows;
    }

    /**
     * 把「工单采纳了 AI 结构化结果」这件事与工单号关联起来，详情页才能展示 AI 凭证。
     * 与 intake 阶段那次调用是两条记录：前者是"识别了一次"，这里是"这张工单用了识别结果"。
     */
    private void linkAiAudit(ItTicket ticket)
    {
        if (StringUtils.isEmpty(ticket.getAiSource()))
        {
            return;
        }
        String summary = "device=" + (ticket.getDeviceInfo() == null ? 0 : ticket.getDeviceInfo().length())
                + ";symptom=" + (ticket.getSymptom() == null ? 0 : ticket.getSymptom().length())
                + ";impact=" + ticket.getImpactScope();
        aiAuditService.record("intake_commit", ticket.getTicketId(), ticket.getAiSource(), "", false, 0,
                ticket.getDescription(), summary);
    }

    /**
     * AI 槽位的服务端白名单处理：长度截断 + 枚举归一 + 来源白名单。
     * 非法的 impact_scope / ai_source 一律丢弃，绝不让脏值进库。
     */
    private void sanitizeAiSlots(ItTicket ticket)
    {
        ticket.setDeviceInfo(SlotSchema.truncate(ticket.getDeviceInfo(), SlotSchema.MAX_DEVICE));
        ticket.setSymptom(SlotSchema.truncate(ticket.getSymptom(), SlotSchema.MAX_SYMPTOM));
        ticket.setTriedActions(SlotSchema.truncate(ticket.getTriedActions(), SlotSchema.MAX_TRIED));

        String impact = ticket.getImpactScope();
        if (StringUtils.isNotEmpty(impact))
        {
            String trimmed = impact.trim();
            ticket.setImpactScope(SlotSchema.IMPACT_SCOPES.contains(trimmed) ? trimmed : null);
        }
        String source = ticket.getAiSource();
        if (StringUtils.isNotEmpty(source))
        {
            ticket.setAiSource(SlotSchema.normalizeSource(source));
        }
    }

    @Override
    @Transactional
    public int assignTicket(Long ticketId, String handler, String username)
    {
        if (StringUtils.isEmpty(handler))
        {
            throw new ServiceException("处理人不能为空");
        }
        ItTicket ticket = requireTicket(ticketId);
        TicketStateMachine.assertTransition(ticket.getStatus(), TicketStateMachine.STATUS_PROCESSING);
        ItTicket update = new ItTicket();
        update.setTicketId(ticketId);
        update.setHandler(handler);
        update.setStatus(TicketStateMachine.STATUS_PROCESSING);
        update.setUpdateBy(username);
        int rows = ticketMapper.updateItTicket(update);
        insertRecord(ticketId, username, "ASSIGN", "分配给 " + handler, update.getStatus());
        return rows;
    }

    @Override
    @Transactional
    public int processTicket(Long ticketId, String note, String username)
    {
        return transition(ticketId, TicketStateMachine.STATUS_COMPLETED, "PROCESS", note, username, true);
    }

    @Override
    @Transactional
    public int rejectTicket(Long ticketId, String note, String username)
    {
        return transition(ticketId, TicketStateMachine.STATUS_REJECTED, "REJECT", note, username, true);
    }

    @Override
    @Transactional
    public int closeTicket(Long ticketId, String note, String username)
    {
        return transition(ticketId, TicketStateMachine.STATUS_CLOSED, "CLOSE", note, username, false);
    }

    @Override
    public int deleteItTicketByIds(Long[] ticketIds)
    {
        return ticketMapper.deleteItTicketByIds(ticketIds);
    }

    @Override
    public int scanOverdueTickets()
    {
        List<ItTicket> tickets = ticketMapper.selectOverduePendingTickets();
        for (ItTicket ticket : tickets)
        {
            SysNotice notice = new SysNotice();
            notice.setNoticeTitle("IT 工单超时未处理：" + ticket.getTicketNo());
            notice.setNoticeType("1");
            notice.setNoticeContent("工单 " + ticket.getTicketNo() + " 已超过 24 小时未处理，请及时跟进。");
            notice.setStatus("0");
            notice.setCreateBy("system");
            noticeService.insertNotice(notice);
        }
        return tickets.size();
    }

    private int transition(Long ticketId, String nextStatus, String action, String note, String username, boolean complete)
    {
        ItTicket ticket = requireTicket(ticketId);
        TicketStateMachine.assertTransition(ticket.getStatus(), nextStatus);
        ItTicket update = new ItTicket();
        update.setTicketId(ticketId);
        update.setStatus(nextStatus);
        update.setUpdateBy(username);
        if (complete)
        {
            update.setCompleteTime(new Date());
        }
        int rows = ticketMapper.updateItTicket(update);
        insertRecord(ticketId, username, action, note, nextStatus);
        return rows;
    }

    private ItTicket requireTicket(Long ticketId)
    {
        ItTicket ticket = ticketMapper.selectItTicketById(ticketId);
        if (ticket == null)
        {
            throw new ServiceException("工单不存在");
        }
        return ticket;
    }

    private void insertRecord(Long ticketId, String username, String action, String note, String result)
    {
        ItTicketRecord record = new ItTicketRecord();
        record.setTicketId(ticketId);
        record.setProcessor(username);
        record.setAction(action);
        record.setProcessNote(note);
        record.setProcessResult(result);
        record.setProcessTime(new Date());
        record.setCreateBy(username);
        recordMapper.insertItTicketRecord(record);
    }

    private boolean canHandleTicket()
    {
        return SecurityUtils.isAdmin() || SecurityUtils.hasPermi("it:ticket:handle") || SecurityUtils.hasPermi("it:ticket:all");
    }
}
