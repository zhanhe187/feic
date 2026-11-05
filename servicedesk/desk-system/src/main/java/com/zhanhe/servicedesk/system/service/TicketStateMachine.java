package com.zhanhe.servicedesk.system.service;

import java.util.Arrays;
import java.util.List;

public final class TicketStateMachine {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_PROCESSING = "processing";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_CLOSED = "closed";

    private TicketStateMachine() {
    }

    public static void assertTransition(String currentStatus, String nextStatus) {
        if (!allowedNextStatuses(currentStatus).contains(nextStatus)) {
            throw new IllegalStateException("工单状态不允许从 " + currentStatus + " 流转到 " + nextStatus);
        }
    }

    public static List<String> allowedNextStatuses(String currentStatus) {
        if (STATUS_PENDING.equals(currentStatus)) {
            return Arrays.asList(STATUS_PROCESSING, STATUS_REJECTED, STATUS_CLOSED);
        }
        if (STATUS_PROCESSING.equals(currentStatus)) {
            return Arrays.asList(STATUS_COMPLETED, STATUS_REJECTED, STATUS_CLOSED);
        }
        if (STATUS_REJECTED.equals(currentStatus)) {
            return Arrays.asList(STATUS_CLOSED);
        }
        if (STATUS_COMPLETED.equals(currentStatus)) {
            return Arrays.asList(STATUS_CLOSED);
        }
        return Arrays.asList();
    }
}
