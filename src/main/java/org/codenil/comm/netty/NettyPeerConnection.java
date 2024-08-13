package org.codenil.comm.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import org.codenil.comm.connections.AbstractPeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.codenil.comm.message.DisconnectMessage;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.Message;
import org.codenil.comm.message.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;

public class NettyPeerConnection extends AbstractPeerConnection {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPeerConnection.class);

    private final ChannelHandlerContext ctx;

    private final PeerConnectionEvents connectionEvents;

    public NettyPeerConnection(
            final ChannelHandlerContext ctx,
            final String remoteIdentifier,
            final PeerConnectionEvents connectionEvents) {
        super(remoteIdentifier);
        this.ctx = ctx;
        this.connectionEvents = connectionEvents;
    }

    @Override
    public void terminateConnection() {
        if (terminatedImmediately.compareAndSet(false, true)) {
            if (disconnected.compareAndSet(false, true)) {
                connectionEvents.dispatchDisconnect(this);
            }

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

    @Override
    protected void doSendMessage(final Message message) {
        RawMessage rawMessage = RawMessage.create(message.code());
        rawMessage.setData(message.data());
        ctx.channel().writeAndFlush(rawMessage);
    }

    @Override
    protected void doReplaceHandler(String name, ChannelHandler newHandler) {
        ctx.channel().pipeline()
                .replace(name, name, newHandler);
    }

    @Override
    protected void closeConnectionImmediately() {
        ctx.close();
    }

    @Override
    protected void closeConnection() {
        ctx.channel().eventLoop().schedule((Callable<ChannelFuture>) ctx::close, 2L, SECONDS);
    }
}
