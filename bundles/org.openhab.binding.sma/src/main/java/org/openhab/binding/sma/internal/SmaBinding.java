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
package org.openhab.binding.sma.internal;

import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class SmaBinding {

    public class Device {
        private final String plant;
        private final String userPassword;

        public Device(String plant, String userPassword) {
            this.plant = plant;
            this.userPassword = userPassword;
        }

        public SmaBluetoothAddress getBTAdress() {
            return null;
        }

        public String getPassword() {
            return "0000";
        }

        public SmaBluetoothAddress getPlant() {
            return new SmaBluetoothAddress("00:80:25:15:B6:06", 1);
        }

        public boolean isLoginAsInstaller() {
            return false;
        }
    }

    public Device createDevice(String plant, String userPassword) {
        return new Device(plant, userPassword);
    }
}
