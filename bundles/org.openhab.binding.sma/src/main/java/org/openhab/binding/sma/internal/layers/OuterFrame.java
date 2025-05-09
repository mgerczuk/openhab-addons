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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sma.internal.hardware.devices.SmaBluetoothAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class OuterFrame {

    private final Logger logger = LoggerFactory.getLogger(OuterFrame.class);

    // length of package header
    private static final int HEADERLENGTH = 18;

    public static final int CMD_USERDATA = 1;
    public static final int CMD_VERINFO = 2;
    public static final int CMD_PARAMREQ = 3;
    public static final int CMD_PARAMCONFIRM = 4;
    public static final int CMD_NODEINFO = 5;
    public static final int CMD_NETW_ESTAB = 6;
    public static final int CMD_ERRORINDIC = 7;
    public static final int CMD_USERDATAMORE = 8;
    public static final int CMD_ECUDATA = 9;
    public static final int CMD_ROOTID = 10;
    public static final int CMD_INFO_INDICATION = 12;
    public static final int CMD_CONF_ACCES_REQ = 0x0201;
    public static final int CMD_CONF_ACCES_INDICATION = 0x0202;
    public static final int CMD_UPLINK_TABLE = 0x1001;

    public static final int CMD_ANY = 0xFF;

    private final int command;
    private final SmaBluetoothAddress sourceAddress;
    private final SmaBluetoothAddress destinationAddress;
    private final byte[] payload;

    public OuterFrame(int command, SmaBluetoothAddress localaddress, SmaBluetoothAddress destaddress, byte[] payload) {
        this.command = command;
        this.sourceAddress = localaddress;
        this.destinationAddress = destaddress;
        this.payload = payload;
    }

    public OuterFrame(int control, SmaBluetoothAddress localaddress, SmaBluetoothAddress destaddress, SMANetFrame frame)
            throws IOException {
        this.command = control;
        this.sourceAddress = localaddress;
        this.destinationAddress = destaddress;

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        frame.write(os);
        payload = os.toByteArray();
    }

    public int getCommand() {
        return command;
    }

    public SmaBluetoothAddress getSourceAddress() {
        return sourceAddress;
    }

    public SmaBluetoothAddress getDestinationAddress() {
        return destinationAddress;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void write(OutputStream os) throws IOException {
        int totalLength = HEADERLENGTH + payload.length;
        BinaryOutputStream wr = new BinaryOutputStream(totalLength);

        wr.writeByte(PPPFrame.HDLC_SYNC);
        wr.writeShort(totalLength);
        byte[] cs = wr.toByteArray();
        wr.writeByte(cs[0] ^ cs[1] ^ cs[2]);

        wr.writeBytes(sourceAddress.getAddress());
        wr.writeBytes(destinationAddress.getAddress());

        wr.writeShort(command);

        wr.writeBytes(payload);

        byte[] frame = wr.toByteArray();

        if (logger.isTraceEnabled()) {
            logger.trace("Sending {} bytes:\n{}", frame.length, Utils.bytesToHex(frame));
        }

        os.write(frame);
    }

    public static OuterFrame read(InputStream is) throws IOException {
        byte[] header = new byte[HEADERLENGTH];
        if (is.read(header) < header.length) {
            throw new IOException("EOF");
        }

        BinaryInputStream rd = new BinaryInputStream(header);
        int sync = rd.readByte();
        if (sync != PPPFrame.HDLC_SYNC) {
            throw new IOException("SYNC expected");
        }

        int payloadLength = rd.readUShort() - HEADERLENGTH;
        int cs = rd.readByte();
        if ((header[0] ^ header[1] ^ header[2]) != cs) {
            throw new IOException("Checksum error");
        }

        SmaBluetoothAddress localaddress = new SmaBluetoothAddress(rd.readBytes(SmaBluetoothAddress.NBYTES));
        SmaBluetoothAddress destaddress = new SmaBluetoothAddress(rd.readBytes(SmaBluetoothAddress.NBYTES));
        int control = rd.readUShort();

        byte[] payload = new byte[payloadLength];
        if (is.read(payload) < payloadLength) {
            throw new IOException("EOF");
        }

        Logger logger = LoggerFactory.getLogger(OuterFrame.class);
        if (logger.isTraceEnabled()) {
            byte[] all = new byte[HEADERLENGTH + payloadLength];
            System.arraycopy(header, 0, all, 0, HEADERLENGTH);
            System.arraycopy(payload, 0, all, HEADERLENGTH, payloadLength);

            logger.trace("Received {} bytes: \n{}", all.length, Utils.bytesToHex(all));
            logger.trace("source: {}", localaddress.toString());
            logger.trace("destination: {}", destaddress.toString());
            logger.trace("receiving cmd {}", control);

        }

        return new OuterFrame(control, localaddress, destaddress, payload);
    }
}
