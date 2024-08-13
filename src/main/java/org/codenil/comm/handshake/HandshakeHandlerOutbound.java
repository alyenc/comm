package org.codenil.comm.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 出站握手信息处理器
 */
public class HandshakeHandlerOutbound extends AbstractHandshakeHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHandshakeHandler.class);

    private final ByteBuf first;

    public HandshakeHandlerOutbound(
            final String selfIdentifier,
            final CompletableFuture<PeerConnection> connectionFuture,
            final PeerConnectionEvents connectionEvent,
            final Handshaker handshaker) {
        super(selfIdentifier, connectionFuture, connectionEvent, handshaker);

        handshaker.prepareInitiator();
        this.first = handshaker.firstMessage();
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

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.writeAndFlush(first)
                .addListener(f -> {
                    if (f.isSuccess()) {
                      logger.trace("Wrote initial crypto handshake message to {}.", ctx.channel().remoteAddress());
                    }
                });
    }
}
