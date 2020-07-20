/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.oldwet.hometvs;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.*;
import android.widget.*;
import android.widget.ExpandableListView.OnChildClickListener;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.oldwet.hometvs.Channel.UriChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** An activity for selecting from a list of media channels. */
public class ChannelChooserActivity extends AppCompatActivity
    implements OnChildClickListener {

  private static final String TAG = "ChannelChooserActivity";

  private boolean useExtensionRenderers;
  private ChannelAdapter channelAdapter;
  private MenuItem preferExtensionDecodersMenuItem;
  private MenuItem randomAbrMenuItem;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.channel_chooser_activity);
    channelAdapter = new ChannelAdapter();
    ExpandableListView channelListView = findViewById(R.id.channel_list);
    channelListView.setAdapter(channelAdapter);
    channelListView.setOnChildClickListener(this);

    Intent intent = getIntent();
    String dataUri = intent.getDataString();
    String[] uris;
    if (dataUri != null) {
      uris = new String[] {dataUri};
    } else {
      ArrayList<String> uriList = new ArrayList<>();
      AssetManager assetManager = getAssets();
      try {
        for (String asset : assetManager.list("")) {
          if (asset.endsWith(".exolist.json")) {
            uriList.add("asset:///" + asset);
          }
        }
      } catch (IOException e) {
        Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
            .show();
      }
      uris = new String[uriList.size()];
      uriList.toArray(uris);
      Arrays.sort(uris);
    }

    HomeTVsApplication application = (HomeTVsApplication) getApplication();
    useExtensionRenderers = application.useExtensionRenderers();
    ChannelListLoader loaderTask = new ChannelListLoader();
    loaderTask.execute(uris);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.sample_chooser_menu, menu);
    preferExtensionDecodersMenuItem = menu.findItem(R.id.prefer_extension_decoders);
    preferExtensionDecodersMenuItem.setVisible(useExtensionRenderers);
    randomAbrMenuItem = menu.findItem(R.id.random_abr);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    item.setChecked(!item.isChecked());
    return true;
  }

  @Override
  public void onStart() {
    super.onStart();
    channelAdapter.notifyDataSetChanged();
  }

  @Override
  public void onStop() {
    super.onStop();
  }


  private void onChannelGroups(final List<ChannelGroup> groups, boolean sawError) {
    if (sawError) {
      Toast.makeText(getApplicationContext(), R.string.sample_list_load_error, Toast.LENGTH_LONG)
          .show();
    }
    channelAdapter.setChannelGroups(groups);
  }

  @Override
  public boolean onChildClick(
      ExpandableListView parent, View view, int groupPosition, int childPosition, long id) {
    Channel channel = (Channel) view.getTag();
    Intent intent = new Intent(this, LivePlayerActivity.class);
    intent.putExtra(
            LivePlayerActivity.PREFER_EXTENSION_DECODERS_EXTRA,
        isNonNullAndChecked(preferExtensionDecodersMenuItem));
    channel.addToIntent(intent);
    startActivity(intent);
    return true;
  }

  private static boolean isNonNullAndChecked(@Nullable MenuItem menuItem) {
    // Temporary workaround for layouts that do not inflate the options menu.
    return menuItem != null && menuItem.isChecked();
  }

  private final class ChannelListLoader extends AsyncTask<String, Void, List<ChannelGroup>> {

    private boolean sawError;

    @Override
    protected List<ChannelGroup> doInBackground(String... uris) {
      List<ChannelGroup> result = new ArrayList<>();
      Context context = getApplicationContext();
      String userAgent = Util.getUserAgent(context, "ExoPlayer");
      DataSource dataSource =
          new DefaultDataSource(context, userAgent, /* allowCrossProtocolRedirects= */ false);
      for (String uri : uris) {
        DataSpec dataSpec = new DataSpec(Uri.parse(uri));
        InputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
        try {
          readChannelGroups(new JsonReader(new InputStreamReader(inputStream, "UTF-8")), result);
        } catch (Exception e) {
          Log.e(TAG, "Error loading channel list: " + uri, e);
          sawError = true;
        } finally {
          Util.closeQuietly(dataSource);
        }
      }
      return result;
    }

    @Override
    protected void onPostExecute(List<ChannelGroup> result) {
      onChannelGroups(result, sawError);
    }

    private void readChannelGroups(JsonReader reader, List<ChannelGroup> groups) throws IOException {
      reader.beginArray();
      while (reader.hasNext()) {
        readChannelGroup(reader, groups);
      }
      reader.endArray();
    }

    private void readChannelGroup(JsonReader reader, List<ChannelGroup> groups) throws IOException {
      String groupName = "";
      ArrayList<Channel> channels = new ArrayList<>();

      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        switch (name) {
          case "name":
            groupName = reader.nextString();
            break;
          case "samples":
            reader.beginArray();
            while (reader.hasNext()) {
              channels.add(readEntry(reader, false));
            }
            reader.endArray();
            break;
          case "_comment":
            reader.nextString(); // Ignore.
            break;
          default:
            throw new ParserException("Unsupported name: " + name);
        }
      }
      reader.endObject();

      ChannelGroup group = getGroup(groupName, groups);
      group.channels.addAll(channels);
    }

    private Channel readEntry(JsonReader reader, boolean insidePlaylist) throws IOException {
      String channelName = null;
      Uri uri = null;
      String extension = null;
      String drmScheme = null;
      String drmLicenseUrl = null;
      String[] drmKeyRequestProperties = null;
      boolean drmMultiSession = false;
      ArrayList<UriChannel> playlistChannels = null;
      String adTagUri = null;
      String sphericalStereoMode = null;

      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        switch (name) {
          case "name":
            channelName = reader.nextString();
            break;
          case "uri":
            uri = Uri.parse(reader.nextString());
            break;
          case "extension":
            extension = reader.nextString();
            break;
          case "drm_scheme":
            drmScheme = reader.nextString();
            break;
          case "drm_license_url":
            drmLicenseUrl = reader.nextString();
            break;
          case "drm_key_request_properties":
            ArrayList<String> drmKeyRequestPropertiesList = new ArrayList<>();
            reader.beginObject();
            while (reader.hasNext()) {
              drmKeyRequestPropertiesList.add(reader.nextName());
              drmKeyRequestPropertiesList.add(reader.nextString());
            }
            reader.endObject();
            drmKeyRequestProperties = drmKeyRequestPropertiesList.toArray(new String[0]);
            break;
          case "drm_multi_session":
            drmMultiSession = reader.nextBoolean();
            break;
          case "playlist":
            Assertions.checkState(!insidePlaylist, "Invalid nesting of playlists");
            playlistChannels = new ArrayList<>();
            reader.beginArray();
            while (reader.hasNext()) {
              playlistChannels.add((UriChannel) readEntry(reader, true));
            }
            reader.endArray();
            break;
          case "ad_tag_uri":
            adTagUri = reader.nextString();
            break;
          case "spherical_stereo_mode":
            Assertions.checkState(
                !insidePlaylist, "Invalid attribute on nested item: spherical_stereo_mode");
            sphericalStereoMode = reader.nextString();
            break;
          default:
            throw new ParserException("Unsupported attribute name: " + name);
        }
      }
      reader.endObject();
      if (playlistChannels != null) {
        UriChannel[] playlistChannelsArray = playlistChannels.toArray(new UriChannel[0]);
        return new Channel.PlaylistChannel(channelName, playlistChannelsArray);
      } else {
        return new UriChannel(
            channelName,
            uri,
            extension);
      }
    }

    private ChannelGroup getGroup(String groupName, List<ChannelGroup> groups) {
      for (int i = 0; i < groups.size(); i++) {
        if (Util.areEqual(groupName, groups.get(i).title)) {
          return groups.get(i);
        }
      }
      ChannelGroup group = new ChannelGroup(groupName);
      groups.add(group);
      return group;
    }

  }

  private final class ChannelAdapter extends BaseExpandableListAdapter {

    private List<ChannelGroup> channelGroups;

    public ChannelAdapter() {
      channelGroups = Collections.emptyList();
    }

    public void setChannelGroups(List<ChannelGroup> channelGroups) {
      this.channelGroups = channelGroups;
      notifyDataSetChanged();
    }

    @Override
    public Channel getChild(int groupPosition, int childPosition) {
      return getGroup(groupPosition).channels.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
      return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
        View convertView, ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view = getLayoutInflater().inflate(R.layout.channel_list_item, parent, false);
      }
      initializeChildView(view, getChild(groupPosition, childPosition));
      return view;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
      return getGroup(groupPosition).channels.size();
    }

    @Override
    public ChannelGroup getGroup(int groupPosition) {
      return channelGroups.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
      return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
        ViewGroup parent) {
      View view = convertView;
      if (view == null) {
        view =
            getLayoutInflater()
                .inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
      }
      ((TextView) view).setText(getGroup(groupPosition).title);
      return view;
    }

    @Override
    public int getGroupCount() {
      return channelGroups.size();
    }

    @Override
    public boolean hasStableIds() {
      return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
      return true;
    }


    private void initializeChildView(View view, Channel channel) {
      view.setTag(channel);
      TextView channelTitle = view.findViewById(R.id.channel_title);
      channelTitle.setText(channel.name);

    }
  }

  private static final class ChannelGroup {

    public final String title;
    public final List<Channel> channels;

    public ChannelGroup(String title) {
      this.title = title;
      this.channels = new ArrayList<>();
    }

  }
}
