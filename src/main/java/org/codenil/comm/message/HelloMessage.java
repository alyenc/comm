package org.codenil.comm.message;

public class HelloMessage extends AbstractMessage {

    public HelloMessage(final byte[] data) {
        super.setData(data);
    }

    public static HelloMessage create(byte[] bytes) {
        return new HelloMessage(new byte[0]);
    }

    @Override
    public int code() {
        return MessageCodes.HELLO;
    }
}
