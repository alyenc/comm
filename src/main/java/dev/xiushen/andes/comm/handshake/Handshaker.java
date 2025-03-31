package dev.xiushen.andes.comm.handshake;

import java.util.Optional;

public interface Handshaker {

    void prepareInitiator();

    void prepareResponder();

    HandshakeStatus getStatus();

    String firstMessage();

    Optional<String> handleMessage(String buf);
}
