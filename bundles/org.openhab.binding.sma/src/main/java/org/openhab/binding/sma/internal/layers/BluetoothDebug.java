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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothDebug extends Bluetooth {
    private static final Logger logger = LoggerFactory.getLogger(BluetoothDebug.class);

    public BluetoothDebug() {
        super("00802515B606");
        AppSerial = 934043669;
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() {
    }

    @Override
    public void send() throws IOException {
        writePacketLength();
        logger.debug("\n{}\n{} Bytes sent", bytesToHex(buffer, packetposition, ' '), packetposition);
    }

    @Override
    protected int read(byte[] buf, int offset, int len) throws IOException {
        ReadCall c = calls[callInx++];
        System.arraycopy(c.data, 0, buf, offset, c.data.length);
        if (c.result < 0) {
            throw new IOException("Timeout reading socket");
        }

        logger.debug("\nReceived {} bytes", len);
        return c.result;
    }

    public static class ReadCall {
        int result;
        byte[] data;

        public ReadCall(int result, byte[] data) {
            this.result = result;
            this.data = data;
        }
    }

    private static ReadCall[] calls;
    private static int callInx = 0;

    public static void setDebugData(ReadCall[] calls0) {
        calls = calls0;
    }

    static int timeInx = 0;
    static int[] timeValues = { 1509799909, 1509799910, 1509799910, 1509799910, 1509799910, 1509799910 };

    @Override
    public int currentTimeSeconds() {
        return timeValues[timeInx++];
    }
}
