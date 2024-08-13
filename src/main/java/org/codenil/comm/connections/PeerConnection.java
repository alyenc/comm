package org.codenil.comm.connections;

import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.Message;

public interface PeerConnection {

    String peerIdentity();

    void send(final Message message) throws Exception;

    void disconnect(DisconnectReason reason) throws Exception;

    void terminateConnection();
}
