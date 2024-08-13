package org.codenil.comm;

public class RemotePeer {

    private String pkiId;

    private final String endpoint;

    public RemotePeer(
            final String endpoint) {
        this.endpoint = endpoint;
    }

    public String pkiId() {
        return pkiId;
    }

    public String endpoint() {
        return endpoint;
    }

    public void setPkiId(String pkiId) {
        this.pkiId = pkiId;
    }

    public String toString() {
        return String.format("%s, pkiId:%s", this.endpoint(), this.pkiId);
    }

}
