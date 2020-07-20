/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.source.rtsp.message;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;

import java.lang.annotation.Retention;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public final class Range {
    // We only support the Normal Play Time (npt)
    private static final Pattern regexNptRange =
            Pattern.compile("npt=\\s*(\\d+:[0-5][0-9]:[0-5][0-9]|now|\\d+.\\d+|\\d*)?\\s*-\\s*" +
                    "(\\d+:[0-5][0-9]:[0-5][0-9]|end|\\d+.\\d+|\\d*)?\\s*");
    private static final Pattern regexUtcRange =
            Pattern.compile("clock=\\s*(\\d.*Z)?\\s*-\\s*" +
                    "(\\d.*Z)?\\s*");

    @Retention(SOURCE)
    @IntDef({NPT_RANGE, UTC_RANGE})
    public @interface RangeType {
    }

    public static final int NPT_RANGE = 0;
    public static final int UTC_RANGE = 1;

    private final String range;
    private final double startTime;
    private final String startTimeStr;
    private final double endTime;
    private final String endTimeStr;
    private final long duration;


    private final @RangeType int rangeType;

    Range(String range, double startTime,String startTimeStr, double endTime,String endTimeStr,@RangeType int rangeType) {
        this.range = range;
        this.startTime = startTime;
        this.startTimeStr=startTimeStr;
        this.endTime = endTime;
        this.endTimeStr=endTimeStr;
        this.rangeType=rangeType;

        if ((startTime != C.TIME_UNSET) && (endTime != C.TIME_UNSET)) {
            duration = (long)(endTime - startTime) * C.MICROS_PER_SECOND;
        } else {
            duration = C.TIME_UNSET;
        }
    }

    public double startTime() { return startTime; }

    public double endTime() { return endTime; }

    public long duration() {
        return duration;
    }

    public String getPosToEnd(long Pos) {
        //TODO
        String result;
        switch (this.rangeType){
            case NPT_RANGE:
                result = "npt=" + startTimeStr + "-";
                return result;
            case UTC_RANGE:
                //TODO
                result = "clock="+ startTimeStr +"-";
                return result;
        }
        return null;
    }

    public String getStartToEnd() {
        String result;
        switch (this.rangeType){
            case NPT_RANGE:
                result = "npt=" + startTimeStr + "-";
                return result;
                //TODO
            case UTC_RANGE:
                result = "clock=20200510T133837.00Z-";
                return result;
        }
        return null;
    }

    @Nullable
    public static Range parse(String rangeStr) {
        if (rangeStr.contains("npt=")){
            return parseNptRnage(rangeStr);
        }else{
            return parseUtcRnage(rangeStr);
        }
    }

    private static Range parseUtcRnage(String rangeUtc) {
        try {

            Matcher matcher = regexUtcRange.matcher(rangeUtc);

            if (matcher.find()) {

                double startTime = C.TIME_UNSET;
                double endTime = C.TIME_UNSET;

                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss.SS'Z'");

                String start = matcher.group(1);
                if (start!=null && start.equals("")) {
                    startTime = sdf.parse(start).getTime();
                }

                String end = matcher.group(2);
                if (end!=null && end.equals("")) {
                    endTime = sdf.parse(end).getTime();
                }
                String range = matcher.group(1) + "-" + matcher.group(2);
                return new Range(range, startTime, start, endTime, end, UTC_RANGE);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    private static Range parseNptRnage(String rangeNpt) {
        try {

            Matcher matcher = regexNptRange.matcher(rangeNpt);

            if (matcher.find()) {

                double startTime = C.TIME_UNSET;
                double endTime = C.TIME_UNSET;
                String start = matcher.group(1);
                String end = matcher.group(2);
                if (start != null) {

                    try {
                        startTime = Double.parseDouble(matcher.group(1));

                    } catch (NumberFormatException ex) {

                        if (!start.equals("now") && !start.equals("")) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                                startTime = sdf.parse(start).getTime();

                            } catch (ParseException e) {
                                return null;
                            }
                        }
                    }

                    try {
                        endTime = Double.parseDouble(matcher.group(2));

                    } catch (NumberFormatException ex) {

                        if (!end.equals("end") && !end.equals("")) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                                endTime = sdf.parse(end).getTime();

                            } catch (ParseException e) {
                                return null;
                            }
                        }
                    }

                    String range = matcher.group(1) + "-" + matcher.group(2);
                    return new Range(range, startTime, start,endTime,end, NPT_RANGE);
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Returns the string used to identify this range
     */
    @Override public String toString() {
        StringBuilder str = new StringBuilder();
        switch(rangeType){
            case NPT_RANGE:
                str.append("npt=");
                break;
            case UTC_RANGE:
                str.append("clock=");
                break;
        }
        str.append(range);
        return str.toString();
    }
}
