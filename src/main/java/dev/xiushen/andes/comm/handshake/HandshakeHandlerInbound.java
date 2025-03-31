package dev.xiushen.andes.comm.handshake;

import dev.xiushen.andes.comm.connections.PeerConnection;
import dev.xiushen.andes.comm.connections.PeerConnectionEvents;

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
    protected Optional<String> nextHandshakeMessage(String msg) {
        final Optional<String> nextMsg;
        if (handshaker.getStatus() == HandshakeStatus.IN_PROGRESS) {
            nextMsg = handshaker.handleMessage(msg);
        } else {
            nextMsg = Optional.empty();
        }
        return nextMsg;
    }
}
