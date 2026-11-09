package com.eftyg4n.campusmate.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemTeacherBinding;
import com.eftyg4n.campusmate.models.Teacher;

// Fills the teacher contact card (item_teacher.xml) with one teacher's data.
public class TeacherCardBinder {

    private TeacherCardBinder() {
    }

    public static void bind(Context context, ItemTeacherBinding row, Teacher teacher) {
        row.textTeacherName.setText(teacher.getName());

        StringBuilder role = new StringBuilder();
        appendPart(role, teacher.getShortCode());
        appendPart(role, teacher.getDesignation());
        appendPart(role, teacher.getDepartment());
        row.textTeacherRole.setText(role.toString());

        StringBuilder place = new StringBuilder();
        if (hasText(teacher.getRoomNo())) {
            appendPart(place, context.getString(R.string.teacher_room, teacher.getRoomNo()));
        }
        if (hasText(teacher.getExtension())) {
            appendPart(place, context.getString(R.string.teacher_ext, teacher.getExtension()));
        }
        boolean hasPlace = showText(row.textTeacherRoom, place.toString());

        final String phone = teacher.getPhone();
        boolean hasPhone = showText(row.textTeacherPhone,
                hasText(phone) ? context.getString(R.string.teacher_phone, phone) : null);
        if (hasPhone) {
            // Implicit Intent: open the dialer with this number (does not call).
            row.textTeacherPhone.setOnClickListener(view -> openIntent(context,
                    new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone)))));
        }

        final String email = teacher.getEmail();
        boolean hasEmail = showText(row.textTeacherEmail,
                hasText(email) ? context.getString(R.string.teacher_email, email) : null);
        if (hasEmail) {
            // Implicit Intent: open an email app with this address.
            row.textTeacherEmail.setOnClickListener(view -> openIntent(context,
                    new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email))));
        }

        boolean noInfo = !hasPlace && !hasPhone && !hasEmail;
        row.textTeacherNoInfo.setVisibility(noInfo ? View.VISIBLE : View.GONE);
    }

    private static void openIntent(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.common_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    // Shows the text and returns true, or hides the view and returns false if it is empty.
    private static boolean showText(TextView view, String text) {
        if (hasText(text)) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
            return true;
        }
        view.setVisibility(View.GONE);
        return false;
    }

    private static void appendPart(StringBuilder builder, String part) {
        if (hasText(part)) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append(part);
        }
    }
}