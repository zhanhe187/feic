package com.zhanhe.servicedesk.system.mapper;

import java.util.List;
import com.zhanhe.servicedesk.system.domain.ItTicketRecord;

public interface ItTicketRecordMapper
{
    List<ItTicketRecord> selectItTicketRecordList(Long ticketId);

    int insertItTicketRecord(ItTicketRecord record);
}
