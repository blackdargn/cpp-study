package com.example.pool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Akon-Home on 15/7/8.
 */
public class DirectByteBufferPool {

    private static DirectByteBufferPool instance;
    private DirectByteBufferPool(){
        poolMap = new HashMap<Integer,BytesPool>();
        memoryBlocks = new ArrayList<>(10);
    }
    public static DirectByteBufferPool getInstance(){
        if(instance == null){
            instance = new DirectByteBufferPool();
        }
        return instance;
    }

    private ArrayList<Integer> sortKeys = new ArrayList<>(100);
    private ReentrantLock lock = new ReentrantLock();
    private boolean sortDirty;

    private HashMap<Integer,BytesPool> poolMap;

    private ArrayList<MemoryBlock> memoryBlocks;
    private int blockSize = BytesPool.maxKeepSize;

    public void setMaxPoolSize(int maxSize){
        BytesPool.maxKeepSize = maxSize;
    }

    /** 只第一次初始化有效 */
    public void setBlockSize(int blockSize){
        this.blockSize = blockSize;
    }

    private CByteBuffer slice(int n){
        lock.lock();
        try {
            if(memoryBlocks.size() == 0){
                newBlock();
            }
            CByteBuffer memoryBlock = findSlice(n);
            if(memoryBlock == null) {
                newBlock();
                return findSlice(n);
            }else{
                return memoryBlock;
            }
        }finally {
            lock.unlock();
        }
    }

    private void newBlock(){
        MemoryBlock one = new MemoryBlock(blockSize);
        memoryBlocks.add(0,one);
        System.out.println("## new block " + blockSize);
    }

    private CByteBuffer findSlice(int n){
        CByteBuffer one ;
        for(MemoryBlock memoryBlock : memoryBlocks){
            one = memoryBlock.slice(n);
            if(one != null){
                return one;
            }
        }
        return null;
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
        BytesPool pools = poolMap.get(one.cbb.pointer.capacity());
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
                return one.zero();
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
                DirectByteBufferPool.getInstance().reduceSize(maxKeepSize/2);
            }
        }

        private int reduceSize(int decSize){
            int size = 0;
            while(!cache.isEmpty() && size < decSize){
                cache.poll().recycle();
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

    private static class MemoryBlock{
        private java.nio.ByteBuffer block;
        private ArrayList<Pointer>  recyclePointers;

        private MemoryBlock(int blockSize){
            block = java.nio.ByteBuffer.allocateDirect(blockSize);
            block.limit(0);
            block.position(0);
        }

        private void recycle(Bytes one){
            if(recyclePointers == null){
                recyclePointers = new ArrayList<>(10);
            }
            recyclePointers.add(one.cbb.pointer);
        }

        private CByteBuffer slice(int n){
            if(recyclePointers != null && recyclePointers.size() > 0){
                for(Pointer one : recyclePointers){
                    if(one.capacity() >= n){
                        int rem = one.capacity() - n;

                        Pointer pointer = new Pointer(one.position, one.position + n);

                        int oldPosition = block.position();
                        int oldLimit = block.limit();

                        block.position(pointer.position);
                        block.limit(pointer.limit);

                        ByteBuffer data = block.slice();

                        block.position(oldPosition);
                        block.limit(oldLimit);

                        if(rem == 0){
                            recyclePointers.remove(one);
                        }else{
                            one.position += n;
                        }
                        if(data != null) {
                            return new CByteBuffer(this, data, pointer);
                        }else{
                            System.out.println("#slice3 null, pos:"+pointer.position
                                    +",limit:"+pointer.limit
                                    +",cap:"+block.capacity());
                            return null;
                        }
                    }
                }
            }
            int limit = block.position() + n;
            if(limit > block.capacity()){
                // 空间不足, 下一个
                System.out.println("#slice2 null, pos:" + block.position()
                        +"limit:" + limit + ","
                        +"cap:" + block.capacity());
                return null;
            }else {
                // 空间够，分配一个
                block.limit(limit);
                java.nio.ByteBuffer data = block.slice();
                int position = block.position();
                Pointer pointer = new Pointer(position, limit);
                block.position(limit);

                if(data != null) {
                    return new CByteBuffer(this, data, pointer);
                }else{
                    System.out.println("#slice1 null, pos:" + position
                            +"limit:" + limit + ","
                            +"cap:" + block.capacity());
                    return null;
                }
            }
        }
    }

    private static class Pointer{
        private int position;
        private int limit;

        private Pointer(int position, int limit){
            this.position = position;
            this.limit = limit;
        }

        int capacity(){
            return limit - position;
        }
    }

    private static class CByteBuffer{
        private MemoryBlock block;
        private java.nio.ByteBuffer data;
        private Pointer pointer;

        private CByteBuffer(MemoryBlock block, java.nio.ByteBuffer data, Pointer pointer){
            this.block = block;
            this.data  = data;
            this.pointer = pointer;

            assert (block != null);
            assert (data != null);
            assert (pointer != null);
        }
    }

    public static class Bytes {
        private CByteBuffer cbb;
        private boolean isRetain;

        private Bytes(int n){
            this.cbb = DirectByteBufferPool.getInstance().slice(n);
            assert (cbb != null);
        }

        public java.nio.ByteBuffer get(){
            return cbb.data;
        }

        public Bytes zero(){
            cbb.data.clear();
            return this;
        }

        private Bytes retain(){
            isRetain = true;
            return this;
        }

        private void recycle(){
            cbb.block.recycle(this);
        }

        public void release(){
            if(isRetain) {
                isRetain = false;
                DirectByteBufferPool.getInstance().back(this);
            }
        }
    }
}
