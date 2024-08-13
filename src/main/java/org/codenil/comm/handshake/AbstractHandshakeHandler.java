package org.codenil.comm.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToByteEncoder;

import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.codenil.comm.handler.MessageFrameDecoder;
import org.codenil.comm.message.HelloMessage;
import org.codenil.comm.message.MessageCodes;
import org.codenil.comm.message.RawMessage;
import org.codenil.comm.serialize.SerializeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractHandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

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
    protected void channelRead0(final ChannelHandlerContext ctx, final ByteBuf msg) {
        final Optional<ByteBuf> nextMsg = nextHandshakeMessage(msg);

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
            HelloMessage helloMessage = HelloMessage.create(selfIdentifier.getBytes(StandardCharsets.UTF_8));
            RawMessage rawMessage = RawMessage.create(helloMessage.code());
            rawMessage.setData(helloMessage.data());
            rawMessage.setRequestId(helloMessage.requestId());
            ctx.writeAndFlush(rawMessage)
                    .addListener(ff -> {
                        if (ff.isSuccess()) {
                          logger.trace("Successfully wrote hello message");
                        }
                    });

            msg.retain();
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable throwable) {
        logger.trace("Handshake error:", throwable);
        connectionFuture.completeExceptionally(throwable);
        ctx.close();
    }

    protected abstract Optional<ByteBuf> nextHandshakeMessage(ByteBuf msg);

    /** Ensures that wire hello message is the first message written. */
    private static class FirstMessageFrameEncoder extends MessageToByteEncoder<RawMessage> {

        private FirstMessageFrameEncoder() {}

        @Override
        protected void encode(
                final ChannelHandlerContext context,
                final RawMessage msg,
                final ByteBuf out) {
            if (msg.code() != MessageCodes.HELLO) {
                throw new IllegalStateException("First wire message sent wasn't a HELLO.");
            }
            byte[] idBytes = Optional.ofNullable(msg.requestId()).orElse("").getBytes(StandardCharsets.UTF_8);

            SerializeHelper builder = new SerializeHelper();
            ByteBuf buf = builder.writeBytes(idBytes)
                    .writeInt(msg.code())
                    .writeBytes(msg.data())
                    .build();

            out.writeBytes(buf);
            buf.release();
            context.pipeline().remove(this);
        }
    }
}
