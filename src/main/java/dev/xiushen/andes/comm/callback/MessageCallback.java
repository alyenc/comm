package dev.xiushen.andes.comm.callback;

import dev.xiushen.andes.comm.message.DefaultMessage;

@FunctionalInterface
public interface MessageCallback {
    void onMessage(final DefaultMessage message);
}
