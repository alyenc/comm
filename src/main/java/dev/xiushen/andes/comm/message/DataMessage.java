package dev.xiushen.andes.comm.message;

public abstract class DataMessage extends AbstractMessage {

    private final String data;

    public DataMessage(final int code, final String data) {
        super(code);
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
