package com.mindshare.counter.schema;

public final class CounterKeys {

    private CounterKeys() {
    }

    public static String sdsKey(String entityType, String entityId) {
        return "cnt:" + CounterSchema.SCHEMA_ID + ":" + entityType + ":" + entityId;
    }

    public static String aggKey(String entityType, String entityId) {
        return "agg:" + CounterSchema.SCHEMA_ID + ":" + entityType + ":" + entityId;
    }

    public static String bitmapKey(String metric, String entityType, String entityId, long chunk) {
        return "bm:" + metric + ":" + entityType + ":" + entityId + ":" + chunk;
    }
}
