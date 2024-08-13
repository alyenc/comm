package org.codenil.comm.message;

public class DisconnectMessage extends AbstractMessage {

    private DisconnectMessage(final byte[] data) {
        super.setData(data);
    }

    public static DisconnectMessage create(final DisconnectReason reason) {
        return new DisconnectMessage(new byte[]{reason.code()});
    }

    public static DisconnectMessage readFrom(final Message message) {
        if (message instanceof DisconnectMessage) {
            return (DisconnectMessage) message;
        }
        final int code = message.code();
        if (code != MessageCodes.DISCONNECT) {
            throw new IllegalArgumentException(
                    String.format("Message has code %d and thus is not a DisconnectMessage.", code));
        }
        return new DisconnectMessage(message.data());
    }

    @Override
    public int code() {
        return MessageCodes.DISCONNECT;
    }
}
