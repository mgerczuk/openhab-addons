/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import org.openhab.binding.sma.internal.SmaBinding.Device;
import org.openhab.binding.sma.internal.layers.Bluetooth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class BluetoothSolarInverter extends BluetoothSolarInverterPlant {
    private static final Logger logger = LoggerFactory.getLogger(BluetoothSolarInverter.class);

    public BluetoothSolarInverter(Device device) {
        super(device);
    }

    /*
     * public BluetoothSolarInverter(String address) {
     * super(address);
     * }
     */
    @Override
    public void init() throws IOException {
        this.layer = new Bluetooth(device.getBTAdress());
    }
}
