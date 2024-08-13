package org.codenil.comm.message;

import org.codenil.comm.connections.PeerConnection;

public class DefaultMessage {

    private final RawMessage message;

    private final PeerConnection connection;

    public DefaultMessage(
            final PeerConnection connection,
            final RawMessage message) {
        this.message = message;
        this.connection = connection;
    }

    public RawMessage message() {
        return message;
    }

    public PeerConnection connection() {
        return connection;
    }
}

