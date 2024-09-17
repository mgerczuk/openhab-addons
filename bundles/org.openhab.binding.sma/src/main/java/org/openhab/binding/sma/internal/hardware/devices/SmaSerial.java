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

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class SmaSerial {
    public short suSyID;
    public long serial;

    public SmaSerial(short suSyID, long serial) {
        this.suSyID = suSyID;
        this.serial = serial;
    }

    @Override
    public String toString() {
        return "SmaSerial [suSyID=" + suSyID + ", serial=" + serial + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (serial ^ (serial >>> 32));
        result = prime * result + suSyID;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SmaSerial other = (SmaSerial) obj;
        if (serial != other.serial) {
            return false;
        }
        if (suSyID != other.suSyID) {
            return false;
        }
        return true;
    }
}
