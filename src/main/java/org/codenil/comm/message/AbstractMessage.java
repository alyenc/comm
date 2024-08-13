package org.codenil.comm.message;

public abstract class AbstractMessage implements Message {

    private String requestId;

    private byte[] data;

    @Override
    public String requestId() {
        return requestId;
    }

    @Override
    public final int size() {
        return data.length;
    }

    @Override
    public byte[] data() {
        return data;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
