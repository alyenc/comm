package org.codenil.comm.callback;

import org.codenil.comm.connections.PeerConnection;

/**
 * 连接回调
 */
@FunctionalInterface
public interface ConnectCallback {
    void onConnect(final PeerConnection peer);
}
