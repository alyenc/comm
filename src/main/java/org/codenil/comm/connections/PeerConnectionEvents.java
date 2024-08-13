package org.codenil.comm.connections;

import org.codenil.comm.callback.ConnectCallback;
import org.codenil.comm.callback.DisconnectCallback;
import org.codenil.comm.callback.MessageCallback;
import org.codenil.comm.message.DefaultMessage;
import org.codenil.comm.message.RawMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerConnectionEvents {

    /** 连接回调订阅 */
    private final Subscribers<ConnectCallback> connectSubscribers = Subscribers.create(true);

    private final Subscribers<DisconnectCallback> disconnectSubscribers = Subscribers.create(true);

    private final Subscribers<MessageCallback> messageSubscribers = Subscribers.create(true);

    private final Map<Integer, Subscribers<MessageCallback>> subscribersByCode = new ConcurrentHashMap<>();

    public PeerConnectionEvents() {}

    public void subscribeConnect(final ConnectCallback callback) {
        connectSubscribers.subscribe(callback);
    }

    public void subscribeDisconnect(final DisconnectCallback callback) {
        disconnectSubscribers.subscribe(callback);
    }

    public void subscribeMessage(final MessageCallback callback) {
        messageSubscribers.subscribe(callback);
    }

    public void subscribeByCode(final int messageCode, final MessageCallback callback) {
        subscribersByCode
                .computeIfAbsent(messageCode, _ -> Subscribers.create())
                .subscribe(callback);
    }

    public void dispatchConnect(
            final PeerConnection connection) {
        connectSubscribers.forEach(s -> s.onConnect(connection));
    }

    public void dispatchDisconnect(
            final PeerConnection connection) {
        disconnectSubscribers.forEach(s -> s.onDisconnect(connection));
    }

    public void dispatchMessage(final PeerConnection connection, final RawMessage message) {
        final DefaultMessage msg = new DefaultMessage(connection, message);
        messageSubscribers.forEach(s -> s.onMessage(msg));
    }

    public void dispatchMessageByCode(final int code, final PeerConnection connection, final RawMessage message) {
        final DefaultMessage msg = new DefaultMessage(connection, message);
        subscribersByCode.get(code).forEach(s -> s.onMessage(msg));
    }
}
