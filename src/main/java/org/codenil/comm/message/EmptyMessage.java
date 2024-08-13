package org.codenil.comm.message;

public abstract class EmptyMessage implements Message {

    @Override
    public final int size() {
        return 0;
    }

    @Override
    public byte[] data() {
        return new byte[]{};
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{ code=" + code() + ", size=0}";
    }
}
