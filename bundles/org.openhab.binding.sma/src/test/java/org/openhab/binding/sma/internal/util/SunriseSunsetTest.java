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
package org.openhab.binding.sma.internal.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;

import org.junit.jupiter.api.Test;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class SunriseSunsetTest {

    private static final double longitude = 8.1258;
    private static final double latitude = 48.6501;

    Calendar createCalendar(int year, int month0, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month0);
        calendar.set(Calendar.DATE, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    @Test
    void testSunrise() {
        SunriseSunset ss = new SunriseSunset(latitude, longitude);

        Calendar sunrise = ss.getSunrise(createCalendar(2024, 8, 24, 6, 0, 0));
        assertTrue(sunrise.equals(createCalendar(2024, 8, 24, 7, 18, 0)));
    }

    @Test
    void testSunset() {
        SunriseSunset ss = new SunriseSunset(latitude, longitude);

        Calendar sunset = ss.getSunset(createCalendar(2024, 8, 24, 6, 0, 0));
        assertTrue(sunset.equals(createCalendar(2024, 8, 24, 19, 20, 0)));
    }

    private boolean isDark(Calendar now) {
        // code from SmaBridgeHandler.run()
        SunriseSunset srs = new SunriseSunset(latitude, longitude);
        Calendar sunrise = srs.getSunrise(now);
        Calendar sunset = srs.getSunset(now);
        sunset.add(Calendar.MINUTE, 5);

        return (now.before(sunrise) || now.after(sunset));
    }

    @Test
    void testCompare() {
        assertTrue(isDark(createCalendar(2024, 8, 24, 6, 0, 0)));
        assertFalse(isDark(createCalendar(2024, 8, 24, 7, 19, 0)));
        assertFalse(isDark(createCalendar(2024, 8, 24, 19, 24, 0)));
        assertTrue(isDark(createCalendar(2024, 8, 24, 19, 26, 0)));
    }
}
