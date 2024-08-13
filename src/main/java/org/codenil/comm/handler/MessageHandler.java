package org.codenil.comm.handler;

import io.netty.buffer.ByteBuf;
import org.codenil.comm.handshake.PlainMessage;
import org.codenil.comm.message.MessageType;
import org.codenil.comm.serialize.SerializeHelper;

public class MessageHandler {
    public static byte[] buildMessage(final PlainMessage message) {
        SerializeHelper builder = new SerializeHelper();
        ByteBuf buf = builder.writeInt(message.messageType().getValue())
                .writeInt(message.code())
                .writeBytes(message.data()).build();

        byte[] result = new byte[buf.readableBytes()];
        buf.readBytes(result);
        buf.release();
        return result;
    }

    public static byte[] buildMessage(
            final MessageType messageType,
            final int code,
            final byte[] data) {
        return buildMessage(new PlainMessage(messageType, code, data));
    }

    public static PlainMessage parseMessage(final ByteBuf buf) {
        PlainMessage ret = null;

        buf.readerIndex(0);

        //跳过版本
        int versionLength = buf.readInt();
        buf.skipBytes(versionLength);

        int payloadLength = buf.readInt();
        if(payloadLength < 8) {
            return ret;
        }

        int messageType = buf.readInt();
        int code = buf.readInt();
        int dataLength = buf.readInt();
        byte[] data = new byte[dataLength];
        buf.readBytes(data);

        ret = new PlainMessage(MessageType.forNumber(messageType), code, data);
        return ret;
    }
}
