package com.github.topi314.lavasrc.lastfm;

import com.github.topi314.lavasrc.LavaSrcTools;
import com.github.topi314.lavasrc.mirror.DefaultMirroringAudioTrackResolver;
import com.github.topi314.lavasrc.mirror.MirroringAudioSourceManager;
import com.github.topi314.lavasrc.mirror.MirroringAudioTrackResolver;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class LastFmSourceManager extends MirroringAudioSourceManager implements HttpConfigurable {

    public static final Pattern URL_PATTERN = Pattern.compile(
            "https?://(?:(?:www|m)\\.)?lastfm\\.com/(?:music|track)/(?<artist>[^/]+)/(?<track>[^/]+)(?:\\?.*)?");

    public static final String SEARCH_PREFIX = "lfmsearch:";
    public static final String PUBLIC_API_BASE = "https://ws.audioscrobbler.com/2.0/";
    private static final String USER_AGENT = "Last.fm/1.0";
    private static final String LASTFM_API_KEY = "1461d6f83b49566dab4c5538d79387fc";  
    private static final Logger log = LoggerFactory.getLogger(LastFmSourceManager.class);

    private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    private int searchLimit = 6;
    private final String countryCode;

    public LastFmSourceManager(String[] providers, String countryCode, Function<Void, AudioPlayerManager> audioPlayerManager) {
        this(countryCode, audioPlayerManager, new DefaultMirroringAudioTrackResolver(providers));
    }

    public LastFmSourceManager(String countryCode, Function<Void, AudioPlayerManager> audioPlayerManager, MirroringAudioTrackResolver mirroringAudioTrackResolver) {
        super(audioPlayerManager, mirroringAudioTrackResolver);
        this.countryCode = (countryCode == null || countryCode.isEmpty()) ? "US" : countryCode;
    }

    public void setSearchLimit(int searchLimit) {
        this.searchLimit = searchLimit;
    }

    @Override
    public String getSourceName() {
        return "lastfm";
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        var extendedAudioTrackInfo = super.decodeTrack(input);
        return new LastFmAudioTrack(trackInfo, extendedAudioTrackInfo.albumName, extendedAudioTrackInfo.albumUrl, extendedAudioTrackInfo.artistUrl, extendedAudioTrackInfo.artistArtworkUrl, extendedAudioTrackInfo.previewUrl, extendedAudioTrackInfo.isPreview, this);
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            var identifier = reference.identifier;
            var matcher = URL_PATTERN.matcher(identifier);
            if (matcher.matches()) {
                String artist = matcher.group("artist");
                String track = matcher.group("track");

                return getTrack(artist, track);
            } else if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                String query = reference.identifier.substring(SEARCH_PREFIX.length());
                if (!query.isEmpty()) {
                    return getSearch(query);
                } else {
                    return AudioReference.NO_TRACK;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private JsonBrowser getApiResponse(String apiUrl) throws IOException {
        var request = new HttpGet(apiUrl);
        request.setHeader("user-agent", USER_AGENT);
        request.setHeader("api_key", LASTFM_API_KEY);
        return LavaSrcTools.fetchResponseAsJson(httpInterfaceManager.getInterface(), request);
    }

    private List<AudioTrack> parseTracks(JsonBrowser json) {
        var tracks = new ArrayList<AudioTrack>();
        for (var audio : json.values()) {
            var parsedTrack = parseTrack(audio);
            if (parsedTrack != null) {
                tracks.add(parsedTrack);
            }
        }
        return tracks;
    }

    private AudioItem getSearch(String query) throws IOException {
        try {
            String apiUrl = PUBLIC_API_BASE +
                    "?method=track.search&track=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8) +
                    "&limit=" +
                    searchLimit +
                    "&api_key=" +
                    LASTFM_API_KEY +
                    "&format=json";
            var json = getApiResponse(apiUrl);

            if (json.get("results").get("trackmatches").get("track").isNull()) {
                return AudioReference.NO_TRACK;
            }

            var tracks = parseTracks(json.get("results").get("trackmatches").get("track"));

            if (tracks.isEmpty()) {
                return AudioReference.NO_TRACK;
            }

            return new BasicAudioPlaylist("Last.fm Music Search: " + query, tracks, null, true);
        } catch (SocketTimeoutException e) {
            return AudioReference.NO_TRACK;
        }
    }

    private AudioTrack parseTrack(JsonBrowser audio) {
        var artist = audio.get("artist").text();
        var title = audio.get("name").text();
        var url = audio.get("url").text();
        var duration = audio.get("duration").text();
        var albumName = audio.get("album").get("title").text();
        var coverUrl = audio.get("image").index(3).get("#text").text();  
        
        try {
            var durationMillis = Long.parseLong(duration) * 1000;
            return new LastFmAudioTrack(new AudioTrackInfo(title, artist, durationMillis, url, false, url, coverUrl, null), this);
        } catch (NumberFormatException e) {
            log.error("Error parsing duration for track. Audio JSON: {}", audio, e);
            return null;
        }
    }

    private AudioItem getTrack(String artist, String track) throws IOException {
        try {
            String apiUrl = PUBLIC_API_BASE +
                    "?method=track.getinfo&artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8) +
                    "&track=" + URLEncoder.encode(track, StandardCharsets.UTF_8) +
                    "&api_key=" + LASTFM_API_KEY +
                    "&format=json";
            var json = getApiResponse(apiUrl);

            if (json == null || json.get("track").isNull()) {
                log.info("Track not found for {} - {}", artist, track);
                return AudioReference.NO_TRACK;
            }

            var trackItem = parseTrack(json.get("track"));

            if (trackItem == null) {
                log.info("Failed to parse track for {} - {}", artist, track);
                return AudioReference.NO_TRACK;
            }

            log.info("Track loaded successfully for {} - {}", artist, track);
            return trackItem;
        } catch (SocketTimeoutException e) {
            log.error("Socket timeout while fetching track: {} - {}", artist, track, e);
            return AudioReference.NO_TRACK;
        }
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        httpInterfaceManager.configureBuilder(configurator);
    }

    @Override
    public void shutdown() {
        try {
            httpInterfaceManager.close();
        } catch (IOException e) {
            log.error("Failed to close HTTP interface manager", e);
        }
    }

    public HttpInterface getHttpInterface() {
        return httpInterfaceManager.getInterface();
    }
}

