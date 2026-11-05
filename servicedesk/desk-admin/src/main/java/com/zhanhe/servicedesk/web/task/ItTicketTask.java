package com.zhanhe.servicedesk.web.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.zhanhe.servicedesk.system.service.IItTicketService;

@Component("itTicketTask")
public class ItTicketTask
{
    @Autowired
    private IItTicketService ticketService;

    public void scanOverdueTickets()
    {
        ticketService.scanOverdueTickets();
    }
}
