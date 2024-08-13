package org.codenil.comm.message;

public class PingMessage extends EmptyMessage {
    private static final PingMessage INSTANCE = new PingMessage();

    public static PingMessage get() {
        return INSTANCE;
    }

    private PingMessage() {}

    @Override
    public String getRequestId() {
        return "";
    }

    @Override
    public int getCode() {
        return MessageCodes.PING;
    }

    @Override
    public String toString() {
        return "PingMessage{data=''}";
    }
}
