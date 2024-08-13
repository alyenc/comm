package org.codenil.comm;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.tuweni.bytes.Bytes;
import org.codenil.comm.connections.*;
import org.codenil.comm.message.DefaultMessage;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.Message;
import org.codenil.comm.message.RawMessage;
import org.codenil.comm.netty.NettyConnectionInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class NetworkService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class);

    private final CountDownLatch shutdown = new CountDownLatch(1);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final Map<Integer, Subscribers<MessageCallback>> listenersByCode = new ConcurrentHashMap<>();
    private final Map<Integer, MessageResponse> messageResponseByCode = new ConcurrentHashMap<>();

    private final AtomicInteger outstandingRequests = new AtomicInteger(0);
    private final AtomicLong requestIdCounter = new AtomicLong(1);
    private final Clock clock = Clock.systemUTC();
    private final PeerReputation reputation = new PeerReputation();
    private final ConnectionStore connectionStore = new ConnectionStore();

    private final Communication communication;

    private volatile long lastRequestTimestamp = 0;

    public NetworkService(final NetworkConfig networkConfig) {
        PeerConnectionEvents connectionEvents = new PeerConnectionEvents();
        ConnectionInitializer connectionInitializer = new NettyConnectionInitializer(networkConfig, connectionEvents);
        this.communication = new Communication(connectionEvents, connectionInitializer);
    }

    public CompletableFuture<Integer> start() {
        if (started.compareAndSet(false, true)) {
            logger.info("Starting Network.");
            setupHandlers();
            return communication.start();
        } else {
            logger.error("Attempted to start already running network.");
            return CompletableFuture.failedFuture(new Throwable("Attempted to start already running network."));
        }
    }

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

    public CompletableFuture<PeerConnection> connect(final RemotePeer remotePeer) {
        return communication.connect(remotePeer);
    }

    public void sendRequest(final String peerIdentity, final Message message) {
        lastRequestTimestamp = clock.millis();
        this.dispatchRequest(
                msg -> {
                    try {
                        PeerConnection connection = connectionStore.getConnection(peerIdentity);
                        if(Objects.nonNull(connection)) {
                            connection.send(msg);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                message);
    }

    public long subscribe(final int messageCode, final MessageCallback callback) {
        return listenersByCode
                .computeIfAbsent(messageCode, _ -> Subscribers.create())
                .subscribe(callback);
    }

    public void unsubscribe(final long subscriptionId, final int messageCode) {
        if (listenersByCode.containsKey(messageCode)) {
            listenersByCode.get(messageCode).unsubscribe(subscriptionId);
            if (listenersByCode.get(messageCode).getSubscriberCount() < 1) {
                listenersByCode.remove(messageCode);
            }
        }
    }

    public void registerResponse(
            final int messageCode, final MessageResponse messageResponse) {
        messageResponseByCode.put(messageCode, messageResponse);
    }

    private void setupHandlers() {
        communication.subscribeConnect(this::registerNewConnection);
        communication.subscribeMessage(message -> {
            try {
                this.receiveMessage(message);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    /**
     * 注册新连接
     */
    private void registerNewConnection(final PeerConnection newConnection) {
        synchronized (this) {
            connectionStore.registerConnection(newConnection);
        }
    }

    private boolean registerDisconnect(final PeerConnection connection) {
        return connectionStore.unRegisterConnection(connection);
    }

    private void dispatchRequest(final RequestSender sender, final Message message) {
        outstandingRequests.incrementAndGet();
        sender.send(message.wrapMessage(requestIdCounter.getAndIncrement() + ""));
    }

    private void receiveMessage(final DefaultMessage message) throws Exception {

        //处理自定义回复处理器
        Optional<Message> maybeResponse = Optional.empty();
        try {
            final int code = message.message().getCode();
            Optional.ofNullable(listenersByCode.get(code))
                    .ifPresent(listeners -> listeners.forEach(messageCallback -> messageCallback.exec(message)));

            Message requestIdAndEthMessage = message.message();
            maybeResponse = Optional.ofNullable(messageResponseByCode.get(code))
                    .map(messageResponse -> messageResponse.response(message))
                    .map(responseData -> responseData.wrapMessage(requestIdAndEthMessage.getRequestId()));
        } catch (Exception e) {
            logger.atDebug()
                    .setMessage("Received malformed message {}, {}")
                    .addArgument(message)
                    .addArgument(e::toString)
                    .log();
            this.disconnect(message.connection().peerIdentity(), DisconnectReason.UNKNOWN);
        }

        maybeResponse.ifPresent(
                responseData -> {
                    try {
                        sendRequest(message.connection().peerIdentity(), responseData);
                    } catch (Exception __) {}
                });
    }

    private void disconnect(final String peerIdentity, final DisconnectReason reason) {
        PeerConnection connection = connectionStore.getConnection(peerIdentity);
        if(Objects.nonNull(connection)) {
            try {
                connection.disconnect(reason);
            } catch (Exception e) {
                logger.debug("Disconnect by reason {}", reason);
            }
        }
    }

    private void recordRequestTimeout(final PeerConnection connection, final int requestCode) {
        logger.atDebug()
                .setMessage("Timed out while waiting for response")
                .log();
        logger.trace("Timed out while waiting for response from peer {}", this);
        reputation.recordRequestTimeout(requestCode)
                .ifPresent(reason -> {
                    this.disconnect(connection.peerIdentity(), reason);
                });
    }

    private void recordUselessResponse(final PeerConnection connection, final String requestType) {
        logger.atTrace()
                .setMessage("Received useless response for request type {}")
                .addArgument(requestType)
                .log();
        reputation.recordUselessResponse(System.currentTimeMillis())
                .ifPresent(reason -> {
                    this.disconnect(connection.peerIdentity(), reason);
                });
    }

    @FunctionalInterface
    public interface RequestSender {
        void send(final RawMessage message);
    }

    @FunctionalInterface
    public interface MessageCallback {
        void exec(DefaultMessage message);
    }

    @FunctionalInterface
    public interface MessageResponse {
        Message response(DefaultMessage message);
    }
}
