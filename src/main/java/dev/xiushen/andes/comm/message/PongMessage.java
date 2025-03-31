package dev.xiushen.andes.comm.message;

public class PongMessage extends EmptyMessage {

    public PongMessage() {
        super(MessageCodes.PONG);
    }
}
