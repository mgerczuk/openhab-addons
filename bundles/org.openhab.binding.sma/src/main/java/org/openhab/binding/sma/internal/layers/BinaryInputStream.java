/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sma.internal.layers;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Stream for reading little-endian encoded binary data from a byte buffer.
 *
 * The methods are similar to java.io.DataInput.
 *
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class BinaryInputStream {

    private final byte[] buf;
    private int pos;

    public BinaryInputStream(byte[] buf) {
        this.buf = buf;
        pos = 0;
    }

    public void seek(int newPos) throws IOException {
        if (newPos < 0 || newPos >= buf.length) {
            throw new IOException("invalid seek");
        }

        pos = newPos;
    }

    public int readByte() throws IOException {
        if (pos >= buf.length) {
            throw new IOException("read after eof");
        }

        return buf[pos++] & 0xFF;
    }

    public int readUShort() throws IOException {
        int lo = readByte();
        int hi = readByte();
        return lo | (hi << 8);
    }

    public long readUInt() throws IOException {
        int lo = readUShort();
        int hi = readUShort();
        return lo | (hi << 16);
    }

    public long readULong() throws IOException {
        long lo = readUInt();
        long hi = readUInt();
        return lo | (hi << 32);
    }

    public String readString(int maxSize) throws IOException {
        StringBuilder sb = new StringBuilder(maxSize);
        for (int i = 0; i < maxSize; i++) {
            int c = readByte();
            if (c == 0) {
                break;
            }
            sb.appendCodePoint(c);
        }
        return sb.toString();
    }
}
