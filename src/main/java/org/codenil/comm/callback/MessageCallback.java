package org.codenil.comm.callback;

import org.codenil.comm.message.DefaultMessage;

@FunctionalInterface
public interface MessageCallback {

    void onMessage(final DefaultMessage message);
}
