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
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class PPPFrame {

    private static final Logger logger = LoggerFactory.getLogger(PPPFrame.class);

    public static final byte HDLC_ADR_BROADCAST = (byte) 0xff;

    static final byte HDLC_ESC = 0x7d;
    static final byte HDLC_SYNC = 0x7e;

    private byte address;
    private byte control;
    private short protocol;

    byte[] payload;

    public PPPFrame(byte address, byte control, short protocol, byte[] payload) {
        this.address = address;
        this.control = control;
        this.protocol = protocol;
        this.payload = payload;
    }

    public byte getAddress() {
        return address;
    }

    public byte getControl() {
        return control;
    }

    public short getProtocol() {
        return protocol;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] getFrame() {
        byte[] frame = new byte[8 + payload.length];

        frame[0] = HDLC_SYNC;
        frame[1] = address;
        frame[2] = control;
        short2be(frame, 3, protocol);

        System.arraycopy(payload, 0, frame, 5, payload.length);

        CRC crc = new CRC();
        for (int i = 1; i < 5 + payload.length; i++) {
            crc.writeByte(frame[i]);
        }

        short2le(frame, 5 + payload.length, crc.get());
        frame[frame.length - 1] = HDLC_SYNC;

        if (frame[5 + payload.length] == HDLC_SYNC || frame[5 + payload.length] == HDLC_ESC
                || frame[6 + payload.length] == HDLC_SYNC || frame[6 + payload.length] == HDLC_ESC) {
            logger.warn("CRC contains special character - problems?");
        }

        return frame;
    }

    public static boolean peek(InputStream is, byte address, byte control, short protocol) throws IOException {
        is.mark(5);

        byte[] header = new byte[5];
        if (is.read(header) < header.length) {
            return false;
        }

        is.reset();

        return header[0] == HDLC_SYNC && header[1] == address && header[2] == control
                && be2short(header, 3) == protocol;
    }

    public static PPPFrame read(InputStream is) throws IOException {

        byte[] header = new byte[5];
        if (is.read(header) < header.length) {
            throw new IOException("EOF");
        }

        if (header[0] != HDLC_SYNC) {
            throw new IOException("SYNC expected");
        }

        byte address = header[1];
        byte control = header[2];
        short protocol = (short) be2short(header, 3);

        CRC crc = new CRC();
        for (int i = 1; i < header.length; i++) {
            crc.writeByte(header[i]);
        }

        EscapeInputStream esc = new EscapeInputStream(is);
        ByteArrayOutputStream payloadStream = new ByteArrayOutputStream(1024);

        int c = esc.read();
        while (c >= 0) {
            payloadStream.write(c);
            c = esc.read();
        }

        byte[] buffer = payloadStream.toByteArray();
        byte[] payload = Arrays.copyOf(buffer, buffer.length - 2);
        short fcs_read = (short) le2short(buffer, buffer.length - 2);

        for (int i = 0; i < payload.length; i++) {
            crc.writeByte(payload[i]);
        }

        short fcs_calc = crc.get();
        if (fcs_read != fcs_calc) {
            throw new IOException("FCS mismatch");
        }

        PPPFrame result = new PPPFrame(address, control, protocol, payload);

        return result;
    }

    public void write(OutputStream os) throws IOException {

        os.write(HDLC_SYNC);
        byte[] unescaped = getFrame();
        EscapeOutputStream esc = new EscapeOutputStream(os);
        esc.write(unescaped, 1, unescaped.length - 2);
        os.write(HDLC_SYNC);
    }

    private static int be2short(byte[] buffer, int i) {
        return ((buffer[i] << 8) & 0xff00) | (buffer[i + 1] & 0xff);
    }

    private static void short2be(byte[] buffer, int i, short v) {
        buffer[i] = (byte) ((v >>> 8) & 0xFF);
        buffer[i + 1] = (byte) ((v >>> 0) & 0xFF);
    }

    private static int le2short(byte[] buffer, int i) {
        return (buffer[i] & 0xff) | ((buffer[i + 1] << 8) & 0xff00);
    }

    private static void short2le(byte[] buffer, int i, short v) {
        buffer[i] = (byte) ((v >>> 0) & 0xFF);
        buffer[i + 1] = (byte) ((v >>> 8) & 0xFF);
    }
}
