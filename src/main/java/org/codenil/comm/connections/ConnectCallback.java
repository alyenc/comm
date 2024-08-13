package org.codenil.comm.connections;

/**
 * 连接回调
 */
@FunctionalInterface
public interface ConnectCallback {
    void onConnect(final PeerConnection peer);
}
