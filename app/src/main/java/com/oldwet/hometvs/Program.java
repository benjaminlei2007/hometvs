package com.oldwet.hometvs;

import java.util.Date;

public class Program {

    public int channelId;

    public int programeId;

    public String programeName;

    public Date startTime;

    public Date endTime;

    public Program(int channelId, int programeId, String programeName, Date startTime, Date endTime) {
        this.channelId = channelId;
        this.programeId = programeId;
        this.programeName = programeName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getProgrameId() {
        return programeId;
    }

    public void setProgrameId(int programeId) {
        this.programeId = programeId;
    }

    public String getProgrameName() {
        return programeName;
    }

    public void setProgrameName(String programeName) {
        this.programeName = programeName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
}
