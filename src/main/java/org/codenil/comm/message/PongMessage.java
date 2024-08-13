package org.codenil.comm.message;

public class PongMessage extends EmptyMessage {
    private static final PongMessage INSTANCE = new PongMessage();

    public static PongMessage get() {
        return INSTANCE;
    }

    private PongMessage() {}

    @Override
    public String requestId() {
        return "";
    }

    @Override
    public int code() {
        return MessageCodes.PONG;
    }
}
