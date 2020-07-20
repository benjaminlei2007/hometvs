package com.oldwet.hometvs;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import static com.oldwet.hometvs.LivePlayerActivity.*;

public class Channel {

    public void addToIntent(Intent intent){};

    @Nullable
    public String name;

    public int channelID;

    public String channelName;

    public String channelLogo;

    public int channelNumber;

    public Uri liveUri;

    public Uri timeshiftUri;

    public Uri catchupUri;

    public Channel(String name) {
        this.name = name;
    }

    public Channel(int channelID, String channelName,String channelLogo, int channelNumber) {
        this.channelID=channelID;
        this.channelName=channelName;
        this.channelLogo=channelLogo;
        this.channelNumber=channelNumber;
    }

    public int getChannelID() {
        return channelID;
    }

    public void setChannelID(int channelID) {
        this.channelID = channelID;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelLogo() {
        return channelLogo;
    }

    public void setChannelLogo(String channelLogo) {
        this.channelLogo = channelLogo;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
    }

    public Uri getLiveUri() {
        return liveUri;
    }

    public void setLiveUri(Uri liveUri) {
        this.liveUri = liveUri;
    }

    public Uri getTimeshiftUri() {
        return timeshiftUri;
    }

    public void setTimeshiftUri(Uri timeshiftUri) {
        this.timeshiftUri = timeshiftUri;
    }

    public Uri getCatchupUri() {
        return catchupUri;
    }

    public void setCatchupUri(Uri catchupUri) {
        this.catchupUri = catchupUri;
    }

    public static Channel createFromIntent(Intent intent) {
        if (ACTION_VIEW_LIST.equals(intent.getAction())) {
            ArrayList<String> intentUris = new ArrayList<>();
            int index = 0;
            while (intent.hasExtra(URI_EXTRA + "_" + index)) {
                intentUris.add(intent.getStringExtra(URI_EXTRA + "_" + index));
                index++;
            }
            UriChannel[] children = new UriChannel[intentUris.size()];
            for (int i = 0; i < children.length; i++) {
                Uri uri = Uri.parse(intentUris.get(i));
                children[i] = UriChannel.createFromIntent(uri, intent, /* extrasKeySuffix= */ "_" + i);
            }
            return new PlaylistChannel(/* name= */ null, children);
        } else {
            return UriChannel.createFromIntent(intent.getData(), intent, /* extrasKeySuffix= */ "");
        }
    }

    public static final class UriChannel extends Channel {

        public static UriChannel createFromIntent(Uri uri, Intent intent, String extrasKeySuffix) {
            String extension = intent.getStringExtra(EXTENSION_EXTRA + extrasKeySuffix);
            return new UriChannel(
                    /* name= */ null,
                    uri,
                    extension);
        }

        public final Uri uri;
        public final String extension;

        public UriChannel(
                String name,
                Uri uri,
                String extension) {
            super(name);
            this.uri = uri;
            this.extension = extension;
        }

        @Override
        public void addToIntent(Intent intent) {
            intent.setAction(LivePlayerActivity.ACTION_VIEW).setData(uri);
            addPlayerConfigToIntent(intent, /* extrasKeySuffix= */ "");
        }

        public void addToPlaylistIntent(Intent intent, String extrasKeySuffix) {
            intent.putExtra(LivePlayerActivity.URI_EXTRA + extrasKeySuffix, uri.toString());
            addPlayerConfigToIntent(intent, extrasKeySuffix);
        }

        private void addPlayerConfigToIntent(Intent intent, String extrasKeySuffix) {
            intent.putExtra(EXTENSION_EXTRA + extrasKeySuffix, extension);
        }
    }

    public static final class PlaylistChannel extends Channel {

        public final UriChannel[] children;

        public PlaylistChannel(String name, UriChannel... children) {
            super(name);
            this.children = children;
        }

        @Override
        public void addToIntent(Intent intent) {
            intent.setAction(LivePlayerActivity.ACTION_VIEW_LIST);
            for (int i = 0; i < children.length; i++) {
                children[i].addToPlaylistIntent(intent, /* extrasKeySuffix= */ "_" + i);
            }
        }
    }
}
