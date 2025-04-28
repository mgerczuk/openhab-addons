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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataHeader {
    private final Logger logger = LoggerFactory.getLogger(DataHeader.class);

    private int a;
    private short suSyID;
    private long serial;
    private int status;
    private int pcktCount;
    private int rcvPcktID;
    private long c;
    private long b;

    public void read(BinaryInputStream rd) throws IOException {
        a = rd.readByte();
        byte unkn1[] = new byte[9];
        for (int i = 0; i < 9; i++) {
            unkn1[i] = (byte) rd.readByte();
        }
        suSyID = (short) rd.readUShort(); // 10
        serial = rd.readUInt(); // 12
        int unkn2 = rd.readUShort(); // 16
        status = rd.readUShort(); // 18
        pcktCount = rd.readUShort(); // 20
        rcvPcktID = rd.readUShort(); // 22
        long unkn3 = rd.readUInt(); // 24
        c = rd.readUInt(); // 28
        b = rd.readUInt(); // 32

        logger.debug("DataHeader:");
        logger.debug("  a         = {}", a);
        logger.debug("              {}", Utils.bytesToHexRaw(unkn1));
        logger.debug("  suSyID    = {}", suSyID);
        logger.debug("  serial    = {}", serial);
        logger.debug("              {}", String.format("%04X", unkn2));
        logger.debug("  status    = {}", status);
        logger.debug("  pcktCount = {}", pcktCount);
        logger.debug("  rcvPcktID = {}", rcvPcktID & 0x7FFF);
        logger.debug("              {}", String.format("%08X", unkn3));
        logger.debug("  c         = {}", c);
        logger.debug("  b         = {}", b);
    }

    public short getSuSyID() {
        return suSyID;
    }

    public long getSerial() {
        return serial;
    }

    public int getStatus() {
        return status;
    }

    public int getPcktID() {
        return rcvPcktID & 0x7FFF;
    }

    public int getRecordSize() {
        return 4 * (a - 9) / (int) (b - c + 1);
    }

}
