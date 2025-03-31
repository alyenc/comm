package dev.xiushen.andes.comm.handshake;

import dev.xiushen.andes.comm.connections.PeerConnection;
import dev.xiushen.andes.comm.connections.PeerConnectionEvents;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 出站握手信息处理器
 */
public class HandshakeHandlerOutbound extends AbstractHandshakeHandler {

    private static final Logger logger = LoggerFactory.getLogger(HandshakeHandlerOutbound.class);

    private final String first;

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
    protected Optional<String> nextHandshakeMessage(String msg) {
        final Optional<String> nextMsg;
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
