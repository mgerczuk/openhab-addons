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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice;
import org.openhab.binding.sma.internal.hardware.devices.SmaDevice.LRIDefinition;

/**
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class Value {
    private int mCls;
    private SmaDevice.LRIDefinition mLri;
    private int mDataType;
    private Date mDatetime;

    private static final int DT_ULONG = 0;
    private static final int DT_STATUS = 8;
    private static final int DT_STRING = 16;
    private static final int DT_FLOAT = 32;
    private static final int DT_SLONG = 64;

    public SmaDevice.LRIDefinition getLri() {
        return mLri;
    }

    public Date getDatetime() {
        return mDatetime;
    }

    public long getULongValue() {
        return ((ULong) this).getValue();
    }

    public List<Integer> getStatusTags() {
        return ((Status) this).getTags();
    }

    public java.lang.String getStringValue() {
        return ((Value.String) this).getValue();
    }

    public long getSLongValue() {
        return ((SLong) this).getValue();
    }

    protected Value(int cls, LRIDefinition lri, int dataType, Date datetime) {
        mCls = cls;
        mLri = lri;
        mDataType = dataType;
        mDatetime = datetime;
    }

    protected Value(Value other) {
        mCls = other.mCls;
        mLri = other.mLri;
        mDataType = other.mDataType;
        mDatetime = other.mDatetime;
    }

    public static Value read(BinaryInputStream rd, int recordSize) throws IOException {
        int startPos = rd.tell();

        int code = (int) rd.readUInt(); // recptr
        SmaDevice.LRIDefinition lri = SmaDevice.LRIDefinition.fromOrdinal(code & 0x00FFFF00);
        int cls = code & 0xFF;
        int dataType = code >>> 24;
        Date datetime = new Date(rd.readUInt() * 1000L); // recptr + 4
        Value d = new Value(cls, lri, dataType, datetime);

        switch (d.mDataType) {
            case DT_ULONG:
                d = new ULong(d, rd, recordSize);
                break;
            case DT_STATUS:
                d = new Status(d, rd, recordSize);
                break;
            case DT_STRING:
                d = new String(d, rd, recordSize);
                break;
            case DT_FLOAT:
                throw new IOException("DT_FLOAT not supported");
            case DT_SLONG:
                d = new SLong(d, rd, recordSize);
                break;
            default:
                throw new IOException("unknown data type " + Integer.toString(d.mDataType));
        }

        rd.seek(startPos + recordSize);
        return d;
    }

    public static class ULong extends Value {

        public static final long NAN = 0xFFFFFFFFFFFFFFFFL;

        long mValue;

        protected long getValue() {
            return mValue;
        }

        protected ULong(Value d, BinaryInputStream rd, int recordSize) throws IOException {
            super(d);
            switch (recordSize) {
                case 16:
                    mValue = rd.readULong();
                    break;
                case 28:
                    rd.readULong();
                    mValue = rd.readUInt();
                    if (mValue == 0xFFFFFFFFL) {
                        mValue = NAN;
                    }
                    break;
                case 40:
                    rd.readULong();
                    rd.readULong();
                    mValue = rd.readUInt();
                    if (mValue == 0xFFFFFFFFL) {
                        mValue = NAN;
                    }
                    break;
                default:
                    throw new IOException("invalid record size");
            }
        }
    }

    public static class Status extends Value {

        private List<Integer> mTags = new ArrayList<Integer>();

        protected List<Integer> getTags() {
            return mTags;
        }

        protected Status(Value d, BinaryInputStream rd, int recordSize) throws IOException {
            super(d);

            for (int i = 0; i < 8; i++) {
                int attribute = (int) rd.readUInt();
                int tag = attribute & 0xFFFFFF;
                if (tag == 0xFFFFFE) {
                    break;
                }
                if ((attribute >>> 24) == 1) {
                    mTags.add(tag);
                }
            }
        }
    }

    public static class String extends Value {

        java.lang.String mValue;

        protected java.lang.String getValue() {
            return mValue;
        }

        protected String(Value d, BinaryInputStream rd, int recordSize) throws IOException {
            super(d);
            mValue = rd.readString(recordSize - 8);
        }
    }

    public static class SLong extends Value {

        public static final long NAN = 0x8000000000000000L;

        long mValue;

        protected long getValue() {
            return mValue;
        }

        protected SLong(Value d, BinaryInputStream rd, int recordSize) throws IOException {
            super(d);
            switch (recordSize) {
                case 16:
                    mValue = rd.readULong();
                    break;
                case 28:
                    rd.readULong();
                    mValue = rd.readUInt();
                    if (mValue == 0x80000000) {
                        mValue = NAN;
                    }
                    break;
                case 40:
                    rd.readULong();
                    rd.readULong();
                    mValue = rd.readUInt();
                    if (mValue == 0x80000000) {
                        mValue = NAN;
                    }
                    break;
                default:
                    throw new IOException("invalid record size");
            }
        }
    }
}
