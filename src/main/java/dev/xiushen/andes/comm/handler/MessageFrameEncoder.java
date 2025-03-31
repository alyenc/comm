package dev.xiushen.andes.comm.handler;

import com.google.gson.Gson;
import dev.xiushen.andes.comm.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

public class MessageFrameEncoder extends MessageToMessageEncoder<Message> {

    public MessageFrameEncoder() {}

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, List<Object> list) {
        list.add(new Gson().toJson(message));
    }
}