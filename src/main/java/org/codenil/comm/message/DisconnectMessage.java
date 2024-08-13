package org.codenil.comm.message;

import com.google.gson.Gson;

public class DisconnectMessage extends AbstractMessage {

    private DisconnectMessage(final byte[] data) {
        super(data);
    }

    public static DisconnectMessage create(final DisconnectReason reason) {
        return new DisconnectMessage(new Gson().toJson(reason).getBytes());
    }

    public static DisconnectMessage readFrom(final Message message) {
        if (message instanceof DisconnectMessage) {
            return (DisconnectMessage) message;
        }
        final int code = message.getCode();
        if (code != MessageCodes.DISCONNECT) {
            throw new IllegalArgumentException(
                    String.format("Message has code %d and thus is not a DisconnectMessage.", code));
        }
        return new DisconnectMessage(message.getData());
    }

    @Override
    public int getCode() {
        return MessageCodes.DISCONNECT;
    }
}
