package com.eftyg4n.campusmate.dialogs;

import android.content.Context;
import android.view.LayoutInflater;

import com.eftyg4n.campusmate.R;
import com.eftyg4n.campusmate.databinding.ItemTeacherBinding;
import com.eftyg4n.campusmate.models.Teacher;
import com.eftyg4n.campusmate.utils.TeacherCardBinder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

// Pop-up with one teacher's contact card (room, phone, email).
public class TeacherDetailDialog {

    private TeacherDetailDialog() {
    }

    public static void show(Context context, Teacher teacher) {
        ItemTeacherBinding card = ItemTeacherBinding.inflate(LayoutInflater.from(context));
        TeacherCardBinder.bind(context, card, teacher);

        new MaterialAlertDialogBuilder(context)
                .setView(card.getRoot())
                .setPositiveButton(R.string.dialog_close, null)
                .show();
    }
}