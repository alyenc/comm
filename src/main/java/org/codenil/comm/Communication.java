package org.codenil.comm;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.codenil.comm.connections.*;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.MessageCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 网络底层通信模块
 */
public class Communication {

    private static final Logger logger = LoggerFactory.getLogger(Communication.class);

    /**
     * 连接初始化
     */
    private final ConnectionInitializer connectionInitializer;
    /**
     * 消息订阅
     */
    private final PeerConnectionEvents connectionEvents;
    /**
     * 连接回调订阅
     */
    private final Subscribers<ConnectCallback> connectSubscribers = Subscribers.create();
    /**
     * 连接缓存
     */
    private final Cache<String, CompletableFuture<PeerConnection>> peersConnectingCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30L)).concurrencyLevel(1).build();

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public Communication(
            final PeerConnectionEvents connectionEvents,
            final ConnectionInitializer connectionInitializer) {
        this.connectionEvents = connectionEvents;
        this.connectionInitializer = connectionInitializer;
    }

    /**
     * 启动
     */
    public CompletableFuture<Integer> start() {
        if (!started.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Unable to start an already started " + getClass().getSimpleName()));
        }

        //注册回调监听
        setupListeners();

        //启动连接初始化
        return connectionInitializer
                .start()
                .thenApply((socketAddress) -> {
                    logger.info("P2P RLPx agent started and listening on {}.", socketAddress);
                    return socketAddress.getPort();
                })
                .whenComplete((_, err) -> {
                    if (err != null) {
                        logger.error("Failed to start Communication. Check for port conflicts.");
                    }
                });
    }

    public CompletableFuture<Void> stop() {
        if (!started.get() || !stopped.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Illegal attempt to stop " + getClass().getSimpleName()));
        }

        peersConnectingCache.asMap()
                .values()
                .forEach((conn) -> {
                    try {
                        conn.get().disconnect(DisconnectReason.UNKNOWN);
                    } catch (Exception e) {
                        logger.debug("Failed to disconnect.");
                    }
                });
        return connectionInitializer.stop();
    }

    /**
     * 连接到远程节点
     */
    public CompletableFuture<PeerConnection> connect(final RemotePeer remotePeer) {
        final CompletableFuture<PeerConnection> peerConnectionCompletableFuture;
        try {
            synchronized (this) {
                //尝试从缓存获取链接，获取不到就创建一个
                peerConnectionCompletableFuture = peersConnectingCache.get(
                        remotePeer.ip(), () -> createConnection(remotePeer));
            }
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
        return peerConnectionCompletableFuture;
    }

    /**
     * 订阅消息
     */
    public void subscribeMessage(final MessageCallback callback) {
        connectionEvents.subscribeMessage(callback);
    }

    /**
     * 订阅连接
     */
    public void subscribeConnect(final ConnectCallback callback) {
        connectSubscribers.subscribe(callback);
    }

    /**
     * 创建远程连接
     */
    @Nonnull
    private CompletableFuture<PeerConnection> createConnection(final RemotePeer remotePeer) {
        CompletableFuture<PeerConnection> completableFuture = initiateOutboundConnection(remotePeer);

        completableFuture.whenComplete((peerConnection, throwable) -> {
            if (throwable == null) {
                dispatchConnect(peerConnection);
            }
        });
        return completableFuture;
    }

    /**
     * 初始化远程连接
     */
    private CompletableFuture<PeerConnection> initiateOutboundConnection(final RemotePeer remotePeer) {
        logger.trace("Initiating connection to peer: {}:{}", remotePeer.ip(), remotePeer.listeningPort());

        return connectionInitializer
                .connect(remotePeer)
                .whenComplete((conn, err) -> {
                    if (err != null) {
                        logger.debug("Failed to connect to peer {}: {}", remotePeer.ip(), err.getMessage());
                    } else {
                        logger.debug("Outbound connection established to peer: {}", remotePeer.ip());
                    }
                });
    }

    private void setupListeners() {
        connectionInitializer.subscribeIncomingConnect(this::handleIncomingConnection);
    }

    private void handleIncomingConnection(final PeerConnection peerConnection) {
        dispatchConnect(peerConnection);
    }

    /**
     * 连接完成后调用注册的回调
     */
    private void dispatchConnect(final PeerConnection connection) {
        connectSubscribers.forEach(c -> c.onConnect(connection));
    }
}
