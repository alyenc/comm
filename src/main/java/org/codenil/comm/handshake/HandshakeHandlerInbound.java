package org.codenil.comm.handshake;

import io.netty.buffer.ByteBuf;
import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 入站握手信息处理器
 */
public class HandshakeHandlerInbound extends AbstractHandshakeHandler {

    public HandshakeHandlerInbound(
            final String selfIdentifier,
            final CompletableFuture<PeerConnection> connectionFuture,
            final PeerConnectionEvents connectionEvent,
            final Handshaker handshaker) {
        super(selfIdentifier, connectionFuture, connectionEvent, handshaker);
        handshaker.prepareResponder();
    }

    @Override
    protected Optional<ByteBuf> nextHandshakeMessage(ByteBuf msg) {
        final Optional<ByteBuf> nextMsg;
        if (handshaker.getStatus() == HandshakeStatus.IN_PROGRESS) {
            nextMsg = handshaker.handleMessage(msg);
        } else {
            nextMsg = Optional.empty();
        }
        return nextMsg;
    }
}
