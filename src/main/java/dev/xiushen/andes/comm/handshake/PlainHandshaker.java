package dev.xiushen.andes.comm.handshake;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.xiushen.andes.comm.message.MessageCodes;
import dev.xiushen.andes.comm.message.PingMessage;
import dev.xiushen.andes.comm.message.PongMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;

public class PlainHandshaker implements Handshaker {

    private static final Logger logger = LoggerFactory.getLogger(PlainHandshaker.class);

    private final AtomicReference<HandshakeStatus> status =
            new AtomicReference<>(HandshakeStatus.UNINITIALIZED);

    private boolean initiator;
    private String initiatorMsg;
    private String responderMsg;

    @Override
    public void prepareInitiator() {
        checkState(status.compareAndSet(
                HandshakeStatus.UNINITIALIZED, HandshakeStatus.PREPARED),
                "handshake was already prepared");
        this.initiator = true;
    }

    @Override
    public void prepareResponder() {
        checkState(status.compareAndSet(
                HandshakeStatus.UNINITIALIZED, HandshakeStatus.IN_PROGRESS),
                "handshake was already prepared");
        this.initiator = false;
    }

    @Override
    public HandshakeStatus getStatus() {
        return status.get();
    }

    @Override
    public String firstMessage() {
        checkState(initiator, "illegal invocation of firstMessage on non-initiator end of handshake");
        checkState(status.compareAndSet(HandshakeStatus.PREPARED, HandshakeStatus.IN_PROGRESS),
                "illegal invocation of firstMessage, handshake had already started");
        logger.trace("First plain handshake message under INITIATOR role");
        return new Gson().toJson(new PingMessage());
    }

    @Override
    public Optional<String> handleMessage(String message) {
        checkState(status.get() == HandshakeStatus.IN_PROGRESS,
                "illegal invocation of onMessage on handshake that is not in progress");

        JsonObject jsonObject = new Gson().fromJson(message, JsonObject.class);
        int code = jsonObject.getAsJsonPrimitive("code").getAsInt();
        Optional<String> nextMsg = Optional.empty();
        if (initiator) {
            checkState(responderMsg == null,
                    "unexpected message: responder message had " + "already been received");

            checkState(code == MessageCodes.PONG,
                    "unexpected message: needs to be a pong");
            responderMsg = message;
        } else {
            checkState(initiatorMsg == null,
                    "unexpected message: initiator message " + "had already been received");
            checkState(code == MessageCodes.PING,
                    "unexpected message: needs to be a ping");

            initiatorMsg = message;
            responderMsg = new Gson().toJson(new PongMessage());
            nextMsg = Optional.of(responderMsg);
        }
        status.set(HandshakeStatus.SUCCESS);
        logger.trace("Handshake status set to {}", status.get());
        return nextMsg;
    }
}
