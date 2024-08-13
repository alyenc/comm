package org.codenil.comm.message;

public interface Message {

    String requestId();

    int size();

    int code();

    byte[] data();

    default RawMessage wrapMessage(final String requestId) {
        RawMessage rawMessage = RawMessage.create(code());
        rawMessage.setRequestId(requestId);
        rawMessage.setData(data());
        return rawMessage;
    }
}
