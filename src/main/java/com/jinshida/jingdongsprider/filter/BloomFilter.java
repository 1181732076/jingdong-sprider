//package com.jinshida.jingdongsprider.filter;
//
//import com.google.common.hash.Funnel;
//import com.google.common.hash.Hashing;
//import com.google.common.primitives.Longs;
//
//import java.io.Serializable;
//
///**
// * This program refers to the java-bloomfilter,you can get its details form https://github.com/MagnusS/Java-BloomFilter.
// * You have any questions about this program please put issues on github to
// * https://github.com/wxisme/bloomfilter
// *
// * @param <E> Element type
// */
//public class BloomFilter<E> implements Cloneable, Serializable {
//
//    private BitArray bits;
//    private long bitsSize;
//    private long expectedNumber;
//    private int numHashFunctions;
//
//    private final Funnel funnel;
//
//    /**
//     * 使用绑定的 bits
//     * @param bits
//     * @param falsePositiveProbability
//     * @param expectedNumber
//     */
//    public BloomFilter(BitArray bits, double falsePositiveProbability, long expectedNumber) {
//        this(bits, falsePositiveProbability, expectedNumber, false);
//    }
//
//    /**
//     * 用于创建 bloomfilter 实例
//     * force=true,实例存在则删除实例，创建新实例。实例不存在，创建实例
//     * force=false,实例存在则使用实例。实例不存在，创建实例
//     *
//     * @param bits                      bits
//     * @param falsePositiveProbability  预期误判率
//     * @param expectedNumber            预期数量级
//     * @param force                     强制创建新的过滤实例
//     */
//    public BloomFilter(BitArray bits, double falsePositiveProbability, long expectedNumber, boolean force) {
//        this.bits = bits;
//
//        // 加载已经存在的实例
//        boolean exists = bits.exists();
//        if (exists && (force == false)) {
//            bits.loadMeta();
//            this.expectedNumber = bits.expected();
//            this.bitsSize = bits.size();
//        } else {
//            bits.clear();
//            this.expectedNumber = expectedNumber;
//            this.bitsSize = optimalNumOfBits(expectedNumber, falsePositiveProbability);
//            this.saveState();
//        }
//
//        this.numHashFunctions = optimalNumOfHashFunctions(this.expectedNumber, this.bitsSize);
//        this.funnel = (Funnel<String>) (from, into) -> into.putBytes(from.getBytes());
//    }
//
//    /**
//     * Compares the contents of two instances to see if they are equal.
//     *
//     * @param obj is the object to compare to.
//     * @return True if the contents of the objects are equal.
//     */
//    @Override
//    public boolean equals(Object obj) {
//        if (obj == null) {
//            return false;
//        }
//        if (getClass() != obj.getClass()) {
//            return false;
//        }
//        final BloomFilter<E> other = (BloomFilter<E>) obj;
//        if (this.expectedNumber != other.expectedNumber) {
//            return false;
//        }
//        if (this.numHashFunctions != other.numHashFunctions) {
//            return false;
//        }
//        if (this.bitsSize != other.bitsSize) {
//            return false;
//        }
//        if (this.bits != other.bits && (this.bits == null || !this.bits.equals(other.bits))) {
//            return false;
//        }
//        return true;
//    }
//
//    /**
//     * Calculates a hash code for this class.
//     *
//     * @return hash code representing the contents of an instance of this class.
//     */
//    @Override
//    public int hashCode() {
//        long hash = 7;
//        hash = 61 * hash + (this.bits != null ? this.bits.hashCode() : 0);
//        hash = 61 * hash + this.expectedNumber;
//        hash = 61 * hash + this.bitsSize;
//        hash = 61 * hash + this.numHashFunctions;
//        return (int) hash;
//    }
//
//    /**
//     * Sets all bits to false in the Bloom filter.
//     */
//    public void clear() {
//        bits.clear();
//    }
//
//    public <T> boolean put(Object object) {
//        long bitSize = this.bitsSize;
//        byte[] bytes = Hashing.murmur3_128().hashObject(object, this.funnel).asBytes();
//        long hash1 = lowerEight(bytes);
//        long hash2 = upperEight(bytes);
//
//        boolean bitsChanged = false;
//        long combinedHash = hash1;
//        for (int i = 0; i < this.numHashFunctions; i++) {
//            // Make the combined hash positive and indexable
//            bitsChanged |= bits.set((combinedHash & Long.MAX_VALUE) % bitSize);
//            combinedHash += hash2;
//        }
//        return bitsChanged;
//    }
//
//    private long lowerEight(byte[] bytes) {
//        return Longs.fromBytes(
//                bytes[7], bytes[6], bytes[5], bytes[4], bytes[3], bytes[2], bytes[1], bytes[0]);
//    }
//
//    private long upperEight(byte[] bytes) {
//        return Longs.fromBytes(
//                bytes[15], bytes[14], bytes[13], bytes[12], bytes[11], bytes[10], bytes[9], bytes[8]);
//    }
//
//    public <T> boolean contain(T object) {
//        long bitSize = this.bits.size();
//        byte[] bytes = Hashing.murmur3_128().hashObject(object, funnel).asBytes();
//        long hash1 = lowerEight(bytes);
//        long hash2 = upperEight(bytes);
//
//        long combinedHash = hash1;
//        for (int i = 0; i < this.numHashFunctions; i++) {
//            // Make the combined hash positive and indexable
//            if (!bits.get((combinedHash & Long.MAX_VALUE) % bitSize)) {
//                return false;
//            }
//            combinedHash += hash2;
//        }
//        return true;
//    }
//
//    /**
//     * 保存状态
//     */
//    public void saveState() {
//        bits.setSize(this.bitsSize);
//        bits.setExpected(this.expectedNumber);
//        bits.saveMeta();
//    }
//
//    /**
//     * 计算合理的hash值
//     * @param n 期望插入的数据
//     * @param m bits的大小
//     * @return
//     */
//    static int optimalNumOfHashFunctions(long n, long m) {
//        // (m / n) * log(2), but avoid truncation due to division!
//        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
//    }
//
//    /**
//     * 计算需要的bit表大小
//     * @param n 期望插入的数量
//     * @param p 冲突率
//     * @return
//     */
//    static long optimalNumOfBits(long n, double p) {
//        if (p == 0) {
//            p = Double.MIN_VALUE;
//        }
//        return (long) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
//    }
//}
