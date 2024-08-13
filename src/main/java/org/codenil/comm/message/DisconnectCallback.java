package org.codenil.comm.message;

import org.codenil.comm.connections.PeerConnection;

@FunctionalInterface
public interface DisconnectCallback {
    void onDisconnect(final PeerConnection connection);
}
