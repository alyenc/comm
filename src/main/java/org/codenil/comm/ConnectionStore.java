package org.codenil.comm;

import org.codenil.comm.connections.PeerConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionStore {

    private final Map<String, PeerConnection> pki2Conn = new ConcurrentHashMap<>();

    public void registerConnection(PeerConnection peerConnection) {
        pki2Conn.put(peerConnection.peerIdentity(), peerConnection);
        System.out.println(peerConnection.peerIdentity());
    }

    public boolean unRegisterConnection(PeerConnection peerConnection) {
        return pki2Conn.remove(peerConnection.peerIdentity(), peerConnection);
    }

    public PeerConnection getConnection(String peerIdentity) {
        return pki2Conn.get(peerIdentity);
    }
}

