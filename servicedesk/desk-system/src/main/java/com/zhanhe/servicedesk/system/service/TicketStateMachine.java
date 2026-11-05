package com.zhanhe.servicedesk.system.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机：5 个状态、8 条合法流转，全部写在一张流转表里。
 *
 * <p>用表而不是 if 分支，是因为状态流转规则最容易随业务变化：
 * 加一条流转只改 {@link #TRANSITIONS} 一处，非法流转的判断、可选项渲染、
 * 单元测试都从同一份数据出发，不会出现「校验放行但页面没有这个按钮」这类错位。
 */
public final class TicketStateMachine {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CLOSED = "closed";

    /** 当前状态 -> 允许流转到的目标状态集合 */
    private static final Map<String, Set<String>> TRANSITIONS;

    static {
        Map<String, Set<String>> table = new LinkedHashMap<>();
        table.put(STATUS_PENDING, new LinkedHashSet<>(Arrays.asList(
                STATUS_PROCESSING, STATUS_REJECTED, STATUS_CLOSED)));
        table.put(STATUS_PROCESSING, new LinkedHashSet<>(Arrays.asList(
                STATUS_COMPLETED, STATUS_REJECTED, STATUS_CLOSED)));
        table.put(STATUS_REJECTED, new LinkedHashSet<>(Collections.singletonList(STATUS_CLOSED)));
        table.put(STATUS_COMPLETED, new LinkedHashSet<>(Collections.singletonList(STATUS_CLOSED)));
        TRANSITIONS = Collections.unmodifiableMap(table);
    }

    private TicketStateMachine() {
    }

    public static void assertTransition(String currentStatus, String nextStatus) {
        if (!allowedNextStatuses(currentStatus).contains(nextStatus)) {
            throw new IllegalStateException("工单状态不允许从 " + currentStatus + " 流转到 " + nextStatus);
        }
    }

    public static List<String> allowedNextStatuses(String currentStatus) {
        Set<String> next = TRANSITIONS.get(currentStatus);
        return next == null ? Collections.<String>emptyList() : new ArrayList<>(next);
    }

    /** 流转表里的合法边数量，测试与文档用它对齐「8 条流转」这一口径 */
    public static int legalTransitionCount() {
        int count = 0;
        for (Set<String> next : TRANSITIONS.values()) {
            count += next.size();
        }
        return count;
    }
}
