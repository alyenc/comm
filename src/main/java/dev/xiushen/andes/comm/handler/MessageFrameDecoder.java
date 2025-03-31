package dev.xiushen.andes.comm.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.xiushen.andes.comm.connections.KeepAlive;
import dev.xiushen.andes.comm.connections.PeerConnection;
import dev.xiushen.andes.comm.connections.PeerConnectionEvents;
import dev.xiushen.andes.comm.message.DisconnectMessage;
import dev.xiushen.andes.comm.message.DisconnectReason;
import dev.xiushen.andes.comm.message.HelloMessage;
import dev.xiushen.andes.comm.message.MessageCodes;
import dev.xiushen.andes.comm.netty.NettyPeerConnection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageFrameDecoder extends MessageToMessageDecoder<String> {

    private static final Logger logger = LoggerFactory.getLogger(MessageFrameDecoder.class);
    private static final int LENGTH_FIELD_LENGTH = 4;        // 长度字段占4字节

    private final CompletableFuture<PeerConnection> connectionFuture;
    private final PeerConnectionEvents connectionEvents;

    private boolean hellosExchanged;

    public MessageFrameDecoder(
            final PeerConnectionEvents connectionEvents,
            final CompletableFuture<PeerConnection> connectionFuture) {
        this.connectionEvents = connectionEvents;
        this.connectionFuture = connectionFuture;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, String message, List<Object> out) {
        // 创建消息对象
        JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);
        int code = jsonObject.getAsJsonPrimitive("code").getAsInt();
        if (hellosExchanged) {
            out.add(message);
        } else if (code == MessageCodes.HELLO) {
            hellosExchanged = true;

            HelloMessage helloMessage = new Gson().fromJson(message, HelloMessage.class);
            String remoteIdentifier = helloMessage.getData();
            final PeerConnection connection = new NettyPeerConnection(ctx, remoteIdentifier, connectionEvents);

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
                    .addLast("IdleState", new IdleStateHandler(15, 0, 0))
                    .addLast("KeepAlive", new KeepAlive(connection, waitingForPong))
                    .addLast("Common", new CommonHandler(connection, connectionEvents, waitingForPong))
                    .addLast("FrameEncoder", new MessageFrameEncoder());
            connectionFuture.complete(connection);
        } else if (code == MessageCodes.DISCONNECT) {
            logger.debug("Disconnected before sending HELLO.");
            ctx.close();
            connectionFuture.completeExceptionally(new RuntimeException("Disconnect"));
        } else {
            if(code != MessageCodes.PONG) {
                logger.debug(
                        "Message received before HELLO's exchanged, disconnecting.  Code: {}",
                        code);

                DisconnectMessage disconnectMessage = new DisconnectMessage(DisconnectReason.UNKNOWN.message());
                ctx.writeAndFlush(disconnectMessage).addListener(Void -> ctx.close());
                connectionFuture.completeExceptionally(new RuntimeException("Message received before HELLO's exchanged"));
            }
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
            if (connectionFuture.isDone() && !connectionFuture.isCompletedExceptionally()) {
                connectionFuture.get().disconnect(DisconnectReason.INVALID_MESSAGE_RECEIVED);
                return;
            }
        } else if (cause instanceof IOException) {
            // IO failures are routine when communicating with random peers across the network.
            logger.debug("IO error while processing incoming message", throwable);
        } else {
            logger.error("Exception while processing incoming message", throwable);
        }
        if (connectionFuture.isDone() && !connectionFuture.isCompletedExceptionally()) {
            connectionFuture.get().terminateConnection();
        } else {
            connectionFuture.completeExceptionally(throwable);
            ctx.close();
        }
    }
}
