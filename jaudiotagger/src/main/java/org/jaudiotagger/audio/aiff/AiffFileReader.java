package org.jaudiotagger.audio.aiff;

import android.content.Context;
import android.net.Uri;

import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.generic.AudioFileReader2;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.IOException;

/**
 * Reads Audio and Metadata information contained in Aiff file.
 */
public class AiffFileReader extends AudioFileReader2 {
    private AiffInfoReader ir = new AiffInfoReader();
    private AiffTagReader im = new AiffTagReader();

    @Override
    protected GenericAudioHeader getEncodingInfo(File file) throws CannotReadException, IOException {
        return ir.read(file);
    }

    @Override
    protected Tag getTag(File file) throws CannotReadException, IOException {
        return im.read(file);
    }

    @Override
    protected GenericAudioHeader getEncodingInfo(Context context, Uri uri) throws CannotReadException, IOException {
        return ir.read(context, uri);
    }

    @Override
    protected Tag getTag(Context context, Uri uri) throws CannotReadException, IOException {
        return im.read(context, uri);
    }
}
