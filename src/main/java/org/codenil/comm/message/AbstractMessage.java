package org.codenil.comm.message;

public abstract class AbstractMessage implements Message {

    private String requestId;

    private byte[] data = new byte[]{};

    public AbstractMessage(
            final byte[] data) {
        this.data = data;
    }

    @Override
    public String getRequestId() {
        return requestId;
    }

    @Override
    public final int getSize() {
        return data.length;
    }

    @Override
    public byte[] getData() {
        return data;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
