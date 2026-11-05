package com.zhanhe.servicedesk.web.controller.it;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zhanhe.servicedesk.common.annotation.Log;
import com.zhanhe.servicedesk.common.core.controller.BaseController;
import com.zhanhe.servicedesk.common.core.domain.AjaxResult;
import com.zhanhe.servicedesk.common.core.page.TableDataInfo;
import com.zhanhe.servicedesk.common.enums.BusinessType;
import com.zhanhe.servicedesk.system.domain.ItTicket;
import com.zhanhe.servicedesk.system.domain.ItTicketRecord;
import com.zhanhe.servicedesk.system.service.IItTicketService;

@RestController
@RequestMapping("/it/ticket")
public class ItTicketController extends BaseController
{
    @Autowired
    private IItTicketService ticketService;

    @PreAuthorize("@ss.hasPermi('it:ticket:list')")
    @GetMapping("/list")
    public TableDataInfo list(ItTicket ticket)
    {
        startPage();
        List<ItTicket> list = ticketService.selectItTicketList(ticket);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:query')")
    @GetMapping("/{ticketId}")
    public AjaxResult getInfo(@PathVariable Long ticketId)
    {
        AjaxResult ajax = success(ticketService.selectItTicketById(ticketId));
        List<ItTicketRecord> records = ticketService.selectRecordList(ticketId);
        ajax.put("records", records);
        return ajax;
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:add')")
    @Log(title = "IT 工单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody ItTicket ticket)
    {
        return toAjax(ticketService.createTicket(ticket, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:handle')")
    @Log(title = "IT 工单", businessType = BusinessType.UPDATE)
    @PutMapping("/{ticketId}/assign")
    public AjaxResult assign(@PathVariable Long ticketId, @RequestBody ItTicket ticket)
    {
        return toAjax(ticketService.assignTicket(ticketId, ticket.getHandler(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:handle')")
    @Log(title = "IT 工单", businessType = BusinessType.UPDATE)
    @PutMapping("/{ticketId}/process")
    public AjaxResult process(@PathVariable Long ticketId, @RequestBody ItTicket ticket)
    {
        return toAjax(ticketService.processTicket(ticketId, ticket.getRemark(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:handle')")
    @Log(title = "IT 工单", businessType = BusinessType.UPDATE)
    @PutMapping("/{ticketId}/reject")
    public AjaxResult reject(@PathVariable Long ticketId, @RequestBody ItTicket ticket)
    {
        return toAjax(ticketService.rejectTicket(ticketId, ticket.getRemark(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:close')")
    @Log(title = "IT 工单", businessType = BusinessType.UPDATE)
    @PutMapping("/{ticketId}/close")
    public AjaxResult close(@PathVariable Long ticketId, @RequestBody ItTicket ticket)
    {
        return toAjax(ticketService.closeTicket(ticketId, ticket.getRemark(), getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:remove')")
    @Log(title = "IT 工单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ticketIds}")
    public AjaxResult remove(@PathVariable Long[] ticketIds)
    {
        return toAjax(ticketService.deleteItTicketByIds(ticketIds));
    }

    @PreAuthorize("@ss.hasPermi('it:ticket:handle')")
    @PostMapping("/scanOverdue")
    public AjaxResult scanOverdue()
    {
        return success(ticketService.scanOverdueTickets());
    }
}
