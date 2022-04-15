/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.Random;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public abstract class AbstractPhysicalLayer implements PhysicalLayer {
    protected static final short fcstab[] = new short[] { 0x0000, 0x1189, 0x2312, 0x329b, 0x4624, 0x57ad, 0x6536,
            0x74bf, (short) 0x8c48, (short) 0x9dc1, (short) 0xaf5a, (short) 0xbed3, (short) 0xca6c, (short) 0xdbe5,
            (short) 0xe97e, (short) 0xf8f7, 0x1081, 0x0108, 0x3393, 0x221a, 0x56a5, 0x472c, 0x75b7, 0x643e,
            (short) 0x9cc9, (short) 0x8d40, (short) 0xbfdb, (short) 0xae52, (short) 0xdaed, (short) 0xcb64,
            (short) 0xf9ff, (short) 0xe876, 0x2102, 0x308b, 0x0210, 0x1399, 0x6726, 0x76af, 0x4434, 0x55bd,
            (short) 0xad4a, (short) 0xbcc3, (short) 0x8e58, (short) 0x9fd1, (short) 0xeb6e, (short) 0xfae7,
            (short) 0xc87c, (short) 0xd9f5, 0x3183, 0x200a, 0x1291, 0x0318, 0x77a7, 0x662e, 0x54b5, 0x453c,
            (short) 0xbdcb, (short) 0xac42, (short) 0x9ed9, (short) 0x8f50, (short) 0xfbef, (short) 0xea66,
            (short) 0xd8fd, (short) 0xc974, 0x4204, 0x538d, 0x6116, 0x709f, 0x0420, 0x15a9, 0x2732, 0x36bb,
            (short) 0xce4c, (short) 0xdfc5, (short) 0xed5e, (short) 0xfcd7, (short) 0x8868, (short) 0x99e1,
            (short) 0xab7a, (short) 0xbaf3, 0x5285, 0x430c, 0x7197, 0x601e, 0x14a1, 0x0528, 0x37b3, 0x263a,
            (short) 0xdecd, (short) 0xcf44, (short) 0xfddf, (short) 0xec56, (short) 0x98e9, (short) 0x8960,
            (short) 0xbbfb, (short) 0xaa72, 0x6306, 0x728f, 0x4014, 0x519d, 0x2522, 0x34ab, 0x0630, 0x17b9,
            (short) 0xef4e, (short) 0xfec7, (short) 0xcc5c, (short) 0xddd5, (short) 0xa96a, (short) 0xb8e3,
            (short) 0x8a78, (short) 0x9bf1, 0x7387, 0x620e, 0x5095, 0x411c, 0x35a3, 0x242a, 0x16b1, 0x0738,
            (short) 0xffcf, (short) 0xee46, (short) 0xdcdd, (short) 0xcd54, (short) 0xb9eb, (short) 0xa862,
            (short) 0x9af9, (short) 0x8b70, (short) 0x8408, (short) 0x9581, (short) 0xa71a, (short) 0xb693,
            (short) 0xc22c, (short) 0xd3a5, (short) 0xe13e, (short) 0xf0b7, 0x0840, 0x19c9, 0x2b52, 0x3adb, 0x4e64,
            0x5fed, 0x6d76, 0x7cff, (short) 0x9489, (short) 0x8500, (short) 0xb79b, (short) 0xa612, (short) 0xd2ad,
            (short) 0xc324, (short) 0xf1bf, (short) 0xe036, 0x18c1, 0x0948, 0x3bd3, 0x2a5a, 0x5ee5, 0x4f6c, 0x7df7,
            0x6c7e, (short) 0xa50a, (short) 0xb483, (short) 0x8618, (short) 0x9791, (short) 0xe32e, (short) 0xf2a7,
            (short) 0xc03c, (short) 0xd1b5, 0x2942, 0x38cb, 0x0a50, 0x1bd9, 0x6f66, 0x7eef, 0x4c74, 0x5dfd,
            (short) 0xb58b, (short) 0xa402, (short) 0x9699, (short) 0x8710, (short) 0xf3af, (short) 0xe226,
            (short) 0xd0bd, (short) 0xc134, 0x39c3, 0x284a, 0x1ad1, 0x0b58, 0x7fe7, 0x6e6e, 0x5cf5, 0x4d7c,
            (short) 0xc60c, (short) 0xd785, (short) 0xe51e, (short) 0xf497, (short) 0x8028, (short) 0x91a1,
            (short) 0xa33a, (short) 0xb2b3, 0x4a44, 0x5bcd, 0x6956, 0x78df, 0x0c60, 0x1de9, 0x2f72, 0x3efb,
            (short) 0xd68d, (short) 0xc704, (short) 0xf59f, (short) 0xe416, (short) 0x90a9, (short) 0x8120,
            (short) 0xb3bb, (short) 0xa232, 0x5ac5, 0x4b4c, 0x79d7, 0x685e, 0x1ce1, 0x0d68, 0x3ff3, 0x2e7a,
            (short) 0xe70e, (short) 0xf687, (short) 0xc41c, (short) 0xd595, (short) 0xa12a, (short) 0xb0a3,
            (short) 0x8238, (short) 0x93b1, 0x6b46, 0x7acf, 0x4854, 0x59dd, 0x2d62, 0x3ceb, 0x0e70, 0x1ff9,
            (short) 0xf78f, (short) 0xe606, (short) 0xd49d, (short) 0xc514, (short) 0xb1ab, (short) 0xa022,
            (short) 0x92b9, (short) 0x8330, 0x7bc7, 0x6a4e, 0x58d5, 0x495c, 0x3de3, 0x2c6a, 0x1ef1, 0x0f78 };

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, bytes.length, ' ');
    }

    public static final String bytesToHex(byte[] bytes, int length) {
        return bytesToHex(bytes, length, ' ');
    }

    public static final String bytesToHex(byte[] bytes, char delimiter) {
        return bytesToHex(bytes, bytes.length, delimiter);
    }

    public static final String toHex(byte b) {
        return new String(new char[] { hexArray[(b & 0xf0) >>> 4], hexArray[b & 0x0F] });
    }

    public static final String toHex(short s) {
        return toHex((byte) (s >>> 8)) + toHex((byte) (s & 0xff));
    }

    public static final String toHex(int i) {
        return toHex((short) (i >>> 16)) + toHex((short) (i & 0xffff));
    }

    public static final String bytesToHex(byte[] bytes, int length, char delimiter) {
        StringBuilder sb = new StringBuilder();
        sb.append("--------: 00 01 02 03 04 05 06 07 08 09\n00000000: ");
        int i = 0;
        for (int j = 0; j < length; j++) {
            int v = bytes[j] & 0xFF;
            sb.append(hexArray[v >>> 4]);
            sb.append(hexArray[v & 0x0F]);

            if (j % 10 == 9) {
                sb.append(String.format("\n%08d: ", j + 1));
            } else {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static final String bytesToBtAddress(byte[] bytes) {
        int length = bytes.length;
        char[] hexChars = new char[length * 2];
        int i = 0;
        for (int j = 0; j < length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[i++] = hexArray[v >>> 4];
            hexChars[i++] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte getByte(byte[] buffer, int i) {
        return (buffer[i]);
    }

    public static int getShort(byte[] buffer, int i) {
        return ((buffer[i] & 0xff) | ((buffer[i + 1] << 8) & 0xff00));
    }

    public static int getInt(byte[] buffer, int i) {
        return ((AbstractPhysicalLayer.getShort(buffer, i + 2) << 16) & 0xffff0000)
                | (AbstractPhysicalLayer.getShort(buffer, i) & 0xffff);
    }

    public static long getLong(byte[] buffer, int i) {
        return (((long) AbstractPhysicalLayer.getInt(buffer, i + 4) << 32) & 0xffffffff00000000l)
                | (AbstractPhysicalLayer.getInt(buffer, i) & 0xffffffffl);
    }

    public static String getString(byte[] buffer, int i, int length) {
        String s = new String(buffer, i, length);
        int term = s.indexOf('\0');
        if (term >= 0) {
            s = s.substring(0, term);
        }
        return s;
    }

    public static double tokWh(long value) {
        return (double) (value) / 1000;
    }

    public static float tokW(long value) {
        return (float) (value) / 1000;
    }

    public static double toHour(long value) {
        return (double) (value) / 3600;
    }

    public static float toAmp(long value) {
        return (float) value / 1000;
    }

    public static float toVolt(long value) {
        return (float) value / 100;
    }

    public static float toHz(long value) {
        return (float) value / 100;
    }

    public static float toTemp(long value) {
        return (float) value / 100;
    }

    protected byte[] buffer = new byte[520];
    protected int packetposition = 0;

    // Generate a Serial Number for application
    protected short AppSUSyID = 125;
    protected int AppSerial;

    public AbstractPhysicalLayer() {
        super();
        Random randomGenerator = new Random();
        AppSerial = 900000000 + randomGenerator.nextInt(100000000);
    }

    @Override
    public void write(int v) {
        writeByte((byte) ((v >>> 0) & 0xFF));
        writeByte((byte) ((v >>> 8) & 0xFF));
        writeByte((byte) ((v >>> 16) & 0xFF));
        writeByte((byte) ((v >>> 24) & 0xFF));
    }

    @Override
    public void writeShort(short v) {
        writeByte((byte) ((v >>> 0) & 0xFF));
        writeByte((byte) ((v >>> 8) & 0xFF));
    }

    @Override
    public void write(final byte[] bytes, int count) {
        for (int i = 0; i < count; i++) {
            writeByte(bytes[i]);
        }
    }

    @Override
    public boolean isCrcValid() {
        return true;
    }
}
