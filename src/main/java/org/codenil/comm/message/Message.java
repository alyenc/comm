package org.codenil.comm.message;

public interface Message {

    String getRequestId();

    int getSize();

    int getCode();

    byte[] getData();

    default RawMessage wrapMessage(final String requestId) {
        RawMessage rawMessage = new RawMessage(getCode(), getData());
        rawMessage.setRequestId(requestId);
        return rawMessage;
    }
}
