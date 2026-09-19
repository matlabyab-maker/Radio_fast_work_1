package com.fast.radiofinal5;

import android.net.Uri;

public final class TranscoderUrl {
    private TranscoderUrl() {}

    public static String build(String sourceUrl, String codec, int bitrateKbps) {
        if (TranscoderConfig.SERVER_BASE_URL == null || TranscoderConfig.SERVER_BASE_URL.isEmpty()) return sourceUrl;
        return Uri.parse(TranscoderConfig.SERVER_BASE_URL + "/stream")
                .buildUpon()
                .appendQueryParameter("url", sourceUrl)
                .appendQueryParameter("codec", codec)
                .appendQueryParameter("bitrate", String.valueOf(bitrateKbps))
                .build().toString();
    }
}
