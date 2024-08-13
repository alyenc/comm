package org.codenil.comm.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.MessageToByteEncoder;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.codenil.comm.message.*;
import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.netty.handler.MessageFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractHandshakeHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHandshakeHandler.class);

    private final CompletableFuture<PeerConnection> connectionFuture;
    private final PeerConnectionEvents connectionEvents;
    protected final Handshaker handshaker;

    protected AbstractHandshakeHandler(
            final CompletableFuture<PeerConnection> connectionFuture,
            final PeerConnectionEvents connectionEvents,
            final Handshaker handshaker) {
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
                    .replace(this, "DeFramer", new MessageFrameDecoder(connectionEvents, connectionFuture))
                    .addBefore("DeFramer", "validate", new FirstMessageFrameEncoder());

            /*
             * 替换完编解码器后发送Hello消息
             */
            HelloMessage helloMessage = HelloMessage.create();
            RawMessage rawMessage = new RawMessage(helloMessage.getCode(), helloMessage.getData());
            rawMessage.setRequestId(helloMessage.getRequestId());
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
            if (msg.getCode() != MessageCodes.HELLO) {
                throw new IllegalStateException("First message sent wasn't a HELLO.");
            }
            byte[] idBytes = Optional.ofNullable(msg.getRequestId()).orElse("").getBytes(StandardCharsets.UTF_8);
            int channelsSize = msg.getChannels().size();

            int channelBytesLength = 0;
            for (String channel : msg.getChannels()) {
                byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
                channelBytesLength = channelBytesLength + 4 + channelBytes.length;
            }

            int payloadLength = 4 + 4 + idBytes.length + 4 + channelBytesLength + 4 + msg.getData().length;

            // 写入协议头：消息总长度
            out.writeInt(payloadLength + 4);

            // 写入payload
            // 写入code
            out.writeInt(msg.getCode());

            // 写入id
            out.writeInt(idBytes.length);
            out.writeBytes(idBytes);

            // 写入channels
            out.writeInt(channelsSize);
            for (String channel : msg.getChannels()) {
                byte[] channelBytes = channel.getBytes(StandardCharsets.UTF_8);
                out.writeInt(channelBytes.length);
                out.writeBytes(channelBytes);
            }

            // 写入data
            out.writeInt(msg.getData().length);
            out.writeBytes(msg.getData());
            context.pipeline().remove(this);
        }
    }
}
