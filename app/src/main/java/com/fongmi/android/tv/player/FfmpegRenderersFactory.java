package com.fongmi.android.tv.player;

import android.content.Context;
import android.os.Handler;

import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.audio.AudioRendererEventListener;
import androidx.media3.exoplayer.audio.AudioSink;
import androidx.media3.exoplayer.audio.DecoderAudioRenderer;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer;

import java.util.ArrayList;

public class FfmpegRenderersFactory extends DefaultRenderersFactory {

    /**
     * @param context A {@link Context}.
     */
    public FfmpegRenderersFactory(Context context) {
        super(context);
    }

    @Override
    protected void buildAudioRenderers(
            Context context,
            @ExtensionRendererMode int extensionRendererMode,
            MediaCodecSelector mediaCodecSelector,
            boolean enableDecoderFallback,
            AudioSink audioSink,
            Handler eventHandler,
            AudioRendererEventListener eventListener,
            ArrayList<Renderer> out) {
        out.add(new FfmpegAudioRenderer());
        out.add(new FfmpegAudioRenderer());
        super.buildAudioRenderers(
                context,
                extensionRendererMode,
                mediaCodecSelector,
                enableDecoderFallback,
                audioSink,
                eventHandler,
                eventListener,
                out);
    }
}
