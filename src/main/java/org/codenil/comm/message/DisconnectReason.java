package org.codenil.comm.message;

import java.util.Optional;

public enum DisconnectReason {

    UNKNOWN(null),

    TIMEOUT((byte) 0x0b),

    INVALID_MESSAGE_RECEIVED((byte) 0x02, "An exception was caught decoding message"),
    ;

    private final Optional<Byte> code;
    private final Optional<String> message;

    DisconnectReason(final Byte code) {
        this.code = Optional.ofNullable(code);
        this.message = Optional.empty();
    }

    DisconnectReason(final Byte code, final String message) {
        this.code = Optional.ofNullable(code);
        this.message = Optional.of(message);
    }

}
