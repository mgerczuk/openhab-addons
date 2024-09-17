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
package org.openhab.binding.sma.internal.hardware.devices;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class SmaBluetoothAddress {
    public static final SmaBluetoothAddress BROADCAST = new SmaBluetoothAddress("FF:FF:FF:FF:FF:FF");

    // stores BT address in "little endian"
    private byte[] address;

    private int port;

    public SmaBluetoothAddress(String address, int port) {
        this.address = new byte[6];
        this.port = port;
        this.setBigEndianAddress(hexStringToByteArray(address.replaceAll(":", "").replaceAll(" ", "")), 0);
    }

    public SmaBluetoothAddress(byte[] address) {
        this.address = address;
    }

    public SmaBluetoothAddress() {
        this.address = new byte[] { 0, 0, 0, 0, 0, 0 };
    }

    public SmaBluetoothAddress(byte[] data, int start) {
        this.address = new byte[6];
        this.setAddress(data, start);
    }

    public SmaBluetoothAddress(String address) {
        this(address, 1);
    }

    public final void setAddress(byte[] src, int start) {
        address[0] = src[start + 0];
        address[1] = src[start + 1];
        address[2] = src[start + 2];
        address[3] = src[start + 3];
        address[4] = src[start + 4];
        address[5] = src[start + 5];
        // System.arraycopy(src, start, address, 0, 6);
    }

    public final void setBigEndianAddress(byte[] src, int start) {
        address[0] = src[start + 5];
        address[1] = src[start + 4];
        address[2] = src[start + 3];
        address[3] = src[start + 2];
        address[4] = src[start + 1];
        address[5] = src[start + 0];
    }

    public byte[] getAddress() {
        return address;
    }

    public byte get(int i) {
        return address[i];
    }

    @Override
    public String toString() {
        char[] hexChars = new char[17];
        int i = 0;
        for (int j = 5; j >= 0; j--) {
            int v = address[j] & 0xFF;
            hexChars[i++] = hexArray[(v & 0xF0) >>> 4];
            hexChars[i++] = hexArray[v & 0x0F];
            if (j > 0) {
                hexChars[i++] = ':';
            }
        }
        return new String(hexChars);
    }

    public String getConnectorString() {
        char[] hexChars = new char[12];
        int i = 0;
        for (int j = 5; j >= 0; j--) {
            int v = address[j] & 0xFF;
            hexChars[i++] = hexArray[v >>> 4];
            hexChars[i++] = hexArray[v & 0x0F];
        }
        return "btspp://" + (new String(hexChars)) + ":" + this.port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SmaBluetoothAddress) {
            SmaBluetoothAddress other = (SmaBluetoothAddress) obj;
            for (int i = 0; i < 6; i++) {
                if ((address[i] != other.address[i]) && (other.address[i] != (byte) 0xFF)
                        && (address[i] != (byte) 0xFF)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Convert an hex string into byte array
     *
     * @param s
     * @return byte array
     */
    protected byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
}
