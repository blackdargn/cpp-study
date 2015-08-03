package com.example.network;

import com.example.pool.DirectByteBufferPool;
import com.example.protocol.HeadPkg;
import com.example.protocol.Package;
import com.example.protocol.PackageFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Akon-Home on 15/7/30.
 */
public class NioHander {
    /** 通道管理器 */
    protected Selector selector;

    protected void select() throws IOException {
        if (selector.select() > 0) {
            Iterator<SelectionKey> ite = selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey key = ite.next();
                // 删除已选的key,以防重复处理
                ite.remove();
                // 客户端请求连接事件
                if (key.isConnectable()) {
                    handConnected(key);
                } else if (key.isAcceptable()) {
                    handAccept(key);
                } else if (key.isReadable()) {
                    try {
                        handRead(key);
                    }catch (IOException e){
                        closeChannel((SocketChannel)key.channel());
                        throw e;
                    }
                } else if (key.isWritable()) {
                    try {
                        handWrite(key);
                    }catch (IOException e){
                        closeChannel((SocketChannel)key.channel());
                        throw e;
                    }
                }
            }
        }
    }

    protected void handAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        // 获得和客户端连接的通道
        SocketChannel channel = server.accept();
        channel.configureBlocking(false);
        channel.register(this.selector, SelectionKey.OP_READ, new KeyOption());
        onConnected(channel);
    }

    protected void handConnected(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        if (ch.finishConnect()) {
            key.interestOps(key.interestOps() ^ SelectionKey.OP_CONNECT);
            key.interestOps(key.interestOps() | SelectionKey.OP_READ);
            onConnected(ch);
        }
    }

    protected void onConnected(SocketChannel channel) {

    }

    protected void onDisconnected(SocketChannel channel){

    }

    protected static class KeyOption {
        DirectByteBufferPool.Bytes unComplete;
        Package inputPkg;

        boolean isNewPkg() {
            return inputPkg == null;
        }

        void finish() {
            if(unComplete != null){
                unComplete.release();
            }
            inputPkg = null;
            unComplete = null;
        }
    }

    private DirectByteBufferPool.Bytes headBuf = DirectByteBufferPool.getInstance().take(HeadPkg.LEN_HEAD - 1);
    private DirectByteBufferPool.Bytes headMagic = DirectByteBufferPool.getInstance().take(1);
    private int readCount = 0;
    private int readCountMaxByOne = 5;

    protected void handRead(SelectionKey key) throws IOException {
        System.out.println("handRead ");
        KeyOption option = (KeyOption) key.attachment();

        if (option.isNewPkg()) {

            headBuf.zero();
            headMagic.zero();
            int bytesOp;

            SocketChannel channel = (SocketChannel) key.channel();

            while ((bytesOp = channel.read(headMagic.get())) > 0) {
                headMagic.get().flip();
                if (headMagic.get().get() == HeadPkg.MAGIC) {
                    break;
                } else {
                    headMagic.zero();
                }
            }
            if (bytesOp == 0) {
                // wait next read
                System.out.println("read wait head " + bytesOp);
                return;
            } else if (bytesOp < 0) {
                // peer close
                System.out.println("peer closed read channel");
                closeChannel(channel);
                return;
            }

            while (headBuf.get().hasRemaining()
                    && (bytesOp = channel.read(headBuf.get())) > 0) {
            }

            if (!headBuf.get().hasRemaining()) {
                // read
                HeadPkg head = HeadPkg.make();
                headBuf.get().flip();
                if (head.decode(headBuf.get())) {
                    // read package body
                    int bodyLen = head.getBodyLen();
                    DirectByteBufferPool.Bytes body =
                            DirectByteBufferPool.getInstance().take(bodyLen);
                    Package pkg = PackageFactory.getPackage(head.getCmd(), head.getSeq());
                    pkg.setHead(head);

                    option.inputPkg   = pkg;
                    option.unComplete = body;

                    doRead(option, key);
                } else {
                    System.out.println("invalid package head");
                }
            } else if (bytesOp < 0) {
                // peer close
                System.out.println("peer closed read channel");
                closeChannel(channel);
            } else {
                // invalid package
                System.out.println("invalid package channel close it");
                closeChannel(channel);
            }
        } else {
            doRead(option, key);
        }
    }

    private void closeChannel(SocketChannel channel) throws IOException {
        onDisconnected(channel);
        channel.close();
    }

    private void doRead(KeyOption option, SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        DirectByteBufferPool.Bytes body = option.unComplete;
        Package pack = option.inputPkg;
        int bytesOp = 0;

        try {
            while (body.get().hasRemaining()
                    && (bytesOp = channel.read(body.get())) > 0) {
            }
        }catch (IOException e){
            option.finish();
            throw e;
        }
        if (!body.get().hasRemaining()) {
            body.get().flip();
            // read complete package
            boolean ok = false;
            try {
                ok = pack.decode(body.get());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("decode package exception : " + e.getMessage());
            }
            option.finish();

            if (ok) {
                // dispatch bean business logic layer
                pack.dump();
                handPkg(pack, channel);
            } else {
                // report body exception
                System.out.println("body decode exception : " + pack.dumpHead());
            }

            body = null;
            pack = null;
            ++readCount;

            if (readCount % readCountMaxByOne == 0) {
                // change to another select event
            } else {
                // continue read
                handRead(key);
            }
        } else if (bytesOp < 0) {
            // peer close
            System.out.println("peer closed read channel");
            closeChannel(channel);
        } else {
            // wait next read complete
            System.out.println("read wait body " + bytesOp);
        }
    }

    private LinkedList<Package> outPkgList = new LinkedList<>();
    private ReentrantLock lock = new ReentrantLock();

    public boolean sendPkg(Package pkg) {
        SocketChannel channel = getChannelBySessionId(pkg.getSession());
        if (channel == null){
            System.out.println("send fail : session no map");
            return false;
        }
        if(!channel.isConnectionPending()) {
            if (!channel.isConnected()
                    || !channel.socket().isConnected()
                    || channel.socket().isOutputShutdown()) {
                System.out.println("send fail : not Connected yet or closed");
                return false;
            }
        }

        lock.lock();
        outPkgList.add(pkg);
        lock.unlock();

        SelectionKey key = channel.keyFor(selector);
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup();

        System.out.println("add send queue");
        return true;
    }

    protected SocketChannel getChannelBySessionId(int sessionId) {
        return null;
    }

    private int writeCount = 0;
    private int writeCountMaxByOne = 5;

    protected void handWrite(SelectionKey key) throws IOException {
        System.out.println("handWrite " + outPkgList.size());

        lock.lock();
        Package pkg = outPkgList.poll();
        lock.unlock();

        if (pkg == null) {
            key.interestOps(key.interestOps() ^ SelectionKey.OP_WRITE);
        } else {
            int bodyLen = pkg.getPkgLen();
            DirectByteBufferPool.Bytes body = DirectByteBufferPool.getInstance().take(bodyLen);
            boolean ok = false;
            try {
                ok = pkg.encode(body.get());
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("encode package exception : " + e.getMessage());
            }
            if(!ok){
                System.out.println("encode fail : " + pkg.dumpHead());
            }else{
                pkg.dump();
            }
            body.get().flip();

            int bytesOp = 0;
            SocketChannel ch = (SocketChannel) key.channel();
            try {
                while (ok && body.get().hasRemaining() && (bytesOp = ch.write(body.get())) > 0) {
                }
            }finally {
                body.release();
            }

            if (bytesOp == -1) {
                // peer close
                System.out.println("peer closed write channel");
                ch.close();
            } else {
                pkg = null;
                ++writeCount;
                System.out.println("send pkg ok");

                if (writeCount % writeCountMaxByOne == 0) {
                    // change to another select event
                } else {
                    // continue write util to writeCountMaxByOne times
                    handWrite(key);
                }
            }
        }
    }

    protected void handPkg(Package pkg, SocketChannel channel) {

    }
}
