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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class SMAFrame {

    // length of package header
    public static final int HEADERLENGTH = 18;

    int control;
    SmaBluetoothAddress sourceAddress;
    SmaBluetoothAddress destinationAddress;

    byte[] payload;

    public SMAFrame(int control, SmaBluetoothAddress localaddress, SmaBluetoothAddress destaddress, byte[] payload) {
        this.control = control;
        this.sourceAddress = localaddress;
        this.destinationAddress = destaddress;
        this.payload = payload;
    }

    public SMAFrame(int control, SmaBluetoothAddress localaddress, SmaBluetoothAddress destaddress, PPPFrame frame)
            throws IOException {
        this.control = control;
        this.sourceAddress = localaddress;
        this.destinationAddress = destaddress;

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        frame.write(os);
        payload = os.toByteArray();
    }

    public int getControl() {
        return control;
    }

    public SmaBluetoothAddress getSourceAddress() {
        return sourceAddress;
    }

    public SmaBluetoothAddress getDestinationAddress() {
        return destinationAddress;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] getFrame() {

        int totalLength = HEADERLENGTH + payload.length;
        byte[] buffer = new byte[totalLength];

        buffer[0] = PPPFrame.HDLC_SYNC;
        buffer[1] = (byte) (totalLength & 0xFF);
        buffer[2] = (byte) ((totalLength >>> 8) & 0xFF);
        buffer[3] = (byte) (buffer[0] ^ buffer[1] ^ buffer[2]);

        for (int i = 0; i < 6; i++) {
            buffer[4 + i] = sourceAddress.get(i);
        }

        for (int i = 0; i < 6; i++) {
            buffer[10 + i] = destinationAddress.get(i);
        }

        buffer[16] = (byte) (control & 0xFF);
        buffer[17] = (byte) (control >>> 8);

        System.arraycopy(payload, 0, buffer, HEADERLENGTH, payload.length);

        return buffer;
    }

    public static SMAFrame read(InputStream is) throws IOException {

        byte[] header = new byte[HEADERLENGTH];
        if (is.read(header) < header.length) {
            throw new IOException("EOF");
        }

        if (header[0] != PPPFrame.HDLC_SYNC) {
            throw new IOException("SYNC expected");
        }

        int length = le2short(header, 1) - HEADERLENGTH;
        byte[] payload = new byte[length];
        if (is.read(payload) < length) {
            throw new IOException("EOF");
        }

        SmaBluetoothAddress localaddress = new SmaBluetoothAddress(header, 4);
        SmaBluetoothAddress destaddress = new SmaBluetoothAddress(header, 10);
        int control = le2short(header, 16);

        return new SMAFrame(control, localaddress, destaddress, payload);
    }

    public void write(OutputStream os) throws IOException {
        os.write(getFrame());
    }

    private static int le2short(byte[] buffer, int i) {
        return (buffer[i] & 0xff) | ((buffer[i + 1] << 8) & 0xff00);
    }

    private static void short2le(byte[] buffer, int i, short v) {
        buffer[i] = (byte) ((v >>> 0) & 0xFF);
        buffer[i + 1] = (byte) ((v >>> 8) & 0xFF);
    }
}
