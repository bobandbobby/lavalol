package com.github.topi314.lavasrc.lastfm;

import com.github.topi314.lavasrc.mirror.MirroringAudioSourceManager;
import com.github.topi314.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.tools.io.SeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.InternalAudioTrack;

public class LastFmAudioTrack extends MirroringAudioTrack {

  private final String lastFmTrackUrl;
  private final String lastFmArtistUrl;
  private final String lastFmAlbumUrl;

  public LastFmAudioTrack(
    AudioTrackInfo trackInfo,
    String lastFmTrackUrl,
    String lastFmArtistUrl,
    String lastFmAlbumUrl,
    MirroringAudioSourceManager sourceManager
  ) {
    super(trackInfo, null, null, null, null, null, false, sourceManager);
    this.lastFmTrackUrl = lastFmTrackUrl;
    this.lastFmArtistUrl = lastFmArtistUrl;
    this.lastFmAlbumUrl = lastFmAlbumUrl;
  }

  public String getLastFmTrackUrl() {
    return lastFmTrackUrl;
  }

  public String getLastFmArtistUrl() {
    return lastFmArtistUrl;
  }

  public String getLastFmAlbumUrl() {
    return lastFmAlbumUrl;
  }

  @Override
  protected InternalAudioTrack createAudioTrack(
    AudioTrackInfo trackInfo,
    SeekableInputStream stream
  ) {
    return new Mp3AudioTrack(trackInfo, stream);
  }

  @Override
  protected AudioTrack makeShallowClone() {
    return new LastFmAudioTrack(
      this.trackInfo,
      this.lastFmTrackUrl,
      this.lastFmArtistUrl,
      this.lastFmAlbumUrl,
      (MirroringAudioSourceManager) this.sourceManager
    );
  }
}
