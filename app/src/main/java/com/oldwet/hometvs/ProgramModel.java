package com.oldwet.hometvs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProgramModel {
    public static List<Program> getProgramList(int channelId) {
        List<Program> programList = new ArrayList<>();
        programList.add(new Program(1,1,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,2,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,3,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,4,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,5,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,6,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,7,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,8,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,9,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,10,"节目"+String.valueOf(channelId),new Date(),new Date()));
        programList.add(new Program(1,11,"节目"+String.valueOf(channelId),new Date(),new Date()));

        return programList;
    }
}
