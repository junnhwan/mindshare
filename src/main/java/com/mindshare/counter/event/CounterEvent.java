package com.mindshare.counter.event;

public record CounterEvent(
        String entityType,
        String entityId,
        String metric,
        int idx,
        long userId,
        int delta
) {
    public static CounterEvent of(String entityType, String entityId, String metric, int idx, long userId, int delta) {
        return new CounterEvent(entityType, entityId, metric, idx, userId, delta);
    }
}
