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
 * @author Martin Gerczuk - Initial contribution
 */
public class Bluetooth {

    private static final Logger logger = LoggerFactory.getLogger(Bluetooth.class);

    private static final long READ_TIMEOUT_MILLIS = 15000;

    // stores address in low endian
    public SmaBluetoothAddress localAddress = new SmaBluetoothAddress();
    public SmaBluetoothAddress destAddress;

    protected static StreamConnection connection;
    protected static DataOutputStream out;
    protected static InputStream in;

    public Bluetooth(SmaBluetoothAddress destAdress) {
        super();

        this.destAddress = destAdress;
    }

    public Bluetooth(String destAd) {
        this(destAd, 1);
    }

    public Bluetooth(String destAdr, int port) {
        super();

        this.destAddress = new SmaBluetoothAddress(destAdr, port);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        if (connection != null) {
            logger.error("Bluetooth({}).finalize(): resource leak!", System.identityHashCode(this));
        }
    }

    protected StreamConnection getConnection() throws IOException {
        return (StreamConnection) Connector.open(destAddress.getConnectorString());
    }

    private SmaBluetoothAddress currentHeaderAddress; // TODO: get rid of hack!

    public SmaBluetoothAddress getHeaderAddress() {
        return currentHeaderAddress;
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

    public void sendFrame(SMAFrame frame) throws IOException {

        ByteArrayOutputStream temp = new ByteArrayOutputStream();
        frame.write(temp);
        byte[] buffer = temp.toByteArray();

        logger.trace("Sending {} bytes:\n{}", buffer.length, Utils.bytesToHex(buffer));
        out.write(buffer);
    }

    public byte[] receive(int wait4Command) throws IOException {
        return receive(destAddress, wait4Command);
    }

    public byte[] receiveAll(int wait4Command) throws IOException {
        return receive(SmaBluetoothAddress.BROADCAST, wait4Command);
    }

    protected byte[] receive(SmaBluetoothAddress destAddress, int wait4Command) throws IOException {

        logger.trace("receive(...,{})", wait4Command);

        int command = 0;
        ByteArrayOutputStream os = null;
        SMAFrame f = null;

        do {
            f = SMAFrame.read(in);

            logger.trace("data received: \n{}", Utils.bytesToHex(f.getFrame()));

            if (destAddress.equals(f.getSourceAddress())) {

                logger.trace("source: {}", f.getSourceAddress().toString());
                logger.trace("destination: {}", f.getDestinationAddress().toString());

                logger.trace("receiving cmd {}", f.getControl());

                currentHeaderAddress = f.getSourceAddress();

                command = f.getControl();
                if (PPPFrame.peek(f.getPayload(), PPPFrame.HDLC_ADR_BROADCAST, SMAPPPFrame.CONTROL,
                        SMAPPPFrame.PROTOCOL)) {
                    os = new ByteArrayOutputStream();
                }

                if (os != null) {
                    os.write(f.getPayload());
                }
            }
        } while ((command != wait4Command) && (0xFF != wait4Command));

        if (os != null) {
            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            PPPFrame pf = PPPFrame.read(is);
            return pf.getFrame(); // TODO: getPayload()
        }

        return f.getFrame(); // TODO: getPayload()
    }

    public int currentTimeSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public int getTimezoneOffset() {
        TimeZone timeZone = TimeZone.getDefault();
        return timeZone.getOffset(new Date().getTime()) / 1000;
    }
}
