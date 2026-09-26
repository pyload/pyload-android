package org.pyload.android.client.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.pyload.android.client.R;

public class ClickNLoadPreferenceScreen extends Preference {

    public ClickNLoadPreferenceScreen(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_widget_checkbox);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        View widget = holder.findViewById(android.R.id.checkbox);
        if (widget instanceof CheckBox checkBox) {
            boolean isChecked = getSharedPreferences() != null && getSharedPreferences().getBoolean(getKey(), false);
            checkBox.setChecked(isChecked);
            checkBox.setClickable(false);
            checkBox.setFocusable(false);
        }
    }
}
