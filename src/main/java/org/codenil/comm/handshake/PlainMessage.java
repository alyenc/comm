package org.codenil.comm.handshake;

import org.codenil.comm.message.MessageType;

public class PlainMessage {
    private final MessageType messageType;
    private final int code;
    private final byte[] data;

    public PlainMessage(final MessageType messageType, final byte[] data) {
        this(messageType, -1, data);
    }

    public PlainMessage(final MessageType messageType, final int code, final byte[] data) {
        this.messageType = messageType;
        this.code = code;
        this.data = data;
    }

    public MessageType messageType() {
        return messageType;
    }

    public byte[] data() {
        return data;
    }

    public int code() {
        return code;
    }
}
