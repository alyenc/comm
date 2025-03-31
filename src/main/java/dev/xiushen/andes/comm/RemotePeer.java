package dev.xiushen.andes.comm;

public class RemotePeer {

    private String identifier;

    private final String endpoint;

    public RemotePeer(
            final String endpoint) {
        this.endpoint = endpoint;
    }

    public String identifier() {
        return identifier;
    }

    public String endpoint() {
        return endpoint;
    }

    public void setPkiId(String pkiId) {
        this.identifier = pkiId;
    }

    public String toString() {
        return String.format("%s, identifier:%s", this.endpoint(), this.identifier);
    }

}
