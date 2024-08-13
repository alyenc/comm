package org.codenil.comm.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.codenil.comm.message.MessageCodes;
import org.codenil.comm.message.PongMessage;
import org.codenil.comm.message.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommonHandler extends SimpleChannelInboundHandler<RawMessage> {

    private static final Logger logger = LoggerFactory.getLogger(CommonHandler.class);

    private final AtomicBoolean waitingForPong;
    private final PeerConnection connection;
    private final PeerConnectionEvents connectionEvents;

    public CommonHandler(
            final PeerConnection connection,
            final PeerConnectionEvents connectionEvents,
            final AtomicBoolean waitingForPong) {
        this.connection = connection;
        this.connectionEvents = connectionEvents;
        this.waitingForPong = waitingForPong;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final RawMessage originalMessage) {
        logger.debug("Received a message from {}", originalMessage.code());
        switch (originalMessage.code()) {
            case MessageCodes.PING:
                logger.trace("Received Wire PING");
                try {
                    connection.send(PongMessage.get());
                } catch (Exception e) {
                    // Nothing to do
                }
                break;
            case MessageCodes.PONG:
                logger.trace("Received Wire PONG");
                waitingForPong.set(false);
                break;
            case MessageCodes.DISCONNECT:
                try {
                    logger.trace("Received DISCONNECT Message");
                } catch (final Exception e) {
                    logger.error("Received Wire DISCONNECT, but unable to parse reason. ");
                }
                connection.terminateConnection();
        }

        connectionEvents.dispatchMessage(connection, originalMessage);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable throwable) {
        logger.error("Error:", throwable);
        connectionEvents.dispatchDisconnect(connection);
        ctx.close();
    }
}
