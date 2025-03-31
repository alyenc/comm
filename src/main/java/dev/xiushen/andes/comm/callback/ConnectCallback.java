package dev.xiushen.andes.comm.callback;

import dev.xiushen.andes.comm.connections.PeerConnection;

/**
 * 连接回调
 */
@FunctionalInterface
public interface ConnectCallback {
    void onConnect(final PeerConnection peer);
}
