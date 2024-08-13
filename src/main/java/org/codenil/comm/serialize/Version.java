package org.codenil.comm.serialize;

public class Version {

    public static final String VERSION_1_0 = "1.0";

    public static boolean supported(String version) {
        return VERSION_1_0.equals(version);
    }

    public static String defaultVersion() {
        return VERSION_1_0;
    }
}
