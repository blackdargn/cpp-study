package com.example.network;

import com.example.pool.DirectByteBufferPool;
import com.example.protocol.*;
import com.example.protocol.Package;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Created by Akon-Home on 15/7/29.
 */
public class NioClient extends NioHander implements Runnable{
    
    private SocketAddress address;
    private SocketChannel channel;

    public NioClient(){
        // init pool
        DirectByteBufferPool.getInstance().setBlockSize(100 * 1024);
        DirectByteBufferPool.getInstance().setMaxPoolSize(200*1024);
    }

    public void setAddress(String ip, int port){
        this.address = new InetSocketAddress(ip, port);
    }

    /** connect */
    private void connect() throws IOException {
        // client channel
        channel = SocketChannel.open();
        // no blacking
        channel.configureBlocking(false);
        channel.socket().setSendBufferSize(48*1024);
        channel.socket().setReceiveBufferSize(48*1024);
        channel.socket().setKeepAlive(true);
        channel.socket().setReuseAddress(true);
        channel.socket().setSoLinger(false, 0);
        channel.socket().setSoTimeout(5000);
        channel.socket().setTcpNoDelay(true);
        // init selector
        this.selector = Selector.open();
        // connect
        channel.connect(address);
        channel.register(selector, SelectionKey.OP_CONNECT, new KeyOption());
    }

    @Override
    public void run() {
        System.out.println("event loop running");
        while (!Thread.interrupted()){
            // connect and loop
            try {
                connect();
                while(!thread.isInterrupted() && channel.isOpen()) {
                    select();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("exception" + e);
            }finally {
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("channel closed exception : " + e.getMessage());
                    }
                }
                if (selector != null){
                    try {
                        selector.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("selector closed exception : " + e.getMessage());
                    }
                }
                System.out.println("connection closed");
            }
            // sleep and reconnect
            try {
                Thread.sleep(5000);
                System.out.println("reconnecting to " + address);
            } catch (InterruptedException e) {
                break;
            }
        }
        System.out.println("event loop terminated");
    }

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private Thread thread;

    public void start() throws IOException {
        assert address != null && thread == null;

        System.out.println("starting event loop");
        if(thread == null){
            thread = new Thread(this);
        }
        thread.start();
    }

    public void join() throws InterruptedException {
        if (Thread.currentThread().getId() != thread.getId()) thread.join();
    }

    public void stop() throws IOException, InterruptedException {
        System.out.println("stopping event loop");
        thread.interrupt();
        selector.wakeup();
    }

    @Override
    protected void onConnected(SocketChannel channel) {
        connected.set(true);
        if(onReceiverCallBack != null){
            onReceiverCallBack.onConnected();
        }
    }

    @Override
    protected void onDisconnected(SocketChannel channel) {
        connected.set(false);
        if(onReceiverCallBack != null){
            onReceiverCallBack.onDisconnected();
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    @Override
    protected SocketChannel getChannelBySessionId(int sessionId) {
        return channel;
    }

    @Override
    protected void handPkg(Package pkg, SocketChannel channel) {
        if(onReceiverCallBack != null){
            onReceiverCallBack.onReceive(pkg);
        }
    }

    private OnReceiverCallBack onReceiverCallBack;
    public void setOnReceiverCallBack(OnReceiverCallBack callBack){
        this.onReceiverCallBack = callBack;
    }
    public static interface OnReceiverCallBack{
        void onReceive(Package pkg);

        void onConnected();

        void onDisconnected();
    }
}
