# BaldMan LavaSrc

> [!WARNING]  
> DO NOT USE LAVASRC and BALDMAN LAVASRC at the same time. Mixing these plugins may lead to unexpected behavior or errors. Choose one plugin and ensure it meets your requirements before using it in your project.

## Purpose

BaldMan LavaSrc is a fork with a simple yet effective goal: provide an alternative method to interact with audio sources like Spotify and Apple Music, Deezer without the hassle of managing API keys or master keys 

> [!TIP]  
> When using Spotify with this Fork, note that while the usual Spotify API enforces rate limits based on the application's client secret and ID, this fork uses anonymous tokens instead. This means you won't encounter rate limits due to client credentials, but keep in mind that you might still be rate limited by IP (429) if too many requests are sent. However, this method significantly increases the ability to accept Spotify links compared to using your client ID.

## Key Features

- **Spotify :**  without the need for a client ID or secret.
- **Apple Music :**  without requiring an API key.
- **Deezer :**  without requiring a Master key.
- **Tidal :**  without requiring a key. (private API not public)

## Getting Started

Check out the example application file to understand the setup


```
plugins:
    - dependency: "com.github.Nansess.BaldMan-LavaSrc:baldman-plugin:v4.5.0"
      repository: "https://jitpack.io"
```

```
plugins:
  lavasrc:
    providers: # Custom providers for track loading. This is the default
      # - "dzisrc:%ISRC%" # Deezer ISRC provider
      # - "dzsearch:%QUERY%" # Deezer search provider
      - "ytsearch:\"%ISRC%\"" # Will be ignored if track does not have an ISRC. See https://en.wikipedia.org/wiki/International_Standard_Recording_Code
      - "ytsearch:%QUERY%" # Will be used if track has no ISRC or no track could be found for the ISRC
      #  you can add multiple other fallback sources here
    sources:
      spotify: false # Enable Spotify source
      applemusic: false # Enable Apple Music source
      deezer: false # Enable Deezer source
      yandexmusic: false # Enable Yandex Music source
      tidal: false
      flowerytts: false # Enable Flowery TTs source
      youtube: true # Enable YouTube search source (https://github.com/topi314/LavaSearch)
    spotify:
      countryCode: "US" # the country code you want to use for filtering the artists top tracks. See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
      playlistLoadLimit: 6 # The number of pages at 100 tracks each
      albumLoadLimit: 6 # The number of pages at 50 tracks each
    applemusic:
      countryCode: "US" # the country code you want to use for filtering the artists top tracks and language. See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
      playlistLoadLimit: 6 # The number of pages at 300 tracks each
      albumLoadLimit: 6 # The number of pages at 300 tracks each
    tidal:
      countryCode: "US"
      searchLimit: 6
    yandexmusic:
      accessToken: "your access token" # the token used for accessing the yandex music api. See https://github.com/TopiSenpai/LavaSrc#yandex-music
    flowerytts:
      voice: "default voice" # (case-sensitive) get default voice here https://flowery.pw/docs/flowery/tts-voices-v-1-tts-voices-get
      translate: false # whether to translate the text to the native language of voice
      silence: 0 # the silence parameter is in milliseconds. Range is 0 to 10000. The default is 0.
      speed: 1.0 # the speed parameter is a float between 0.5 and 10. The default is 1.0. (0.5 is half speed, 2.0 is double speed, etc.)
      audioFormat: "mp3" # supported formats are: mp3, ogg_opus, ogg_vorbis, aac, wav, and flac. Default format is mp3
```

Feel free to contribute, report issues, or share your feedback.
