package com.zhanhe.servicedesk.system.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.zhanhe.servicedesk.system.domain.ItMessage;

public interface ItMessageMapper
{
    int insertMessage(ItMessage message);

    List<ItMessage> selectMessageList(@Param("receiver") String receiver, @Param("readFlag") String readFlag,
            @Param("msgType") String msgType, @Param("limit") Integer limit);

    int countUnread(String receiver);

    int markRead(@Param("receiver") String receiver, @Param("messageId") Long messageId);

    int markAllRead(String receiver);
}
