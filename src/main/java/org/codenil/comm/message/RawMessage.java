package org.codenil.comm.message;

public class RawMessage extends AbstractMessage {

    private final int code;

    public RawMessage(
            final int code,
            final byte[] data) {
        super(data);
        this.code = code;
    }

    @Override
    public int getCode() {
        return code;
    }
}
