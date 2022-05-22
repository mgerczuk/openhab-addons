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
import org.openhab.binding.sma.internal.layers.SMAPPPFrame;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothSolarInverterPlantTest {

    @Test
    void testGetData0() {
        BluetoothDebug bt = new BluetoothDebug();
        SMAPPPFrame.AppSerial = 0x38C6DB35;
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
        bt.addWriteData( //
                "7E 1F 00 61 00 00 00 00 00 00", //
                "06 B6 15 25 80 00 02 00 00 04", //
                "70 00 04 00 00 00 00 01 00 00", //
                "00");
        // Connection to Root Device
        // data = layer.receive(0x0A);
        bt.addReadData( //
                "7E 1F 00 61 06 B6 15 25 80 00", //
                "00 00 00 00 00 00 0A 00 06 B6", //
                "15 25 80 00 01 79 B6 7E 01 5F", //
                "E4");

        // data = layer.receive(0x05);
        bt.addReadData( //
                "7E 14 00 6A 06 B6 15 25 80 00", //
                "00 00 00 00 00 00 0C 00 02 00");
        bt.addReadData( //
                "7E 2A 00 54 06 B6 15 25 80 00", //
                "00 00 00 00 00 00 05 00 54 2D", //
                "15 25 80 00 01 01 06 B6 15 25", //
                "80 00 01 01 79 B6 7E 01 5F E4", //
                "02 01");

        // Send broadcast request for identification
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 02 80 00 02", //
                "00 00 00 00 00 00 00 00 00 00", //
                "61 9B 7E");

        // All inverters *should* reply with their SUSyID & SerialNr
        // (and some other unknown info)
        // data = layer.receiveAll(0x01);
        bt.addReadData( //
                "7E 30 00 4E 06 B6 15 25 80 00", //
                "00 00 00 00 00 00 01 10 54 2D", //
                "15 25 80 00 00 00 00 00 00 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "00 00 00 00 00 00 00 00");

        bt.addReadData( //
                "7E 6A 00 14 06 B6 15 25 80 00", //
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

        // [1]
        bt.addReadData( //
                "7E 68 00 16 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 33 80 7D 5D 00 35", //
                "DB C6 38 00 00 63 00 C5 68 49", //
                "77 00 00 00 00 00 00 02 80 01", //
                "02 00 00 00 00 00 00 00 00 00", //
                "00 00 03 00 00 00 FF 00 00 58", //
                "07 00 00 01 00 63 00 C5 68 49", //
                "77 00 00 0A 00 0C 00 00 00 00", //
                "00 00 00 03 00 00 00 01 01 00", //
                "00 75 49 7E");

        // logoff
        bt.addWriteData( //
                "7E 3B 00 45 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 08 A0 FF FF FF FF FF", //
                "FF 00 03 7D 5D 00 35 DB C6 38", //
                "00 03 00 00 00 00 03 80 0E 01", //
                "FD FF FF FF FF FF 46 54 7E");

        bt.setCurrentTimeSeconds(0x62875F1E, 0x62875F1E);

        // logon() ----------------------------------------------------------
        bt.addWriteData( //
                "7E 53 00 2D 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 0E A0 FF FF FF FF FF", //
                "FF 00 01 7D 5D 00 35 DB C6 38", //
                "00 01 00 00 00 00 04 80 0C 04", //
                "FD FF 07 00 00 00 84 03 00 00", //
                "1E 5F 87 62 00 00 00 00 B8 B8", // 0-3 = time
                "B8 B8 88 88 88 88 88 88 88 88", //
                "73 DF 7E");
        bt.addReadData(//
                "7E 54 00 2A 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 0E 50 7D 5D 00 35 DB", //
                "C6 38 00 01 71 00 2D 38 2F 7D", //
                "5D 00 01 00 00 00 00 04 80 0D", //
                "04 FD FF 07 00 00 00 84 03 00", //
                "00 1E 5F 87 62 00 00 00 00 B8", //
                "B8 B8 B8 88 88 88 88 88 88 88", //
                "88 4A 4B 7E");
        bt.addReadData(//
                "7E 47 00 39 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 0B 80 7D 5D 00 35 DB", //
                "C6 38 00 01 63 00 C5 68 49 77", //
                "00 01 00 00 00 00 04 80 0D 04", //
                "FD FF 07 00 00 00 84 03 00 00", //
                "1E 5F 87 62 00 00 00 00 58 F1", //
                "7E");

        bt.setTimezoneOffset(7200);

        // setInverterTime() ------------------------------------------------
        bt.addWriteData( //
                "7E 5B 00 25 79 B6 7E 01 5F E4", //
                "06 B6 15 25 80 00 01 00 7E FF", //
                "03 60 65 10 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 05 80 0A 02", //
                "00 F0 00 6D 23 00 00 6D 23 00", //
                "00 6D 23 00 1E 5F 87 62 1E 5F", //
                "87 62 1E 5F 87 62 20 1C 00 00", //
                "01 00 00 00 01 00 00 00 F1 FA", //
                "7E");

        // getInverterData(SpotACTotalPower)
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 06 80 00 02", //
                "00 51 00 3F 26 00 FF 3F 26 00", //
                "AE 10 7E");
        bt.addReadData(//
                "7E 5B 00 25 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 10 80 7D 5D 00 35 DB", //
                "C6 38 00 A0 63 00 C5 68 49 77", //
                "00 00 00 00 00 00 06 80 01 02", //
                "00 51 00 00 00 00 00 00 00 00", //
                "01 3F 26 40 33 5F 87 62 E8 09", //
                "00 00 E8 09 00 00 E8 09 00 00", //
                "E8 09 00 00 01 00 00 00 31 32", //
                "7E");
        bt.addReadData(// Packet ID mismatch
                "7E 54 00 2A 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 0E 50 7D 5D 00 35 DB", //
                "C6 38 00 01 71 00 2D 38 2F 7D", //
                "5D 00 01 00 00 00 00 04 80 0D", //
                "04 FD FF 07 00 00 00 84 03 00", //
                "00 1E 5F 87 62 00 00 00 00 B8", //
                "B8 B8 B8 88 88 88 88 88 88 88", //
                "88 4A 4B 7E");
        bt.addReadData(// Packet ID mismatch
                "7E 5E 00 20 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 10 50 7D 5D 00 35 DB", //
                "C6 38 00 A0 71 00 2D 38 2F 7D", //
                "5D 00 00 00 00 00 00 EF FA 0A", //
                "02 00 F0 00 00 00 00 00 00 00", //
                "00 00 6D 23 00 9E 61 87 62 EC", //
                "00 D9 52 9E 61 87 62 7D 31 0E", //
                "00 00 30 FE 7D 5E 00 01 00 00", //
                "00 E4 C5 7E");
        bt.addReadData(//
                "7E 5C 00 22 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 10 90 7D 5D 00 35 DB", //
                "C6 38 00 A0 71 00 2D 38 2F 7D", //
                "5D 00 00 00 00 00 00 06 80 01", //
                "02 00 51 00 00 00 00 00 00 00", //
                "00 01 3F 26 40 D9 61 87 62 BE", //
                "07 00 00 BE 07 00 00 BE 07 00", //
                "00 BE 07 00 00 01 00 00 00 B7", //
                "B6 7E");

        // getInverterData(SpotACVoltage)
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 07 80 00 02", //
                "00 51 00 48 46 00 FF 55 46 00", //
                "C3 2C 7E");
        bt.addReadData(// 1/3
                "7E 6D 00 13 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 08 00 7E FF", //
                "03 60 65 33 90 7D 5D 00 35 DB", //
                "C6 38 00 A0 71 00 2D 38 2F 7D", //
                "5D 00 00 00 00 00 00 07 80 01", //
                "02 00 51 09 00 00 00 0E 00 00", //
                "00 01 48 46 00 D9 61 87 62 EA", //
                "5C 00 00 EA 5C 00 00 EA 5C 00", //
                "00 EA 5C 00 00 01 00 00 00 01", //
                "49 46 00 D9 61 87 62 FF FF FF", //
                "FF FF FF FF FF FF FF FF FF");
        bt.addReadData(// 2/3
                "7E 6D 00 13 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 08 00 FF FF", //
                "FF FF 01 00 00 00 01 4A 46 00", //
                "D9 61 87 62 FF FF FF FF FF FF", //
                "FF FF FF FF FF FF FF FF FF FF", //
                "01 00 00 00 01 50 46 00 D9 61", //
                "87 62 8F 20 00 00 8F 20 00 00", //
                "8F 20 00 00 8F 20 00 00 01 00", //
                "00 00 01 51 46 00 D9 61 87 62", //
                "FF FF FF FF FF FF FF FF FF FF", //
                "FF FF FF FF FF FF 01 00 00");
        bt.addReadData(// 3/3
                "7E 32 00 4C 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 00 01", //
                "52 46 00 D9 61 87 62 FF FF FF", //
                "FF FF FF FF FF FF FF FF FF FF", //
                "FF FF FF 01 00 00 00 4B EF 7E");
        bt.addReadData(//
                "7E 6D 00 13 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 08 00 7E FF", //
                "03 60 65 17 80 7D 5D 00 35 DB", //
                "C6 38 00 A0 63 00 C5 68 49 77", //
                "00 00 00 00 00 00 07 80 01 02", //
                "00 51 04 00 00 00 05 00 00 00", //
                "01 48 46 00 6F 5F 87 62 F8 5C", //
                "00 00 F8 5C 00 00 F8 5C 00 00", //
                "F8 5C 00 00 01 00 00 00 01 50", //
                "46 00 6F 5F 87 62 69 29 00 00", //
                "69 29 00 00 69 29 00 00 69");
        bt.addReadData(//
                "7E 1C 00 62 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 29 00", //
                "00 01 00 00 00 1F 94 7E");

        // getInverterData(EnergyProduction)
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 08 80 00 02", //
                "00 54 00 01 26 00 FF 22 26 00", //
                "39 A9 7E");
        bt.addReadData(//
                "7E 61 00 1F 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 31 90 7D 5D 00 35", //
                "DB C6 38 00 A0 71 00 2D 38 2F", //
                "7D 5D 00 00 00 00 00 00 08 80", //
                "01 02 00 54 00 00 00 00 01 00", //
                "00 00 01 01 26 00 D9 61 87 62", //
                "85 61 65 02 00 00 00 00 01 22", //
                "26 00 D7 61 87 62 DC 10 00 00", //
                "00 00 00 00 D5 6E 7E");
        bt.addReadData(//
                "7E 61 00 1F 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 31 80 7D 5D 00 35", //
                "DB C6 38 00 A0 63 00 C5 68 49", //
                "77 00 00 00 00 00 00 08 80 01", //
                "02 00 54 00 00 00 00 01 00 00", //
                "00 01 01 26 00 70 5F 87 62 D3", //
                "79 7D 32 03 00 00 00 00 01 22", //
                "26 00 70 5F 87 62 C1 15 00 00", //
                "00 00 00 00 3E 21 7E");

        // getInverterData(MaxACPower)
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 09 80 00 02", //
                "00 51 00 1E 41 00 FF 20 41 00", //
                "73 0F 7E");
        bt.addReadData(//
                "7E 6D 00 13 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 08 00 7E FF", //
                "03 60 65 1E 90 7D 5D 00 35 DB", //
                "C6 38 00 A0 71 00 2D 38 2F 7D", //
                "5D 00 00 00 00 00 00 09 80 01", //
                "02 00 51 01 00 00 00 03 00 00", //
                "00 01 1E 41 00 D7 61 87 62 B8", //
                "0B 00 00 B8 0B 00 00 B8 0B 00", //
                "00 B8 0B 00 00 01 00 00 00 01", //
                "1F 41 00 D7 61 87 62 B8 0B 00", //
                "00 B8 0B 00 00 00 00 00 00");
        bt.addReadData(//
                "7E 39 00 47 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 B8 0B", //
                "00 00 00 00 00 00 01 20 41 00", //
                "D7 61 87 62 B8 0B 00 00 B8 0B", //
                "00 00 00 00 00 00 B8 0B 00 00", //
                "00 00 00 00 8F 45 7E");
        bt.addReadData(//
                "7E 6D 00 13 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 08 00 7E FF", //
                "03 60 65 1E 80 7D 5D 00 35 DB", //
                "C6 38 00 A0 63 00 C5 68 49 77", //
                "00 00 00 00 00 00 09 80 01 02", //
                "00 51 01 00 00 00 03 00 00 00", //
                "01 1E 41 00 70 5F 87 62 10 0E", //
                "00 00 10 0E 00 00 10 0E 00 00", //
                "10 0E 00 00 01 00 00 00 01 1F", //
                "41 00 70 5F 87 62 10 0E 00 00", //
                "10 0E 00 00 00 00 00 00 10");
        bt.addReadData(//
                "7E 38 00 46 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 0E 00", //
                "00 00 00 00 00 01 20 41 00 70", //
                "5F 87 62 10 0E 00 00 10 0E 00", //
                "00 00 00 00 00 10 0E 00 00 00", //
                "00 00 00 F8 D8 7E");

        // getInverterData(DeviceStatus)
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 0A 80 00 02", //
                "80 51 00 48 21 00 FF 48 21 00", //
                "D2 25 7E");
        bt.addReadData(//
                "7E 69 00 17 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 33 90 7D 5D 00 35", //
                "DB C6 38 00 A0 71 00 2D 38 2F", //
                "7D 5D 00 00 00 00 00 00 0A 80", //
                "01 02 80 51 00 00 00 00 00 00", //
                "00 00 01 48 21 08 D7 61 87 62", //
                "23 00 00 00 2F 01 00 00 33 01", //
                "00 01 C7 01 00 00 FE FF FF 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "00 00 3F BC 7E");
        bt.addReadData(//
                "7E 68 00 16 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 33 80 7D 5D 00 35", //
                "DB C6 38 00 A0 63 00 C5 68 49", //
                "77 00 00 00 00 00 00 0A 80 01", //
                "02 80 51 00 00 00 00 00 00 00", //
                "00 01 48 21 08 70 5F 87 62 33", //
                "01 00 01 FE FF FF 00 FE FF FF", //
                "00 FE FF FF 00 FE FF FF 00 FE", //
                "FF FF 00 FE FF FF 00 FE FF FF", //
                "00 D5 AA 7E");

        // getInverterData(TypeLabel)
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 0B 80 00 02", //
                "00 58 00 1E 82 00 FF 20 82 00", //
                "5C 2B 7E");
        bt.addReadData(//
                "7E 6D 00 13 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 08 00 7E FF", //
                "03 60 65 27 90 7D 5D 00 35 DB", //
                "C6 38 00 A0 71 00 2D 38 2F 7D", //
                "5D 00 00 00 00 00 00 0B 80 01", //
                "02 00 58 00 00 00 00 02 00 00", //
                "00 01 1E 82 10 A6 7D 32 87 62", //
                "53 4E 3A 20 32 31 30 30 32 34", //
                "36 35 37 33 00 00 E6 00 00 00", //
                "E6 00 00 00 00 00 00 00 00 00", //
                "00 00 01 1F 82 08 A6 7D 32");
        bt.addReadData(//
                "7E 60 00 1E 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 87 62", //
                "41 1F 00 01 FE FF FF 00 00 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "00 00 01 20 82 08 A6 7D 32 87", //
                "62 2E 02 00 01 FE FF FF 00 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "00 00 00 B2 8B 7E");
        bt.addReadData(//
                "7E 6D 00 13 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 08 00 7E FF", //
                "03 60 65 27 80 7D 5D 00 35 DB", //
                "C6 38 00 A0 63 00 C5 68 49 77", //
                "00 00 00 00 00 00 0B 80 01 02", //
                "00 58 00 00 00 00 02 00 00 00", //
                "01 1E 82 10 70 5F 87 62 53 4E", //
                "3A 20 32 30 30 31 32 39 39 36", //
                "35 33 00 00 00 00 00 00 00 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "01 1F 82 08 70 5F 87 62 41");
        bt.addReadData(//
                "7E 5C 00 22 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 1F 00", //
                "01 FE FF FF 00 00 00 00 00 00", //
                "00 00 00 00 00 00 00 00 00 00", //
                "00 00 00 00 00 00 00 00 00 01", //
                "20 82 08 70 5F 87 62 44 23 00", //
                "01 FE FF FF 00 FE FF FF 00 FE", //
                "FF FF 00 FE FF FF 00 FE FF FF", //
                "00 FE FF FF 00 FE FF FF 00 F4", //
                "CB 7E");

        // getInverterData(SoftwareVersion)
        bt.addWriteData( //
                "7E 3F 00 41 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 09 A0 FF FF FF FF FF", //
                "FF 00 00 7D 5D 00 35 DB C6 38", //
                "00 00 00 00 00 00 0C 80 00 02", //
                "00 58 00 34 82 00 FF 34 82 00", //
                "FB 37 7E");
        bt.addReadData(//
                "7E 6A 00 14 06 B6 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 33 90 7D 5D 00 35", //
                "DB C6 38 00 A0 71 00 2D 38 2F", //
                "7D 5D 00 00 00 00 00 00 0C 80", //
                "01 02 00 58 05 00 00 00 05 00", //
                "00 00 01 34 82 00 79 7D 5E 31", //
                "62 00 00 00 00 00 00 00 00 FE", //
                "FF FF FF FE FF FF FF 04 01 08", //
                "02 04 01 08 02 00 00 00 00 00", //
                "00 00 00 E1 FE 7E");
        bt.addReadData(//
                "7E 6A 00 14 54 2D 15 25 80 00", //
                "79 B6 7E 01 5F E4 01 00 7E FF", //
                "03 60 65 7D 33 80 7D 5D 00 35", //
                "DB C6 38 00 A0 63 00 C5 68 49", //
                "77 00 00 00 00 00 00 0C 80 01", //
                "02 00 58 05 00 00 00 05 00 00", //
                "00 01 34 82 00 70 5F 87 62 00", //
                "00 00 00 00 00 00 00 FE FF FF", //
                "FF FE FF FF FF 04 7A 09 7D 32", //
                "04 7A 09 7D 32 00 00 00 00 00", //
                "00 00 00 09 04 7E");

        // logoff();
        bt.addWriteData( //
                "7E 3B 00 45 79 B6 7E 01 5F E4", //
                "FF FF FF FF FF FF 01 00 7E FF", //
                "03 60 65 08 A0 FF FF FF FF FF", //
                "FF 00 03 7D 5D 00 35 DB C6 38", //
                "00 03 00 00 00 00 0D 80 0E 01", //
                "FD FF FF FF FF FF BD D5 7E");

        try {
            plant.init(bt);
            plant.logon(SmaUserGroup.User, "0000");
            plant.setInverterTime();
            plant.getInverterData(SmaDevice.InverterDataType.SpotACTotalPower);
            plant.getInverterData(SmaDevice.InverterDataType.SpotACVoltage);
            plant.getInverterData(SmaDevice.InverterDataType.EnergyProduction);
            plant.getInverterData(SmaDevice.InverterDataType.MaxACPower);
            plant.getInverterData(SmaDevice.InverterDataType.DeviceStatus);
            plant.getInverterData(SmaDevice.InverterDataType.TypeLabel);
            plant.getInverterData(SmaDevice.InverterDataType.SoftwareVersion);
            plant.logoff();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void testGetData() {
        BluetoothDebug bt = new BluetoothDebug();
        SMAPPPFrame.AppSerial = 934043669;
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
