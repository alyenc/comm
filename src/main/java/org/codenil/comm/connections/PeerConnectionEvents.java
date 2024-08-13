package org.codenil.comm.connections;

import org.codenil.comm.message.*;

public class PeerConnectionEvents {

    private final Subscribers<DisconnectCallback> disconnectSubscribers = Subscribers.create(true);

    private final Subscribers<MessageCallback> messageSubscribers = Subscribers.create(true);

    public PeerConnectionEvents() {}

    public void dispatchDisconnect(
            final PeerConnection connection) {
        disconnectSubscribers.forEach(s -> s.onDisconnect(connection));
    }

    public void dispatchMessage(final PeerConnection connection, final RawMessage message) {
        final DefaultMessage msg = new DefaultMessage(connection, message);
        messageSubscribers.forEach(s -> s.onMessage(msg));
    }

    public void subscribeDisconnect(final DisconnectCallback callback) {
        disconnectSubscribers.subscribe(callback);
    }

    public void subscribeMessage(final MessageCallback callback) {
        messageSubscribers.subscribe(callback);
    }
}
