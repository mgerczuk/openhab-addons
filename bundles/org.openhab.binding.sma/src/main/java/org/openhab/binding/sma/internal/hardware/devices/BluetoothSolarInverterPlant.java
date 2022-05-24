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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.binding.sma.internal.SmaBinding.Device;
import org.openhab.binding.sma.internal.layers.Bluetooth;
import org.openhab.binding.sma.internal.layers.LittleEndianByteArrayOutputStream;
import org.openhab.binding.sma.internal.layers.SMAFrame;
import org.openhab.binding.sma.internal.layers.SMAPPPFrame;
import org.openhab.binding.sma.internal.layers.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothSolarInverterPlant extends SolarInverter {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothSolarInverterPlant.class);

    /**
     * defines valid LRIs for that kind of device
     */
    private static final List<LRIDefinition> validLRIDefinition = Arrays.asList(LRIDefinition.MeteringTotWhOut,
            LRIDefinition.MeteringDyWhOut, LRIDefinition.GridMsTotW, LRIDefinition.NameplateLocation);

    private final int connectRetries = 10;
    private boolean isInit = false;
    private SmaBluetoothAddress rootDeviceAdress;

    private ArrayList<BluetoothSolarInverterPlant.Data> inverters;
    protected Map<String, BluetoothSolarInverterPlant.Data> invertersByAddress;
    protected Map<SmaSerial, BluetoothSolarInverterPlant.Data> invertersBySerial;

    protected Bluetooth layer;

    // protected String address;

    public BluetoothSolarInverterPlant(Device device) {
        super(device);
    }

    public void init(Bluetooth layer) throws IOException {

        if (this.layer != null && this.layer.isOpen()) {
            logger.error("Bluetooth already open! Resource leak!");
        }

        this.layer = layer;

        if (isInit) {
            logger.error("unexpected isInit == true");
            return;
        }

        byte[] data;

        SmaBluetoothAddress localDeviceAdress = new SmaBluetoothAddress();
        rootDeviceAdress = layer.destAddress;

        int attempts = 0;
        boolean connected = false;
        while (!connected) {
            try {
                logger.debug("Connecting to {} ({}/{})\n", layer.destAddress, attempts + 1, connectRetries);
                layer.open();
                connected = true;
            } catch (Exception e1) {
                logger.debug("failed: {}", e1.getMessage());
                if (attempts++ >= connectRetries) {
                    throw e1;
                }
            }
        }

        try {
            // query SMA Net ID
            layer.sendFrame(new SMAFrame(0x0201, layer.localAddress,
                    new SmaBluetoothAddress(new byte[] { 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 }), //
                    new LittleEndianByteArrayOutputStream()//
                            .writeBytes("ver\r\n")//
                            .toByteArray()));

            // This can take up to 3 seconds!
            data = layer.receive(0x02);
            int netID = data[22];
            logger.debug("SMA netID = {}\n", netID);

            // check root device Address
            layer.sendFrame(new SMAFrame(0x02, layer.localAddress, layer.destAddress, //
                    new LittleEndianByteArrayOutputStream()//
                            .writeInt(0x00700400)//
                            .writeByte((byte) netID)//
                            .writeInt(0)//
                            .writeInt(1)//
                            .toByteArray()));

            // Connection to Root Device
            data = layer.receive(0x0A);

            // If Root Device has changed, copy the new address
            if (data[24] == 2) {
                rootDeviceAdress.setAddress(data, 18);
                layer.destAddress = rootDeviceAdress;
            }
            logger.debug("Root device address: {}", rootDeviceAdress);

            // Get local BT address
            localDeviceAdress.setAddress(data, 25);
            layer.localAddress = localDeviceAdress;
            logger.debug("Local BT address: {}", localDeviceAdress);

            data = layer.receive(0x05);

            // Get network topology
            int pcktsize = Utils.getShort(data, 1);
            int devcount = 1;
            inverters = new ArrayList<BluetoothSolarInverterPlant.Data>();

            for (int ptr = 18; ptr <= pcktsize - 8; ptr += 8) {
                SmaBluetoothAddress address = new SmaBluetoothAddress(data, ptr);
                // Inverters only - Ignore other devices
                if (data[ptr + 6] == 0x01 && data[ptr + 7] == 0x01) {
                    logger.debug("Device {}: found SMA Inverter @ {}", devcount, address);
                    Data inverter = new BluetoothSolarInverterPlant.Data(address);
                    inverter.netID = netID;
                    inverters.add(inverter);

                } else {
                    // other device
                    logger.debug("Device {}: other device @ {}", devcount, address);
                }
                devcount++;
            }

            /***********************************************************************
             * This part is only needed if you have more then one inverter The
             * purpose is to (re)build the network when we have found only 1
             ************************************************************************/
            if ((inverters.size() == 1) && (netID > 1)) {
                // We need more handshake 03/04 commands to initialise network
                // connection between inverters
                layer.sendFrame(new SMAFrame(0x03, layer.localAddress, layer.destAddress, //
                        new LittleEndianByteArrayOutputStream()//
                                .writeShort((short) 0x000A)//
                                .writeByte((byte) 0xAC)//
                                .toByteArray()));
                data = layer.receive(0x04);

                layer.sendFrame(new SMAFrame(0x03, layer.localAddress, layer.destAddress, //
                        new LittleEndianByteArrayOutputStream()//
                                .writeShort((short) 0x0002)//
                                .toByteArray()));
                data = layer.receive(0x04);

                layer.sendFrame(new SMAFrame(0x03, layer.localAddress, layer.destAddress, //
                        new LittleEndianByteArrayOutputStream()//
                                .writeShort((short) 0x0001)//
                                .writeByte((byte) 0x01)//
                                .toByteArray()));
                data = layer.receive(0x04);

                /******************************************************************
                 * Read the network topology Waiting for a max of 60 sec - 6
                 * times 'timeout' of recv() Should be enough for small networks
                 * (2-3 inverters)
                 *******************************************************************/

                logger.debug("Waiting for network to be built...");

                int packetType = 0;

                for (int i = 0; i < 6; i++) {
                    // Get any packet - should be 0x0005 or 0x1001, but 0x0006
                    // is allowed
                    try {
                        data = layer.receive(0xFF);
                        packetType = Utils.getShort(data, 16);
                        break;
                    } catch (IOException e) {
                    }

                }

                if (packetType == 0) // unable to build inverter network
                {
                    throw new IOException("In case of single inverter system set MIS_Enabled=0 in config file.");
                }

                if (0x1001 == packetType) {
                    packetType = 0; // reset it
                    data = layer.receive(0x05);
                    packetType = Utils.getShort(data, 16);
                }

                logger.debug("PacketType ({})\n", packetType);

                if (0x0005 == packetType) {
                    /*
                     * Get network topology Overwrite all found inverters
                     * starting at index 1
                     */

                    // Get network topology
                    pcktsize = Utils.getShort(data, 1);
                    devcount = 1;
                    inverters.clear();

                    for (int ptr = 18; ptr <= pcktsize - 8; ptr += 8) {
                        if (logger.isDebugEnabled()) {

                            SmaBluetoothAddress dest = new SmaBluetoothAddress(data, ptr);
                            logger.debug("Device {}: {} -> ", devcount, dest);
                        }

                        // Inverters only - Ignore other devices
                        if (data[ptr + 6] == 0x01 && data[ptr + 7] == 0x01) {
                            logger.debug("Inverter");

                            SmaBluetoothAddress address = new SmaBluetoothAddress(data, ptr);

                            BluetoothSolarInverterPlant.Data inverter = new BluetoothSolarInverterPlant.Data(address);
                            inverter.netID = netID;
                            inverters.add(inverter);

                            devcount++;

                        } else {
                            // other device
                        }
                    }
                }

                /*
                 * At this point our netwerk should be ready! In some cases
                 * 0x0005 and 0x1001 are missing and we have already received
                 * 0x0006 (NETWORK IS READY") If not, just wait for it and
                 * ignore any error
                 */
                if (0x06 != packetType) {
                    data = layer.receive(0x06);
                }

            }

            // prepare some caching
            this.invertersByAddress = new HashMap<String, BluetoothSolarInverterPlant.Data>(inverters.size());
            this.invertersBySerial = new HashMap<SmaSerial, BluetoothSolarInverterPlant.Data>(inverters.size());

            for (BluetoothSolarInverterPlant.Data inverter : inverters) {
                BluetoothSolarInverterPlant.Data bTInverter = inverter;
                this.invertersByAddress.put(bTInverter.getBTAddressAsString(), bTInverter);
            }

            // Send broadcast request for identification
            layer.sendFrame(new SMAFrame(0x01, layer.localAddress, SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x09, (byte) 0xA0, (short) 0x0, Utils.ANYSUSYID, Utils.ANYSERIAL,
                                    ++pcktID) //
                            .writeInt(0x00000200)//
                            .writeInt(0x0)//
                            .writeInt(0x0)//
                            .toPPPFrame()));

            // All inverters *should* reply with their SUSyID & SerialNr
            // (and some other unknown info)
            SmaBluetoothAddress address = new SmaBluetoothAddress();
            for (int i = 0; i < inverters.size(); i++) {
                data = layer.receiveAll(0x01);
                address = layer.getHeaderAddress();

                BluetoothSolarInverterPlant.Data current = this.invertersByAddress.get(address.toString());
                if (current != null) {
                    SmaSerial serial = new SmaSerial((short) Utils.getShort(data, 55), Utils.getInt(data, 57));
                    current.setSerial(serial);

                    logger.debug("SUSyID: {} - SN: {}\n", serial.suSyID, serial.serial);

                    this.invertersBySerial.put(serial, current);
                } else {
                    logger.debug("Unexpected response from {} -> ", address);
                }
            }

            isInit = true;

            logoff();

            return;
        } catch (IOException e) {
            layer.close();
            throw new IOException("can't initialize inverter plant: " + e.getMessage());
        } finally {
            // layer.close();
        }
    }

    @Override
    public void exit() {
        layer.close();
    }

    @Override
    public void logon(SmaUserGroup userGroup, String password) throws IOException {
        logger.debug("logon SMA Inverter");

        try {
            // layer.open();
            byte pw[] = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

            byte encChar = (byte) ((userGroup == SmaUserGroup.User) ? 0x88 : 0xBB);
            // Encode password
            int idx;
            for (idx = 0; (idx < password.length()) && (idx < 12); idx++) {
                pw[idx] = (byte) (password.charAt(idx) + (encChar & 0xff));
            }
            for (; idx < 12; idx++) {
                pw[idx] = encChar;
            }

            boolean validPcktID = false;

            int now;

            now = layer.currentTimeSeconds();

            layer.sendFrame(new SMAFrame(0x01, layer.localAddress, SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x0E, (byte) 0xA0, (short) 0x0100, Utils.ANYSUSYID, Utils.ANYSERIAL,
                                    ++pcktID)//
                            .writeInt(0xFFFD040C)//
                            .writeInt(userGroup.getValue()) // User / Installer
                            .writeInt(0x00000384) // Timeout = 900sec ?
                            .writeInt(now)//
                            .writeInt(0x0)//
                            .writeBytes(pw)//
                            .toPPPFrame()));

            do {
                // All inverters *should* reply with their SUSyID & SerialNr
                // (and some other unknown info)
                for (int i = 0; i < inverters.size(); i++) {
                    byte[] data = layer.receiveAll(0x01);
                    SmaBluetoothAddress address = layer.getHeaderAddress();

                    short rcvpcktID = (short) (Utils.getShort(data, 27) & 0x7FFF);
                    logger.debug("rcvpcktID id {}", rcvpcktID);

                    if ((pcktID == rcvpcktID) && (Utils.getInt(data, 41) == now)) {
                        BluetoothSolarInverterPlant.Data current = this.invertersByAddress.get(address.toString());
                        if (current != null) {
                            current.setSerial(new SmaSerial((short) Utils.getShort(data, 15), Utils.getInt(data, 17)));

                            validPcktID = true;
                        } else {
                            logger.debug("Unexpected response from {}", address.toString());
                        }
                    }

                }
            } while (!validPcktID);
        } catch (IOException e) {
            throw new IOException("logon failed: " + e.getMessage());
        }
    }

    @Override
    public void logoff() throws IOException {
        logger.debug("logoff SMA Inverter");
        try {
            layer.sendFrame(new SMAFrame(0x01, layer.localAddress, SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x08, (byte) 0xA0, (short) 0x0300, Utils.ANYSUSYID, Utils.ANYSERIAL,
                                    ++pcktID) //
                            .writeInt(0xFFFD010E)//
                            .writeInt(0xFFFFFFFF)//
                            .toPPPFrame()));
        } catch (IOException e) {
            throw new IOException("logoff failed: " + e.getMessage());
        }
    }

    public void setInverterTime() throws IOException {
        logger.debug("SetInverterTime()");

        try {
            int localtime = layer.currentTimeSeconds();
            int tzOffset = layer.getTimezoneOffset();

            logger.debug("Local Time: {}", new Date(localtime * 1000L));
            logger.debug("TZ offset (s): {}", tzOffset);

            pcktID++;
            layer.sendFrame(new SMAFrame(0x01, layer.localAddress, rootDeviceAdress, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x10, (byte) 0xA0, (short) 0, Utils.ANYSUSYID, Utils.ANYSERIAL,
                                    pcktID)//
                            .writeInt(0xF000020A)//
                            .writeInt(0x00236D00)//
                            .writeInt(0x00236D00)//
                            .writeInt(0x00236D00)//
                            .writeInt(localtime)//
                            .writeInt(localtime)//
                            .writeInt(localtime)//
                            .writeInt(tzOffset)//
                            .writeInt(1)//
                            .writeInt(1)//
                            .toPPPFrame()));
        } catch (IOException e) {
            throw new IOException("setInverterTime failed: " + e.getMessage());
        }
    }

    public ArrayList<BluetoothSolarInverterPlant.Data> getInverters() {
        return inverters;
    }

    public final static void readBTAddress(byte[] src, byte[] dest, int start) {
        dest[0] = src[start + 5];
        dest[1] = src[start + 4];
        dest[2] = src[start + 3];
        dest[3] = src[start + 2];
        dest[4] = src[start + 1];
        dest[5] = src[start + 0];
    }

    @Override
    public boolean getInverterData(InverterDataType type) {
        logger.debug("getInverterData({})\n", type);

        if (type == InverterDataType.None) {
            return false;
        }

        int recordsize = 0;
        boolean validPcktID = false;

        final int command = type.getCommand();
        final int first = type.getFirst();
        final int last = type.getLast();

        try {
            // layer.open();

            layer.sendFrame(new SMAFrame(0x01, layer.localAddress, SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x09, (byte) 0xA0, (short) 0, Utils.ANYSUSYID, Utils.ANYSERIAL,
                                    ++pcktID)//
                            .writeInt(command)//
                            .writeInt(first)//
                            .writeInt(last)//
                            .toPPPFrame()));

            byte[] data;

            for (int j = 0; j < inverters.size(); j++) {
                validPcktID = false;
                do {
                    data = layer.receiveAll(0x01);
                    /*
                     * if ((ConnType == CT_BLUETOOTH) && (!validateChecksum()))
                     * return E_CHKSUM; else
                     */
                    {
                        short rcvpcktID = (data.length < 29) ? -1 : (short) (Utils.getShort(data, 27) & 0x7FFF);
                        if (pcktID == rcvpcktID) {

                            SmaSerial serial = new SmaSerial((short) Utils.getShort(data, 15), Utils.getInt(data, 17));
                            BluetoothSolarInverterPlant.Data current = invertersBySerial.get(serial);

                            if (current != null) {
                                validPcktID = true;
                                int value = 0;
                                long value64 = 0;
                                for (int i = 41; i < data.length - 3; i += recordsize) {
                                    int code = Utils.getInt(data, i);
                                    // LRIDefinition lri = LRIDefinition
                                    // .fromOrdinal(code & 0x00FFFF00);
                                    // int cls = code & 0xFF;
                                    LRIDefinition lri = LRIDefinition.fromOrdinal(code & 0x00FFFF00);

                                    if (lri == null) { // LRI not found. Maybe separated into classes
                                        lri = LRIDefinition.fromOrdinal(code & 0x00FFFFFF);

                                        // skip unknown codes
                                        if (lri == null) {
                                            if (recordsize == 0) {
                                                recordsize = 12;
                                            }
                                            continue;
                                        }
                                    }

                                    char dataType = (char) (code >>> 24);
                                    Date datetime = new Date(Utils.getInt(data, i + 4) * 1000L);

                                    // fix: We can't rely on dataType because it
                                    // can be both 0x00 or 0x40 for DWORDs
                                    if ((lri == LRIDefinition.MeteringDyWhOut)
                                            || (lri == LRIDefinition.MeteringTotWhOut)
                                            || (lri == LRIDefinition.MeteringTotFeedTms)
                                            || (lri == LRIDefinition.MeteringTotOpTms)) // QWORD
                                    {
                                        value64 = Utils.getLong(data, i + 8);
                                        if ((value64 == NaN_S64) || (value64 == NaN_U64)) {
                                            value64 = 0;
                                        }
                                    } else if ((dataType != 0x10) && (dataType != 0x08))
                                    // Not TEXT or STATUS, so it should be DWORD
                                    {
                                        value = Utils.getInt(data, i + 8);
                                        if ((value == NaN_S32) || (value == NaN_U32)) {
                                            value = 0;
                                        }
                                    }

                                    if (lri == LRIDefinition.MeteringDyWhOut) {
                                        // This function gives us the current
                                        // inverter time
                                        current.inverterTime = datetime;
                                    }

                                    switch (lri) {
                                        case GridMsTotW: // SPOT_PACTOT
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            // This function gives us the time when
                                            // the inverter was switched off
                                            current.sleepTime = datetime;
                                            current.setValue(lri, Utils.tokW(value));
                                            current.flags |= type.getValue();
                                            logger.debug(strkW, lri.getCode(), Utils.tokW(value), datetime);
                                            break;

                                        case OperationHealthSttOk: // INV_PACMAX1
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.pmax1 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strWatt, "INV_PACMAX1", value, datetime);
                                            break;

                                        case OperationHealthSttWrn: // INV_PACMAX2
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.pmax2 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strWatt, "INV_PACMAX2", value, datetime);
                                            break;

                                        case OperationHealthSttAlm: // INV_PACMAX3
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.pmax3 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strWatt, "INV_PACMAX3", value, datetime);
                                            break;

                                        case GridMsWphsA: // SPOT_PAC1
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.pac1 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strWatt, "SPOT_PAC1", value, datetime);
                                            break;

                                        case GridMsWphsB: // SPOT_PAC2
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.pac2 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strWatt, "SPOT_PAC2", value, datetime);
                                            break;

                                        case GridMsWphsC: // SPOT_PAC3
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.pac3 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strWatt, "SPOT_PAC3", value, datetime);
                                            break;

                                        case GridMsPhVphsA: // SPOT_UAC1
                                        case GridMsPhVphsB: // SPOT_UAC2
                                        case GridMsPhVphsC: // SPOT_UAC3
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.setValue(lri, Utils.toVolt(value));
                                            current.flags |= type.getValue();
                                            logger.debug(strVolt, lri.getCode(), Utils.toVolt(value), datetime);
                                            break;

                                        case GridMsAphsA_1: // SPOT_IAC1
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.iac1 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strAmp, "SPOT_IAC1", Utils.toAmp(value), datetime);
                                            break;

                                        case GridMsAphsB_1: // SPOT_IAC2
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.iac2 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strAmp, "SPOT_IAC2", Utils.toAmp(value), datetime);
                                            break;

                                        case GridMsAphsC_1: // SPOT_IAC3
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.iac3 = value;
                                            current.flags |= type.getValue();
                                            logger.debug(strAmp, "SPOT_IAC3", Utils.toAmp(value), datetime);
                                            break;

                                        case GridMsHz: // SPOT_FREQ
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.gridFreq = value;
                                            current.flags |= type.getValue();
                                            logger.debug("{}: {} (Hz) {}", "SPOT_FREQ", Utils.toHz(value), datetime);
                                            break;

                                        case DcMsWatt1: // SPOT_PDC1
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.pdc1 = value;
                                            logger.debug(strWatt, "SPOT_PDC1", value, datetime);
                                            current.flags |= type.getValue();
                                            break;

                                        case DcMsWatt2: // SPOT_PDC2
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }

                                            current.pdc2 = value;
                                            logger.debug(strWatt, "SPOT_PDC2", value, datetime);

                                            current.flags |= type.getValue();
                                            break;
                                        case DcMsVol1: // SPOT_UDC2
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }

                                            current.udc1 = value;
                                            logger.debug(strVolt, "SPOT_UDC1", Utils.toVolt(value), datetime);
                                            current.flags |= type.getValue();
                                            break;

                                        case DcMsVol2: // SPOT_UDC2
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }
                                            current.udc2 = value;
                                            logger.debug(strVolt, "SPOT_UDC2", Utils.toVolt(value), datetime);
                                            current.flags |= type.getValue();
                                            break;

                                        case DcMsAmp1: // SPOT_IDC1
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }

                                            current.idc1 = value;
                                            logger.debug(strAmp, "SPOT_IDC1", Utils.toAmp(value), datetime);

                                            current.flags |= type.getValue();
                                            break;

                                        case DcMsAmp2: // SPOT_IDC2
                                            if (recordsize == 0) {
                                                recordsize = 28;
                                            }

                                            current.idc2 = value;
                                            logger.debug(strAmp, "SPOT_IDC2", Utils.toAmp(value), datetime);

                                            current.flags |= type.getValue();
                                            break;

                                        case MeteringTotWhOut: // SPOT_ETOTAL
                                        case MeteringDyWhOut: // SPOT_ETODAY
                                            if (recordsize == 0) {
                                                recordsize = 16;
                                            }
                                            current.setValue(lri, Utils.tokWh(value64));
                                            current.flags |= type.getValue();
                                            logger.debug(strkWh, current + lri.getCode(), Utils.tokWh(value64),
                                                    datetime);
                                            break;

                                        case MeteringTotOpTms: // SPOT_OPERTM
                                            if (recordsize == 0) {
                                                recordsize = 16;
                                            }
                                            current.operationTime = value64;
                                            current.flags |= type.getValue();
                                            logger.debug(strHour, "SPOT_OPERTM", Utils.toHour(value64), datetime);
                                            break;

                                        case MeteringTotFeedTms: // SPOT_FEEDTM
                                            if (recordsize == 0) {
                                                recordsize = 16;
                                            }
                                            current.feedInTime = value64;
                                            current.flags |= type.getValue();
                                            logger.debug(strHour, "SPOT_FEEDTM", Utils.toHour(value64), datetime);
                                            break;

                                        case NameplateLocation: // INV_NAME
                                            if (recordsize == 0) {
                                                recordsize = 40;
                                            }
                                            // This function gives us the time when the inverter was switched on
                                            current.wakeupTime = datetime;
                                            current.setDeviceName(Utils.getString(data, i + 8, 32));
                                            current.flags |= type.getValue();
                                            logger.debug("INV_NAME: {}   {}", current.getDeviceName(), datetime);
                                            break;

                                        case NameplatePkgRev: // INV_SWVER
                                            if (recordsize == 0) {
                                                recordsize = 40;
                                            }
                                            byte vType = data[i + 24];
                                            byte vBuild = 0;
                                            byte vMinor = 0;
                                            byte vMajor = 0;

                                            String releaseType;
                                            if (vType > 5) {
                                                releaseType = Byte.toString(vType);
                                            } else {
                                                releaseType = String.valueOf("NEABRS".charAt(vType)); // NOREV-EXPERIMENTAL-ALPHA-BETA-RELEASE-SPECIAL
                                            }

                                            vBuild = data[i + 25];
                                            vMinor = data[i + 26];
                                            vMajor = data[i + 27];
                                            // Vmajor and Vminor = 0x12 should be printed as '12' and not '18' (BCD)
                                            current.swVersion = String.format("%c%c.%c%c.%02d.%s", '0' + (vMajor >> 4),
                                                    '0' + (vMajor & 0x0F), '0' + (vMinor >> 4), '0' + (vMinor & 0x0F),
                                                    vBuild, releaseType);
                                            current.flags |= type.getValue();
                                            logger.debug("INV_SWVER: '{}' {}", current.swVersion, datetime);
                                            break;

                                        case NameplateModel: // INV_TYPE
                                            if (recordsize == 0) {
                                                recordsize = 40;
                                            }
                                            for (int idx = 8; idx < recordsize; idx += 4) {
                                                int attribute = (int) (Utils.getLong(data, i + idx) & 0x00FFFFFF);
                                                byte status = data[i + idx + 3];
                                                if (attribute == 0xFFFFFE) {
                                                    break;
                                                } // End of attributes
                                                if (status == 1) {
                                                    current.setDeviceType(SmaDevice.getModel(attribute));
                                                    current.setValue(lri, current.getDeviceType());
                                                }
                                            }
                                            current.flags |= type.getValue();
                                            logger.debug("INV_TYPE: '{}' {}", current.getDeviceType(), datetime);
                                            break;

                                        /*
                                         * case NameplateMainModel: //INV_CLASS
                                         * if (recordsize == 0) recordsize = 40;
                                         * for (int idx = 8; idx < recordsize; idx += 4)
                                         * {
                                         * unsigned long attribute = ((unsigned long)get_long(pcktBuf + i + idx)) &
                                         * 0x00FFFFFF;
                                         * unsigned char attValue = pcktBuf[i + idx + 3];
                                         * if (attribute == 0xFFFFFE) break; //End of attributes
                                         * if (attValue == 1)
                                         * {
                                         * current.DevClass = (DEVICECLASS)attribute;
                                         * string devclass = tagdefs.getDesc(attribute);
                                         * if (!devclass.empty())
                                         * strncpy(current.DeviceClass, devclass.c_str(), sizeof(current.DeviceClass));
                                         * else
                                         * {
                                         * strncpy(current.DeviceClass, "UNKNOWN CLASS", sizeof(current.DeviceClass));
                                         * printf("Unknown Device Class. Report this issue at https://sbfspot.codeplex.com/workitem/list/basic with following info:\n"
                                         * );
                                         * printf("0x%08lX and Device Class=...\n", attribute);
                                         * }
                                         * }
                                         * }
                                         * current.flags |= type.getValue();
                                         * if (DEBUG_NORMAL) printf("%-12s: '%s' %s", "INV_CLASS", current.DeviceClass,
                                         * ctime(&datetime));
                                         * break;
                                         *
                                         * case OperationHealth: //INV_STATUS:
                                         * if (recordsize == 0) recordsize = 40;
                                         * for (int idx = 8; idx < recordsize; idx += 4)
                                         * {
                                         * unsigned long attribute = ((unsigned long)get_long(pcktBuf + i + idx)) &
                                         * 0x00FFFFFF;
                                         * unsigned char attValue = pcktBuf[i + idx + 3];
                                         * if (attribute == 0xFFFFFE) break; //End of attributes
                                         * if (attValue == 1)
                                         * current.DeviceStatus = attribute;
                                         * }
                                         * current.flags |= type.getValue();
                                         * if (DEBUG_NORMAL) printf("%-12s: '%s' %s", "INV_STATUS",
                                         * tagdefs.getDesc(current.DeviceStatus, "?").c_str(), ctime(&datetime));
                                         * break;
                                         *
                                         * case OperationGriSwStt: //INV_GRIDRELAY
                                         * if (recordsize == 0) recordsize = 40;
                                         * for (int idx = 8; idx < recordsize; idx += 4)
                                         * {
                                         * unsigned long attribute = ((unsigned long)get_long(pcktBuf + i + idx)) &
                                         * 0x00FFFFFF;
                                         * unsigned char attValue = pcktBuf[i + idx + 3];
                                         * if (attribute == 0xFFFFFE) break; //End of attributes
                                         * if (attValue == 1)
                                         * current.GridRelayStatus = attribute;
                                         * }
                                         * current.flags |= type.getValue();
                                         * if (DEBUG_NORMAL) printf("%-12s: '%s' %s", "INV_GRIDRELAY",
                                         * tagdefs.getDesc(current.GridRelayStatus, "?").c_str(), ctime(&datetime));
                                         * break;
                                         *
                                         * case BatChaStt:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.BatChaStt = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         *
                                         * case BatDiagCapacThrpCnt:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.BatDiagCapacThrpCnt = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         *
                                         * case BatDiagTotAhIn:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.BatDiagTotAhIn = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         *
                                         * case BatDiagTotAhOut:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.BatDiagTotAhOut = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         *
                                         * case BatTmpVal:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.BatTmpVal = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         *
                                         * case BatVol:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.BatVol = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         *
                                         * case BatAmp:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.BatAmp = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         *
                                         * case CoolsysTmpNom:
                                         * if (recordsize == 0) recordsize = 28;
                                         * current.Temperature = value;
                                         * current.flags |= type.getValue();
                                         * break;
                                         */
                                        default:
                                            if (recordsize == 0) {
                                                recordsize = 12;
                                            }
                                    }
                                }
                            }
                        } else {
                            logger.debug("Packet ID mismatch. Expected {}, received {}", pcktID, rcvpcktID);
                        }
                    }
                } while (!validPcktID);
            }

        } catch (IOException e) {
            logger.debug("getInverterData({}) failed: {}", type, e.getMessage());
            return false;
        } finally {
            // layer.close();
        }
        return true;
    }

    @Override
    public String toString() {
        return "BluetoothSolarInverterPlant [rootAddress=" + rootDeviceAdress + ", data=" + data + "]";
    }

    public static class Data extends SolarInverter.Data {
        protected SmaBluetoothAddress address;

        public Data(SmaBluetoothAddress address) {
            super();

            this.address = address;
        }

        public SmaBluetoothAddress getBTAddress() {
            return this.address;
        }

        public void setBTAddress(SmaBluetoothAddress bTAddress) {
            this.address = bTAddress;
        }

        public String getBTAddressAsString() {
            return this.address.toString();
        }

        @Override
        public String toString() {
            return "Data [address=" + address + ", " + super.toString() + "]";
        }
    }
}
