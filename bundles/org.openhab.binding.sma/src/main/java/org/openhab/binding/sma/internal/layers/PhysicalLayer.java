/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

/**
 * @author Martin Gerczuk - Initial contribution
 */
public interface PhysicalLayer {

    public static final short ANYSUSYID = (short) 0xFFFF;
    public static final int ANYSERIAL = 0xFFFFFFFF;

    public void open() throws IOException;

    public void close();

    public void send() throws IOException;

    public byte[] receive(int i) throws IOException;

    public void writePacketHeader(int control);

    public void writePacketTrailer();

    public void writePacket(byte longwords, byte ctrl, short ctrl2, short dstSUSyID, int dstSerial, short pcktID);

    public void write(final byte[] bytes, int loopcount);

    public void writeByte(byte v);

    public void writeShort(short v);

    public void write(int v);

    void writePacketLength();

    boolean isCrcValid();
}
