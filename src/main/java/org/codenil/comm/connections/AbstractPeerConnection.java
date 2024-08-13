package org.codenil.comm.connections;

import io.netty.channel.ChannelHandler;
import org.codenil.comm.RemotePeer;
import org.codenil.comm.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractPeerConnection implements PeerConnection {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPeerConnection.class);

    private final String remoteIdentifier;

    protected final AtomicBoolean disconnected = new AtomicBoolean(false);

    protected final AtomicBoolean terminatedImmediately = new AtomicBoolean(false);

    private RemotePeer remotePeer;

    protected AbstractPeerConnection(
            final String remoteIdentifier) {
        this.remoteIdentifier = remoteIdentifier;
    }

    @Override
    public String pkiId() {
        if(Objects.isNull(remotePeer)) {
            throw new IllegalStateException("connection not complated yet");
        }
        return remotePeer.pkiId();
    }

    @Override
    public void send(final Message message) {
        doSendMessage(message);
    }

    @Override
    public String remoteIdentifier() {
        return remoteIdentifier;
    }

    @Override
    public boolean disconnected() {
        return disconnected.get() || terminatedImmediately.get();
    }

    @Override
    public RemotePeer remotePeer() {
        return remotePeer;
    }

    @Override
    public void setRemotePeer(RemotePeer remotePeer) {
        this.remotePeer = remotePeer;
    }

    @Override
    public void replaceHandler(String name, ChannelHandler newHandler) {
        doReplaceHandler(name, newHandler);
    }

    protected abstract void doSendMessage(final Message message);

    protected abstract void doReplaceHandler(String name, ChannelHandler newHandler);

    protected abstract void closeConnection();

    protected abstract void closeConnectionImmediately();
}
