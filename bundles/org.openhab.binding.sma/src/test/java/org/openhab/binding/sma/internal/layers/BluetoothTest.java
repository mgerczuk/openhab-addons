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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class BluetoothTest {

    @Test
    void testReceive1() {
        BluetoothDebug bt = new BluetoothDebug();

        bt.addReadData("7E 6A 00 14 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 33 90 7D 5D 00 35", //
                "DB C6 38 00 00 71 00 2D 38 2F", //
                "7D 5D 00 00 00 00 00 00 02 80", //
                "01 02 00 00 00 00 00 00 00 00", //
                "00 00 00 03 00 00 00 FF 00 00", //
                "80 07 00 60 01 00 71 00 2D 38", //
                "2F 7D 5D 00 00 0A 00 0C 00 00", //
                "00 00 00 00 00 03 00 00 00 01", //
                "01 00 00 5B 55 7E");
        byte[] expected = BluetoothDebug.stringToBytes("13 90 7D 00 35 DB C6 38 00 00 " + //
                "71 00 2D 38 2F 7D 00 00 00 00 " + //
                "00 00 02 80 01 02 00 00 00 00 " + //
                "00 00 00 00 00 00 00 03 00 00 " + //
                "00 FF 00 00 80 07 00 60 01 00 " + //
                "71 00 2D 38 2F 7D 00 00 0A 00 " + //
                "0C 00 00 00 00 00 00 00 03 00 " + //
                "00 00 01 01 00 00");
        try {
            bt.open();
            SMANetFrame frame = bt.receivePPPFrame((short) 2);

            assertTrue(Arrays.equals(expected, frame.getPayload()));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testReceive2() {
        BluetoothDebug.ReadCall[] read = new BluetoothDebug.ReadCall[] {
                new BluetoothDebug.ReadCall(18,
                        new byte[] { 0x7E, 0x6D, 0x00, 0x13, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x79,
                                (byte) 0xB6, 0x7E, 0x01, 0x5F, (byte) 0xE4, 0x08, 0x00 }),
                new BluetoothDebug.ReadCall(76,
                        new byte[] { 0x7E, (byte) 0xFF, 0x03, 0x60, 0x65, 0x27, (byte) 0x90, 0x7D, 0x5D, 0x00, 0x35,
                                (byte) 0xDB, (byte) 0xC6, 0x38, 0x00, (byte) 0xA0, 0x71, 0x00, 0x2D, 0x38, 0x2F, 0x7D,
                                0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0B, (byte) 0x80, 0x01, 0x02, 0x00, 0x58,
                                0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x1E, (byte) 0x82, 0x10,
                                (byte) 0xA6, 0x7D, 0x32, (byte) 0x87, 0x62, 0x53, 0x4E, 0x3A, 0x20, 0x32, 0x31, 0x30,
                                0x30, 0x32, 0x34, 0x36, 0x35, 0x37, 0x33, 0x00, 0x00, (byte) 0xE6, 0x00, 0x00, 0x00,
                                (byte) 0xE6, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01,
                                0x1F, (byte) 0x82, 0x08, (byte) 0xA6, 0x7D, 0x32 }),
                new BluetoothDebug.ReadCall(18,
                        new byte[] { 0x7E, 0x60, 0x00, 0x1E, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x79,
                                (byte) 0xB6, 0x7E, 0x01, 0x5F, (byte) 0xE4, 0x01, 0x00 }),
                new BluetoothDebug.ReadCall(78,
                        new byte[] { (byte) 0x87, 0x62, 0x41, 0x1F, 0x00, 0x01, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF,
                                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x20,
                                (byte) 0x82, 0x08, (byte) 0xA6, 0x7D, 0x32, (byte) 0x87, 0x62, 0x2E, 0x02, 0x00, 0x01,
                                (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                                0x00, 0x00, 0x00, (byte) 0xB2, (byte) 0x8B, 0x7E }) };

        byte[] expected = new byte[] { 0x7E, (byte) 0xFF, 0x03, 0x60, 0x65, 0x27, (byte) 0x90, 0x7D, 0x00, 0x35,
                (byte) 0xDB, (byte) 0xC6, 0x38, 0x00, (byte) 0xA0, 0x71, 0x00, 0x2D, 0x38, 0x2F, 0x7D, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x0B, (byte) 0x80, 0x01, 0x02, 0x00, 0x58, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00,
                0x00, 0x01, 0x1E, (byte) 0x82, 0x10, (byte) 0xA6, 0x12, (byte) 0x87, 0x62, 0x53, 0x4E, 0x3A, 0x20, 0x32,
                0x31, 0x30, 0x30, 0x32, 0x34, 0x36, 0x35, 0x37, 0x33, 0x00, 0x00, (byte) 0xE6, 0x00, 0x00, 0x00,
                (byte) 0xE6, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x1F, (byte) 0x82,
                0x08, (byte) 0xA6, 0x12, (byte) 0x87, 0x62, 0x41, 0x1F, 0x00, 0x01, (byte) 0xFE, (byte) 0xFF,
                (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x20, (byte) 0x82, 0x08, (byte) 0xA6,
                0x12, (byte) 0x87, 0x62, 0x2E, 0x02, 0x00, 0x01, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0xB2, (byte) 0x8B, 0x7E };

        BluetoothDebug bt = new BluetoothDebug();
        bt.setDebugData(read, null);
        try {
            bt.open();
            SMANetFrame frame = bt.receivePPPFrame((short) 11);

            assertArrayEquals(expected, frame.getFrame());
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
