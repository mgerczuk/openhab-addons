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

import java.util.Date;
import java.util.TimeZone;

/*
 * This new algorithm should fix a few issues with sunrise/sunset in different timezones - Thanks to Ron Patton
 * Issue 20: Version 2.0.1 has lost a grip of time
 * Issue 50: SMAspot stopped functioning completely. Must use -finq option to produce output.
 * Issue 51: Timezone awareness broken again
 */

// C program calculating the sunrise and sunset for the current date and a fixed location(latitude,longitude)
// Note, twilight calculation gives insufficient accuracy of results
// Jarmo Lammi 1999 - 2001
// Last update July 21st, 2001

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class SunriseSunset {
    static final double degs = 180.0 / Math.PI; // rtd()
    static final double rads = Math.PI / 180.0; // dtr()

    static double L; // mean longitude of the Sun
    static double g; // mean anomaly of the Sun

    // Get the days to J2000
    // h is UT in decimal hours
    // FNday only works between 1901 to 2099 - see Meeus chapter 7
    static double FNday(int y, int m, int d, double h) {
        long luku = -7 * (y + (m + 9) / 12) / 4 + 275 * m / 9 + d;
        // type casting necessary on PC DOS and TClite to avoid overflow
        luku += (long) y * 367;
        return luku - 730531.5 + h / 24.0;
    }

    // the function below returns an angle in the range 0 to 2*pi
    static double FNrange(double x) {
        double b = 0.5 * x / Math.PI;
        double a = 2.0 * Math.PI * (b - (long) (b));
        if (a < 0) {
            a = 2.0 * Math.PI + a;
        }
        return a;
    }

    // Calculating the hourangle
    static double f0(double lat, double declin) {
        double SunDia = 0.53; // Sunradius degrees
        double AirRefr = 34.0 / 60.0; // athmospheric refraction degrees
        // Correction: different sign at S HS
        double dfo = rads * (0.5 * SunDia + AirRefr);
        if (lat < 0.0) {
            dfo = -dfo;
        }
        double fo = Math.tan(declin + dfo) * Math.tan(lat * rads);
        if (fo > 0.99999) {
            fo = 1.0; // to avoid overflow
        }
        fo = Math.asin(fo) + Math.PI / 2.0;
        return fo;
    }

    // Calculating the hourangle for twilight times
    static double f1(double lat, double declin) {
        // Correction: different sign at S HS
        double df1 = rads * 6.0;
        if (lat < 0.0) {
            df1 = -df1;
        }
        double fi = Math.tan(declin + df1) * Math.tan(lat * rads);
        if (fi > 0.99999) {
            fi = 1.0; // to avoid overflow
        }
        fi = Math.asin(fi) + Math.PI / 2.0;
        return fi;
    };

    // Find the ecliptic longitude of the Sun
    static double FNsun(double d) {
        // mean longitude of the Sun
        L = FNrange(280.461 * rads + .9856474 * rads * d);
        // mean anomaly of the Sun
        g = FNrange(357.528 * rads + .9856003 * rads * d);
        // Ecliptic longitude of the Sun
        return FNrange(L + 1.915 * rads * Math.sin(g) + .02 * rads * Math.sin(2 * g));
    }

    static public boolean sunrise_sunset(double latit, double longit, float offset) {
        // get the date and time from the user
        // read system date and extract the year

        /** First get time **/
        // time_t sekunnit;
        // time(&sekunnit);
        Date date = new Date();

        /** Next get localtime **/
        // struct tm p;
        // memcpy(&p, localtime(&sekunnit), sizeof(p));

        int y = date.getYear() + 1900;
        int m = date.getMonth() + 1;
        int day = date.getDate(); // p.tm_mday;

        double h = 12;

        // Get TZ in hours
        TimeZone timeZone = TimeZone.getDefault();
        double tzone = timeZone.getOffset(date.getTime()) / 1000 / 60 / 60;

        double d = FNday(y, m, day, h);

        // Use FNsun to find the ecliptic longitude of the Sun
        double lambda = FNsun(d);

        // Obliquity of the ecliptic
        double obliq = 23.439 * rads - .0000004 * rads * d;

        // Find the RA and DEC of the Sun
        double alpha = Math.atan2(Math.cos(obliq) * Math.sin(lambda), Math.cos(lambda));
        double delta = Math.asin(Math.sin(obliq) * Math.sin(lambda));

        // Find the Equation of Time in minutes
        // Correction suggested by David Smith
        double LL = L - alpha;
        if (L < Math.PI) {
            LL += 2.0 * Math.PI;
        }
        double equation = 1440.0 * (1.0 - LL / Math.PI / 2.0);
        double ha = f0(latit, delta);
        // double hb = f1(latit, delta);
        // double twx = 12.0 * (hb - ha) / pi; // length of twilight in hours
        // double twam = riset - twx; // morning twilight begin
        // double twpm = settm + twx; // evening twilight end

        // Conversion of angle to hours and minutes
        double daylen = degs * ha / 7.5;
        if (daylen < 0.0001) {
            daylen = 0.0;
        }
        // arctic winter
        double riset = 12.0 - 12.0 * ha / Math.PI + tzone - longit / 15.0 + equation / 60.0;
        double settm = 12.0 + 12.0 * ha / Math.PI + tzone - longit / 15.0 + equation / 60.0;
        // double noontime = riset + 12.0 * ha/pi;
        double altmax = 90.0 + delta * degs - latit;
        // Correction for S HS suggested by David Smith
        // to express altitude as degrees from the N horizon
        if (latit < delta * degs) {
            altmax = 180.0 - altmax;
        }

        if (riset > 24.0) {
            riset -= 24.0;
        }
        if (settm > 24.0) {
            settm -= 24.0;
        }

        float sunrise = (float) riset;
        float sunset = (float) settm;

        // Convert HH:MM to float
        float now = date.getHours() + (float) date.getMinutes() / 60;
        if ((now >= (sunrise - offset)) && (now <= (sunset + offset))) {
            return true; // Sun's up
        } else {
            return false; // Sun's down
        }
    }
}
