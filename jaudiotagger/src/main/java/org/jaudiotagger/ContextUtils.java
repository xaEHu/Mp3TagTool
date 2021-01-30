package org.jaudiotagger;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;

import org.jaudiotagger.audio.exceptions.CannotReadException;

import java.io.IOException;

public class ContextUtils {
    public static ParcelFileDescriptor.AutoCloseInputStream getParcelInputStream(Context context, Uri uri) throws CannotReadException,IOException {
        if (context.checkUriPermission(uri, Binder.getCallingPid(),Binder.getCallingUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == PackageManager.PERMISSION_DENIED) {
            throw new CannotReadException(uri.getPath() + "can not read and write");
        }
        return new ParcelFileDescriptor.AutoCloseInputStream(context.getContentResolver().openFileDescriptor(uri,"rw"));
    }
}
