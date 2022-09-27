package com.jinshida.jingdongsprider.filter;

public interface BitArray {
    boolean exists();

    void loadMeta();

    void saveMeta();

    boolean set(long bitIndex);

    boolean get(long bitIndex);

    void clear(long bitIndex);

    void clear();

    long size();

    void setSize(long size);

    long expected();

    void setExpected(long expected);
}
