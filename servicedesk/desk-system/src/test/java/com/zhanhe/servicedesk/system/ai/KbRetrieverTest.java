package com.zhanhe.servicedesk.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.zhanhe.servicedesk.system.domain.ItTicket;
import com.zhanhe.servicedesk.system.mapper.ItTicketMapper;

/**
 * 历史工单检索（BM25）的回归用例。
 * 检索刻意不用大模型：结果必须可复现、可解释，因此这里用固定语料断言排序。
 */
class KbRetrieverTest
{
    private static ItTicketMapper mapperWith(List<Map<String, Object>> rows)
    {
        return new ItTicketMapper()
        {
            @Override
            public List<Map<String, Object>> selectTicketsForRetrieval()
            {
                return rows;
            }

            @Override
            public ItTicket selectItTicketById(Long ticketId)
            {
                return null;
            }

            @Override
            public List<ItTicket> selectItTicketList(ItTicket ticket)
            {
                return new ArrayList<>();
            }

            @Override
            public List<ItTicket> selectOverduePendingTickets()
            {
                return new ArrayList<>();
            }

            @Override
            public int insertItTicket(ItTicket ticket)
            {
                return 0;
            }

            @Override
            public int updateItTicket(ItTicket ticket)
            {
                return 0;
            }

            @Override
            public int deleteItTicketByIds(Long[] ticketIds)
            {
                return 0;
            }
        };
    }

    private static Map<String, Object> row(long id, String no, String title, String desc, String note)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("ticketId", id);
        map.put("ticketNo", no);
        map.put("ticketType", "设备故障");
        map.put("title", title);
        map.put("description", desc);
        map.put("deviceInfo", null);
        map.put("symptom", null);
        map.put("impactScope", "single");
        map.put("processNote", note);
        return map;
    }

    private static List<Map<String, Object>> corpus()
    {
        return Arrays.asList(
                row(1L, "IT-PRINTER", "打印机脱机无法打印", "三楼打印机显示脱机，重启打印机也无效",
                        "重新安装打印驱动并清空打印队列后恢复"),
                row(2L, "IT-VPN", "VPN 连不上内网", "在家连 VPN 一直转圈，内网系统打不开",
                        "更换 VPN 节点并重置密钥后正常"),
                row(3L, "IT-ACCOUNT", "账号被锁定", "密码输错多次账号被锁定，无法登录",
                        "解锁账号并重置密码，提示用户开启二次验证"));
    }

    @Test
    void ranksPrinterTicketFirstForPrinterQuery()
    {
        KbRetriever retriever = new KbRetriever(mapperWith(corpus()));
        List<TicketDoc> docs = retriever.search("打印机脱机，打印不了", null, 3);

        // 只返回与查询有词面重合的工单（零分不返回，避免给 IT 看一堆无关工单）
        assertEquals(1, docs.size());
        assertEquals("IT-PRINTER", docs.get(0).getTicketNo());
        assertEquals(100, docs.get(0).getRelevance(), "最高分应归一化为 100");
    }

    @Test
    void dropsUnrelatedTickets()
    {
        KbRetriever retriever = new KbRetriever(mapperWith(corpus()));
        List<TicketDoc> docs = retriever.search("会议室投影仪没有信号", null, 3);
        assertTrue(docs.isEmpty(), "语料里没有投影相关工单，不应硬凑结果");
    }

    @Test
    void findsSolutionFromProcessNote()
    {
        KbRetriever retriever = new KbRetriever(mapperWith(corpus()));
        List<TicketDoc> docs = retriever.search("VPN 内网打不开", null, 2);
        assertEquals("IT-VPN", docs.get(0).getTicketNo());
        assertTrue(docs.get(0).getProcessNote().contains("VPN"));
    }

    @Test
    void excludesCurrentTicket()
    {
        KbRetriever retriever = new KbRetriever(mapperWith(corpus()));
        List<TicketDoc> docs = retriever.search("打印机脱机", 1L, 3);
        assertTrue(docs.stream().noneMatch(d -> d.getTicketId() == 1L));
    }

    @Test
    void returnsEmptyForBlankQueryOrEmptyCorpus()
    {
        KbRetriever empty = new KbRetriever(mapperWith(new ArrayList<>()));
        assertTrue(empty.search("打印机", null, 3).isEmpty());
        KbRetriever retriever = new KbRetriever(mapperWith(corpus()));
        assertTrue(retriever.search("  ", null, 3).isEmpty());
    }

    @Test
    void tokenizesChineseAsBigramAndAsciiAsWord()
    {
        List<String> tokens = KbRetriever.tokenize("打印机 VPN 连不上");
        assertTrue(tokens.contains("打印"));
        assertTrue(tokens.contains("印机"));
        assertTrue(tokens.contains("vpn"));
    }
}
