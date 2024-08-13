package org.codenil.comm;

import java.net.InetSocketAddress;

public class RemotePeer {

    private final String peerIdentity;

    private final String ip;

    private final int listeningPort;

    public RemotePeer(
            final String ip,
            final int listeningPort) {
        this.ip = ip;
        this.listeningPort = listeningPort;
        this.peerIdentity = new InetSocketAddress(ip, listeningPort).toString();
    }

    public String peerIdentity() {
        return peerIdentity;
    }

    public String ip() {
        return ip;
    }

    public int listeningPort() {
        return listeningPort;
    }
}
