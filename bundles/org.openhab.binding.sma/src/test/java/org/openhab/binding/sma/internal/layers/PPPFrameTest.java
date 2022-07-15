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

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * @author Martin Gerczuk - Initial contribution
 */
class PPPFrameTest {

    @Test
    void testRead() {
        InputStream is = new ByteArrayInputStream(new byte[] {

                0x7E, (byte) 0xFF, 0x03, 0x60, 0x65, 0x27, (byte) 0x80, 0x7D, 0x5D, 0x00, 0x35, (byte) 0xDB,
                (byte) 0xC6, 0x38, 0x00, (byte) 0xA0, 0x63, 0x00, (byte) 0xC5, 0x68, 0x49, 0x77, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x0B, (byte) 0x80, 0x01, 0x02, 0x00, 0x58, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00,
                0x01, 0x1E, (byte) 0x82, 0x10, 0x70, 0x5F, (byte) 0x87, 0x62, 0x53, 0x4E, 0x3A, 0x20, 0x32, 0x30, 0x30,
                0x31, 0x32, 0x39, 0x39, 0x36, 0x35, 0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x1F, (byte) 0x82, 0x08, 0x70, 0x5F, (byte) 0x87,
                0x62, 0x41, 0x1F, 0x00, 0x01, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x01, 0x20, (byte) 0x82, 0x08, 0x70, 0x5F, (byte) 0x87, 0x62, 0x44, 0x23, 0x00, 0x01,
                (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE,
                (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF,
                (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF,
                0x00, (byte) 0xF4, (byte) 0xCB, 0x7E });

        byte[] expected = new byte[] { 0x27, (byte) 0x80, 0x7D, 0x00, 0x35, (byte) 0xDB, (byte) 0xC6, 0x38, 0x00,
                (byte) 0xA0, 0x63, 0x00, (byte) 0xC5, 0x68, 0x49, 0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0B,
                (byte) 0x80, 0x01, 0x02, 0x00, 0x58, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x01, 0x1E,
                (byte) 0x82, 0x10, 0x70, 0x5F, (byte) 0x87, 0x62, 0x53, 0x4E, 0x3A, 0x20, 0x32, 0x30, 0x30, 0x31, 0x32,
                0x39, 0x39, 0x36, 0x35, 0x33, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x1F, (byte) 0x82, 0x08, 0x70, 0x5F, (byte) 0x87, 0x62, 0x41,
                0x1F, 0x00, 0x01, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x01, 0x20, (byte) 0x82, 0x08, 0x70, 0x5F, (byte) 0x87, 0x62, 0x44, 0x23, 0x00, 0x01, (byte) 0xFE,
                (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF,
                (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF,
                0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xFE, (byte) 0xFF, (byte) 0xFF, 0x00 };

        try {
            PPPFrame f = PPPFrame.read(is);

            assertEquals(-1, f.getAddress());
            assertEquals(3, f.getControl());
            assertEquals(0x6065, f.getProtocol());
            assertTrue(Arrays.areEqual(expected, f.getPayload()));

        } catch (IOException e) {
            fail(e.getMessage());
        }
    }
}
