ByteArraysTestpackage com.example;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Akon-Home on 15/7/8.
 */
public class ByteArrays {

    private static ByteArrays instance;
    private ByteArrays(){
        poolMap = new ConcurrentHashMap<Integer,BytesPool>();
    }
    public synchronized static ByteArrays getInstance(){
        if(instance == null){
            instance = new ByteArrays();
        }
        return instance;
    }

    private ConcurrentHashMap<Integer,BytesPool> poolMap;

    public Bytes take(int n){
        if( n <= 0) return null;

        BytesPool pools = poolMap.get(n);
        if(pools == null){
            pools = new BytesPool(n);
            poolMap.put(n, pools);
        }
        return pools.take();
    }

    private void back(Bytes one){
        if(one == null) return;
        BytesPool pools = poolMap.get(one.data.length);
        if(pools != null){
            pools.back(one);
        }
    }

    public synchronized void dump(){
        BytesPool.dump();
    }

    private static class BytesPool{
        private static int size = 0, newCount = 0, backCount = 0, takeCount = 0;
        private ConcurrentLinkedQueue<Bytes> cache;
        private int len;
        private final ReentrantLock lock = new ReentrantLock();

        public BytesPool(int n){
            cache = new ConcurrentLinkedQueue<Bytes>();
            this.len = n;
        }

        public Bytes take(){
            lock.lock();
            try {
                size -= len;
                ++takeCount;
                if (cache.isEmpty()) {
                    ++newCount;
                    return new Bytes(len);
                }
                Bytes one = cache.poll();
                if (one != null) {
                    return one;
                } else {
                    ++newCount;
                    return new Bytes(len);
                }
            }finally {
                lock.unlock();
            }
        }

        public void back(Bytes one){
            lock.lock();
            try {
                cache.offer(one);
                size += one.data.length;
                ++backCount;
            }finally {
                lock.unlock();
            }
        }

        public synchronized static void dump(){
            System.out.println("size = B" + size +",new="+newCount+",take="+takeCount+",back="+backCount);
        }
    }

    public static class Bytes {

        private byte[] data;

        private Bytes(int n){
            this.data = new byte[n];
        }

        public void release(){
            ByteArrays.getInstance().back(this);
        }
    }
}
