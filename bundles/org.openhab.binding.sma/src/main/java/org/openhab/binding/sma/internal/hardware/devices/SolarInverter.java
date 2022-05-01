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
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.openhab.binding.sma.internal.SmaBinding.Device;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public abstract class SolarInverter extends AbstractSmaDevice {
    private static final Logger logger = LoggerFactory.getLogger(SolarInverter.class);

    protected Device device;
    protected Data data;

    protected short pcktID = 1;

    public SolarInverter(Device device) {
        super();
        this.device = device;
    }

    protected abstract boolean getInverterData(InverterDataType type);

    public SmaSerial getSerial() {
        return data.serial;
    }

    public void setSerial(SmaSerial serial) {
        this.data.serial = serial;
    }

    @Override
    public String getValueAsString(LRIDefinition element) {
        InverterDataType data = element.getData();
        /*
         * if (!this.hasValidValues(data)) {
         * this.getInverterData(data);
         * }
         *
         * if ((flags & InverterDataType.EnergyProduction.getValue()) == 0)
         * this.getInverterData(InverterDataType.EnergyProduction);
         */

        try {

            this.logon(device.isLoginAsInstaller() ? SmaUserGroup.Installer : SmaUserGroup.User, device.getPassword());
            this.getInverterData(element.getData());
            this.logoff();

        } catch (IOException e) {
            logger.error("Login failed!", e);
        }

        // TODO Auto-generated method stub
        return "";
    }

    private boolean hasValidValues(InverterDataType data) {
        return false; // ((flags & data.getValue()) != 0);
    }

    public static abstract class Data {

        private HashMap<LRIDefinition, State> values = new HashMap<LRIDefinition, State>();

        public Data() {
        }

        private String deviceName;
        private SmaSerial serial;
        protected int netID;

        // public short suSyID;
        // protected int serial;
        // protected float btSignal;
        protected Date inverterTime;
        protected Date wakeupTime;
        protected Date sleepTime;
        protected long pdc1, pdc2;
        protected long udc1, udc2;
        protected long idc1, idc2;
        protected long pmax1, pmax2, pmax3;
        // protected long totalPac;
        protected long pac1, pac2, pac3;
        // protected long uac1, uac2, uac3;
        protected long iac1, iac2, iac3;
        protected long gridFreq;
        protected long operationTime;
        protected long feedInTime;
        // public long eToday;
        // public long eTotal;
        // protected short modelID;
        private String deviceType;

        public String deviceClass;
        protected DeviceClass devClass;
        public String swVersion; // "03.01.05.R"
        protected int deviceStatus;
        protected int gridRelayStatus;

        // Flag to signal which data is already loaded
        protected int flags;

        public int getNetID() {
            return netID;
        }

        public void setNetID(int netID) {
            this.netID = netID;
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

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
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

        public void setValue(LRIDefinition lri, double value) {
            values.put(lri, new DecimalType(value));
        }

        public void setValue(LRIDefinition lri, String value) {
            values.put(lri, new StringType(value));
        }

        public boolean sumDataAvailable() {
            return getState(SmaDevice.LRIDefinition.MeteringTotWhOut) != null;
        }

        @Override
        public String toString() {
            return "deviceName=" + getDeviceName() + ", netID=" + netID + ", serial=" + serial;
        }
    }
}
