package com.oldwet.hometvs;

import java.util.ArrayList;
import java.util.List;

public class ChannelModel {
    public static List<Channel> getChannelList() {
        List<Channel> channelList = new ArrayList<>();
        channelList.add(new Channel(1,"湖南卫视","https://123.123.123.123",1));
        channelList.add(new Channel(2,"湖北卫视","https://123.123.123.123",2));
        channelList.add(new Channel(3,"江西卫视","https://123.123.123.123",3));
        channelList.add(new Channel(4,"海南卫视","https://123.123.123.123",4));
        channelList.add(new Channel(5,"海南卫视","https://123.123.123.123",5));
        channelList.add(new Channel(6,"海南卫视","https://123.123.123.123",6));
        channelList.add(new Channel(7,"海南卫视","https://123.123.123.123",7));
        channelList.add(new Channel(8,"海南卫视","https://123.123.123.123",8));
        channelList.add(new Channel(9,"海南卫视","https://123.123.123.123",9));
        channelList.add(new Channel(10,"海南卫视","https://123.123.123.123",10));
        channelList.add(new Channel(11,"海南卫视","https://123.123.123.123",11));

        return channelList;
    }
}
