package dev.xiushen.andes.comm.message;

import dev.xiushen.andes.comm.connections.PeerConnection;

@FunctionalInterface
public interface DisconnectCallback {
    void onDisconnect(final PeerConnection connection);
}
