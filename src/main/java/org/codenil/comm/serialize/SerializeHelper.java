package org.codenil.comm.serialize;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 根据协议格式写入数据
 * 返回组装好协议的ByteBuf
 * 只能通过这个方法创建协议，其他方式创建的ByteBuf
 * 后续修改协议头时不确定兼容
 */
public class SerializeHelper {

    private final ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;

    private ByteBuf buf;

    private boolean hasHeader = false;

    public SerializeHelper() {
        buf = allocator.buffer();
    }

    public SerializeHelper(
            final ByteBuf buf,
            final boolean hasHeader) {
        this.buf = buf;
        this.hasHeader = hasHeader;
    }

    /**
     * 协议版本号，固定长度4
     */
    public SerializeHelper writeVersion(final String version) {
        if (!Version.supported(version)) {
            throw new IllegalArgumentException("Unsupported protocol version: " + version);
        }

        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Version cannot be null or empty");
        }
        byte[] bytes = version.getBytes();
        if (bytes.length > 4) {
            throw new IllegalArgumentException("Version length exceeds 4 bytes");
        }

        ByteBuf buf = allocator.buffer();
        if (this.buf.readableBytes() == 0) {
            buf.writeInt(bytes.length);
            buf.writeBytes(bytes);
            buf.writeInt(0);
            this.buf.release();
            this.buf = buf;
            return this;
        }

        String oldVersion = readVersion(this.buf);
        if (oldVersion.equals(version)) {
            return this;
        }

        //version长度
        buf.writeInt(bytes.length);
        //version内容
        buf.writeBytes(bytes);
        //payload长度
        buf.writeInt(0);

        this.buf.release();
        this.buf = buf;
        this.hasHeader = true;
        return this;
    }

    public SerializeHelper writeInt(int value) {
        ByteBuf buf = allocator.buffer();

        this.buf.readerIndex(0);
        if (this.buf.readableBytes() == 0) {
            buf.writeInt(value);

            this.buf.release();
            this.buf = buf;
            return this;
        }

        if (hasHeader) {
            String version = readVersion(this.buf);
            buf.writeInt(version.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(version.getBytes(StandardCharsets.UTF_8));

            //写入总长度
            int payloadLength = this.buf.readInt();
            buf.writeInt(payloadLength + 4);
        }

        byte[] payloadBytes = new byte[buf.readableBytes()];
        this.buf.readBytes(payloadBytes);
        buf.writeBytes(payloadBytes);
        buf.writeInt(value);

        this.buf.release();
        this.buf = buf;
        return this;
    }

    public SerializeHelper writeLong(long value) {
        ByteBuf buf = allocator.buffer();

        this.buf.readerIndex(0);
        if (this.buf.readableBytes() == 0) {
            buf.writeLong(value);

            this.buf.release();
            this.buf = buf;
            return this;
        }

        if (hasHeader) {
            String version = readVersion(this.buf);
            buf.writeInt(version.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(version.getBytes(StandardCharsets.UTF_8));

            int payloadLength = this.buf.readInt();
            buf.writeInt(payloadLength + 8);
        }

        byte[] payloadBytes = new byte[buf.readableBytes()];
        this.buf.readBytes(payloadBytes);
        buf.writeBytes(payloadBytes);
        buf.writeLong(value);

        this.buf.release();
        this.buf = buf;
        return this;
    }

    public SerializeHelper writeString(String value) {
        if (value == null) {
            return this;
        }
        ByteBuf buf = allocator.buffer();

        this.buf.readerIndex(0);
        if (this.buf.readableBytes() == 0) {
            buf.writeInt(value.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(value.getBytes(StandardCharsets.UTF_8));

            this.buf.release();
            this.buf = buf;
            return this;
        }

        if (hasHeader) {
            String version = readVersion(this.buf);
            buf.writeInt(version.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(version.getBytes(StandardCharsets.UTF_8));

            int payloadLength = this.buf.readInt();
            buf.writeInt(payloadLength + value.getBytes(StandardCharsets.UTF_8).length + 4);
        }

        byte[] payloadBytes = new byte[buf.readableBytes()];
        this.buf.readBytes(payloadBytes);
        buf.writeBytes(payloadBytes);

        buf.writeInt(value.getBytes(StandardCharsets.UTF_8).length);
        buf.writeBytes(value.getBytes(StandardCharsets.UTF_8));

        this.buf.release();
        this.buf = buf;
        return this;
    }

    public SerializeHelper writeBoolean(boolean value) {
        ByteBuf buf = allocator.buffer();

        this.buf.readerIndex(0);
        if (this.buf.readableBytes() == 0) {
            buf.writeInt(1);
            buf.writeBoolean(value);

            this.buf.release();
            this.buf = buf;
            return this;
        }

        if (hasHeader) {
            String version = readVersion(this.buf);
            buf.writeInt(version.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(version.getBytes(StandardCharsets.UTF_8));

            int payloadLength = this.buf.readInt();
            buf.writeInt(payloadLength + 1 + 4);
        }

        byte[] payloadBytes = new byte[buf.readableBytes()];
        this.buf.readBytes(payloadBytes);
        buf.writeBytes(payloadBytes);

        buf.writeInt(1);
        buf.writeBoolean(value);

        this.buf.release();
        this.buf = buf;
        return this;
    }

    public SerializeHelper writeBytes(byte[] value) {
        ByteBuf buf = allocator.buffer();

        this.buf.readerIndex(0);
        if (this.buf.readableBytes() == 0) {
            buf.writeInt(value.length);
            buf.writeBytes(value);

            this.buf.release();
            this.buf = buf;
            return this;
        }

        if (hasHeader) {
            String version = readVersion(this.buf);
            buf.writeInt(version.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(version.getBytes(StandardCharsets.UTF_8));

            int payloadLength = this.buf.readInt();
            buf.writeInt(payloadLength + value.length + 4);
        }

        byte[] payloadBytes = new byte[buf.readableBytes()];
        this.buf.readBytes(payloadBytes);
        buf.writeBytes(payloadBytes);

        buf.writeInt(value.length);
        buf.writeBytes(value);

        this.buf.release();
        this.buf = buf;
        return this;
    }

    public SerializeHelper writeBytes(List<byte[]> values) {
        if (values == null) {
            return this;
        }

        int totalLength = values.stream()
                .mapToInt(bytes -> bytes.length + 4).sum();

        ByteBuf buf = allocator.buffer();

        this.buf.readerIndex(0);
        if (this.buf.readableBytes() == 0) {
            buf.writeInt(totalLength);
            values.forEach(value -> {
                buf.writeInt(value.length);
                buf.writeBytes(value);
            });

            this.buf.release();
            this.buf = buf;
            return this;
        }

        if (hasHeader) {
            String version = readVersion(this.buf);
            buf.writeInt(version.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(version.getBytes(StandardCharsets.UTF_8));

            int payloadLength = this.buf.readInt();
            buf.writeInt(payloadLength + totalLength);
        }

        byte[] payloadBytes = new byte[buf.readableBytes()];
        this.buf.readBytes(payloadBytes);
        buf.writeBytes(payloadBytes);

        buf.writeInt(totalLength);
        values.forEach(value -> {
            buf.writeInt(value.length);
            buf.writeBytes(value);
        });
        this.buf.release();
        this.buf = buf;
        return this;
    }

    public SerializeHelper writeList(List<String> values) {
        if (values == null) {
            return this;
        }

        int totalLength = values.stream()
                .map(value -> value.getBytes(StandardCharsets.UTF_8))
                .mapToInt(bytes -> bytes.length + 4)
                .sum();

        ByteBuf buf = allocator.buffer();

        this.buf.readerIndex(0);
        if (this.buf.readableBytes() == 0) {
            buf.writeInt(Version.defaultVersion().getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(Version.defaultVersion().getBytes(StandardCharsets.UTF_8));

            buf.writeInt(totalLength);

            values.forEach(value -> {
                buf.writeInt(value.getBytes(StandardCharsets.UTF_8).length);
                buf.writeBytes(value.getBytes(StandardCharsets.UTF_8));
            });

            this.buf.release();
            this.buf = buf;
            return this;
        }

        String version = readVersion(this.buf);
        buf.writeInt(version.getBytes(StandardCharsets.UTF_8).length);
        buf.writeBytes(version.getBytes(StandardCharsets.UTF_8));

        int payloadLength = this.buf.readInt();
        byte[] payloadBytes = new byte[payloadLength];
        this.buf.readBytes(payloadBytes);

        buf.writeInt(payloadLength + totalLength);

        buf.writeBytes(payloadBytes);

        values.forEach(value -> {
            buf.writeInt(value.getBytes(StandardCharsets.UTF_8).length);
            buf.writeBytes(value.getBytes(StandardCharsets.UTF_8));
        });
        this.buf.release();
        this.buf = buf;
        return this;
    }

    public String readVersion(ByteBuf buf) {
        this.buf.readerIndex(0);
        int versionLength = this.buf.readInt();
        byte[] versionBytes = new byte[versionLength];
        this.buf.readBytes(versionBytes);

        return new String(versionBytes);
    }

    public byte[] readPayload(ByteBuf buf) {
        this.buf.readerIndex(0);

        if (hasHeader) {
            int versionLength = this.buf.readInt();
            this.buf.skipBytes(versionLength);
        }

        if (this.buf.readableBytes() == 0) {
            return new byte[]{};
        }

        int payloadLength = this.buf.readInt();
        byte[] payloadBytes = new byte[payloadLength];
        this.buf.readBytes(payloadBytes);

        return payloadBytes;
    }

    public ByteBuf build() {
        return buf;
    }
}