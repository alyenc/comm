package org.codenil.comm.message;

public enum DisconnectReason {

    UNKNOWN((byte) 0x00, ""),

    TIMEOUT((byte) 0x0b, ""),

    INVALID_MESSAGE_RECEIVED((byte) 0x02, "An exception was caught decoding message"),

    ;

    private final Byte code;
    private final String message;

    DisconnectReason(final Byte code) {
        this.code = code;
        this.message = "";
    }

    DisconnectReason(final Byte code, final String message) {
        this.code = code;
        this.message = message;
    }

    public Byte code() {
        return code;
    }

    public String message() {
        return message;
    }
}
