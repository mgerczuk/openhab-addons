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
import java.util.Random;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class InnerFrame {
    public static final byte CONTROL = 0x03;
    public static final short PROTOCOL = 0x6065;

    // Generate a Serial Number for application
    public static final short APP_SUSY_ID = 125;
    public static int appSerial = generateAppSerial();

    public static final short ANYSUSYID = (short) 0xFFFF;
    public static final int ANYSERIAL = 0xFFFFFFFF;

    private static int generateAppSerial() {
        Random randomGenerator = new Random();
        return 900000000 + randomGenerator.nextInt(100000000);
    }

    protected final Logger logger = LoggerFactory.getLogger(InnerFrame.class);

    private int lengthDWords;
    private int ctrl;
    private int dstSUSyID;
    private long dstSerial;
    private int ctrl2;
    private short srcSUSyID;
    private long srcSerial;
    private int ctrl3;
    private int status;
    private int pcktCount;
    private int pcktID;

    public InnerFrame() {
    }

    public InnerFrame(int lengthDWords, int ctrl, int dstSUSyID, long dstSerial, int ctrl2, short srcSUSyID,
            long srcSerial, int ctrl3, int status, int pcktCount, int rcvPcktID) {
        this.lengthDWords = lengthDWords;
        this.ctrl = ctrl;
        this.dstSUSyID = dstSUSyID;
        this.dstSerial = dstSerial;
        this.ctrl2 = ctrl2;
        this.srcSUSyID = srcSUSyID;
        this.srcSerial = srcSerial;
        this.ctrl3 = ctrl3;
        this.status = status;
        this.pcktCount = pcktCount;
        this.pcktID = rcvPcktID;
    }

    public static BinaryOutputStream writePppHeader(byte longwords, byte ctrl, short ctrl2, short dstSUSyID,
            int dstSerial, short pcktID) throws IOException {
        BinaryOutputStream b = new BinaryOutputStream();
        new InnerFrame(longwords, ctrl, dstSUSyID, dstSerial, ctrl2, APP_SUSY_ID, appSerial, ctrl2, 0, 0, pcktID)
                .write(b);

        return b;
    }

    public void write(BinaryOutputStream wr) throws IOException {
        wr.writeByte(lengthDWords);
        wr.writeByte(ctrl);
        wr.writeShort(dstSUSyID);
        wr.writeUInt(dstSerial);
        wr.writeShort(ctrl2);
        wr.writeShort(srcSUSyID);
        wr.writeUInt(srcSerial);
        wr.writeShort(ctrl3);
        wr.writeShort(status);
        wr.writeShort(pcktCount);
        wr.writeShort(pcktID | 0x8000);
    }

    public void read(BinaryInputStream rd) throws IOException {
        lengthDWords = rd.readByte();
        ctrl = rd.readByte();
        dstSUSyID = (short) rd.readUShort();
        dstSerial = rd.readUInt();
        ctrl2 = rd.readUShort();
        srcSUSyID = (short) rd.readUShort(); // 10
        srcSerial = rd.readUInt(); // 12
        ctrl3 = rd.readUShort(); // 16
        status = rd.readUShort(); // 18
        pcktCount = rd.readUShort(); // 20
        pcktID = rd.readUShort() & 0x7FFF; // 22

        logger.debug("SMAPPPFrame:");
        logger.debug("  longWords = {}", lengthDWords);
        logger.debug("  ctrl      = {}", String.format("0x%02X", ctrl));
        logger.debug("  dstSUSyID = {}", dstSUSyID);
        logger.debug("  dstSerial = {}", dstSerial);
        logger.debug("  ctrl2     = {}", String.format("0x%04X", ctrl2));
        logger.debug("  srcSUSyID = {}", srcSUSyID);
        logger.debug("  srcSerial = {}", srcSerial);
        logger.debug("              {}", String.format("0x%04X", ctrl3));
        logger.debug("  status    = {}", status);
        logger.debug("  pcktCount = {}", pcktCount);
        logger.debug("  rcvPcktID = {}", pcktID & 0x7FFF);
    }

    public int getLengthDWords() {
        return lengthDWords;
    }

    public short getSrcSUSyID() {
        return srcSUSyID;
    }

    public long getSrcSerial() {
        return srcSerial;
    }

    public int getStatus() {
        return status;
    }

    public int getPcktID() {
        return pcktID;
    }
}
