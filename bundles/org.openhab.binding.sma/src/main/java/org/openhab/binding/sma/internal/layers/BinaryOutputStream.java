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
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Stream for writing little-endian encoded binary data to a byte buffer.
 *
 * The methods are similar to java.io.DataOutput but allow method chaining.
 *
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class BinaryOutputStream {

    private final ByteArrayOutputStream os;

    public BinaryOutputStream() {
        this(new ByteArrayOutputStream());
    }

    private BinaryOutputStream(ByteArrayOutputStream os) {
        this.os = os;
    }

    public byte[] toByteArray() throws IOException {
        os.close();
        return os.toByteArray();
    }

    public PPPFrame toPPPFrame() {
        return new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, SMAPPPFrame.CONTROL, SMAPPPFrame.PROTOCOL, os.toByteArray());
    }

    public BinaryOutputStream writeByte(int v) throws IOException {
        os.write(v);
        return this;
    }

    public BinaryOutputStream writeShort(int v) throws IOException {
        os.write(0xFF & v);
        os.write(0xFF & (v >> 8));
        return this;
    }

    public BinaryOutputStream writeInt(int v) throws IOException {
        os.write(0xFF & v);
        os.write(0xFF & (v >> 8));
        os.write(0xFF & (v >> 16));
        os.write(0xFF & (v >> 24));
        return this;
    }

    public BinaryOutputStream writeBytes(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            os.write((byte) s.charAt(i));
        }
        return this;
    }

    public BinaryOutputStream writeBytes(byte[] data) throws IOException {
        os.write(data);
        return this;
    }
}
