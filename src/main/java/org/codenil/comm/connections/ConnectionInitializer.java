package org.codenil.comm.connections;

import org.codenil.comm.RemotePeer;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;

public interface ConnectionInitializer {

    CompletableFuture<InetSocketAddress> start();

    CompletableFuture<Void> stop();

    CompletableFuture<PeerConnection> connect(RemotePeer remotePeer);
}

