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

import org.openhab.binding.sma.internal.SmaBinding;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class EthernetSolarInverter extends SolarInverter {

    public EthernetSolarInverter(SmaBinding.Device dev) {
        super(dev);
    }

    @Override
    public void init() throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public void exit() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getValueAsString(LRIDefinition element) {
        // TODO Auto-generated method stub
        return "";
    }

    @Override
    protected boolean getInverterData(InverterDataType energyproduction) {
        return false;
        // TODO Auto-generated method stub
    }

    @Override
    public void logon(SmaUserGroup userGroup, String password) throws IOException {
        // TODO Auto-generated method stub
    }

    @Override
    public void logoff() throws IOException {
        // TODO Auto-generated method stub
    }
}
