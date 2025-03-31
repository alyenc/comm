package dev.xiushen.andes.comm.callback;

import dev.xiushen.andes.comm.message.DefaultMessage;
import dev.xiushen.andes.comm.message.Message;

@FunctionalInterface
public interface ResponseCallback {
    Message response(DefaultMessage message);
}
