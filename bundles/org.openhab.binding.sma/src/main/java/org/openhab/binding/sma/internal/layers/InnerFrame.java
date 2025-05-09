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

    public static class Address {
        public int header;
        public int suSyID;
        public long serial;

        public Address(int header, int suSyID, long serial) {
            this.header = header;
            this.suSyID = suSyID;
            this.serial = serial;
        }
    }

    private int lengthDWords;
    private Address dst;
    private Address src;
    private int ctrl3;
    private int status;
    private int pcktCount;
    private int pcktID;

    protected InnerFrame() {
        dst = new Address(0, 0, 0);
        src = new Address(0, 0, 0);
    }

    public InnerFrame(Address dst, Address src, int ctrl3, short rcvPcktID) {
        this.lengthDWords = 0;
        this.dst = dst;
        this.src = src;
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
        wr.writeByte(0); // written in SMANetFrame.getFrame()
        wr.writeByte(dst.header);
        wr.writeShort(dst.suSyID);
        wr.writeUInt(dst.serial);
        wr.writeByte(0);
        wr.writeByte(src.header);
        wr.writeShort(src.suSyID);
        wr.writeUInt(src.serial);
        wr.writeByte(0);
        wr.writeByte(ctrl3);
        wr.writeShort(status);
        wr.writeShort(pcktCount);
        wr.writeShort(pcktID | 0x8000);
    }

    public void read(BinaryInputStream rd) throws IOException {
        lengthDWords = rd.readByte();
        dst.header = rd.readByte();
        dst.suSyID = (short) rd.readUShort();
        dst.serial = rd.readUInt();
        int b1 = rd.readByte();
        src.header = rd.readByte();
        src.suSyID = (short) rd.readUShort(); // 10
        src.serial = rd.readUInt(); // 12
        int b2 = rd.readByte(); // 16
        ctrl3 = rd.readByte();
        status = rd.readUShort(); // 18
        pcktCount = rd.readUShort(); // 20
        pcktID = rd.readUShort() & 0x7FFF; // 22

        if (b1 != 0 || b2 != 0) {
            logger.error("Fill bytes are {} and {} != 0!!", b1, b2);
        }

        logger.debug("SMAPPPFrame:");
        logger.debug("  longWords = {}", lengthDWords);
        logger.debug("  dstHeader = {}", String.format("0x%02X", dst.header));
        logger.debug("  dstSUSyID = {}", dst.suSyID);
        logger.debug("  dstSerial = {}", dst.serial);
        logger.debug("  srcHeader = {}", String.format("0x%02X", src.header));
        logger.debug("  srcSUSyID = {}", src.suSyID);
        logger.debug("  srcSerial = {}", src.serial);
        logger.debug("  ??          {}", String.format("0x%02X", ctrl3));
        logger.debug("  status    = {}", status);
        logger.debug("  pcktCount = {}", pcktCount);
        logger.debug("  rcvPcktID = {}", pcktID & 0x7FFF);
    }

    public int getLengthDWords() {
        return lengthDWords;
    }

    public short getSrcSUSyID() {
        return (short) src.suSyID;
    }

    public long getSrcSerial() {
        return src.serial;
    }

    public int getStatus() {
        return status;
    }

    public int getPcktID() {
        return pcktID;
    }
}
