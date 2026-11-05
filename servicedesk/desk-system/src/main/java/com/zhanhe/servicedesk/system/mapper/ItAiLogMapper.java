package com.zhanhe.servicedesk.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.zhanhe.servicedesk.system.domain.ItAiLog;

public interface ItAiLogMapper
{
    int insertItAiLog(ItAiLog log);

    List<ItAiLog> selectRecentLogs(@Param("scene") String scene, @Param("ticketId") Long ticketId,
            @Param("limit") int limit);
}
