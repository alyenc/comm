package org.codenil.comm.message;

public class PongMessage extends EmptyMessage {
    private static final PongMessage INSTANCE = new PongMessage();

    public static PongMessage get() {
        return INSTANCE;
    }

    private PongMessage() {}

    @Override
    public String getRequestId() {
        return "";
    }

    @Override
    public int getCode() {
        return MessageCodes.PONG;
    }
}
