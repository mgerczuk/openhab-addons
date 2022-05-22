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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.openhab.binding.sma.internal.SmaBinding;
import org.openhab.binding.sma.internal.hardware.devices.BluetoothSolarInverterPlant.Data;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.SmaUserGroup;
import org.openhab.binding.sma.internal.layers.BluetoothDebug;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothSolarInverterPlantTest {
    @Test
    void testGetData() {
        BluetoothDebug bt = new BluetoothDebug();
        BluetoothSolarInverterPlant plant = new BluetoothSolarInverterPlant(
                new SmaBinding().createDevice("00:80:25:15:B6:06", "0000"));

        // init() -----------------------------------------------------------
        // query SMA Net ID
        bt.addWriteData(new byte[] { 0x7E, 0x17, 0x00, 0x69, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x01, 0x02, 0x76, 0x65, 0x72, 0x0D, 0x0A });
        // data = layer.receive(0x02);
        bt.addReadData(new byte[] { 0x7E, 0x1F, 0x00, 0x61, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x04, 0x70, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00 });

        // check root device Address
        bt.addWriteData(new byte[] { 0x7E, 0x1F, 0x00, 0x61, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x06, (byte) 0xB6,
                0x15, 0x25, (byte) 0x80, 0x00, 0x02, 0x00, 0x00, 0x04, 0x70, 0x00, 0x04, 0x00, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00 });
        // Connection to Root Device
        // data = layer.receive(0x0A);
        bt.addReadData(new byte[] { 0x7E, 0x1F, 0x00, 0x61, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x01, 0x3C,
                0x40, (byte) 0xB8, (byte) 0xEB, 0x27, (byte) 0xB8 });

        // data = layer.receive(0x05);
        bt.addReadData(new byte[] { 0x7E, 0x14, 0x00, 0x6A, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x00, //
                0x02, 0x00 });
        bt.addReadData(new byte[] { 0x7E, 0x2A, 0x00, 0x54, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x00, //
                0x54, 0x2D, 0x15, 0x25, (byte) 0x80, 0x00, 0x01, 0x01, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00,
                0x01, 0x01, 0x3C, 0x40, (byte) 0xB8, (byte) 0xEB, 0x27, (byte) 0xB8, 0x02, 0x01 });

        // Send broadcast request for identification
        bt.addWriteData(new byte[] { 0x7E, 0x3F, 0x00, 0x41, 0x3C, 0x40, (byte) 0xB8, (byte) 0xEB, 0x27, (byte) 0xB8,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x01, 0x00, 0x7E,
                (byte) 0xFF, 0x03, 0x60, 0x65, 0x09, (byte) 0xA0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x7D, 0x5D, 0x00, 0x15, 0x60, (byte) 0xAC, 0x37, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x02, (byte) 0x80, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, (byte) 0xDE, 0x6B, 0x7E });

        // All inverters *should* reply with their SUSyID & SerialNr
        // (and some other unknown info)
        // data = layer.receiveAll(0x01);
        bt.addReadData(new byte[] { 0x7E, 0x12, 0x00, 0x6C, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x06, 0x00 });

        bt.addReadData(new byte[] { 0x7E, 0x30, 0x00, 0x4E, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x10, //
                0x54, 0x2D, 0x15, 0x25, (byte) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

        bt.addReadData(new byte[] { 0x7E, 0x6A, 0x00, 0x14, 0x06, (byte) 0xB6, 0x15, 0x25, (byte) 0x80, 0x00, 0x3C,
                0x40, (byte) 0xB8, (byte) 0xEB, 0x27, (byte) 0xB8, 0x01, 0x00, //
                0x7E, (byte) 0xFF, 0x03, 0x60, 0x65, 0x7D, 0x33, (byte) 0x90, 0x7D, 0x5D, 0x00, 0x15, 0x60, (byte) 0xAC,
                0x37, 0x00, 0x00, 0x71, 0x00, 0x2D, 0x38, 0x2F, 0x7D, 0x5D, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                (byte) 0x80, 0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00,
                0x00, 0x00, (byte) 0xFF, 0x00, 0x00, (byte) 0x80, 0x07, 0x00, 0x60, 0x01, 0x00, 0x71, 0x00, 0x2D, 0x38,
                0x2F, 0x7D, 0x5D, 0x00, 0x00, 0x0A, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00,
                0x00, 0x00, 0x01, 0x01, 0x00, 0x00, (byte) 0xC6, 0x0F, 0x7E });

        // [1]
        bt.addReadData(new byte[] { 0x7E, 0x69, 0x00, 0x17, 0x54, 0x2D, 0x15, 0x25, (byte) 0x80, 0x00, 0x3C, 0x40,
                (byte) 0xB8, (byte) 0xEB, 0x27, (byte) 0xB8, 0x01, 0x00, //
                0x7E, (byte) 0xFF, 0x03, 0x60, 0x65, 0x7D, 0x33, (byte) 0x80, 0x7D, 0x5D, 0x00, 0x15, 0x60, (byte) 0xAC,
                0x37, 0x00, 0x00, 0x63, 0x00, (byte) 0xC5, 0x68, 0x49, 0x77, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02,
                (byte) 0x80, 0x01, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00,
                0x00, 0x00, (byte) 0xFF, 0x00, 0x00, 0x58, 0x07, 0x00, 0x00, 0x01, 0x00, 0x63, 0x00, (byte) 0xC5, 0x68,
                0x49, 0x77, 0x00, 0x00, 0x0A, 0x00, 0x0C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00,
                0x00, 0x01, 0x01, 0x00, 0x00, (byte) 0xE8, 0x7D, 0x33, 0x7E });

        // logoff
        bt.addWriteData(new byte[] { 0x7E, 0x3B, 0x00, 0x45, 0x3C, 0x40, (byte) 0xB8, (byte) 0xEB, 0x27, (byte) 0xB8,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x01, 0x00, 0x7E,
                (byte) 0xFF, 0x03, 0x60, 0x65, 0x08, (byte) 0xA0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, 0x00, 0x03, 0x7D, 0x5D, 0x00, 0x15, 0x60, (byte) 0xAC, 0x37, 0x00, 0x03, 0x00,
                0x00, 0x00, 0x00, 0x03, (byte) 0x80, 0x0E, 0x01, (byte) 0xFD, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, 0x4E, (byte) 0xCD, 0x7E });

        // logon() ----------------------------------------------------------
        bt.addWriteData( //
                "7E 53 00 2D 3C 40 B8 EB 27 B8", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 0E A0 FF FF FF FF FF", //
                "FF 00 01 7D 5D 00 15 60 AC 37", //
                "00 01 00 00 00 00 04 80 0C 04", //
                "FD FF 07 00 00 00 84 03 00 00", //
                "E5 B7 FD 59 00 00 00 00 B8 B8", //
                "B8 B8 88 88 88 88 88 88 88 88", //
                "9E 64 7E");
        bt.addReadData(//
                "7E 54 00 2A 06 B6 15 25 80 00", //
                "3C 40 B8 EB 27 B8 01 00 7E FF", //
                "03 60 65 0E 50 7D 5D 00 15 60", //
                "AC 37 00 01 71 00 2D 38 2F 7D", //
                "5D 00 01 00 00 00 00 04 80 0D", //
                "04 FD FF 07 00 00 00 84 03 00", //
                "00 E5 B7 FD 59 00 00 00 00 B8", //
                "B8 B8 B8 88 88 88 88 88 88 88", //
                "88 F2 09 7E");
        bt.addReadData(//
                "7E 47 00 39 54 2D 15 25 80 00", //
                "3C 40 B8 EB 27 B8 01 00 7E FF", //
                "03 60 65 0B 80 7D 5D 00 15 60", //
                "AC 37 00 01 63 00 C5 68 49 77", //
                "00 01 00 00 00 00 04 80 0D 04", //
                "FD FF 07 00 00 00 84 03 00 00", //
                "E5 B7 FD 59 00 00 00 00 08 F1", //
                "7E");

        // setInverterTime() ------------------------------------------------
        bt.addWriteData( //
                "7E 5B 00 25 3C 40 B8 EB 27 B8", //
                "06 B6 15 25 80 00 01 00 7E FF", //
                "03 60 65 10 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 15 60 AC 37", //
                "00 00 00 00 00 00 05 80 0A 02", //
                "00 F0 00 6D 23 00 00 6D 23 00", //
                "00 6D 23 00 E6 B7 FD 59 E6 B7", //
                "FD 59 E6 B7 FD 59 10 0E 00 00", //
                "01 00 00 00 01 00 00 00 28 E0", //
                "7E");

        // getInverterData(SmaDevice.InverterDataType.SoftwareVersion = 2048)

        bt.addWriteData( //
                "7E 3F 00 41 3C 40 B8 EB 27 B8", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 15 60 AC 37", //
                "00 00 00 00 00 00 06 80 00 02", //
                "00 58 00 34 82 00 FF 34 82 00", //
                "F3 D5 7E");
        bt.addReadData(//
                "7E 6A 00 14 54 2D 15 25 80 00", //
                "3C 40 B8 EB 27 B8 01 00 7E FF", //
                "03 60 65 7D 33 80 7D 5D 00 15", //
                "60 AC 37 00 A0 63 00 C5 68 49", //
                "77 00 00 00 00 00 00 06 80 01", //
                "02 00 58 05 00 00 00 05 00 00", //
                "00 01 34 82 00 FE B7 FD 59 00", //
                "00 00 00 00 00 00 00 FE FF FF", //
                "FF FE FF FF FF 04 7A 09 7D 32", //
                "04 7A 09 7D 32 00 00 00 00 00", //
                "00 00 00 60 6F 7E");
        bt.addReadData(// HACK: repeat last frame...
                "7E 6A 00 14 54 2D 15 25 80 00", //
                "3C 40 B8 EB 27 B8 01 00 7E FF", //
                "03 60 65 7D 33 80 7D 5D 00 15", //
                "60 AC 37 00 A0 63 00 C5 68 49", //
                "77 00 00 00 00 00 00 06 80 01", //
                "02 00 58 05 00 00 00 05 00 00", //
                "00 01 34 82 00 FE B7 FD 59 00", //
                "00 00 00 00 00 00 00 FE FF FF", //
                "FF FE FF FF FF 04 7A 09 7D 32", //
                "04 7A 09 7D 32 00 00 00 00 00", //
                "00 00 00 60 6F 7E");

        bt.setCurrentTimeSeconds(1509799909, 1509799910, 1509799910, 1509799910, 1509799910, 1509799910);
        bt.setTimezoneOffset(3600);

        try {
            plant.init(bt);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        ArrayList<Data> inverters = plant.getInverters();
        assertEquals(2, inverters.size());

        assertEquals(4, inverters.get(0).getNetID());
        assertEquals("00:80:25:15:2D:54", inverters.get(0).getBTAddressAsString());
        assertEquals("SmaSerial [suSyID=99, serial=2001299653]", inverters.get(0).getSerial().toString());

        assertEquals(4, inverters.get(1).getNetID());
        assertEquals("00:80:25:15:B6:06", inverters.get(1).getBTAddressAsString());
        assertEquals("SmaSerial [suSyID=113, serial=2100246573]", inverters.get(1).getSerial().toString());

        try {
            plant.logon(SmaUserGroup.User, "0000");
            plant.setInverterTime();
            plant.getInverterData(SmaDevice.InverterDataType.SoftwareVersion);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    static BluetoothDebug.ReadCall[] concat(BluetoothDebug.ReadCall[]... arrays) {
        int length = 0;
        for (BluetoothDebug.ReadCall[] array : arrays) {
            length += array.length;
        }
        BluetoothDebug.ReadCall[] result = new BluetoothDebug.ReadCall[length];
        int pos = 0;
        for (BluetoothDebug.ReadCall[] array : arrays) {
            for (BluetoothDebug.ReadCall element : array) {
                result[pos] = element;
                pos++;
            }
        }
        return result;
    }
}
