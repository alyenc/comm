package org.codenil.comm.netty;

import org.codenil.comm.message.Message;

public class OutboundMessage {

    private final Message message;

    public OutboundMessage(
            final Message message) {
        this.message = message;
    }

    public Message message() {
        return message;
    }
}
