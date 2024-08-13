package org.codenil.comm.message;

public abstract class EmptyMessage implements Message {

    @Override
    public final int getSize() {
        return 0;
    }

    @Override
    public byte[] getData() {
        return new byte[]{};
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{ code=" + getCode() + ", size=0}";
    }
}
