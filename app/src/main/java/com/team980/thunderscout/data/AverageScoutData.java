/*
 * MIT License
 *
 * Copyright (c) 2016 - 2017 Luke Myers (FRC Team 980 ThunderBots)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.team980.thunderscout.data;

import com.team980.thunderscout.data.enumeration.ClimbingStats;
import com.team980.thunderscout.data.enumeration.FuelDumpAmount;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Class to manage averaging of values and objects.
 * Created by TeamWrapper.
 */

public class AverageScoutData implements Serializable {

    private List<ScoutData> dataList;

    public AverageScoutData(List<ScoutData> d) {
        dataList = d;
    }

    //INIT

    public String getTeamNumber() {
        return dataList.get(0).getTeamNumber();
    }

    public long getLastUpdated() {
        return dataList.get(0).getDateAdded(); //dateAdded of newest match
    }

    public int getNumberOfMatches() {
        return dataList.size();
    }

    //AUTO

    public float getAverageAutoGearsDelivered() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getAutoGearsDelivered();
        }

        return i / dataList.size();
    }

    //Averages the ordinal
    public FuelDumpAmount getAverageAutoLowGoalDumpAmount() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getAutoLowGoalDumpAmount().ordinal();
        }

        int average = (int) (i / dataList.size());

        return FuelDumpAmount.values()[average];
    }

    public float getAverageAutoHighGoals() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getAutoHighGoals();
        }

        return i / dataList.size();
    }

    public float getAverageAutoMissedHighGoals() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getAutoMissedHighGoals();
        }

        return i / dataList.size();
    }

    public double getCrossedBaselinePercentage() {
        double i = 0;
        for (ScoutData data : dataList) {
            if (data.hasCrossedBaseline()) {
                i++;
            }
        }

        return (i / dataList.size()) * 100;
    }

    //TELEOP

    public float getAverageTeleopGearsDelivered() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getTeleopGearsDelivered();
        }

        return i / dataList.size();
    }

    public float getAverageTeleopDumpFrequency() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getTeleopLowGoalDumps().size();
        }

        return i / dataList.size();
    }

    //Uses ordinal
    public FuelDumpAmount getAverageTeleopLowGoalDumpAmount() {
        float i = 0;
        for (ScoutData data : dataList) {
            for (FuelDumpAmount amount : data.getTeleopLowGoalDumps()) {
                i += amount.ordinal();
            }
        }

        int average = (int) (i / dataList.size());

        if (average >= FuelDumpAmount.values().length) {
            average = FuelDumpAmount.values().length - 1;
        }

        return FuelDumpAmount.values()[average];
    }

    public float getAverageTeleopHighGoals() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getTeleopHighGoals();
        }

        return i / dataList.size();
    }

    public float getAverageTeleopMissedHighGoals() {
        float i = 0;
        for (ScoutData data : dataList) {
            i += data.getTeleopMissedHighGoals();
        }

        return i / dataList.size();
    }

    public double getClimbingStatsPercentage(ClimbingStats stat) {
        double i = 0;
        for (ScoutData data : dataList) {
            if (data.getClimbingStats() == stat) {
                i++;
            }
        }

        return (i / dataList.size()) * 100;
    }

    //SUMMARY

    public List<String> getTroublesList() {
        List<String> troublesList = new ArrayList<>();

        for (ScoutData data : dataList) {
            if (data.getTroubleWith() != null && !data.getTroubleWith().isEmpty()) {
                troublesList.add(data.getTroubleWith());
            }
        }

        return troublesList;
    }

    public List<String> getCommentsList() {
        List<String> commentsList = new ArrayList<>();

        for (ScoutData data : dataList) {
            if (data.getComments() != null && !data.getComments().isEmpty()) {
                commentsList.add(data.getComments());
            }
        }
        return commentsList;
    }
}
