package org.codenil.comm.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class TimeoutHandler<C extends Channel> extends ChannelInitializer<C> {
    private final Supplier<Boolean> condition;
    private final int timeoutInSeconds;
    private final OnTimeoutCallback callback;

    public TimeoutHandler(
            final Supplier<Boolean> condition,
            final int timeoutInSeconds,
            final OnTimeoutCallback callback) {
        this.condition = condition;
        this.timeoutInSeconds = timeoutInSeconds;
        this.callback = callback;
    }

    @Override
    protected void initChannel(final C ch) throws Exception {
        ch.eventLoop().schedule(() -> {
                if (!condition.get()) {
                    callback.invoke();
                    ch.close();
                }
            }, timeoutInSeconds, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    public interface OnTimeoutCallback {
        void invoke();
    }
}

