package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Akon-Home on 15/7/8.
 */
public class ByteArrays {

    private static ByteArrays instance;
    private ByteArrays(){
        poolMap = new HashMap<Integer,BytesPool>();
    }
    public static ByteArrays getInstance(){
        if(instance == null){
            instance = new ByteArrays();
        }
        return instance;
    }

    private ArrayList<Integer> sortKeys = new ArrayList<>(100);
    private ReentrantLock lock = new ReentrantLock();
    private boolean sortDirty;

    private HashMap<Integer,BytesPool> poolMap;

    public void setMaxPoolSize(int maxSize){
        BytesPool.maxKeepSize = maxSize;
    }

    public Bytes take(int n){
        if( n < 0) return null;
        lock.lock();
        try {
            BytesPool pools = poolMap.get(n);
            if (pools == null) {
                pools = new BytesPool(n);
                poolMap.put(n, pools);
                sortKeys.add(n);
                sortDirty = true;
            }
            return pools.take().retain();
        }finally {
            lock.unlock();
        }
    }

    private void back(Bytes one){
        if(one == null) return;
        lock.lock();
        BytesPool pools = poolMap.get(one.data.length);
        if(pools != null){
            pools.back(one);
        }
        lock.unlock();
    }

    private void reduceSize(int decSize){
        lock.lock();
        System.out.println("reduceSize");
        if(sortDirty){
            // dec
            Collections.sort(sortKeys, new Comparator<Integer>() {
                @Override
                public int compare(Integer lo, Integer ro) {
                    return lo - ro >= 0 ? -1 : 1;
                }
            });
            sortDirty = false;
        }
        for(int n : sortKeys){
            decSize -= poolMap.get(n).reduceSize(decSize);
            if(decSize <= 0){
                break;
            }
        }
        lock.unlock();
    }

    public void dump(){
        lock.lock();
        for (int n : sortKeys){
            poolMap.get(n)._dump();
        }
        BytesPool.dump();
        lock.unlock();
    }

    private static class BytesPool{
        private static int newCount = 0, backCount = 0, takeCount = 0;
        private static int keepSize, maxKeepSize = 30*1024;

        private ConcurrentLinkedQueue<Bytes> cache;
        private int len;

        private BytesPool(int n){
            cache = new ConcurrentLinkedQueue<Bytes>();
            this.len = n;
        }

        private Bytes take(){
            ++takeCount;
            if (cache.isEmpty()) {
                ++newCount;
                return new Bytes(len);
            }
            Bytes one = cache.poll();
            if (one != null) {
                keepSize -= len;
                return one;
            } else {
                ++newCount;
                return new Bytes(len);
            }
        }

        private void back(Bytes one){
            cache.offer(one);
            keepSize += len;
            ++backCount;
            if(keepSize > maxKeepSize){
                ByteArrays.getInstance().reduceSize(maxKeepSize/2);
            }
        }

        private int reduceSize(int decSize){
            int size = 0;
            while(!cache.isEmpty() && size < decSize){
                cache.poll();
                size += len;
            }
            keepSize -= size;
            return size;
        }

        private void _dump(){
            System.out.println("size = N:" + len + ",len=" + cache.size() + ",total=" + (len*cache.size()));
        }

        private static void dump(){
            System.out.println("size = B:" + keepSize +",new="+newCount+",take="+takeCount+",back="+backCount);
        }
    }

    public static class Bytes {

        private byte[] data;
        private boolean isRetain;

        private Bytes(int n){
            this.data = new byte[n];
        }

        public byte[] get(){
            return data;
        }

        private Bytes retain(){
            isRetain = true;
            return this;
        }

        public void release(){
            if(isRetain) {
                isRetain = false;
                ByteArrays.getInstance().back(this);
            }
        }
    }
}
