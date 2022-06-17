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
package org.openhab.binding.sma.internal.layers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.TimeZone;

import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read/write frames from/to Bluetooth connection
 *
 * @author Martin Gerczuk - Initial contribution
 */
public class Bluetooth {

    private static final Logger logger = LoggerFactory.getLogger(Bluetooth.class);

    private static final long READ_TIMEOUT_MILLIS = 15000;

    private SmaBluetoothAddress localAddress = new SmaBluetoothAddress();
    private SmaBluetoothAddress destAddress;

    protected static StreamConnection connection;
    protected static DataOutputStream out;
    protected static InputStream in;

    public Bluetooth(SmaBluetoothAddress destAdress) {
        super();

        this.destAddress = destAdress;
    }

    public SmaBluetoothAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(SmaBluetoothAddress localAddress) {
        this.localAddress = localAddress;
    }

    public SmaBluetoothAddress getDestAddress() {
        return destAddress;
    }

    public void setDestAddress(SmaBluetoothAddress destAddress) {
        this.destAddress = destAddress;
    }

    protected StreamConnection getConnection() throws IOException {
        return (StreamConnection) Connector.open(destAddress.getConnectorString());
    }

    public boolean isOpen() {
        return (connection != null);
    }

    public void open() throws IOException {

        close();
        if (connection == null) {
            logger.debug("Bluetooth({}).open()", System.identityHashCode(this));

            connection = getConnection();

            out = connection.openDataOutputStream();
            in = new TimeoutInputStream(connection.openDataInputStream(), READ_TIMEOUT_MILLIS);
        }
    }

    public void close() {

        if (connection != null) {
            try {
                logger.debug("Bluetooth({}).close()", System.identityHashCode(this));

                out.close();
                in.close();
                connection.close();
            } catch (IOException e) {
                logger.error("Error closing Bluetooth socket", e);
            }
        }

        connection = null;
        out = null;
        in = null;
    }

    public void sendSMAFrame(SMAFrame frame) throws IOException {

        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        frame.write(temp);
        byte[] buffer = temp.toByteArray();

        logger.trace("Sending {} bytes:\n{}", buffer.length, Utils.bytesToHex(buffer));
        out.write(buffer);
    }

    public SMAFrame receiveSMAFrame(int wait4Command) throws IOException {

        logger.trace("receiveSMAFrame(...,{})", wait4Command);

        int command = 0;
        SMAFrame f = null;

        do {
            f = SMAFrame.read(in);

            logger.trace("data received: \n{}", Utils.bytesToHex(f.getFrame()));

            if (destAddress.equals(f.getSourceAddress())) {

                logger.trace("source: {}", f.getSourceAddress().toString());
                logger.trace("destination: {}", f.getDestinationAddress().toString());

                logger.trace("receiving cmd {}", f.getControl());

                command = f.getControl();
            }
        } while ((command != wait4Command) && (0xFF != wait4Command));

        return f;
    }

    public PPPFrame receivePPPFrame(short pktId) throws IOException {

        logger.trace("receivePPPFrame({})", pktId);

        PPPFrame ppp = null;
        short rcvpcktID = -1;
        SmaBluetoothAddress lastSourceAddress;

        do {
            int command = 0;
            ByteArrayOutputStream os = null;
            SMAFrame f = null;

            do {
                f = SMAFrame.read(in);

                logger.trace("data received: \n{}", Utils.bytesToHex(f.getFrame()));
                logger.trace("source: {}", f.getSourceAddress().toString());
                logger.trace("destination: {}", f.getDestinationAddress().toString());

                logger.trace("receiving cmd {}", f.getControl());

                lastSourceAddress = f.getSourceAddress();

                command = f.getControl();
                if (PPPFrame.peek(f.getPayload(), PPPFrame.HDLC_ADR_BROADCAST, SMAPPPFrame.CONTROL,
                        SMAPPPFrame.PROTOCOL)) {
                    os = new ByteArrayOutputStream();
                }

                if (os != null) {
                    os.write(f.getPayload());
                }

            } while (command != 1);

            if (os != null) {
                ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
                ppp = PPPFrame.read(is);
                ppp.setFrameSourceAddress(lastSourceAddress);
            }

            rcvpcktID = (ppp == null || ppp.payload.length < 24) ? -1
                    : (short) (Utils.getShort(ppp.payload, 22) & 0x7FFF);

            if (ppp != null) {
                logger.trace("rcvpcktID id {}", rcvpcktID);
            }

        } while (rcvpcktID != pktId);

        return ppp;
    }

    // TODO: find better place
    public int currentTimeSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    // TODO: find better place
    public int getTimezoneOffset() {
        TimeZone timeZone = TimeZone.getDefault();
        return timeZone.getOffset(new Date().getTime()) / 1000;
    }
}
