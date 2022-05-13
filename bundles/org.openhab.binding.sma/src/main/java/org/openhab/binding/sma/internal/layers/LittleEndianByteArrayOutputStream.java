/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class LittleEndianByteArrayOutputStream extends FilterOutputStream implements DataOutput {

    ByteArrayOutputStream os;

    public LittleEndianByteArrayOutputStream() {
        this(new ByteArrayOutputStream());
    }

    public LittleEndianByteArrayOutputStream(ByteArrayOutputStream os) {
        super(new DataOutputStream(os));
        this.os = os;
    }

    public byte[] toByteArray() {
        return os.toByteArray();
    }

    public PPPFrame getFrame() {
        return new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, SMANetFrame.CONTROL, SMANetFrame.PROTOCOL, os.toByteArray());
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        ((DataOutputStream) out).writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        ((DataOutputStream) out).writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        out.write(0xFF & v);
        out.write(0xFF & (v >> 8));
    }

    @Override
    public void writeChar(int v) throws IOException {
        writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        out.write(0xFF & v);
        out.write(0xFF & (v >> 8));
        out.write(0xFF & (v >> 16));
        out.write(0xFF & (v >> 24));
    }

    @Override
    public void writeLong(long v) throws IOException {
        throw new IOException("LittleEndianDataOutputStream.writeLong not supported");
    }

    @Override
    public void writeFloat(float v) throws IOException {
        writeInt(Float.floatToIntBits(v));
    }

    @Override
    public void writeDouble(double v) throws IOException {
        writeLong(Double.doubleToLongBits(v));
    }

    @Override
    public void writeBytes(String s) throws IOException {
        ((DataOutputStream) out).writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeUTF(String s) throws IOException {
        ((DataOutputStream) out).writeUTF(s);
    }
}
