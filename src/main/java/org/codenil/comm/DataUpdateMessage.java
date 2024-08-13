package org.codenil.comm;

import org.codenil.comm.message.AbstractMessage;
import org.codenil.comm.message.Message;
import org.codenil.comm.message.MessageCodes;

public class DataUpdateMessage extends AbstractMessage {

    public DataUpdateMessage(byte[] data) {
        super(data);
    }

    public static DataUpdateMessage readFrom(final Message message) {
        if (message instanceof DataUpdateMessage) {
            return (DataUpdateMessage) message;
        }
        return new DataUpdateMessage(message.getData());
    }

    public static DataUpdateMessage create(final String data) {
        return new DataUpdateMessage(data.getBytes());
    }

    @Override
    public int getCode() {
        return MessageCodes.DATA_UPDATE;
    }

}
