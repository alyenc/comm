package dev.xiushen.andes.comm.message;

public class HelloMessage extends DataMessage {

    public HelloMessage(final String data) {
        super(MessageCodes.HELLO, data);
    }
}
