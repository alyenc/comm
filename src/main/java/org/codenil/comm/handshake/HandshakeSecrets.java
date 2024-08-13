package org.codenil.comm.handshake;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.crypto.digests.KeccakDigest;

import java.util.Arrays;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public class HandshakeSecrets {
    private final byte[] aesSecret;
    private final byte[] macSecret;
    private final byte[] token;
    private final KeccakDigest egressMac = new KeccakDigest(Bytes32.SIZE * 8);
    private final KeccakDigest ingressMac = new KeccakDigest(Bytes32.SIZE * 8);

    public HandshakeSecrets(final byte[] aesSecret, final byte[] macSecret, final byte[] token) {
        checkArgument(aesSecret.length == Bytes32.SIZE, "aes secret must be exactly 32 bytes long");
        checkArgument(macSecret.length == Bytes32.SIZE, "mac secret must be exactly 32 bytes long");
        checkArgument(token.length == Bytes32.SIZE, "token must be exactly 32 bytes long");

        this.aesSecret = aesSecret;
        this.macSecret = macSecret;
        this.token = token;
    }

    public HandshakeSecrets updateEgress(final byte[] bytes) {
        egressMac.update(bytes, 0, bytes.length);
        return this;
    }

    public HandshakeSecrets updateIngress(final byte[] bytes) {
        ingressMac.update(bytes, 0, bytes.length);
        return this;
    }

    @Override
    public String toString() {
        return "HandshakeSecrets{"
                + "aesSecret="
                + Bytes.wrap(aesSecret)
                + ", macSecret="
                + Bytes.wrap(macSecret)
                + ", token="
                + Bytes.wrap(token)
                + ", egressMac="
                + Bytes.wrap(snapshot(egressMac))
                + ", ingressMac="
                + Bytes.wrap(snapshot(ingressMac))
                + '}';
    }

    public byte[] getAesSecret() {
        return aesSecret;
    }

    public byte[] getMacSecret() {
        return macSecret;
    }

    public byte[] getToken() {
        return token;
    }

    public byte[] getEgressMac() {
        return snapshot(egressMac);
    }

    public byte[] getIngressMac() {
        return snapshot(ingressMac);
    }

    private static byte[] snapshot(final KeccakDigest digest) {
        final byte[] out = new byte[Bytes32.SIZE];
        new KeccakDigest(digest).doFinal(out, 0);
        return out;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass") // checked in delegated method
    @Override
    public boolean equals(final Object obj) {
        return equals(obj, false);
    }

    public boolean equals(final Object o, final boolean flipMacs) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HandshakeSecrets that = (HandshakeSecrets) o;
        final KeccakDigest vsEgress = flipMacs ? that.ingressMac : that.egressMac;
        final KeccakDigest vsIngress = flipMacs ? that.egressMac : that.ingressMac;
        return Arrays.equals(aesSecret, that.aesSecret)
                && Arrays.equals(macSecret, that.macSecret)
                && Arrays.equals(token, that.token)
                && Arrays.equals(snapshot(egressMac), snapshot(vsEgress))
                && Arrays.equals(snapshot(ingressMac), snapshot(vsIngress));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                Arrays.hashCode(aesSecret),
                Arrays.hashCode(macSecret),
                Arrays.hashCode(token),
                egressMac,
                ingressMac);
    }
}
