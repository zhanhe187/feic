package com.zhanhe.servicedesk.system.service;

import java.util.List;
import com.zhanhe.servicedesk.system.domain.ItTicket;
import com.zhanhe.servicedesk.system.domain.ItTicketRecord;

public interface IItTicketService
{
    ItTicket selectItTicketById(Long ticketId);

    List<ItTicket> selectItTicketList(ItTicket ticket);

    List<ItTicketRecord> selectRecordList(Long ticketId);

    int createTicket(ItTicket ticket, String username);

    int assignTicket(Long ticketId, String handler, String username);

    int processTicket(Long ticketId, String note, String username);

    int rejectTicket(Long ticketId, String note, String username);

    int closeTicket(Long ticketId, String note, String username);

    int deleteItTicketByIds(Long[] ticketIds);

    int scanOverdueTickets();
}
