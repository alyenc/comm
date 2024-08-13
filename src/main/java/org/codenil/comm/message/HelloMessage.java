package org.codenil.comm.message;

public class HelloMessage extends AbstractMessage {

    public HelloMessage(final byte[] data) {
        super(data);
    }

    public static HelloMessage create() {
        return new HelloMessage(new byte[0]);
    }

    @Override
    public int getCode() {
        return MessageCodes.HELLO;
    }
}
