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

import java.io.IOException;

import javax.microedition.io.StreamConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothDebug extends Bluetooth {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothDebug.class);

    private StreamConnectionDebug connectionDebug = new StreamConnectionDebug();

    public BluetoothDebug() {
        super("00802515B606");
        SMAPPPFrame.AppSerial = 934043669;
    }

    @Override
    protected StreamConnection getConnection() throws IOException {
        return connectionDebug;
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

    private ReadCall[] calls;
    private int callInx = 0;
    private WriteCall[] writes;
    private int writeInx = 0;

    public void setDebugData(ReadCall[] calls0, WriteCall[] writes0) {
        calls = calls0;
        writes = writes0;

        for (ReadCall c : calls0) {
            connectionDebug.addReadData(c.data);
        }

        if (writes0 != null) {
            for (WriteCall c : writes0) {
                connectionDebug.addWriteData(c.data);
            }
        }
    }

    static int timeInx = 0;
    static int[] timeValues = { 1509799909, 1509799910, 1509799910, 1509799910, 1509799910, 1509799910 };

    @Override
    public int currentTimeSeconds() {
        return timeValues[timeInx++];
    }
}
