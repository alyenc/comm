package dev.xiushen.andes.comm.message;

public class DisconnectMessage extends DataMessage {

    public DisconnectMessage(final String data) {
        super(MessageCodes.DISCONNECT, data);
    }
}
