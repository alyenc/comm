package dev.xiushen.andes.comm;

import dev.xiushen.andes.comm.connections.PeerConnection;
import dev.xiushen.andes.comm.message.MessageCodes;
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
        config.setBindHost("192.168.0.26");
        config.setBindPort(8080);

        NetworkService service = new NetworkService(config, "");
        service.start();
        CompletableFuture<Integer> start = service.start();

        start.whenComplete((res, err) -> {
            service.subscribeMessageByCode(MessageCodes.GOSSIP, message -> {
                logger.info("接收到消息：{}", message.message());
            });
        });
    }

    private static void client() throws Exception {
        NetworkConfig config = new NetworkConfig();
        config.setBindHost("192.168.31.58");
        config.setBindPort(9091);

        NetworkService service = new NetworkService(config, "");
        CompletableFuture<Integer> start = service.start();

        start.whenComplete((res, err) -> {
            RemotePeer remotePeer = new RemotePeer("192.168.31.58:9090");
            CompletableFuture<PeerConnection> conn = service.connect(remotePeer);

            conn.whenComplete((cres, cerr) -> {
                if (cerr == null) {
                    try {

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        });
    }
}
