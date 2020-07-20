package com.oldwet.hometvs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.exoplayer2.*;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.*;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.rtsp.RtspDefaultClient;
import com.google.android.exoplayer2.source.rtsp.RtspMediaSource;
import com.google.android.exoplayer2.source.rtsp.core.Client;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.*;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.util.ErrorMessageProvider;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Util;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class CatchupPlayerActivity extends AppCompatActivity
        implements PlaybackPreparer, PlayerControlView.VisibilityListener {

    public static final String URI_EXTRA = "uri";
    public static final String EXTENSION_EXTRA = "extension";
    private static final String KEY_TRACK_SELECTOR_PARAMETERS = "track_selector_parameters";
    private static final String KEY_WINDOW = "window";
    private static final String KEY_POSITION = "position";
    private static final String KEY_AUTO_PLAY = "auto_play";
    public static final String PREFER_EXTENSION_DECODERS_EXTRA = "prefer_extension_decoders";

    // Actions.

    public static final String ACTION_VIEW = "com.oldwet.hometvs.action.VIEW";
    public static final String ACTION_VIEW_LIST =
            "com.oldwet.hometvs.action.VIEW_LIST";

    private SimpleExoPlayer player;
    private PlayerView playerView;
//    private final ArrayList<FrameworkMediaDrm> mediaDrms;
    private MediaSource mediaSource;
    private boolean startAutoPlay;
    private int startWindow;
    private long startPosition;

    private DefaultTrackSelector trackSelector;
    private DefaultTrackSelector.Parameters trackSelectorParameters;
    private TrackGroupArray lastSeenTrackGroupArray;
    private DataSource.Factory dataSourceFactory;

    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    public CatchupPlayerActivity() {
//        mediaDrms = new ArrayList<>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        super.onCreate(savedInstanceState);
        dataSourceFactory = buildDataSourceFactory();
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        setContentView(R.layout.live_player_activity);
        playerView = findViewById(R.id.live_player_view);
        playerView.setControllerVisibilityListener(this);
        playerView.setErrorMessageProvider(new CatchupPlayerActivity.PlayerErrorMessageProvider());
        playerView.requestFocus();

        if (savedInstanceState != null) {
            trackSelectorParameters = savedInstanceState.getParcelable(KEY_TRACK_SELECTOR_PARAMETERS);
            startAutoPlay = savedInstanceState.getBoolean(KEY_AUTO_PLAY);
            startWindow = savedInstanceState.getInt(KEY_WINDOW);
            startPosition = savedInstanceState.getLong(KEY_POSITION);
        } else {
            trackSelectorParameters = DefaultTrackSelector.Parameters.getDefaults(/* context= */ this);
            clearStartPosition();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        updateTrackSelectorParameters();
        updateStartPosition();
        outState.putParcelable(KEY_TRACK_SELECTOR_PARAMETERS, trackSelectorParameters);
        outState.putBoolean(KEY_AUTO_PLAY, startAutoPlay);
        outState.putInt(KEY_WINDOW, startWindow);
        outState.putLong(KEY_POSITION, startPosition);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.SDK_INT <= 23 || player == null) {
            initializePlayer();
            if (playerView != null) {
                playerView.onResume();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            if (playerView != null) {
                playerView.onPause();
            }
            releasePlayer();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Activity input

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // See whether the player view wants to handle media or DPAD keys events.
        return playerView.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    // PlaybackControlView.PlaybackPreparer implementation

    @Override
    public void preparePlayback() {
        player.retry();
    }

    // PlaybackControlView.VisibilityListener implementation

    @Override
    public void onVisibilityChange(int visibility) {
        //debugRootView.setVisibility(visibility);
    }

    private void initializePlayer() {
        if (player == null) {
            Intent intent = getIntent();

//            releaseMediaDrms();
            mediaSource = createTopLevelMediaSource(intent);
            if (mediaSource == null) {
                return;
            }

            TrackSelection.Factory trackSelectionFactory = new AdaptiveTrackSelection.Factory();;

            boolean preferExtensionDecoders =
                    intent.getBooleanExtra(PREFER_EXTENSION_DECODERS_EXTRA, false);
            RenderersFactory renderersFactory =
                    ((HomeTVsApplication) getApplication()).buildRenderersFactory(preferExtensionDecoders);

            trackSelector = new DefaultTrackSelector(/* context= */ this, trackSelectionFactory);
            trackSelector.setParameters(trackSelectorParameters);
            lastSeenTrackGroupArray = null;

            player =
                    new SimpleExoPlayer.Builder(/* context= */ this, renderersFactory)
                            .setTrackSelector(trackSelector)
                            .build();
            player.addListener(new CatchupPlayerActivity.PlayerEventListener());
            player.setPlayWhenReady(startAutoPlay);
            player.addAnalyticsListener(new EventLogger(trackSelector));
            playerView.setPlayer(player);
            playerView.setPlaybackPreparer(this);
        }
        boolean haveStartPosition = startWindow != C.INDEX_UNSET;
        if (haveStartPosition) {
            player.seekTo(startWindow, startPosition);
        }
        player.prepare(mediaSource, !haveStartPosition, false);
    }

//    private void releaseMediaDrms() {
//        for (FrameworkMediaDrm mediaDrm : mediaDrms) {
//            mediaDrm.release();
//        }
//        mediaDrms.clear();
//    }

    private void clearStartPosition() {
        startAutoPlay = true;
        startWindow = C.INDEX_UNSET;
        startPosition = C.TIME_UNSET;
    }

    private void showToast(int messageId) {
        showToast(getString(messageId));
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void updateTrackSelectorParameters() {
        if (trackSelector != null) {
            trackSelectorParameters = trackSelector.getParameters();
        }
    }

    private void updateStartPosition() {
        if (player != null) {
            startAutoPlay = player.getPlayWhenReady();
            startWindow = player.getCurrentWindowIndex();
            startPosition = Math.max(0, player.getContentPosition());
        }
    }

    @Nullable
    private MediaSource createTopLevelMediaSource(Intent intent) {
        String action = intent.getAction();
        boolean actionIsListView = ACTION_VIEW_LIST.equals(action);
        if (!actionIsListView && !ACTION_VIEW.equals(action)) {
            showToast(getString(R.string.unexpected_intent_action, action));
            finish();
            return null;
        }

        Channel intentAsChannel = Channel.createFromIntent(intent);
        Channel.UriChannel[] channels =
                intentAsChannel instanceof Channel.PlaylistChannel
                        ? ((Channel.PlaylistChannel) intentAsChannel).children
                        : new Channel.UriChannel[] {(Channel.UriChannel) intentAsChannel};

        for (Channel.UriChannel channel : channels) {
            if (!Util.checkCleartextTrafficPermitted(channel.uri)) {
                showToast(R.string.error_cleartext_not_permitted);
                return null;
            }
            if (Util.maybeRequestReadExternalStoragePermission(/* activity= */ this, channel.uri)) {
                // The player will be reinitialized if the permission is granted.
                return null;
            }
        }

        MediaSource[] mediaSources = new MediaSource[channels.length];
        for (int i = 0; i < channels.length; i++) {
            mediaSources[i] = createLeafMediaSource(channels[i]);
        }
        MediaSource mediaSource =
                mediaSources.length == 1 ? mediaSources[0] : new ConcatenatingMediaSource(mediaSources);

        return mediaSource;
    }

    private MediaSource createLeafMediaSource(Channel.UriChannel parameters) {
//        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
//        Channel.DrmInfo drmInfo = parameters.drmInfo;
//        if (drmInfo != null) {
//            int errorStringId = R.string.error_drm_unknown;
//            if (Util.SDK_INT < 18) {
//                errorStringId = R.string.error_drm_not_supported;
//            } else {
//                try {
//                    if (drmInfo.drmScheme == null) {
//                        errorStringId = R.string.error_drm_unsupported_scheme;
//                    } else {
//                        drmSessionManager =
//                                buildDrmSessionManagerV18(
//                                        drmInfo.drmScheme,
//                                        drmInfo.drmLicenseUrl,
//                                        drmInfo.drmKeyRequestProperties,
//                                        drmInfo.drmMultiSession);
//                    }
//                } catch (UnsupportedDrmException e) {
//                    errorStringId =
//                            e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
//                                    ? R.string.error_drm_unsupported_scheme
//                                    : R.string.error_drm_unknown;
//                }
//            }
//            if (drmSessionManager == null) {
//                showToast(errorStringId);
//                finish();
//                return null;
//            }
//        } else {
//            drmSessionManager = DrmSessionManager.getDummyDrmSessionManager();
//        }

        return createLeafMediaSource(parameters.uri, parameters.extension);
    }

    private MediaSource createLeafMediaSource(
            Uri uri, String extension) {
        @C.ContentType int type = Util.inferContentType(uri, extension);
        switch (type) {
            case C.TYPE_DASH:
                return new DashMediaSource.Factory(dataSourceFactory)
//                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_SS:
                return new SsMediaSource.Factory(dataSourceFactory)
//                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_HLS:
                return new HlsMediaSource.Factory(dataSourceFactory)
//                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
            case C.TYPE_OTHER:
                if (Util.isRtspUri(uri)) {
                    return new RtspMediaSource.Factory(RtspDefaultClient.factory()
                            .setFlags(Client.FLAG_ENABLE_RTCP_SUPPORT)
                            .setNatMethod(Client.RTSP_NAT_DUMMY))
                            .createMediaSource(uri);
                } else {
                    return new ProgressiveMediaSource.Factory(dataSourceFactory)
//                            .setDrmSessionManager(drmSessionManager)
                            .createMediaSource(uri);
                }
            default:
                return new ProgressiveMediaSource.Factory(dataSourceFactory)
//                        .setDrmSessionManager(drmSessionManager)
                        .createMediaSource(uri);
        }
    }

    private void releasePlayer() {
        if (player != null) {
            updateTrackSelectorParameters();
            updateStartPosition();
            player.release();
            player = null;
            mediaSource = null;
            trackSelector = null;
        }
//        releaseMediaDrms();
    }


    private static boolean isBehindLiveWindow(ExoPlaybackException e) {
        if (e.type != ExoPlaybackException.TYPE_SOURCE) {
            return false;
        }
        Throwable cause = e.getSourceException();
        while (cause != null) {
            if (cause instanceof BehindLiveWindowException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

//    private DefaultDrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManagerV18(
//            UUID uuid, String licenseUrl, String[] keyRequestPropertiesArray, boolean multiSession)
//            throws UnsupportedDrmException {
//        HttpDataSource.Factory licenseDataSourceFactory =
//                ((HomeTVsApplication) getApplication()).buildHttpDataSourceFactory();
//        HttpMediaDrmCallback drmCallback =
//                new HttpMediaDrmCallback(licenseUrl, licenseDataSourceFactory);
//        if (keyRequestPropertiesArray != null) {
//            for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
//                drmCallback.setKeyRequestProperty(keyRequestPropertiesArray[i],
//                        keyRequestPropertiesArray[i + 1]);
//            }
//        }
//
//        FrameworkMediaDrm mediaDrm = FrameworkMediaDrm.newInstance(uuid);
//        mediaDrms.add(mediaDrm);
//        return new DefaultDrmSessionManager<>(uuid, mediaDrm, drmCallback, null, multiSession);
//    }

    /** Returns a new DataSource factory. */
    private DataSource.Factory buildDataSourceFactory() {
        return ((HomeTVsApplication) getApplication()).buildDataSourceFactory();
    }

    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, @Player.State int playbackState) {

        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            if (isBehindLiveWindow(e)) {
                clearStartPosition();
                initializePlayer();
            } else {

            }
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            if (trackGroups != lastSeenTrackGroupArray) {
                MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo != null) {
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_VIDEO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_video);
                    }
                    if (mappedTrackInfo.getTypeSupport(C.TRACK_TYPE_AUDIO)
                            == MappingTrackSelector.MappedTrackInfo.RENDERER_SUPPORT_UNSUPPORTED_TRACKS) {
                        showToast(R.string.error_unsupported_audio);
                    }
                }
                lastSeenTrackGroupArray = trackGroups;
            }
        }
    }

    private class PlayerErrorMessageProvider implements ErrorMessageProvider<ExoPlaybackException> {

        @Override
        public Pair<Integer, String> getErrorMessage(ExoPlaybackException e) {
            String errorString = getString(R.string.error_generic);
            if (e.type == ExoPlaybackException.TYPE_RENDERER) {
                Exception cause = e.getRendererException();
                if (cause instanceof MediaCodecRenderer.DecoderInitializationException) {
                    // Special case for decoder initialization failures.
                    MediaCodecRenderer.DecoderInitializationException decoderInitializationException =
                            (MediaCodecRenderer.DecoderInitializationException) cause;
                    if (decoderInitializationException.codecInfo == null) {
                        if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                            errorString = getString(R.string.error_querying_decoders);
                        } else if (decoderInitializationException.secureDecoderRequired) {
                            errorString =
                                    getString(
                                            R.string.error_no_secure_decoder, decoderInitializationException.mimeType);
                        } else {
                            errorString =
                                    getString(R.string.error_no_decoder, decoderInitializationException.mimeType);
                        }
                    } else {
                        errorString =
                                getString(
                                        R.string.error_instantiating_decoder,
                                        decoderInitializationException.codecInfo.name);
                    }
                }
            }
            return Pair.create(0, errorString);
        }
    }
}
