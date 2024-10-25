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

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class Utils {

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

    @Deprecated // use BinaryInputStream
    public static int getShort(byte[] buffer, int i) {
        return ((buffer[i] & 0xff) | ((buffer[i + 1] << 8) & 0xff00));
    }

    @Deprecated // use BinaryInputStream
    public static int getInt(byte[] buffer, int i) {
        return ((Utils.getShort(buffer, i + 2) << 16) & 0xffff0000) | (Utils.getShort(buffer, i) & 0xffff);
    }

    public static double tokWh(long value) {
        return (double) (value) / 1000;
    }

    public static double tokW(long value) {
        return (double) (value) / 1000;
    }

    public static float toAmp(long value) {
        return (float) value / 1000;
    }

    public static float toVolt(long value) {
        return (float) value / 100;
    }

    public static String toVersionString(long version) {
        byte vType = (byte) (version & 0xFF);

        String releaseType;
        if (vType > 5) {
            releaseType = Byte.toString(vType);
        } else {
            releaseType = String.valueOf("NEABRS".charAt(vType)); // NOREV-EXPERIMENTAL-ALPHA-BETA-RELEASE-SPECIAL
        }

        byte vBuild = (byte) ((version >> 8) & 0xFF);
        byte vMinor = (byte) ((version >> 16) & 0xFF);
        byte vMajor = (byte) ((version >> 24) & 0xFF);
        // Vmajor and Vminor = 0x12 should be printed as '12' and not '18' (BCD)
        return String.format("%c%c.%c%c.%02d.%s", '0' + (vMajor >> 4), '0' + (vMajor & 0x0F), '0' + (vMinor >> 4),
                '0' + (vMinor & 0x0F), vBuild, releaseType);
    }
}
