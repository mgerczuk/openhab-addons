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
public class Bluetooth extends AbstractPhysicalLayer {

    private static final Logger logger = LoggerFactory.getLogger(Bluetooth.class);

    private static final long READ_TIMEOUT_MILLIS = 15000;

    private static final int HDLC_ESC = 0x7d;

    private static final int HDLC_SYNC = 0x7e;

    // length of package header
    public static final int HEADERLENGTH = 18;

    protected static final int L2SIGNATURE = 0x656003FF;

    // stores address in low endian
    public SmaBluetoothAddress localAddress = new SmaBluetoothAddress();
    public SmaBluetoothAddress destAddress;

    CRC crc = new CRC();

    protected static StreamConnection connection;
    protected static DataOutputStream out;
    protected static InputStream in;

    private byte[] commBuf;

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

    public SmaBluetoothAddress getHeaderAddress() {
        return new SmaBluetoothAddress(commBuf, 4);
    }

    public boolean isOpen() {
        return (connection != null);
    }

    public void open() throws IOException {

        close();
        if (connection == null) {
            logger.debug("Bluetooth({}).open()", System.identityHashCode(this));

            connection = (StreamConnection) Connector.open(destAddress.getConnectorString());

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

    @Override
    public void writeByte(byte v) {
        // Keep a rolling checksum over the payload
        crc.writeByte(v);

        if (v == HDLC_ESC || v == HDLC_SYNC || v == 0x11 || v == 0x12 || v == 0x13) {
            buffer[packetposition++] = HDLC_ESC;
            buffer[packetposition++] = (byte) (v ^ 0x20);
        } else {
            buffer[packetposition++] = v;
        }
    }

    public void writePppHeader(byte longwords, byte ctrl, short ctrl2, short dstSUSyID, int dstSerial, short pcktID) {
        buffer[packetposition++] = HDLC_SYNC; // Not included in checksum
        write(L2SIGNATURE);
        writeByte(longwords);
        writeByte(ctrl);
        writeShort(dstSUSyID);
        write(dstSerial);
        writeShort(ctrl2);
        writeShort(AppSUSyID);
        write(AppSerial);
        writeShort(ctrl2);
        writeShort((short) 0);
        writeShort((short) 0);
        writeShort((short) (pcktID | 0x8000));
    }

    public void writePppTrailer() {
        short FCSChecksum = crc.get();
        buffer[packetposition++] = (byte) (FCSChecksum & 0x00FF);
        buffer[packetposition++] = (byte) (((FCSChecksum & 0xFF00) >>> 8) & 0x00FF);
        buffer[packetposition++] = HDLC_SYNC; // Trailing byte
    }

    public void writePacketHeader(int control) {
        this.writePacketHeader(control, this.destAddress);
    }

    public void writePacketHeader(int control, SmaBluetoothAddress destaddress) {
        packetposition = 0;
        crc.reset();

        buffer[packetposition++] = HDLC_SYNC;
        buffer[packetposition++] = 0; // placeholder for len1
        buffer[packetposition++] = 0; // placeholder for len2
        buffer[packetposition++] = 0; // placeholder for checksum

        int i;
        for (i = 0; i < 6; i++) {
            buffer[packetposition++] = localAddress.get(i);
        }

        for (i = 0; i < 6; i++) {
            buffer[packetposition++] = destaddress.get(i);
        }

        buffer[packetposition++] = (byte) (control & 0xFF);
        buffer[packetposition++] = (byte) (control >>> 8);
    }

    public void writePacketLength() {
        buffer[1] = (byte) (packetposition & 0xFF); // Lo-Byte
        buffer[2] = (byte) ((packetposition >>> 8) & 0xFF); // Hi-Byte
        buffer[3] = (byte) (buffer[0] ^ buffer[1] ^ buffer[2]); // checksum
    }

    public void send() throws IOException {
        writePacketLength();
        logger.trace("Sending {} bytes:\n{}", packetposition, bytesToHex(buffer, packetposition, ' '));
        out.write(buffer, 0, packetposition);
    }

    public byte[] receive(int wait4Command) throws IOException {
        return receive(destAddress, wait4Command);
    }

    public byte[] receiveAll(int wait4Command) throws IOException {
        return receive(SmaBluetoothAddress.BROADCAST, wait4Command);
    }

    protected byte[] receive(SmaBluetoothAddress destAddress, int wait4Command) throws IOException {
        SmaBluetoothAddress sourceAddr = new SmaBluetoothAddress();
        SmaBluetoothAddress destinationAddr = new SmaBluetoothAddress();
        commBuf = null;

        logger.trace("getPacket({})", wait4Command);

        int index = 0;
        int hasL2pckt = 0;

        int rc = 0;
        int command = 0;
        int bib = 0;
        final byte[] data = new byte[1024];

        do {
            commBuf = new byte[1024];
            bib = read(commBuf, 0, HEADERLENGTH);

            // int SOP = data[0];
            // data are in litle endian. getUnsignedShort exact big endian
            int pkLength = AbstractPhysicalLayer.getShort(commBuf, 1);
            // int pkChecksum = data[3];

            sourceAddr.setAddress(commBuf, 4);
            destinationAddr.setAddress(commBuf, 10);

            command = AbstractPhysicalLayer.getShort(commBuf, 16);

            if (pkLength > HEADERLENGTH) {
                // data = new byte[pkLength - HEADERLENGTH];
                bib += read(commBuf, HEADERLENGTH, pkLength - HEADERLENGTH);

                logger.trace("data received: \n{}", bytesToHex(commBuf, pkLength));
                // Check if data is coming from the right inverter
                if (destAddress.equals(sourceAddr)) {
                    rc = 0;
                    logger.trace("source: {}", sourceAddr.toString());
                    logger.trace("destination: {}", destinationAddr.toString());

                    logger.trace("receiving cmd {}", command);

                    if ((hasL2pckt == 0) && commBuf[18] == (byte) HDLC_SYNC && commBuf[19] == (byte) 0xff
                            && commBuf[20] == (byte) 0x03 && commBuf[21] == (byte) 0x60 && commBuf[22] == (byte) 0x65) // 0x656003FF7E
                    {
                        hasL2pckt = 1;
                    }

                    if (hasL2pckt == 1) {
                        // Copy CommBuf to packetbuffer
                        boolean escNext = false;

                        logger.trace("PacketLength={}", pkLength);

                        for (int i = HEADERLENGTH; i < pkLength; i++) {
                            data[index] = commBuf[i];
                            // Keep 1st byte raw unescaped HDLC_SYNC
                            if (escNext == true) {
                                data[index] ^= 0x20;
                                escNext = false;
                                index++;
                            } else {
                                if (data[index] == HDLC_ESC) {
                                    escNext = true; // Throw away the HDLC_ESC byte
                                } else {
                                    index++;
                                }
                            }
                            if (index >= 520) {
                                logger.warn("Warning: pcktBuf buffer overflow! ({})\n", index);
                                throw new ArrayIndexOutOfBoundsException();
                            }
                        }

                        bib = index;

                        // logger.debug("data decoded: \n{}", bytesToHex(data, data.length));
                    } else {
                        System.arraycopy(commBuf, 0, data, 0, bib);
                    }
                } // isValidSender()
                else {
                    rc = -1; // E_RETRY;
                    logger.debug("Wrong sender: {}", sourceAddr);
                    throw new IOException(String.format("Wrong sender: %s", sourceAddr));
                }

            } else {
                // Check if data is coming from the right inverter
                if (destAddress.equals(sourceAddr)) {
                    bib = commBuf.length;
                    System.arraycopy(commBuf, 0, data, 0, commBuf.length);
                } else {
                    rc = -1; // E_RETRY;
                    logger.debug("Wrong sender: {}", sourceAddr);
                    throw new IOException(String.format("Wrong sender: %s", sourceAddr));
                }
            }
        } while (((command != wait4Command) || (rc == -1/* E_RETRY */)) && (0xFF != wait4Command));

        logger.trace("\n<<<====== Content of pcktBuf =======>>>\n{}\n<<<=================================>>>",
                bytesToHex(data, bib));

        return data;
    }

    @Override
    public boolean isCrcValid() {
        byte lb = buffer[packetposition - 3], hb = buffer[packetposition - 2];

        return !((lb == HDLC_SYNC) || (hb == HDLC_SYNC) || (lb == HDLC_ESC) || (hb == HDLC_ESC));
    }

    protected int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    public int currentTimeSeconds() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    public int getTimezoneOffset() {
        TimeZone timeZone = TimeZone.getDefault();
        return timeZone.getOffset(new Date().getTime()) / 1000;
    }
}
