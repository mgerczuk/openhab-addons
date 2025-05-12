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
package org.openhab.binding.sma.internal.layers;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;

/**
 * PPP frame as used by SMA
 *
 * @author Martin Gerczuk - Initial contribution
 */
public final class SMANetFrame extends PPPFrame {

    public static final byte CONTROL = 0x03;
    public static final short PROTOCOL = 0x6065;

    // storage for source address of outer frame
    private SmaBluetoothAddress frameSourceAddress = new SmaBluetoothAddress();

    public SMANetFrame(byte[] payload) {
        super(HDLC_ADR_BROADCAST, CONTROL, PROTOCOL, payload);

        assert payload.length % 4 == 0;
    }

    private SMANetFrame(@NonNull InputStream is) throws IOException {
        super(is);
    }

    @Override
    public byte @NonNull [] getFrame() {
        // recalc length
        payload[0] = (byte) (payload.length / 4);

        return super.getFrame();
    }

    public static boolean peek(byte[] header) {
        return header.length >= 5 && header[0] == HDLC_SYNC && header[1] == HDLC_ADR_BROADCAST && header[2] == CONTROL
                && be2short(header, 3) == PROTOCOL;
    }

    public static SMANetFrame read(InputStream is) throws IOException {
        return new SMANetFrame(is);
    }

    public SmaBluetoothAddress getFrameSourceAddress() {
        return frameSourceAddress;
    }

    public void setFrameSourceAddress(SmaBluetoothAddress currentHeaderAddress) {
        this.frameSourceAddress = currentHeaderAddress;
    }
}
