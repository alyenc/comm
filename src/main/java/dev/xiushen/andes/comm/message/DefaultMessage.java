package dev.xiushen.andes.comm.message;

import dev.xiushen.andes.comm.connections.PeerConnection;

public class DefaultMessage {

    private final String message;

    private final PeerConnection connection;

    public DefaultMessage(
            final PeerConnection connection,
            final String message) {
        this.message = message;
        this.connection = connection;
    }

    public String message() {
        return message;
    }

    public PeerConnection connection() {
        return connection;
    }
}

