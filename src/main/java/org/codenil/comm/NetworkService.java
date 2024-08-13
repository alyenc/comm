package org.codenil.comm;

import org.codenil.comm.callback.ConnectCallback;
import org.codenil.comm.callback.DisconnectCallback;
import org.codenil.comm.callback.MessageCallback;
import org.codenil.comm.connections.*;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.Message;
import org.codenil.comm.netty.NettyConnectionInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);

    private final CountDownLatch shutdown = new CountDownLatch(1);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final Map<String, PeerConnection> aliveConnections = new ConcurrentHashMap<>();
    private final Map<String, PeerConnection> deadConnections = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    private final Communication communication;

    public NetworkService(
            final NetworkConfig networkConfig,
            final String selfIdentifier) {
        PeerConnectionEvents connectionEvents = new PeerConnectionEvents();
        ConnectionInitializer connectionInitializer = new NettyConnectionInitializer(networkConfig, selfIdentifier, connectionEvents);
        this.communication = new Communication(connectionEvents, connectionInitializer);
    }

    /**
     * 启动网络服务
     */
    public CompletableFuture<Integer> start() {
        if (started.compareAndSet(false, true)) {
            logger.info("Starting Network.");
            return communication.start();
        } else {
            logger.error("Attempted to start already running network.");
            return CompletableFuture.failedFuture(new Throwable("Attempted to start already running network."));
        }
    }

    /**
     * 停止网络服务
     */
    public CompletableFuture<Void> stop() {
        if (stopped.compareAndSet(false, true)) {
            logger.info("Stopping Network.");
            CompletableFuture<Void> stop = communication.stop();
            return stop.whenComplete((result, throwable) -> {
                shutdown.countDown();
            });
        } else {
            logger.error("Attempted to stop already stopped network.");
            return CompletableFuture.failedFuture(new Throwable("Attempted to stop already stopped network."));
        }
    }

    /**
     * 连接到远程节点
     */
    public CompletableFuture<PeerConnection> connect(final RemotePeer remotePeer) {
        return communication.connect(remotePeer);
    }

    /**
     * 断开连接
     */
    public void disconnect(final String pkiId, final DisconnectReason reason) {
        PeerConnection connection = aliveConnections.get(pkiId);
        if(Objects.nonNull(connection)) {
            try {
                connection.disconnect(reason);
            } catch (Exception e) {
                logger.debug("Disconnect by reason {}", reason);
            }
        }
    }

    /**
     * 发送消息
     * @param pkiId 远程节点的PKIID
     * @param message 发送的信息
     */
    public void send(final String pkiId, final Message message) {
        try {
            PeerConnection connection = aliveConnections.get(pkiId);
            if (Objects.nonNull(connection)) {
                connection.send(message);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 订阅消息
     */
    public void subscribeMessage(final MessageCallback callback) {
        //订阅消息，全量订阅
        communication.subscribeMessage(message -> {
            synchronized (this) {
                callback.onMessage(message);
            }
        });
    }

    /**
     * 订阅连接
     */
    public void subscribeConnect(final ConnectCallback callback) {
        //订阅连接成功
        communication.subscribeConnect(newConnection -> {
            synchronized (this) {
                callback.onConnect(newConnection);
                aliveConnections.put(newConnection.pkiId(), newConnection);
                deadConnections.remove(newConnection.pkiId(), newConnection);
            }
        });
    }

    /**
     * 订阅断开
     */
    public void subscribeDisconnect(final DisconnectCallback callback) {
        //订阅断开连接
        communication.subscribeDisconnect(connection -> {
            synchronized (this) {
                callback.onDisconnect(connection);
                aliveConnections.remove(connection.pkiId(), connection);
                deadConnections.put(connection.pkiId(), connection);
            }
        });
    }

    /**
     * 订阅指定code的消息
     */
    public void subscribeMessageByCode(final int code, final MessageCallback callback) {
        communication.subscribeByCode(code, message -> {
            synchronized (this) {
                callback.onMessage(message);
            }
        });
    }

    /**
     * 获取所有存活的连接
     */
    public List<PeerConnection> aliveConnections() {
        return new ArrayList<>(aliveConnections.values());
    }

}
