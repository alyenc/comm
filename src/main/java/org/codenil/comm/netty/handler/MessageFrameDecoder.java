package org.codenil.comm.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.timeout.IdleStateHandler;
import org.codenil.comm.connections.KeepAlive;
import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.connections.PeerConnectionEvents;
import org.codenil.comm.message.DisconnectMessage;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.MessageCodes;
import org.codenil.comm.message.RawMessage;
import org.codenil.comm.netty.NettyPeerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageFrameDecoder extends ByteToMessageDecoder {

    private static final Logger logger = LoggerFactory.getLogger(MessageFrameDecoder.class);

    private final CompletableFuture<PeerConnection> connectFuture;
    private final PeerConnectionEvents connectionEvents;

    private boolean hellosExchanged;

    public MessageFrameDecoder(
            final PeerConnectionEvents connectionEvents,
            final CompletableFuture<PeerConnection> connectFuture) {
        this.connectionEvents = connectionEvents;
        this.connectFuture = connectFuture;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        if (byteBuf.readableBytes() < 4) {
            return; // 不足4字节长度字段，等待更多数据
        }
        byteBuf.readerIndex(0);

        // 读取协议头：消息总长度
        int totalLength = byteBuf.readInt();

        if (byteBuf.readableBytes() < totalLength - 4) {
            return; // 不足消息总长度，等待更多数据
        }

        // 读取payload

        // 读取id
        int idLength = byteBuf.readInt();
        byte[] idBytes = new byte[idLength];
        byteBuf.readBytes(idBytes);
        String id = new String(idBytes, StandardCharsets.UTF_8);

        // 读取code
        int code = byteBuf.readInt();

        // 读取data
        int dataLength = byteBuf.readInt();;
        byte[] data = new byte[dataLength];
        byteBuf.readBytes(data);

        // 创建消息对象
        RawMessage message = new RawMessage(code, data);
        message.setRequestId(id);

        if (hellosExchanged) {
            out.add(message);
        } else if (message.getCode() == MessageCodes.HELLO) {
            hellosExchanged = true;
            final PeerConnection connection = new NettyPeerConnection(ctx, connectionEvents);

            /*
             * 如果收到的消息是Hello消息
             * 添加一个空闲链接检测处理器
             * 添加一个连接保活处理器，检测到连接空闲后发送一个Ping消息
             * 通用消息处理器，处理所有的协议消息
             * 添加一个消息封帧处理器
             */
            final AtomicBoolean waitingForPong = new AtomicBoolean(false);
            ctx.channel()
                    .pipeline()
                    .addLast(new IdleStateHandler(15, 0, 0),
                            new KeepAlive(connection, waitingForPong),
                            new CommonHandler(connection, connectionEvents, waitingForPong),
                            new MessageFrameEncoder());
            connectFuture.complete(connection);
        } else if (message.getCode() == MessageCodes.DISCONNECT) {
            logger.debug("Disconnected before sending HELLO.");
            ctx.close();
            connectFuture.completeExceptionally(new RuntimeException("Disconnect"));
        } else {
            logger.debug(
                    "Message received before HELLO's exchanged, disconnecting.  Code: {}, Data: {}",
                    message.getCode(), Arrays.toString(message.getData()));

            DisconnectMessage disconnectMessage = DisconnectMessage.create(DisconnectReason.UNKNOWN);
            ctx.writeAndFlush(new RawMessage(disconnectMessage.getCode(), disconnectMessage.getData()))
                    .addListener((f) -> ctx.close());
            connectFuture.completeExceptionally(new RuntimeException("Message received before HELLO's exchanged"));
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable throwable)
            throws Exception {
        final Throwable cause =
                throwable instanceof DecoderException && throwable.getCause() != null
                        ? throwable.getCause()
                        : throwable;
        if (cause instanceof IllegalArgumentException) {
            logger.debug("Invalid incoming message ", throwable);
            if (connectFuture.isDone() && !connectFuture.isCompletedExceptionally()) {
                connectFuture.get().disconnect(DisconnectReason.INVALID_MESSAGE_RECEIVED);
                return;
            }
        } else if (cause instanceof IOException) {
            // IO failures are routine when communicating with random peers across the network.
            logger.debug("IO error while processing incoming message", throwable);
        } else {
            logger.error("Exception while processing incoming message", throwable);
        }
        if (connectFuture.isDone() && !connectFuture.isCompletedExceptionally()) {
            connectFuture.get().terminateConnection();
        } else {
            connectFuture.completeExceptionally(throwable);
            ctx.close();
        }
    }
}
