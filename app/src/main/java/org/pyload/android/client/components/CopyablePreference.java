package org.pyload.android.client.components;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.google.android.material.snackbar.Snackbar;

import org.pyload.android.client.R;
import org.pyload.android.client.pyLoadApp;

public class CopyablePreference extends Preference {

    private static final long LONG_PRESS_DURATION_MS = 1500L;

    public CopyablePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CopyablePreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CopyablePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CopyablePreference(@NonNull Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final Handler handler = new Handler(Looper.getMainLooper());
        final int touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop() * 2;

        holder.itemView.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private boolean isLongPressed = false;

            private final Runnable longClickRunnable = () -> {
                isLongPressed = true;
                holder.itemView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                copyToClipboard();
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isLongPressed = false;
                        downX = event.getX();
                        downY = event.getY();
                        handler.postDelayed(longClickRunnable, LONG_PRESS_DURATION_MS);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getX() - downX;
                        float deltaY = event.getY() - downY;
                        if (Math.hypot(deltaX, deltaY) > touchSlop) {
                            handler.removeCallbacks(longClickRunnable);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(longClickRunnable);
                        if (isLongPressed) {
                            return true;
                        } else {
                            v.performClick();
                            return true;
                        }

                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longClickRunnable);
                        return true;
                }
                return false;
            }
        });
    }

    private void copyToClipboard() {
        CharSequence textToCopy = getSummary();
        if (TextUtils.isEmpty(textToCopy)) {
            textToCopy = getTitle();
        }
        if (!TextUtils.isEmpty(textToCopy)) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText(null, textToCopy);
                clipboard.setPrimaryClip(clip);

                Context appContext = getContext().getApplicationContext();
                if (appContext instanceof pyLoadApp) {
                    ((pyLoadApp) appContext).showCenteredSnackbar(R.string.copied, Snackbar.LENGTH_SHORT);
                }
            }
        }
    }
}
