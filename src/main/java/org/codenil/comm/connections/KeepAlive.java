package org.codenil.comm.connections;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.PingMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class KeepAlive extends ChannelDuplexHandler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAlive.class);

    private final AtomicBoolean waitingForPong;

    private final PeerConnection connection;

    public KeepAlive(
            final PeerConnection connection,
            final AtomicBoolean waitingForPong) {
        this.connection = connection;
        this.waitingForPong = waitingForPong;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) {
        if (!(evt instanceof IdleStateEvent
                && ((IdleStateEvent) evt).state() == IdleState.READER_IDLE)) {
            return;
        }

        if (waitingForPong.get()) {
            logger.debug("PONG never received, disconnecting from peer.");
            try {
                connection.disconnect(DisconnectReason.TIMEOUT);
            } catch (Exception e) {
                logger.warn("Exception while disconnecting from peer.", e);
            }
            return;
        }

        try {
            logger.debug("Idle connection detected, sending Wire PING to peer.");
            connection.send(PingMessage.get());
            waitingForPong.set(true);
        } catch (Exception e) {
            logger.trace("PING not sent because peer is already disconnected");
        }
    }
}
