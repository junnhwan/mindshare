package com.mindshare.counter.schema;

public final class BitmapShard {

    public static final int CHUNK_SIZE = 32_768;

    private BitmapShard() {
    }

    public static long chunkOf(long userId) {
        return userId / CHUNK_SIZE;
    }

    public static long bitOf(long userId) {
        return userId % CHUNK_SIZE;
    }
}
