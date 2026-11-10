package com.eftyg4n.campusmate.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.format.Formatter;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import com.eftyg4n.campusmate.models.Resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

// File helpers for the Resource Library.
// Key idea: a picked content:// Uri belongs to ANOTHER app and can stop working
// (file moved, permission lost). So we COPY the bytes into our own private folder.
public class ResourceFileUtils {

    private ResourceFileUtils() {
    }

    public static class PickedInfo {
        public String displayName;
        public long size;       // -1 = unknown
        public String mimeType;
    }

    // Asks the other app's provider for the file name and size.
    public static PickedInfo queryInfo(Context context, Uri uri) {
        PickedInfo info = new PickedInfo();
        ContentResolver resolver = context.getContentResolver();
        info.mimeType = resolver.getType(uri);
        info.size = -1;

        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) {
                    info.displayName = cursor.getString(nameIndex);
                }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    info.size = cursor.getLong(sizeIndex);
                }
            }
        } catch (RuntimeException ignored) {
            // Some providers throw when a column is missing: we just keep the defaults.
        }
        if (info.displayName == null) {
            info.displayName = uri.getLastPathSegment() != null
                    ? uri.getLastPathSegment() : "file";
        }
        return info;
    }

    // filesDir/resources/  (the same folder is declared in res/xml/file_paths.xml)
    public static File resourcesDir(Context context) {
        File dir = new File(context.getFilesDir(), "resources");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    // Copies the picked file into resourcesDir. Run it on a BACKGROUND thread.
    // Returns the new file, or throws IOException.
    public static File copyToAppStorage(Context context, Uri source, String displayName)
            throws IOException {
        File target = uniqueFile(resourcesDir(context), sanitize(displayName));
        try (InputStream in = context.getContentResolver().openInputStream(source);
             OutputStream out = new java.io.FileOutputStream(target)) {
            if (in == null) {
                throw new IOException("Cannot open the picked file");
            }
            byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException | RuntimeException e) {
            //noinspection ResultOfMethodCallIgnored
            target.delete(); // do not leave a half-copied file behind
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException(e);
        }
        return target;
    }

    // "My notes?.pdf" -> "My_notes_.pdf"  (no path tricks like ../ either)
    public static String sanitize(String name) {
        String clean = name == null ? "file" : name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (clean.startsWith(".")) {
            clean = "_" + clean;
        }
        return clean.isEmpty() ? "file" : clean;
    }

    // notes.pdf, notes_1.pdf, notes_2.pdf ... never overwrite an existing file.
    public static File uniqueFile(File dir, String name) {
        File file = new File(dir, name);
        if (!file.exists()) {
            return file;
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        int i = 1;
        while (file.exists()) {
            file = new File(dir, base + "_" + i + ext);
            i++;
        }
        return file;
    }

    public static void deleteLocalFile(String path) {
        if (path != null) {
            //noinspection ResultOfMethodCallIgnored
            new File(path).delete();
        }
    }

    public static String formatSize(Context context, long bytes) {
        return bytes <= 0 ? "" : Formatter.formatShortFileSize(context, bytes);
    }

    // A content:// Uri that OTHER apps may read, through our FileProvider.
    // The authority must be the same as android:authorities in the Manifest.
    public static Uri fileUri(Context context, File file) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    public static String mimeFromName(String name) {
        int dot = name == null ? -1 : name.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
    }

    // A first guess for the category dropdown; the student can change it.
    public static String guessCategory(String mime) {
        if (mime == null) {
            return Resource.CATEGORY_OTHER;
        }
        if (mime.startsWith("image/")) {
            return Resource.CATEGORY_IMAGE;
        }
        if (mime.contains("presentation") || mime.contains("powerpoint")) {
            return Resource.CATEGORY_SLIDES;
        }
        return Resource.CATEGORY_NOTES;
    }
}
