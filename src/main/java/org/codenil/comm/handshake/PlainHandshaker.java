package org.codenil.comm.handshake;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.codenil.comm.message.MessageType;
import org.codenil.comm.netty.handler.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkState;

public class PlainHandshaker implements Handshaker {

    private static final Logger logger = LoggerFactory.getLogger(AbstractHandshakeHandler.class);

    private final AtomicReference<HandshakeStatus> status =
            new AtomicReference<>(HandshakeStatus.UNINITIALIZED);

    private boolean initiator;
    private byte[] initiatorMsg;
    private byte[] responderMsg;

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
    public ByteBuf firstMessage() {
        checkState(initiator, "illegal invocation of firstMessage on non-initiator end of handshake");
        checkState(status.compareAndSet(HandshakeStatus.PREPARED, HandshakeStatus.IN_PROGRESS),
                "illegal invocation of firstMessage, handshake had already started");
        initiatorMsg = MessageHandler.buildMessage(MessageType.PING, MessageType.PING.getValue(), new byte[0]);
        logger.trace("First plain handshake message under INITIATOR role");
        return Unpooled.wrappedBuffer(initiatorMsg);
    }

    @Override
    public Optional<ByteBuf> handleMessage(ByteBuf buf) {
        checkState(status.get() == HandshakeStatus.IN_PROGRESS,
                "illegal invocation of onMessage on handshake that is not in progress");

        PlainMessage message = MessageHandler.parseMessage(buf);

        Optional<byte[]> nextMsg = Optional.empty();
        if (initiator) {
            checkState(responderMsg == null,
                    "unexpected message: responder message had " + "already been received");

            checkState(message.messageType().equals(MessageType.PONG),
                    "unexpected message: needs to be a pong");
            responderMsg = message.data();

        } else {
            checkState(initiatorMsg == null,
                    "unexpected message: initiator message " + "had already been received");
            checkState(message.messageType().equals(MessageType.PING),
                    "unexpected message: needs to be a ping");

            initiatorMsg = message.data();
            responderMsg = MessageHandler.buildMessage(MessageType.PONG, MessageType.PONG.getValue(), new byte[0]);
            nextMsg = Optional.of(responderMsg);
        }
        status.set(HandshakeStatus.SUCCESS);
        logger.trace("Handshake status set to {}", status.get());
        return nextMsg.map(Unpooled::wrappedBuffer);
    }

    @Override
    public HandshakeSecrets secrets() {
        return null;
    }
}
