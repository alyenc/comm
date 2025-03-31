package dev.xiushen.andes.comm.message;

public abstract class AbstractMessage implements Message {

    private final int code;

    public AbstractMessage(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
