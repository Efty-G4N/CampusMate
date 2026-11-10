package com.eftyg4n.campusmate.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.models.Resource;
import com.eftyg4n.campusmate.repositories.ResourceRepository;

import java.io.File;

// Open / share a resource with IMPLICIT intents.
// For our own files the intent carries a FileProvider Uri + a temporary read grant.
public class ResourceOpener {

    private ResourceOpener() {
    }

    public static void open(Context context, Resource r) {
        Intent intent;
        if (r.hasLocalFile()) {
            Uri uri = ResourceFileUtils.fileUri(context, new File(r.getLocalPath()));
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, r.getMimeType() != null ? r.getMimeType() : "*/*");
            // Without this flag the viewer app gets "permission denied".
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (r.getUri() != null && r.getUri().startsWith("http")) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(r.getUri()));
        } else {
            Toast.makeText(context, R.string.resource_toast_not_available,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            context.startActivity(intent);
            new ResourceRepository(context).markOpened(r.getId());
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.resource_toast_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    public static void share(Context context, Resource r) {
        Intent send = new Intent(Intent.ACTION_SEND);
        if (r.hasLocalFile()) {
            Uri uri = ResourceFileUtils.fileUri(context, new File(r.getLocalPath()));
            send.setType(r.getMimeType() != null ? r.getMimeType() : "*/*");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else if (r.getUri() != null) {
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, r.getTitle() + "\n" + r.getUri());
        } else {
            Toast.makeText(context, R.string.resource_toast_not_available,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        send.putExtra(Intent.EXTRA_SUBJECT, r.getTitle());
        context.startActivity(Intent.createChooser(send,
                context.getString(R.string.resources_share_chooser)));
    }
}
