package org.codenil.comm.netty;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.codenil.comm.connections.AbstractPeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.codenil.comm.message.Message;
import org.codenil.comm.message.RawMessage;

import java.util.concurrent.Callable;

import static java.util.concurrent.TimeUnit.SECONDS;

public class NettyPeerConnection extends AbstractPeerConnection {

    private final ChannelHandlerContext ctx;

    public NettyPeerConnection(
            final ChannelHandlerContext ctx,
            final PeerConnectionEvents connectionEvents) {
        super(connectionEvents);
        this.ctx = ctx;
    }


    @Override
    public String peerIdentity() {
        return ctx.channel().remoteAddress().toString();
    }

    @Override
    protected void doSendMessage(final Message message) {
        ctx.channel().writeAndFlush(new RawMessage(message.getCode(), message.getData()));
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
