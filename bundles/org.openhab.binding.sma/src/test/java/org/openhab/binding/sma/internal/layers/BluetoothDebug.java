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

import javax.microedition.io.StreamConnection;

import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothDebug extends Bluetooth {

    private StreamConnectionDebug connectionDebug = new StreamConnectionDebug();

    public BluetoothDebug() {
        super(new SmaBluetoothAddress("00802515B606", 1));
    }

    @Override
    protected StreamConnection getConnection() throws IOException {
        return connectionDebug;
    }

    public void addReadData(byte[] data) {
        connectionDebug.addReadData(data);
    }

    public void addReadData(String... data) {
        for (String s : data) {
            addReadData(stringToBytes(s));
        }
    }

    public void addWriteData(byte[] data) {
        connectionDebug.addWriteData(data);
    }

    public void addWriteData(String... data) {
        for (String s : data) {
            addWriteData(stringToBytes(s));
        }
    }

    public static byte[] stringToBytes(String data) {
        String[] digits = data.split("\\s+");
        byte[] result = new byte[digits.length];
        for (int i = 0; i < digits.length; i++) {
            result[i] = (byte) Integer.parseInt(digits[i], 16);
        }
        return result;
    }

    public static class ReadCall {
        int result;
        byte[] data;

        public ReadCall(int result, byte[] data) {
            this.result = result;
            this.data = data;
        }
    }

    public static class WriteCall {
        byte[] data;

        public WriteCall(byte[] data) {
            this.data = data;
        }
    }

    public void setDebugData(ReadCall[] calls, WriteCall[] writes) {
        for (ReadCall c : calls) {
            connectionDebug.addReadData(c.data);
        }

        if (writes != null) {
            for (WriteCall c : writes) {
                connectionDebug.addWriteData(c.data);
            }
        }
    }

    int timeInx = 0;
    int[] timeValues;

    public void setCurrentTimeSeconds(int... v) {
        timeValues = v;
        timeInx = 0;
    }

    @Override
    public int currentTimeSeconds() {
        return timeValues[timeInx++];
    }

    int timezoneOffset = 3600;

    public void setTimezoneOffset(int v) {
        timezoneOffset = v;
    }

    @Override
    public int getTimezoneOffset() {
        return timezoneOffset;
    }
}
