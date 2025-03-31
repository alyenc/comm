package dev.xiushen.andes.comm.callback;

import dev.xiushen.andes.comm.connections.PeerConnection;

@FunctionalInterface
public interface DisconnectCallback {
    void onDisconnect(final PeerConnection connection);
}
