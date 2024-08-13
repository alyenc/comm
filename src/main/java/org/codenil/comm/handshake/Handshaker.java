package org.codenil.comm.handshake;

import io.netty.buffer.ByteBuf;

import java.util.Optional;

public interface Handshaker {

    void prepareInitiator();

    void prepareResponder();

    HandshakeStatus getStatus();

    ByteBuf firstMessage();

    Optional<ByteBuf> handleMessage(ByteBuf buf);
}
