package org.codenil.comm.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.codenil.comm.serialize.SerializeHelper;

public class RawMessage extends AbstractMessage {

    private final int code;

    protected RawMessage(final int code) {
        this.code = code;
    }

    public static RawMessage create(
            final int code) {
        return new RawMessage(code);
    }

    public static RawMessage decode(byte[] messageBytes) {
        ByteBuf buf = Unpooled.wrappedBuffer(messageBytes);
        buf.readerIndex(0);

        int code = buf.readInt();

        int requestIdLength = buf.readInt();
        byte[] requestIdBytes = new byte[requestIdLength];
        buf.readBytes(requestIdBytes);

        int dataLength = buf.readInt();
        byte[] dataBytes = new byte[dataLength];
        buf.readBytes(dataBytes);

        RawMessage rawMessage = create(code);
        rawMessage.setRequestId(new String(requestIdBytes));
        rawMessage.setData(dataBytes);
        return rawMessage;
    }

    public static byte[] encode(RawMessage rawMessage) {
        SerializeHelper converter = new SerializeHelper();

        ByteBuf byteBuf = converter
                .writeVersion("1.0")
                .writeInt(rawMessage.code())
                .writeString(rawMessage.requestId())
                .writeBytes(rawMessage.data())
                .build();
        return converter.readPayload(byteBuf);
    }

    @Override
    public int code() {
        return code;
    }
}
