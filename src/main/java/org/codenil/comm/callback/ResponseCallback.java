package org.codenil.comm.callback;

import org.codenil.comm.message.DefaultMessage;
import org.codenil.comm.message.Message;

@FunctionalInterface
public interface ResponseCallback {
    Message response(DefaultMessage message);
}
