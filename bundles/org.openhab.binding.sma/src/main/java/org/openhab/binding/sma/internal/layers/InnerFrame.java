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
    private int dstHeader;
    private int dstSUSyID;
    private long dstSerial;
    private int srcHeader;
    private short srcSUSyID;
    private long srcSerial;
    private int ctrl3;
    private int status;
    private int pcktCount;
    private int pcktID;

    protected InnerFrame() {
    }

    public InnerFrame(int dstHeader, int dstSUSyID, long dstSerial, int srcHeader, int ctrl3, short rcvPcktID) {
        this.lengthDWords = 0;
        this.dstHeader = dstHeader;
        this.dstSUSyID = dstSUSyID;
        this.dstSerial = dstSerial;
        this.srcHeader = srcHeader;
        this.srcSUSyID = APP_SUSY_ID;
        this.srcSerial = appSerial;
        this.ctrl3 = ctrl3;
        this.status = 0;
        this.pcktCount = 0;
        this.pcktID = rcvPcktID;
    }

    public BinaryOutputStream stream() throws IOException {
        BinaryOutputStream b = new BinaryOutputStream();
        write(b);
        return b;
    }

    public void write(BinaryOutputStream wr) throws IOException {
        wr.writeByte(0); // written in PPPFrame.getFrame()
        wr.writeByte(dstHeader);
        wr.writeShort(dstSUSyID);
        wr.writeUInt(dstSerial);
        wr.writeByte(0);
        wr.writeByte(srcHeader);
        wr.writeShort(srcSUSyID);
        wr.writeUInt(srcSerial);
        wr.writeByte(0);
        wr.writeByte(ctrl3);
        wr.writeShort(status);
        wr.writeShort(pcktCount);
        wr.writeShort(pcktID | 0x8000);
    }

    public void read(BinaryInputStream rd) throws IOException {
        lengthDWords = rd.readByte();
        dstHeader = rd.readByte();
        dstSUSyID = (short) rd.readUShort();
        dstSerial = rd.readUInt();
        rd.readByte();
        srcHeader = rd.readByte();
        srcSUSyID = (short) rd.readUShort(); // 10
        srcSerial = rd.readUInt(); // 12
        rd.readByte(); // 16
        ctrl3 = rd.readByte();
        status = rd.readUShort(); // 18
        pcktCount = rd.readUShort(); // 20
        pcktID = rd.readUShort() & 0x7FFF; // 22

        logger.debug("SMAPPPFrame:");
        logger.debug("  longWords = {}", lengthDWords);
        logger.debug("  dstHeader = {}", String.format("0x%02X", dstHeader));
        logger.debug("  dstSUSyID = {}", dstSUSyID);
        logger.debug("  dstSerial = {}", dstSerial);
        logger.debug("  srcHeader = {}", String.format("0x%02X", srcHeader));
        logger.debug("  srcSUSyID = {}", srcSUSyID);
        logger.debug("  srcSerial = {}", srcSerial);
        logger.debug("  ??          {}", String.format("0x%02X", ctrl3));
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
