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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

/**
 * Stream for writing little-endian encoded binary data.
 *
 * The methods are similar to java.io.DataOutput but allow method chaining.
 *
 * @author Martin Gerczuk - Initial contribution
 */
public class BinaryOutputStream extends FilterOutputStream {

    ByteArrayOutputStream os;

    public BinaryOutputStream() {
        this(new ByteArrayOutputStream());
    }

    private BinaryOutputStream(ByteArrayOutputStream os) {
        super(new DataOutputStream(os));
        this.os = os;
    }

    public byte[] toByteArray() throws IOException {
        close();
        return os.toByteArray();
    }

    public PPPFrame toPPPFrame() {
        return new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, SMAPPPFrame.CONTROL, SMAPPPFrame.PROTOCOL, os.toByteArray());
    }

    public BinaryOutputStream writeBoolean(boolean v) throws IOException {
        ((DataOutputStream) out).writeBoolean(v);
        return this;
    }

    public BinaryOutputStream writeByte(int v) throws IOException {
        ((DataOutputStream) out).writeByte(v);
        return this;
    }

    public BinaryOutputStream writeShort(int v) throws IOException {
        out.write(0xFF & v);
        out.write(0xFF & (v >> 8));
        return this;
    }

    public BinaryOutputStream writeChar(int v) throws IOException {
        writeShort(v);
        return this;
    }

    public BinaryOutputStream writeInt(int v) throws IOException {
        out.write(0xFF & v);
        out.write(0xFF & (v >> 8));
        out.write(0xFF & (v >> 16));
        out.write(0xFF & (v >> 24));
        return this;
    }

    // public LittleEndianByteArrayOutputStream writeLong(long v) throws IOException {
    // throw new IOException("LittleEndianDataOutputStream.writeLong not supported");
    // }
    //
    // public LittleEndianByteArrayOutputStream writeFloat(float v) throws IOException {
    // writeInt(Float.floatToIntBits(v));
    // return this;
    // }
    //
    // public LittleEndianByteArrayOutputStream writeDouble(double v) throws IOException {
    // writeLong(Double.doubleToLongBits(v));
    // return this;
    // }

    public BinaryOutputStream writeBytes(String s) throws IOException {
        ((DataOutputStream) out).writeBytes(s);
        return this;
    }

    public BinaryOutputStream writeBytes(byte[] data) throws IOException {
        ((DataOutputStream) out).write(data);
        return this;
    }

    // public LittleEndianByteArrayOutputStream writeChars(String s) throws IOException {
    // for (int i = 0; i < s.length(); i++) {
    // writeChar(s.charAt(i));
    // }
    // return this;
    // }
    //
    // public LittleEndianByteArrayOutputStream writeUTF(String s) throws IOException {
    // ((DataOutputStream) out).writeUTF(s);
    // return this;
    // }
}
