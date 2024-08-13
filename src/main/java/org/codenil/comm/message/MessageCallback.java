package org.codenil.comm.message;

@FunctionalInterface
public interface MessageCallback {

    void onMessage(final DefaultMessage message);
}
