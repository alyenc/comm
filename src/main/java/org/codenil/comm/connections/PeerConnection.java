package org.codenil.comm.connections;

import io.netty.channel.ChannelHandler;
import org.codenil.comm.RemotePeer;
import org.codenil.comm.message.DisconnectReason;
import org.codenil.comm.message.Message;

public interface PeerConnection {

    String pkiId();

    String remoteIdentifier();

    boolean disconnected();

    RemotePeer remotePeer();

    void setRemotePeer(RemotePeer remotePeer);

    void send(final Message message) throws Exception;

    void replaceHandler(String name, ChannelHandler newHandler);

    void disconnect(DisconnectReason reason) throws Exception;

    void terminateConnection();
}
