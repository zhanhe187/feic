package com.zhanhe.servicedesk.system.mapper;

import java.util.List;
import java.util.Map;
import com.zhanhe.servicedesk.system.domain.ItTicket;

public interface ItTicketMapper
{
    ItTicket selectItTicketById(Long ticketId);

    List<ItTicket> selectItTicketList(ItTicket ticket);

    List<ItTicket> selectOverduePendingTickets();

    /**
     * 历史工单语料（已完结且有处理说明），供「历史同类工单」检索。
     * 返回 map：ticketId / ticketNo / ticketType / title / description / deviceInfo / symptom / impactScope / processNote
     */
    List<Map<String, Object>> selectTicketsForRetrieval();

    int insertItTicket(ItTicket ticket);

    int updateItTicket(ItTicket ticket);

    int deleteItTicketByIds(Long[] ticketIds);
}
