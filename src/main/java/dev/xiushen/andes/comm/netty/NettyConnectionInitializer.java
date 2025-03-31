package dev.xiushen.andes.comm.netty;

import dev.xiushen.andes.comm.NetworkConfig;
import dev.xiushen.andes.comm.RemotePeer;
import dev.xiushen.andes.comm.callback.ConnectCallback;
import dev.xiushen.andes.comm.connections.ConnectionInitializer;
import dev.xiushen.andes.comm.connections.PeerConnection;
import dev.xiushen.andes.comm.connections.PeerConnectionEvents;
import dev.xiushen.andes.comm.connections.Subscribers;
import dev.xiushen.andes.comm.handler.TimeoutHandler;
import dev.xiushen.andes.comm.handshake.HandshakeHandlerInbound;
import dev.xiushen.andes.comm.handshake.HandshakeHandlerOutbound;
import dev.xiushen.andes.comm.handshake.PlainHandshaker;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * netty初始化
 */
public class NettyConnectionInitializer implements ConnectionInitializer {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_FRAME_LENGTH = 1024 * 1024; // 最大消息长度
    private static final int LENGTH_FIELD_OFFSET = 0;        // 长度字段起始位置
    private static final int LENGTH_FIELD_LENGTH = 4;        // 长度字段占4字节
    private static final int LENGTH_ADJUSTMENT = 0;          // 长度字段后数据的偏移量
    private static final int INITIAL_BYTES_TO_STRIP = 4;     // 跳过长度字段

    private final Subscribers<ConnectCallback> connectSubscribers = Subscribers.create();
    private final PeerConnectionEvents eventDispatcher;

    private final EventLoopGroup boss = new NioEventLoopGroup(1);
    private final EventLoopGroup workers = new NioEventLoopGroup(10);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final NetworkConfig config;
    private final String selfIdentifier;

    private ChannelFuture server;


    public NettyConnectionInitializer(
            final NetworkConfig config,
            final String selfIdentifier,
            final PeerConnectionEvents eventDispatcher) {
        this.config = config;
        this.selfIdentifier = selfIdentifier;
        this.eventDispatcher = eventDispatcher;
    }

    /**
     * 启动netty服务器
     */
    @Override
    public CompletableFuture<InetSocketAddress> start() {
        final CompletableFuture<InetSocketAddress> listeningPortFuture = new CompletableFuture<>();
        if (!started.compareAndSet(false, true)) {
            listeningPortFuture.completeExceptionally(
                    new IllegalStateException(
                            "Attempt to start an already started " + this.getClass().getSimpleName()));
            return listeningPortFuture;
        }

        this.server = new ServerBootstrap()
                .group(boss, workers)
                .channel(NioServerSocketChannel.class)
                .childHandler(inboundChannelInitializer())
                .bind(config.bindHost(), config.bindPort());
        this.server.addListener(future -> {
            final InetSocketAddress socketAddress = (InetSocketAddress) server.channel().localAddress();
            if (!future.isSuccess() || socketAddress == null) {
                final String message =
                        String.format("Unable to start listening on %s:%s. Check for port conflicts.",
                                config.bindHost(), config.bindPort());
                listeningPortFuture.completeExceptionally(
                        new IllegalStateException(message, future.cause()));
                return;
            }

            listeningPortFuture.complete(socketAddress);
        });

        return listeningPortFuture;
    }

    /**
     * 停止netty服务器
     */
    @Override
    public CompletableFuture<Void> stop() {
        final CompletableFuture<Void> stoppedFuture = new CompletableFuture<>();
        if (!started.get() || !stopped.compareAndSet(false, true)) {
            stoppedFuture.completeExceptionally(
                    new IllegalStateException("Illegal attempt to stop " + this.getClass().getSimpleName()));
            return stoppedFuture;
        }

        workers.shutdownGracefully();
        boss.shutdownGracefully();
        server.channel()
                .closeFuture()
                .addListener((future) -> {
                    if (future.isSuccess()) {
                        stoppedFuture.complete(null);
                    } else {
                        stoppedFuture.completeExceptionally(future.cause());
                    }
                });
        return stoppedFuture;
    }

    /**
     * 连接到远程
     */
    @Override
    public CompletableFuture<PeerConnection> connect(RemotePeer remotePeer) {
        final CompletableFuture<PeerConnection> connectionFuture = new CompletableFuture<>();

        String[] parts = remotePeer.endpoint().split(":");

        new Bootstrap()
                .group(workers)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])))
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, TIMEOUT_SECONDS * 1000)
                .handler(outboundChannelInitializer(remotePeer, connectionFuture))
                .connect()
                .addListener(
                        (f) -> {
                            if (!f.isSuccess()) {
                                connectionFuture.completeExceptionally(f.cause());
                            }
                        });

        return connectionFuture;
    }

    @Nonnull
    private ChannelInitializer<SocketChannel> inboundChannelInitializer() {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                final CompletableFuture<PeerConnection> connectionFuture = new CompletableFuture<>();
                connectionFuture.thenAccept(connection -> connectSubscribers.forEach(c -> c.onConnect(connection)));
                //连接超时处理器
                ch.pipeline().addLast(timeoutHandler(connectionFuture, "Timed out waiting to fully establish incoming connection"));

                ch.pipeline().addLast(new LengthFieldPrepender(4)); // 编码器：添加4字节长度头
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                        MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH,
                        LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                // JSON 编解码（基于String）
                ch.pipeline().addLast(new StringDecoder(StandardCharsets.UTF_8)); // 解码ByteBuf -> String
                ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8)); // 编码String -> ByteBuf

                addAdditionalInboundHandlers(ch);
                //握手消息处理器
                ch.pipeline().addLast(inboundHandler(selfIdentifier, connectionFuture));
            }
        };
    }

    @Nonnull
    private ChannelInitializer<SocketChannel> outboundChannelInitializer(
            final RemotePeer remotePeer, final CompletableFuture<PeerConnection> connectionFuture) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                //连接处理器
                ch.pipeline().addLast(timeoutHandler(connectionFuture, "Timed out waiting to establish connection with peer: " + remotePeer.toString()));

                ch.pipeline().addLast(new LengthFieldPrepender(4)); // 编码器：添加4字节长度头
                ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(
                        MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH,
                        LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                // JSON 编解码（基于String）
                ch.pipeline().addLast(new StringDecoder(StandardCharsets.UTF_8)); // 解码ByteBuf -> String
                ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8)); // 编码String -> ByteBuf

                //其他处理器
                addAdditionalOutboundHandlers(ch, remotePeer);

                //握手消息处理器
                ch.pipeline().addLast(outboundHandler(selfIdentifier, remotePeer, connectionFuture));
            }
        };
    }

    @Nonnull
    private TimeoutHandler<Channel> timeoutHandler(
            final CompletableFuture<PeerConnection> connectionFuture, final String message) {
        return new TimeoutHandler<>(connectionFuture::isDone, TIMEOUT_SECONDS,
                () -> connectionFuture.completeExceptionally(new TimeoutException(message)));
    }

    @Nonnull
    private HandshakeHandlerInbound inboundHandler(
            final String selfIdentifier,
            final CompletableFuture<PeerConnection> connectionFuture) {
        return new HandshakeHandlerInbound(selfIdentifier, connectionFuture,
                eventDispatcher, new PlainHandshaker());
    }

    @Nonnull
    private HandshakeHandlerOutbound outboundHandler(
            final String selfIdentifier,
            final RemotePeer remotePeer,
            final CompletableFuture<PeerConnection> connectionFuture) {
        return new HandshakeHandlerOutbound(selfIdentifier, connectionFuture,
                eventDispatcher, new PlainHandshaker());
    }

    void addAdditionalOutboundHandlers(final Channel channel, final RemotePeer remotePeer)
            throws GeneralSecurityException, IOException {}

    void addAdditionalInboundHandlers(final Channel channel)
            throws GeneralSecurityException, IOException {}
}
