package org.codenil.comm.connections;

import org.codenil.comm.message.DisconnectMessage;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractPeerConnection implements PeerConnection {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPeerConnection.class);

    protected final PeerConnectionEvents connectionEvents;
    private final AtomicBoolean disconnected = new AtomicBoolean(false);
    private final AtomicBoolean terminatedImmediately = new AtomicBoolean(false);

    protected AbstractPeerConnection(final PeerConnectionEvents connectionEvents) {
        this.connectionEvents = connectionEvents;
    }

    @Override
    public void send(final Message message) {
        doSendMessage(message);
    }

    @Override
    public void terminateConnection() {
        if (terminatedImmediately.compareAndSet(false, true)) {
            if (disconnected.compareAndSet(false, true)) {
                connectionEvents.dispatchDisconnect(this);
            }
            // Always ensure the context gets closed immediately even if we previously sent a disconnect
            // message and are waiting to close.
            closeConnectionImmediately();
            logger.atTrace()
                    .setMessage("Terminating connection, reason {}")
                    .addArgument(this)
                    .log();
        }
    }

    @Override
    public void disconnect(DisconnectReason reason) {
        if (disconnected.compareAndSet(false, true)) {
            connectionEvents.dispatchDisconnect(this);
            doSendMessage(DisconnectMessage.create(reason));
            closeConnection();
        }
    }

    protected abstract void doSendMessage(final Message message);

    protected abstract void closeConnection();

    protected abstract void closeConnectionImmediately();
}
