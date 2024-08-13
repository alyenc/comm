package org.codenil.comm.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.codenil.comm.RemotePeer;
import org.codenil.comm.connections.*;
import org.codenil.comm.NetworkConfig;
import org.codenil.comm.handshake.HandshakeHandlerInbound;
import org.codenil.comm.handshake.HandshakeHandlerOutbound;
import org.codenil.comm.handshake.PlainHandshaker;
import org.codenil.comm.netty.handler.TimeoutHandler;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * netty初始化
 */
public class NettyConnectionInitializer implements ConnectionInitializer {

    private static final int TIMEOUT_SECONDS = 10;

    private final Subscribers<ConnectCallback> connectSubscribers = Subscribers.create();
    private final PeerConnectionEvents eventDispatcher;

    private final EventLoopGroup boss = new NioEventLoopGroup(1);
    private final EventLoopGroup workers = new NioEventLoopGroup(10);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    private final NetworkConfig config;

    private ChannelFuture server;

    public NettyConnectionInitializer(
            final NetworkConfig config,
            final PeerConnectionEvents eventDispatcher) {
        this.config = config;
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
        server.addListener(future -> {
            final InetSocketAddress socketAddress =
                    (InetSocketAddress) server.channel().localAddress();
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

    @Override
    public void subscribeIncomingConnect(ConnectCallback callback) {
        connectSubscribers.subscribe(callback);
    }

    /**
     * 连接到远程
     */
    @Override
    public CompletableFuture<PeerConnection> connect(RemotePeer remotePeer) {
        final CompletableFuture<PeerConnection> connectionFuture = new CompletableFuture<>();

        new Bootstrap()
                .group(workers)
                .channel(NioSocketChannel.class)
                .remoteAddress(new InetSocketAddress(remotePeer.ip(), remotePeer.listeningPort()))
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

    private ChannelInitializer<SocketChannel> inboundChannelInitializer() {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(final SocketChannel ch) throws Exception {
                final CompletableFuture<PeerConnection> connectionFuture = new CompletableFuture<>();
                connectionFuture.thenAccept(connection -> connectSubscribers.forEach(c -> c.onConnect(connection)));
                //连接处理器
                ch.pipeline().addLast(timeoutHandler(connectionFuture, "Timed out waiting to fully establish incoming connection"));
                //其他处理器，TLS之类的
                addAdditionalInboundHandlers(ch);
                //握手消息处理器
                ch.pipeline().addLast(inboundHandler(connectionFuture));
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
                ch.pipeline().addLast(timeoutHandler(connectionFuture, "Timed out waiting to establish connection with peer: " + remotePeer.ip()));
                //其他处理器
                addAdditionalOutboundHandlers(ch, remotePeer);
                //握手消息处理器
                ch.pipeline().addLast(outboundHandler(remotePeer, connectionFuture));
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
            final CompletableFuture<PeerConnection> connectionFuture) {
        return new HandshakeHandlerInbound(connectionFuture, eventDispatcher, new PlainHandshaker());
    }

    @Nonnull
    private HandshakeHandlerOutbound outboundHandler(
            final RemotePeer remotePeer, final CompletableFuture<PeerConnection> connectionFuture) {
        return new HandshakeHandlerOutbound(connectionFuture, eventDispatcher, new PlainHandshaker());
    }

    void addAdditionalOutboundHandlers(final Channel channel, final RemotePeer remotePeer)
            throws GeneralSecurityException, IOException {}

    void addAdditionalInboundHandlers(final Channel channel)
            throws GeneralSecurityException, IOException {}
}
