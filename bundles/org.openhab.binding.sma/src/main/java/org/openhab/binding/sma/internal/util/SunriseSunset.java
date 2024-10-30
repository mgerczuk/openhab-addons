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
/************************************************************************************************
    SMAspot - Yet another tool to read power production of SMA solar inverters
    (c)2012-2013, SBF

    Latest version found at http://code.google.com/p/sma-spot/

    License: Attribution-NonCommercial-ShareAlike 3.0 Unported (CC BY-NC-SA 3.0)
    http://creativecommons.org/licenses/by-nc-sa/3.0/

    You are free:
        to Share � to copy, distribute and transmit the work
        to Remix � to adapt the work
    Under the following conditions:
    Attribution:
        You must attribute the work in the manner specified by the author or licensor
        (but not in any way that suggests that they endorse you or your use of the work).
    Noncommercial:
        You may not use this work for commercial purposes.
    Share Alike:
        If you alter, transform, or build upon this work, you may distribute the resulting work
        only under the same or similar license to this one.

DISCLAIMER:
    A user of SMAspot software acknowledges that he or she is receiving this
    software on an "as is" basis and the user is not relying on the accuracy
    or functionality of the software for any purpose. The user further
    acknowledges that any use of this software will be at his own risk
    and the copyright owner accepts no responsibility whatsoever arising from
    the use or application of the software.

************************************************************************************************/

package org.openhab.binding.sma.internal.util;

import java.util.Calendar;
import java.util.TimeZone;

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

/**
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class SunriseSunset {

    private SunriseSunsetCalculator calculator;

    public SunriseSunset(double latitude, double longitude) {
        Location loc = new Location(latitude, longitude);
        calculator = new SunriseSunsetCalculator(loc, TimeZone.getDefault());
    }

    public Calendar getSunrise(Calendar date) {
        return calculator.getOfficialSunriseCalendarForDate(date);
    }

    public Calendar getSunset(Calendar date) {
        return calculator.getOfficialSunsetCalendarForDate(date);
    }

    public boolean logErrors() {
        Calendar now = Calendar.getInstance();
        Calendar sunrise = getSunrise(now);
        Calendar sunset = getSunset(now);

        // log errors as info in first or last half hour
        sunrise.add(Calendar.MINUTE, 30);
        sunset.add(Calendar.MINUTE, -30);

        return now.after(sunrise) && now.before(sunset);
    }
}
