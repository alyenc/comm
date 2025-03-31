package dev.xiushen.andes.comm.connections;

import dev.xiushen.andes.comm.RemotePeer;
import dev.xiushen.andes.comm.message.DisconnectReason;
import dev.xiushen.andes.comm.message.Message;
import io.netty.channel.ChannelHandler;

public interface PeerConnection {

    String remoteIdentifier();

    boolean disconnected();

    RemotePeer remotePeer();

    void setRemotePeer(RemotePeer remotePeer);

    void send(final Message message) throws Exception;

    void replaceHandler(String name, ChannelHandler newHandler);

    void disconnect(DisconnectReason reason) throws Exception;

    void terminateConnection();
}
