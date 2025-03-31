package dev.xiushen.andes.comm.netty;

import dev.xiushen.andes.comm.connections.AbstractPeerConnection;
import dev.xiushen.andes.comm.connections.PeerConnectionEvents;
import dev.xiushen.andes.comm.message.DisconnectMessage;
import dev.xiushen.andes.comm.message.DisconnectReason;
import dev.xiushen.andes.comm.message.Message;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;

public class NettyPeerConnection extends AbstractPeerConnection {

    private static final Logger logger = LoggerFactory.getLogger(NettyPeerConnection.class);

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
            doSendMessage(new DisconnectMessage(reason.message()));
            closeConnection();
        }
    }

    @Override
    protected void doSendMessage(final Message message) {
        ctx.channel().writeAndFlush(message);
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
