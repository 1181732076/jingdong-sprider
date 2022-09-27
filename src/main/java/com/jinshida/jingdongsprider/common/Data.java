package com.jinshida.jingdongsprider.common;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 全局共享数据
 *
 * @author gtf
 */
public class Data {
    /**
     * 字典文件
     */
    public static List<String> dict = new ArrayList<>();

    public static AtomicLong Counter = new AtomicLong(0);
}
