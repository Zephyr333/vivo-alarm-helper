package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class Ui {
    public static final int COLOR_BACKGROUND = Color.rgb(246, 246, 249);
    public static final int COLOR_CARD = Color.WHITE;
    public static final int COLOR_TEXT = Color.rgb(24, 25, 29);
    public static final int COLOR_SUBTEXT = Color.rgb(139, 140, 147);
    public static final int COLOR_PRIMARY = Color.rgb(244, 92, 86);
    public static final int COLOR_BORDER = Color.rgb(231, 231, 235);
    public static final int COLOR_SUCCESS = Color.rgb(44, 148, 90);
    public static final int COLOR_WARNING = Color.rgb(211, 125, 41);

    private Ui() {
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context context, String value, int sizeSp, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.12f);
        return view;
    }

    public static TextView heading(Context context, String value, int sizeSp) {
        TextView view = text(context, value, sizeSp, COLOR_TEXT);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    public static Button button(Context context, String label) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(14);
        button.setMinHeight(dp(context, 48));
        button.setMinWidth(dp(context, 48));
        button.setPadding(dp(context, 10), 0, dp(context, 10), 0);
        button.setBackground(roundedBackground(context,
                Color.rgb(242, 242, 245), Color.TRANSPARENT, 14));
        return button;
    }

    public static Button textButton(Context context, String label) {
        Button button = button(context, label);
        button.setTextColor(COLOR_PRIMARY);
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    public static LinearLayout toolbar(Activity activity, String title,
            String left, View.OnClickListener leftClick,
            String right, View.OnClickListener rightClick) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(activity, 4), dp(activity, 4),
                dp(activity, 4), dp(activity, 4));
        Button cancel = textButton(activity, left);
        cancel.setOnClickListener(leftClick);
        bar.addView(cancel, new LinearLayout.LayoutParams(
                dp(activity, 84), dp(activity, 52)));
        TextView titleView = heading(activity, title, 20);
        titleView.setGravity(Gravity.CENTER);
        bar.addView(titleView, new LinearLayout.LayoutParams(
                0, dp(activity, 52), 1f));
        Button done = textButton(activity, right == null ? "" : right);
        done.setEnabled(right != null);
        if (rightClick != null) done.setOnClickListener(rightClick);
        bar.addView(done, new LinearLayout.LayoutParams(
                dp(activity, 84), dp(activity, 52)));
        return bar;
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        card.setBackground(roundedBackground(context,
                COLOR_CARD, Color.TRANSPARENT, 24));
        return card;
    }

    public static View divider(Context context) {
        View divider = new View(context);
        divider.setBackgroundColor(COLOR_BORDER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1));
        divider.setLayoutParams(params);
        return divider;
    }

    public static LinearLayout row(Context context, String title,
            String summary, boolean arrow, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, summary == null ? 68 : 76));
        row.setPadding(dp(context, 4), dp(context, 9),
                dp(context, 4), dp(context, 9));
        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(context, title, 17, COLOR_TEXT);
        textColumn.addView(titleView);
        if (summary != null && !summary.isEmpty()) {
            TextView summaryView = text(context, summary, 12, COLOR_SUBTEXT);
            summaryView.setPadding(0, dp(context, 4), 0, 0);
            textColumn.addView(summaryView);
        }
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (arrow) {
            TextView value = text(context, "›", 32, Color.rgb(185, 186, 191));
            value.setGravity(Gravity.CENTER);
            row.addView(value, new LinearLayout.LayoutParams(
                    dp(context, 30), dp(context, 48)));
        }
        if (listener != null) {
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(listener);
        }
        return row;
    }

    public static LinearLayout switchRow(Context context, String title,
            String summary, boolean checked,
            CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = row(context, title, summary, false, null);
        Switch toggle = new Switch(context);
        toggle.setChecked(checked);
        toggle.setContentDescription(title);
        toggle.setOnCheckedChangeListener(listener);
        row.addView(toggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 56)));
        return row;
    }

    public static GradientDrawable roundedBackground(Context context,
            int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(fillColor);
        background.setCornerRadius(dp(context, radiusDp));
        if (strokeColor != Color.TRANSPARENT) {
            background.setStroke(dp(context, 1), strokeColor);
        }
        return background;
    }

    public static LinearLayout.LayoutParams matchWrap(int bottomMarginPx) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = bottomMarginPx;
        return params;
    }

    public static LinearLayout.LayoutParams pageCardParams(Context context) {
        LinearLayout.LayoutParams params = matchWrap(dp(context, 18));
        params.setMarginStart(dp(context, 16));
        params.setMarginEnd(dp(context, 16));
        return params;
    }
}
