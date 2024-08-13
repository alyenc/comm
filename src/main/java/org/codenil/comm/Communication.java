package org.codenil.comm;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.codenil.comm.callback.ConnectCallback;
import org.codenil.comm.callback.DisconnectCallback;
import org.codenil.comm.callback.MessageCallback;
import org.codenil.comm.connections.ConnectionInitializer;
import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 网络底层通信模块
 */
public class Communication {

    private static final Logger logger = LoggerFactory.getLogger(Communication.class);

    /** 连接初始化 */
    private final ConnectionInitializer connectionInitializer;

    /** 消息订阅 */
    private final PeerConnectionEvents connectionEvents;

    /** 连接缓存 */
    private final Cache<String, CompletableFuture<PeerConnection>> connectingCache = CacheBuilder.newBuilder()
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
        connectionEvents.subscribeConnect(this::dispatchConnect);

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

        connectingCache.asMap()
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
        final CompletableFuture<PeerConnection> completableFuture;
        try {
            synchronized (this) {
                //尝试从缓存获取链接，获取不到就创建一个
                completableFuture = connectingCache.get(remotePeer.endpoint(),
                        () -> createConnection(remotePeer));
            }
        } catch (final ExecutionException e) {
            throw new RuntimeException(e);
        }
        return completableFuture;
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
        connectionEvents.subscribeConnect(callback);
    }

    /**
     * 订阅断开
     */
    public void subscribeDisconnect(final DisconnectCallback callback) {
        connectionEvents.subscribeDisconnect(callback);
    }

    /**
     * 订阅指定code的消息
     */
    public void subscribeByCode(final int code, final MessageCallback callback) {
        connectionEvents.subscribeByCode(code, callback);
    }

    /**
     * 创建远程连接
     */
    @Nonnull
    private CompletableFuture<PeerConnection> createConnection(final RemotePeer remotePeer) {
        CompletableFuture<PeerConnection> completableFuture = connectionInitializer
                .connect(remotePeer)
                .whenComplete((conn, err) -> {
                    if (err != null) {
                        logger.debug("Failed to connect to peer {}", remotePeer.toString());
                    } else {
                        logger.debug("Outbound connection established to peer: {}", remotePeer.toString());
                    }
                });;

        completableFuture.whenComplete((peerConnection, throwable) -> {
            if (throwable == null) {
                remotePeer.setPkiId(peerConnection.remoteIdentifier());
                peerConnection.setRemotePeer(remotePeer);
                dispatchConnect(peerConnection);
            }
        });
        return completableFuture;
    }

    /**
     * 连接完成后调用注册的回调
     */
    private void dispatchConnect(final PeerConnection connection) {
        connectionEvents.dispatchConnect(connection);
    }

    /**
     * 连接断开后调用注册的回调
     */
    private void dispatchDisconnect(final PeerConnection connection) {
        connectionEvents.dispatchDisconnect(connection);
    }

    /**
     * 收到消息后调用注册的回调
     */
    private void dispatchMessage(final PeerConnection connection, final RawMessage message) {
        connectionEvents.dispatchMessage(connection, message);
    }

    /**
     * 收到指定code消息后调用指定回调
     */
    private void dispatchMessageByCode(final int code, final PeerConnection connection, final RawMessage message) {
        connectionEvents.dispatchMessageByCode(code, connection, message);
    }
}
