package org.codenil.comm.message;

public class MessageCodes {
    public static final int HELLO = 0x00;
    public static final int DISCONNECT = 0x01;
    public static final int PING = 0x02;
    public static final int PONG = 0x03;

    public static final int DATA_UPDATE = 0x04;
    private MessageCodes() {}

    public static String messageName(final int code) {
        return switch (code) {
            case HELLO -> "Hello";
            case DISCONNECT -> "Disconnect";
            case PING -> "Ping";
            case PONG -> "Pong";
            default -> "invalid";
        };
    }
}

