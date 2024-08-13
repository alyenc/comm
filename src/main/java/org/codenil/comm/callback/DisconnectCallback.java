package org.codenil.comm.callback;

import org.codenil.comm.connections.PeerConnection;

@FunctionalInterface
public interface DisconnectCallback {
    void onDisconnect(final PeerConnection connection);
}
