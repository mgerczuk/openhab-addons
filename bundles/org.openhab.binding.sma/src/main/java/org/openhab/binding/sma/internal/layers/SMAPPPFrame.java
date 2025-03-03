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
import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class SMAPPPFrame {

    public static final byte CONTROL = 0x03;
    public static final short PROTOCOL = 0x6065;

    // Generate a Serial Number for application
    public static final short APP_SUSY_ID = 125;
    public static int appSerial = generateAppSerial();

    public static final short ANYSUSYID = (short) 0xFFFF;
    public static final int ANYSERIAL = 0xFFFFFFFF;

    private static int generateAppSerial() {
        Random randomGenerator = new Random();
        return 900000000 + randomGenerator.nextInt(100000000);
    }

    public static BinaryOutputStream writePppHeader(byte longwords, byte ctrl, short ctrl2, short dstSUSyID,
            int dstSerial, short pcktID) throws IOException {
        BinaryOutputStream b = new BinaryOutputStream();

        b.writeByte(longwords);
        b.writeByte(ctrl);
        b.writeShort(dstSUSyID);
        b.writeInt(dstSerial);
        b.writeShort(ctrl2);
        b.writeShort(APP_SUSY_ID);
        b.writeInt(appSerial);
        b.writeShort(ctrl2);
        b.writeShort(0);
        b.writeShort(0);
        b.writeShort(pcktID | 0x8000);

        return b;
    }
}
