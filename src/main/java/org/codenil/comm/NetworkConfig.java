package org.codenil.comm;

public class NetworkConfig {

    private String bindHost;

    private int bindPort;

    public String bindHost() {
        return bindHost;
    }

    public void setBindHost(String bindHost) {
        this.bindHost = bindHost;
    }

    public int bindPort() {
        return bindPort;
    }

    public void setBindPort(int bindPort) {
        this.bindPort = bindPort;
    }
}
