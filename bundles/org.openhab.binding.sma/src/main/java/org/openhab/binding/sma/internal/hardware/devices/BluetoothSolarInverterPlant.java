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
package org.openhab.binding.sma.internal.hardware.devices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openhab.binding.sma.internal.layers.BinaryInputStream;
import org.openhab.binding.sma.internal.layers.BinaryOutputStream;
import org.openhab.binding.sma.internal.layers.Bluetooth;
import org.openhab.binding.sma.internal.layers.PPPFrame;
import org.openhab.binding.sma.internal.layers.SMAFrame;
import org.openhab.binding.sma.internal.layers.SMAPPPFrame;
import org.openhab.binding.sma.internal.layers.Utils;
import org.openhab.binding.sma.internal.layers.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothSolarInverterPlant extends SolarInverter {

    private static final Logger logger = LoggerFactory.getLogger(BluetoothSolarInverterPlant.class);

    private final int connectRetries = 10;
    private boolean isInit = false;
    private SmaBluetoothAddress rootDeviceAdress;

    private ArrayList<BluetoothSolarInverterPlant.Data> inverters;
    protected Map<String, BluetoothSolarInverterPlant.Data> invertersByAddress;
    protected Map<SmaSerial, BluetoothSolarInverterPlant.Data> invertersBySerial;

    protected Bluetooth layer;

    // protected String address;

    public BluetoothSolarInverterPlant() {
        super();
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

        SmaBluetoothAddress localDeviceAdress = new SmaBluetoothAddress();
        rootDeviceAdress = layer.getDestAddress();

        int attempts = 0;
        boolean connected = false;
        while (!connected) {
            try {
                logger.debug("Connecting to {} ({}/{})\n", layer.getDestAddress(), attempts + 1, connectRetries);
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
            layer.sendSMAFrame(new SMAFrame(0x0201, layer.getLocalAddress(),
                    new SmaBluetoothAddress(new byte[] { 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 }), //
                    new BinaryOutputStream()//
                            .writeBytes("ver\r\n")//
                            .toByteArray()));

            // This can take up to 3 seconds!
            SMAFrame frame = layer.receiveSMAFrame(0x02);
            int netID = frame.getPayload()[4];
            logger.debug("SMA netID = {}\n", netID);

            // check root device Address
            layer.sendSMAFrame(new SMAFrame(0x02, layer.getLocalAddress(), layer.getDestAddress(), //
                    new BinaryOutputStream()//
                            .writeInt(0x00700400)//
                            .writeByte((byte) netID)//
                            .writeInt(0)//
                            .writeInt(1)//
                            .toByteArray()));

            // Connection to Root Device
            frame = layer.receiveSMAFrame(0x0A);
            byte[] data = frame.getPayload();

            // If Root Device has changed, copy the new address
            if (data[6] == 2) {
                rootDeviceAdress.setAddress(data, 0);
                layer.setDestAddress(rootDeviceAdress);
            }
            logger.debug("Root device address: {}", rootDeviceAdress);

            // Get local BT address
            localDeviceAdress.setAddress(data, 7);
            layer.setLocalAddress(localDeviceAdress);
            logger.debug("Local BT address: {}", localDeviceAdress);

            frame = layer.receiveSMAFrame(0x05);
            data = frame.getPayload();

            // Get network topology
            int devcount = 1;
            inverters = new ArrayList<BluetoothSolarInverterPlant.Data>();

            for (int ptr = 0; ptr < data.length; ptr += 8) {
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
                layer.sendSMAFrame(new SMAFrame(0x03, layer.getLocalAddress(), layer.getDestAddress(), //
                        new BinaryOutputStream()//
                                .writeShort((short) 0x000A)//
                                .writeByte((byte) 0xAC)//
                                .toByteArray()));
                layer.receiveSMAFrame(0x04);

                layer.sendSMAFrame(new SMAFrame(0x03, layer.getLocalAddress(), layer.getDestAddress(), //
                        new BinaryOutputStream()//
                                .writeShort((short) 0x0002)//
                                .toByteArray()));
                layer.receiveSMAFrame(0x04);

                layer.sendSMAFrame(new SMAFrame(0x03, layer.getLocalAddress(), layer.getDestAddress(), //
                        new BinaryOutputStream()//
                                .writeShort((short) 0x0001)//
                                .writeByte((byte) 0x01)//
                                .toByteArray()));
                layer.receiveSMAFrame(0x04);

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
                        frame = layer.receiveSMAFrame(0xFF);
                        data = frame.getFrame();
                        packetType = frame.getControl();
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
                    frame = layer.receiveSMAFrame(0x05);
                    data = frame.getFrame();
                    packetType = frame.getControl();
                }

                logger.debug("PacketType ({})\n", packetType);

                if (0x0005 == packetType) {
                    /*
                     * Get network topology Overwrite all found inverters
                     * starting at index 1
                     */

                    // Get network topology
                    data = frame.getPayload();
                    int pcktsize = frame.getPayload().length;
                    devcount = 1;
                    inverters.clear();

                    for (int ptr = 0; ptr < pcktsize; ptr += 8) {
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
                 * At this point our network should be ready! In some cases
                 * 0x0005 and 0x1001 are missing and we have already received
                 * 0x0006 (NETWORK IS READY") If not, just wait for it and
                 * ignore any error
                 */
                if (0x06 != packetType) {
                    frame = layer.receiveSMAFrame(0x06);
                    data = frame.getFrame();
                    packetType = frame.getControl();
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
            layer.sendSMAFrame(new SMAFrame(0x01, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x09, (byte) 0xA0, (short) 0x0, SMAPPPFrame.ANYSUSYID,
                                    SMAPPPFrame.ANYSERIAL, ++pcktID) //
                            .writeInt(0x00000200)//
                            .writeInt(0x0)//
                            .writeInt(0x0)//
                            .toPPPFrame()));

            // All inverters *should* reply with their SUSyID & SerialNr
            // (and some other unknown info)
            SmaBluetoothAddress address = new SmaBluetoothAddress();
            for (int i = 0; i < inverters.size(); i++) {
                PPPFrame pppFrame = layer.receivePPPFrame(pcktID);
                data = pppFrame.getPayload();
                address = pppFrame.getFrameSourceAddress();

                BluetoothSolarInverterPlant.Data current = this.invertersByAddress.get(address.toString());
                if (current != null) {
                    SmaSerial serial = new SmaSerial((short) Utils.getShort(data, 50), Utils.getInt(data, 52));
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

            layer.sendSMAFrame(new SMAFrame(0x01, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x0E, (byte) 0xA0, (short) 0x0100, SMAPPPFrame.ANYSUSYID,
                                    SMAPPPFrame.ANYSERIAL, ++pcktID)//
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
                    PPPFrame frame = layer.receivePPPFrame(pcktID);
                    byte[] data = frame.getPayload();
                    SmaBluetoothAddress address = frame.getFrameSourceAddress();

                    if (/* (pcktID == rcvpcktID) && */ (Utils.getInt(data, 36) == now)) {
                        BluetoothSolarInverterPlant.Data current = this.invertersByAddress.get(address.toString());
                        if (current != null) {
                            current.setSerial(new SmaSerial((short) Utils.getShort(data, 10), Utils.getInt(data, 12)));

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
            layer.sendSMAFrame(new SMAFrame(0x01, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x08, (byte) 0xA0, (short) 0x0300, SMAPPPFrame.ANYSUSYID,
                                    SMAPPPFrame.ANYSERIAL, ++pcktID) //
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
            layer.sendSMAFrame(new SMAFrame(0x01, layer.getLocalAddress(), rootDeviceAdress, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x10, (byte) 0xA0, (short) 0, SMAPPPFrame.ANYSUSYID,
                                    SMAPPPFrame.ANYSERIAL, pcktID)//
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

    public boolean getInverterData(InverterDataType type) {
        logger.debug("getInverterData({})\n", type);

        if (type == InverterDataType.None) {
            return false;
        }

        boolean validPcktID = false;

        final int command = type.getCommand();
        final int first = type.getFirst();
        final int last = type.getLast();

        try {

            layer.sendSMAFrame(new SMAFrame(0x01, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                    SMAPPPFrame
                            .writePppHeader((byte) 0x09, (byte) 0xA0, (short) 0, SMAPPPFrame.ANYSUSYID,
                                    SMAPPPFrame.ANYSERIAL, ++pcktID)//
                            .writeInt(command)//
                            .writeInt(first)//
                            .writeInt(last)//
                            .toPPPFrame()));

            for (int j = 0; j < inverters.size(); j++) {
                validPcktID = false;
                do {
                    PPPFrame frame = layer.receivePPPFrame(pcktID);
                    byte[] data = frame.getPayload();
                    BinaryInputStream rd = new BinaryInputStream(data);

                    SmaSerial serial = new SmaSerial((short) Utils.getShort(data, 10), Utils.getInt(data, 12));
                    BluetoothSolarInverterPlant.Data current = invertersBySerial.get(serial);

                    if (current == null) {
                        logger.warn("Unexpected: {} not found!", serial.toString());
                        continue;
                    }

                    validPcktID = true;

                    rd.seek(0);
                    int a = rd.readByte();
                    rd.seek(28);
                    long c = rd.readUInt();
                    long b = rd.readUInt();

                    final int recordsize = 4 * (a - 9) / (int) (b - c + 1);

                    for (int i = 36; i < data.length; i += recordsize) {

                        rd.seek(i);

                        Value val = Value.Read(rd, recordsize);

                        if (val.getLri() == LRIDefinition.MeteringDyWhOut) {
                            // This function gives us the current
                            // inverter time
                            current.inverterTime = val.getDatetime();
                        }

                        switch (val.getLri()) {
                            case GridMsTotW: // SPOT_PACTOT

                                // This function gives us the time when
                                // the inverter was switched off
                                current.sleepTime = val.getDatetime();
                                current.setValue(val.getLri(), Utils.tokW(val.getSLongValue()));
                                current.flags |= type.getValue();
                                logger.debug(strkW, val.getLri().getCode(), Utils.tokW(val.getSLongValue()),
                                        val.getDatetime());
                                break;

                            case OperationHealthSttOk: // INV_PACMAX1

                                current.pmax1 = val.getULongValue();
                                current.flags |= type.getValue();
                                logger.debug(strWatt, "INV_PACMAX1", val.getULongValue(), val.getDatetime());
                                break;

                            case OperationHealthSttWrn: // INV_PACMAX2

                                current.pmax2 = val.getULongValue();
                                current.flags |= type.getValue();
                                logger.debug(strWatt, "INV_PACMAX2", val.getULongValue(), val.getDatetime());
                                break;

                            case OperationHealthSttAlm: // INV_PACMAX3

                                current.pmax3 = val.getULongValue();
                                current.flags |= type.getValue();
                                logger.debug(strWatt, "INV_PACMAX3", val.getULongValue(), val.getDatetime());
                                break;

                            case GridMsPhVphsA: // SPOT_UAC1
                            case GridMsPhVphsB: // SPOT_UAC2
                            case GridMsPhVphsC: // SPOT_UAC3

                                if (val.getULongValue() != Value.ULong.NANVal) {
                                    current.setValue(val.getLri(), Utils.toVolt(val.getULongValue()));
                                    current.flags |= type.getValue();
                                    logger.debug(strVolt, val.getLri().getCode(), Utils.toVolt(val.getULongValue()),
                                            val.getDatetime());
                                }
                                break;

                            case GridMsAphsA_1: // SPOT_IAC1

                                current.iac1 = val.getULongValue();
                                current.flags |= type.getValue();
                                logger.debug(strAmp, "SPOT_IAC1", Utils.toAmp(val.getULongValue()), val.getDatetime());
                                break;

                            case GridMsAphsB_1: // SPOT_IAC2

                                current.iac2 = val.getULongValue();
                                current.flags |= type.getValue();
                                logger.debug(strAmp, "SPOT_IAC2", Utils.toAmp(val.getULongValue()), val.getDatetime());
                                break;

                            case GridMsAphsC_1: // SPOT_IAC3

                                current.iac3 = val.getULongValue();
                                current.flags |= type.getValue();
                                logger.debug(strAmp, "SPOT_IAC3", Utils.toAmp(val.getULongValue()), val.getDatetime());
                                break;

                            case MeteringTotWhOut: // SPOT_ETOTAL
                            case MeteringDyWhOut: // SPOT_ETODAY

                                current.setValue(val.getLri(), Utils.tokWh(val.getULongValue()));
                                current.flags |= type.getValue();
                                logger.debug(strkWh, current + val.getLri().getCode(), Utils.tokWh(val.getULongValue()),
                                        val.getDatetime());
                                break;

                            case NameplateLocation: // INV_NAME

                                // This function gives us the time when the inverter was switched on
                                current.wakeupTime = val.getDatetime();
                                current.setDeviceName(val.getStringValue());
                                current.flags |= type.getValue();
                                logger.debug("INV_NAME: {}   {}", current.getDeviceName(), val.getDatetime());
                                break;

                            case NameplatePkgRev: // INV_SWVER

                                current.swVersion = Utils.toVersionString(val.getULongValue());
                                current.flags |= type.getValue();
                                logger.debug("INV_SWVER: '{}' {}", current.swVersion, val.getDatetime());
                                break;

                            case NameplateModel: // INV_TYPE
                            {
                                List<Integer> tags = val.getStatusTags();

                                if (tags.size() > 0) {
                                    current.setDeviceType(SmaDevice.getModel(tags.get(0)));
                                    current.setValue(val.getLri(), current.getDeviceType());
                                    current.flags |= type.getValue();
                                    logger.debug("INV_TYPE: '{}' {}", current.getDeviceType(), val.getDatetime());
                                }
                            }
                                break;

                            case NameplateMainModel: // INV_CLASS:
                            {
                                List<Integer> tags = val.getStatusTags();

                                if (tags.size() > 0) {
                                    current.setDevClass(SmaDevice.DeviceClass.fromOrdinal(tags.get(0)));
                                    current.setValue(val.getLri(), current.getDevClass().name());
                                    current.flags |= type.getValue();
                                    logger.debug("INV_CLASS: {} {}", current.getDeviceStatus(), val.getDatetime());
                                }
                            }
                                break;

                            case OperationHealth: // INV_STATUS:
                            {
                                List<Integer> tags = val.getStatusTags();

                                if (tags.size() > 0) {
                                    current.setDeviceStatus(tags.get(0));
                                    current.setValue(val.getLri(), current.getDeviceStatus());
                                    current.flags |= type.getValue();
                                    logger.debug("INV_STATUS: {} {}", current.getDeviceStatus(), val.getDatetime());
                                }
                            }
                                break;

                            default:
                                logger.warn("Unhandled LRI {}", val.getLri().getCode());
                        }
                    }

                } while (!validPcktID);
            }

        } catch (IOException e) {
            logger.debug("getInverterData({}) failed: {}", type, e.getMessage());
            return false;
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
