package dev.xiushen.andes.comm.message;

public class PingMessage extends EmptyMessage {

    public PingMessage() {
        super(MessageCodes.PING);
    }

    @Override
    public String toString() {
        return "PingMessage{data=''}";
    }
}
