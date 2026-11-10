package com.zhanhe.servicedesk.system.service;

import java.util.List;

import com.zhanhe.servicedesk.system.domain.ItMessage;

public interface IItMessageService
{
    /** 发一条站内消息（接收人不存在时静默忽略，不影响主流程） */
    void send(ItMessage message);

    /** 按人查消息 */
    List<ItMessage> selectByReceiver(String receiver, String readFlag, String msgType, Integer limit);

    int countUnread(String receiver);

    int markRead(Long messageId);

    int markAllRead();

    /** AI 异常告警：同一场景每天只推一条 */
    void notifyAiAlert(String scene, String detail);
}
