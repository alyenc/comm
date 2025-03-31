package dev.xiushen.andes.comm.message;

@FunctionalInterface
public interface MessageCallback {

    void onMessage(final DefaultMessage message);
}
