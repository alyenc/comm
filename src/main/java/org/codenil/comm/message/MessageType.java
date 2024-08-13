package org.codenil.comm.message;

public enum MessageType {
    PING(0),
    PONG(1),
    DATA(2),
    UNRECOGNIZED(-1),
    ;

    private final int value;

    MessageType(final int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static MessageType forNumber(final int value) {
        return switch (value) {
            case 0 -> PING;
            case 1 -> PONG;
            case 2 -> DATA;
            case -1 -> UNRECOGNIZED;
            default -> null;
        };
    }
}
