package org.codenil.comm.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.codenil.comm.message.RawMessage;
import org.codenil.comm.serialize.SerializeHelper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class MessageFrameEncoder extends MessageToByteEncoder<RawMessage> {

    public MessageFrameEncoder() {}

    @Override
    protected void encode(
            final ChannelHandlerContext ctx,
            final RawMessage msg,
            final ByteBuf out) {
        byte[] idBytes = Optional.ofNullable(msg.getRequestId()).orElse("").getBytes(StandardCharsets.UTF_8);

        SerializeHelper builder = new SerializeHelper();
        ByteBuf buf = builder.writeBytes(idBytes)
                .writeInt(msg.getCode())
                .writeBytes(msg.getData())
                .build();

        out.writeBytes(buf);
        buf.release();
    }
}