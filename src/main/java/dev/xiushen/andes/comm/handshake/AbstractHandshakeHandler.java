package dev.xiushen.andes.comm.handshake;

import com.google.gson.Gson;
import dev.xiushen.andes.comm.connections.PeerConnection;
import dev.xiushen.andes.comm.connections.PeerConnectionEvents;
import dev.xiushen.andes.comm.handler.MessageFrameDecoder;
import dev.xiushen.andes.comm.message.HelloMessage;
import dev.xiushen.andes.comm.message.Message;
import dev.xiushen.andes.comm.message.MessageCodes;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractHandshakeHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHandshakeHandler.class);

    private final CompletableFuture<PeerConnection> connectionFuture;
    private final PeerConnectionEvents connectionEvents;

    /** 本机的标识 */
    private final String selfIdentifier;

    protected final Handshaker handshaker;

    protected AbstractHandshakeHandler(
            final String selfIdentifier,
            final CompletableFuture<PeerConnection> connectionFuture,
            final PeerConnectionEvents connectionEvents,
            final Handshaker handshaker) {
        this.selfIdentifier = selfIdentifier;
        this.connectionFuture = connectionFuture;
        this.connectionEvents = connectionEvents;
        this.handshaker = handshaker;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final String msg) {
        final Optional<String> nextMsg = nextHandshakeMessage(msg);

        if (nextMsg.isPresent()) {
            ctx.writeAndFlush(nextMsg.get());
        } else if (handshaker.getStatus() != HandshakeStatus.SUCCESS) {
            logger.debug("waiting for more bytes");
        } else {
            /*
             * 握手成功后替换掉握手消息处理器
             * 替换为消息解码器
             * 同时添加一个消息编码器
             * 形成一个完整的Message处理链
             * validate处理器只负责检测帧合法性，尝试封帧，封帧成功后移除这个处理器
             */
            ctx.channel()
                    .pipeline()
                    .replace(this, "FrameDecoder", new MessageFrameDecoder(connectionEvents, connectionFuture))
                    .addBefore("FrameDecoder", "validate", new FirstMessageFrameEncoder());

            /*
             * 替换完编解码器后发送Hello消息
             * hello消息需要带一些数据
             */
            HelloMessage helloMessage = new HelloMessage(selfIdentifier);
            ctx.writeAndFlush(helloMessage)
                    .addListener(ff -> {
                        if (ff.isSuccess()) {
                          logger.trace("Successfully wrote hello message");
                        }
                    });
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable throwable) {
        logger.trace("Handshake error:", throwable);
        connectionFuture.completeExceptionally(throwable);
        ctx.close();
    }

    protected abstract Optional<String> nextHandshakeMessage(String msg);

    /** Ensures that wire hello message is the first message written. */
    private static class FirstMessageFrameEncoder extends MessageToMessageEncoder<Message> {

        private FirstMessageFrameEncoder() {}

        @Override
        protected void encode(
                final ChannelHandlerContext context,
                final Message msg,
                final List<Object> list) {
            if (msg.getCode() != MessageCodes.HELLO) {
                throw new IllegalStateException("First wire message sent wasn't a HELLO.");
            }
            list.add(new Gson().toJson(msg));
            context.pipeline().remove(this);
        }
    }
}
