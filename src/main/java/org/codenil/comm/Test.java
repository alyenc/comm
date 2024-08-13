package org.codenil.comm;

import org.codenil.comm.connections.PeerConnection;
import org.codenil.comm.message.MessageCodes;
import org.codenil.comm.message.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Test {

    private static final Logger logger = LoggerFactory.getLogger(Communication.class);

    public static void main(String[] args) throws Exception {

        //server();

        client();
    }

    private static void server() throws Exception {
        NetworkConfig config = new NetworkConfig();
        config.setBindHost("192.168.8.30");
        config.setBindPort(8080);

        NetworkService service = new NetworkService(config, "");
        service.start();
        CompletableFuture<Integer> start = service.start();

        start.whenComplete((res, err) -> {
            service.subscribeMessageByCode(MessageCodes.GOSSIP, message -> {
                logger.info("接收到消息：" + message.message().code());
            });
        });
    }

    private static void client() throws Exception {
        NetworkConfig config = new NetworkConfig();
        config.setBindHost("192.168.8.30");
        config.setBindPort(8090);

        NetworkService service = new NetworkService(config, "");
        CompletableFuture<Integer> start = service.start();

        start.whenComplete((res, err) -> {
            RemotePeer remotePeer = new RemotePeer("192.168.8.30:8080");
            CompletableFuture<PeerConnection> conn = service.connect(remotePeer);

            conn.whenComplete((cres, cerr) -> {
                if (cerr == null) {
                    try {
                        RawMessage rawMessage = RawMessage.create(MessageCodes.GOSSIP);
                        rawMessage.setData("test".getBytes(StandardCharsets.UTF_8));
                        service.send(remotePeer.pkiId(), rawMessage);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });
    }
}
