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

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class Utils {

    public static final short ANYSUSYID = (short) 0xFFFF;
    public static final int ANYSERIAL = 0xFFFFFFFF;

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
        return ((Utils.getShort(buffer, i + 2) << 16) & 0xffff0000) | (Utils.getShort(buffer, i) & 0xffff);
    }

    public static long getLong(byte[] buffer, int i) {
        return (((long) Utils.getInt(buffer, i + 4) << 32) & 0xffffffff00000000l)
                | (Utils.getInt(buffer, i) & 0xffffffffl);
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
}
