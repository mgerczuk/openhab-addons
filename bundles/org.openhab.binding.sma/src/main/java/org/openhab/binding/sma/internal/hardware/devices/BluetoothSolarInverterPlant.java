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
import java.util.Map.Entry;
import java.util.Set;

import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.DeviceClass;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.InverterQuery;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.LRIDefinition;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.SmaUserGroup;
import org.openhab.binding.sma.internal.layers.BinaryInputStream;
import org.openhab.binding.sma.internal.layers.BinaryOutputStream;
import org.openhab.binding.sma.internal.layers.Bluetooth;
import org.openhab.binding.sma.internal.layers.DataFrame;
import org.openhab.binding.sma.internal.layers.InnerFrame;
import org.openhab.binding.sma.internal.layers.InnerFrame.Address;
import org.openhab.binding.sma.internal.layers.OuterFrame;
import org.openhab.binding.sma.internal.layers.PPPFrame;
import org.openhab.binding.sma.internal.layers.Utils;
import org.openhab.binding.sma.internal.layers.Value;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothSolarInverterPlant {

    private final Logger logger = LoggerFactory.getLogger(BluetoothSolarInverterPlant.class);

    private final int connectRetries = 10;
    private boolean isInit = false;
    private SmaBluetoothAddress rootDeviceAdress;

    private ArrayList<BluetoothSolarInverterPlant.Data> inverters;
    protected Map<String, BluetoothSolarInverterPlant.Data> invertersByAddress;
    protected Map<SmaSerial, BluetoothSolarInverterPlant.Data> invertersBySerial;

    protected Bluetooth layer;

    private short pcktID = 1;

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
            layer.sendOuterFrame(new OuterFrame(OuterFrame.CMD_CONF_ACCES_REQ, layer.getLocalAddress(),
                    new SmaBluetoothAddress(new byte[] { 0x01, 0x00, 0x00, 0x00, 0x00, 0x00 }), //
                    new BinaryOutputStream()//
                            .writeBytes("ver\r\n")//
                            .toByteArray()));

            // This can take up to 3 seconds!
            OuterFrame frame = layer.receiveOuterFrame(OuterFrame.CMD_VERINFO);
            int netID = frame.getPayload()[4];
            logger.debug("SMA netID = {}\n", netID);

            // check root device Address
            layer.sendOuterFrame(new OuterFrame(OuterFrame.CMD_VERINFO, layer.getLocalAddress(), layer.getDestAddress(), //
                    new BinaryOutputStream()//
                            .writeInt(0x00700400)//
                            .writeByte(netID)//
                            .writeInt(0)//
                            .writeInt(1)//
                            .toByteArray()));

            // Connection to Root Device
            frame = layer.receiveOuterFrame(OuterFrame.CMD_ROOTID);
            byte[] data1 = frame.getPayload();

            // If Root Device has changed, copy the new address
            if (data1[6] == 2) {
                rootDeviceAdress.setAddress(data1, 0);
                layer.setDestAddress(rootDeviceAdress);
            }
            logger.debug("Root device address: {}", rootDeviceAdress);

            // Get local BT address
            localDeviceAdress.setAddress(data1, 7);
            layer.setLocalAddress(localDeviceAdress);
            logger.debug("Local BT address: {}", localDeviceAdress);

            frame = layer.receiveOuterFrame(OuterFrame.CMD_NODEINFO);
            byte[] data2 = frame.getPayload();

            // Get network topology
            int devcount = 1;
            inverters = new ArrayList<BluetoothSolarInverterPlant.Data>();

            for (int ptr = 0; ptr < data2.length; ptr += 8) {
                SmaBluetoothAddress address = new SmaBluetoothAddress(data2, ptr);
                // Inverters only - Ignore other devices
                if (data2[ptr + 6] == 0x01 && data2[ptr + 7] == 0x01) {
                    logger.debug("Device {}: found SMA Inverter @ {}", devcount, address);
                    Data inverter = new BluetoothSolarInverterPlant.Data(address);
                    inverter.setNetID(netID);
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
                layer.sendOuterFrame(
                        new OuterFrame(OuterFrame.CMD_PARAMREQ, layer.getLocalAddress(), layer.getDestAddress(), //
                                new BinaryOutputStream()//
                                        .writeShort(0x000A)//
                                        .writeByte(0xAC)//
                                        .toByteArray()));
                layer.receiveOuterFrame(OuterFrame.CMD_PARAMCONFIRM);

                layer.sendOuterFrame(
                        new OuterFrame(OuterFrame.CMD_PARAMREQ, layer.getLocalAddress(), layer.getDestAddress(), //
                                new BinaryOutputStream()//
                                        .writeShort(0x0002)//
                                        .toByteArray()));
                layer.receiveOuterFrame(OuterFrame.CMD_PARAMCONFIRM);

                layer.sendOuterFrame(
                        new OuterFrame(OuterFrame.CMD_PARAMREQ, layer.getLocalAddress(), layer.getDestAddress(), //
                                new BinaryOutputStream()//
                                        .writeShort(0x0001)//
                                        .writeByte(0x01)//
                                        .toByteArray()));
                layer.receiveOuterFrame(OuterFrame.CMD_PARAMCONFIRM);

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
                        frame = layer.receiveOuterFrame(OuterFrame.CMD_ANY);
                        packetType = frame.getCommand();
                        break;
                    } catch (IOException e) {
                    }

                }

                if (packetType == 0) // unable to build inverter network
                {
                    throw new IOException("In case of single inverter system set MIS_Enabled=0 in config file.");
                }

                if (OuterFrame.CMD_UPLINK_TABLE == packetType) {
                    packetType = 0; // reset it
                    frame = layer.receiveOuterFrame(OuterFrame.CMD_NODEINFO);
                    packetType = frame.getCommand();
                }

                logger.debug("PacketType ({})\n", packetType);

                if (OuterFrame.CMD_NODEINFO == packetType) {
                    /*
                     * Get network topology Overwrite all found inverters
                     * starting at index 1
                     */

                    // Get network topology
                    byte[] data3 = frame.getPayload();
                    int pcktsize = frame.getPayload().length;
                    devcount = 1;
                    inverters.clear();

                    for (int ptr = 0; ptr < pcktsize; ptr += 8) {
                        if (logger.isDebugEnabled()) {
                            SmaBluetoothAddress dest = new SmaBluetoothAddress(data3, ptr);
                            logger.debug("Device {}: {} -> ", devcount, dest);
                        }

                        // Inverters only - Ignore other devices
                        if (data3[ptr + 6] == 0x01 && data3[ptr + 7] == 0x01) {
                            logger.debug("Inverter");

                            SmaBluetoothAddress address = new SmaBluetoothAddress(data3, ptr);

                            BluetoothSolarInverterPlant.Data inverter = new BluetoothSolarInverterPlant.Data(address);
                            inverter.setNetID(netID);
                            inverters.add(inverter);

                            devcount++;
                        }
                    }
                }

                /*
                 * At this point our network should be ready! In some cases
                 * 0x0005 and 0x1001 are missing and we have already received
                 * 0x0006 (NETWORK IS READY") If not, just wait for it and
                 * ignore any error
                 */
                if (OuterFrame.CMD_NETW_ESTAB != packetType) {
                    frame = layer.receiveOuterFrame(OuterFrame.CMD_NETW_ESTAB);
                    packetType = frame.getCommand();
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
            layer.sendOuterFrame(
                    new OuterFrame(OuterFrame.CMD_USERDATA, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                            new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, InnerFrame.CONTROL, InnerFrame.PROTOCOL, //
                                    new InnerFrame(destAny(), srcMyself(0x00), 0x00, ++pcktID) //
                                            .stream()//
                                            .writeInt(0x00000200)//
                                            .writeInt(0x0)//
                                            .writeInt(0x0)//
                                            .toByteArray())));

            // All inverters *should* reply with their SUSyID & SerialNr
            // (and some other unknown info)
            SmaBluetoothAddress address = new SmaBluetoothAddress();
            for (int i = 0; i < inverters.size(); i++) {
                PPPFrame pppFrame = layer.receivePPPFrame(pcktID);
                byte[] data4 = pppFrame.getPayload();
                address = pppFrame.getFrameSourceAddress();

                BluetoothSolarInverterPlant.Data current = this.invertersByAddress.get(address.toString());
                if (current != null) {
                    SmaSerial serial = new SmaSerial((short) Utils.getShort(data4, 50), Utils.getInt(data4, 52));
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
        }
    }

    public void exit() {
        layer.close();
    }

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

            layer.sendOuterFrame(
                    new OuterFrame(OuterFrame.CMD_USERDATA, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                            new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, InnerFrame.CONTROL, InnerFrame.PROTOCOL,
                                    new InnerFrame(destAny(), srcMyself(0x01), 0x01, ++pcktID)//
                                            .stream()//
                                            .writeInt(0xFFFD040C)//
                                            .writeInt(userGroup.getValue()) // User / Installer
                                            .writeInt(0x00000384) // Timeout = 900sec ?
                                            .writeInt(now)//
                                            .writeInt(0x0)//
                                            .writeBytes(pw)//
                                            .toByteArray())));

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

    public void logoff() throws IOException {
        logger.debug("logoff SMA Inverter");
        try {
            layer.sendOuterFrame(
                    new OuterFrame(OuterFrame.CMD_USERDATA, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                            new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, InnerFrame.CONTROL, InnerFrame.PROTOCOL,
                                    new InnerFrame(destAny(), srcMyself(0x03), 0x03, ++pcktID) //
                                            .stream()//
                                            .writeInt(0xFFFD010E)//
                                            .writeInt(0xFFFFFFFF)//
                                            .toByteArray())));
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

            layer.sendOuterFrame(new OuterFrame(OuterFrame.CMD_USERDATA, layer.getLocalAddress(), rootDeviceAdress, //
                    new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, InnerFrame.CONTROL, InnerFrame.PROTOCOL,
                            new InnerFrame(destAny(), srcMyself(0x00), 0x00, ++pcktID)//
                                    .stream() //
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
                                    .toByteArray())));
        } catch (IOException e) {
            throw new IOException("setInverterTime failed: " + e.getMessage());
        }
    }

    public ArrayList<BluetoothSolarInverterPlant.Data> getInverters() {
        return inverters;
    }

    public static final void readBTAddress(byte[] src, byte[] dest, int start) {
        dest[0] = src[start + 5];
        dest[1] = src[start + 4];
        dest[2] = src[start + 3];
        dest[3] = src[start + 2];
        dest[4] = src[start + 1];
        dest[5] = src[start + 0];
    }

    public boolean getInverterData(InverterQuery type) {
        logger.debug("getInverterData({})\n", type);

        if (type == InverterQuery.None) {
            return false;
        }

        boolean validPcktID = false;

        try {
            layer.sendOuterFrame(
                    new OuterFrame(OuterFrame.CMD_USERDATA, layer.getLocalAddress(), SmaBluetoothAddress.BROADCAST, //
                            new PPPFrame(PPPFrame.HDLC_ADR_BROADCAST, InnerFrame.CONTROL, InnerFrame.PROTOCOL, //
                                    new InnerFrame(destAny(), srcMyself(0x00), 0x00, ++pcktID)//
                                            .stream()//
                                            .writeInt(type.getCommand())//
                                            .writeInt(type.getFirst())//
                                            .writeInt(type.getLast())//
                                            .toByteArray())));

            for (int j = 0; j < inverters.size(); j++) {
                validPcktID = false;
                do {
                    PPPFrame frame = layer.receivePPPFrame(pcktID);
                    byte[] data = frame.getPayload();
                    BinaryInputStream rd = new BinaryInputStream(data);

                    DataFrame hdr = new DataFrame();
                    hdr.read(rd);

                    if (hdr.getStatus() != 0) {
                        logger.error("Header status is {} != 0", hdr.getStatus());
                        return false;
                    }

                    SmaSerial serial = new SmaSerial(hdr.getSrcSUSyID(), hdr.getSrcSerial());
                    BluetoothSolarInverterPlant.Data current = invertersBySerial.get(serial);

                    if (current == null) {
                        continue;
                    }

                    validPcktID = true;
                    if (pcktID != hdr.getPcktID()) {
                        logger.warn("expected pcktID {} - received {}", pcktID, hdr.getPcktID());
                        // this was ignored for a long time, so no abort until we know it won't happen...
                    }

                    final int recordsize = hdr.getRecordSize();

                    List<Integer> tags = null;

                    for (int i = 36; i < data.length; i += recordsize) {

                        if (i != rd.tell()) {
                            logger.error("unexpected stream position");
                        }

                        Value val = Value.read(rd, recordsize);

                        if (val.getLri() == LRIDefinition.MeteringDyWhOut) {
                            // This function gives us the current
                            // inverter time
                            current.setInverterTime(val.getDatetime());
                        }

                        switch (val.getLri()) {
                            case GridMsTotW: // SPOT_PACTOT
                                // This function gives us the time when
                                // the inverter was switched off
                                current.setSleepTime(val.getDatetime());
                                current.setValue(val.getLri(), new QuantityType<>(val.getSLongValue(), Units.WATT),
                                        val.getDatetime());
                                break;

                            case OperationHealthSttOk: // INV_PACMAX1
                            case OperationHealthSttWrn: // INV_PACMAX2
                            case OperationHealthSttAlm: // INV_PACMAX3
                                current.setValue(val.getLri(), new QuantityType<>(val.getULongValue(), Units.WATT),
                                        val.getDatetime());
                                break;

                            case GridMsPhVphsA: // SPOT_UAC1
                            case GridMsPhVphsB: // SPOT_UAC2
                            case GridMsPhVphsC: // SPOT_UAC3
                                if (val.getULongValue() != Value.ULong.NAN) {
                                    current.setValue(val.getLri(),
                                            new QuantityType<>(val.getULongValue() / 100.0, Units.VOLT),
                                            val.getDatetime());
                                }
                                break;

                            case GridMsAphsA_1: // SPOT_IAC1
                            case GridMsAphsB_1: // SPOT_IAC2
                            case GridMsAphsC_1: // SPOT_IAC3
                                if (val.getULongValue() != Value.ULong.NAN) {
                                    current.setValue(val.getLri(),
                                            new QuantityType<>(val.getULongValue() / 1000.0, Units.AMPERE),
                                            val.getDatetime());
                                }
                                break;

                            case MeteringTotWhOut: // SPOT_ETOTAL
                            case MeteringDyWhOut: // SPOT_ETODAY
                                current.setValue(val.getLri(), new QuantityType<>(val.getULongValue(), Units.WATT_HOUR),
                                        val.getDatetime());
                                break;

                            case NameplateLocation: // INV_NAME
                                // This function gives us the time when the inverter was switched on
                                current.setWakeupTime(val.getDatetime());
                                current.setDeviceName(val.getStringValue());
                                logger.debug("INV_NAME: {}   {}", current.getDeviceName(), val.getDatetime());
                                break;

                            case NameplatePkgRev: // INV_SWVER
                                current.setSwVersion(Utils.toVersionString(val.getULongValue()));
                                logger.debug("INV_SWVER: '{}' {}", current.getSwVersion(), val.getDatetime());
                                break;

                            case NameplateModel: // INV_TYPE
                                tags = val.getStatusTags();

                                if (!tags.isEmpty()) {
                                    current.setDeviceType(SmaDevice.getModel(tags.get(0)));
                                    current.setValue(val.getLri(), new StringType(current.getDeviceType()),
                                            val.getDatetime());
                                    logger.debug("INV_TYPE: '{}' {}", current.getDeviceType(), val.getDatetime());
                                }
                                break;

                            case NameplateMainModel: // INV_CLASS:
                                tags = val.getStatusTags();

                                if (!tags.isEmpty()) {
                                    current.setDevClass(SmaDevice.DeviceClass.fromOrdinal(tags.get(0)));
                                    current.setValue(val.getLri(), new StringType(current.getDevClass().name()),
                                            val.getDatetime());
                                }
                                break;

                            case OperationHealth: // INV_STATUS:
                                tags = val.getStatusTags();

                                if (!tags.isEmpty()) {
                                    current.setDeviceStatus(tags.get(0));
                                    current.setValue(val.getLri(), new DecimalType(current.getDeviceStatus()),
                                            val.getDatetime());
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

    private Address destAny() {
        return new Address(0xA0, InnerFrame.ANYSUSYID, InnerFrame.ANYSERIAL);
    }

    private Address srcMyself(int header) {
        return new Address(header, InnerFrame.APP_SUSY_ID, InnerFrame.appSerial);
    }

    @Override
    public String toString() {
        return "BluetoothSolarInverterPlant [rootAddress=" + rootDeviceAdress + "]";
    }

    public class Data {

        private SmaBluetoothAddress address;
        private String deviceName;
        private SmaSerial serial;
        private int netID;
        private Date inverterTime;
        private Date wakeupTime;
        private Date sleepTime;
        private String deviceType;
        private DeviceClass devClass;
        private String swVersion; // "03.01.05.R"
        private int deviceStatus;

        private HashMap<LRIDefinition, State> values = new HashMap<LRIDefinition, State>();

        public Data(SmaBluetoothAddress address) {
            super();

            this.address = address;
        }

        public String getBTAddressAsString() {
            return this.address.toString();
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public void setSerial(SmaSerial smaSerial) {
            this.serial = smaSerial;
        }

        public SmaSerial getSerial() {
            return this.serial;
        }

        public int getNetID() {
            return netID;
        }

        public void setNetID(int netID) {
            this.netID = netID;
        }

        public Date getInverterTime() {
            return inverterTime;
        }

        public void setInverterTime(Date inverterTime) {
            this.inverterTime = inverterTime;
        }

        public Date getWakeupTime() {
            return wakeupTime;
        }

        public void setWakeupTime(Date wakeupTime) {
            this.wakeupTime = wakeupTime;
        }

        public Date getSleepTime() {
            return sleepTime;
        }

        public void setSleepTime(Date sleepTime) {
            this.sleepTime = sleepTime;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public DeviceClass getDevClass() {
            return devClass;
        }

        public void setDevClass(DeviceClass devClass) {
            this.devClass = devClass;
        }

        public String getSwVersion() {
            return swVersion;
        }

        public void setSwVersion(String swVersion) {
            this.swVersion = swVersion;
        }

        public int getDeviceStatus() {
            return deviceStatus;
        }

        public void setDeviceStatus(int deviceStatus) {
            this.deviceStatus = deviceStatus;
        }

        public Set<Entry<LRIDefinition, State>> getEntries() {
            return values.entrySet();
        }

        public boolean isValid(LRIDefinition lri) {
            return values.containsKey(lri);
        }

        public State getState(LRIDefinition lri) {
            return values.get(lri);
        }

        public void setValue(LRIDefinition lri, State value, Date date) {
            logger.debug("{}({}): {}   {}", lri.getCode(), serial.suSyID, value.toString(), date);
            values.put(lri, value);
        }

        @Override
        public String toString() {
            return "Data [address=" + address + ", " + "deviceName=" + deviceName + ", netID=" + netID + ", serial="
                    + serial + "]";
        }
    }
}
