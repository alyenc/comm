package dev.xiushen.andes.comm.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.xiushen.andes.comm.connections.PeerConnection;
import dev.xiushen.andes.comm.connections.PeerConnectionEvents;
import dev.xiushen.andes.comm.message.MessageCodes;
import dev.xiushen.andes.comm.message.PongMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommonHandler extends SimpleChannelInboundHandler<String> {

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
    protected void channelRead0(final ChannelHandlerContext ctx, final String message) {
        JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);
        int code = jsonObject.getAsJsonPrimitive("code").getAsInt();
        logger.debug("Received a message from {}", code);
        switch (code) {
            case MessageCodes.PING:
                logger.trace("Received Wire PING");
                try {
                    connection.send(new PongMessage());
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

        connectionEvents.dispatchMessage(connection, message);
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable throwable) {
        logger.error("Error:", throwable);
        connectionEvents.dispatchDisconnect(connection);
        ctx.close();
    }
}
