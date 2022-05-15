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
import java.util.Random;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class SMAPPPFrame {

    public static final byte CONTROL = 0x03;
    public static final short PROTOCOL = 0x6065;

    // Generate a Serial Number for application
    public final static short AppSUSyID = 125;
    public static int AppSerial = generateAppSerial();

    public static int generateAppSerial() {
        Random randomGenerator = new Random();
        return 900000000 + randomGenerator.nextInt(100000000);
    }

    public static LittleEndianByteArrayOutputStream writePppHeader(byte longwords, byte ctrl, short ctrl2,
            short dstSUSyID, int dstSerial, short pcktID) throws IOException {

        LittleEndianByteArrayOutputStream b = new LittleEndianByteArrayOutputStream();

        b.writeByte(longwords);
        b.writeByte(ctrl);
        b.writeShort(dstSUSyID);
        b.writeInt(dstSerial);
        b.writeShort(ctrl2);
        b.writeShort(AppSUSyID);
        b.writeInt(AppSerial);
        b.writeShort(ctrl2);
        b.writeShort((short) 0);
        b.writeShort((short) 0);
        b.writeShort((short) (pcktID | 0x8000));

        return b;
    }
}
