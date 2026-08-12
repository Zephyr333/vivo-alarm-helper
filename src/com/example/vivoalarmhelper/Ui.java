package com.example.vivoalarmhelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class Ui {
    public static final int COLOR_BACKGROUND = Color.rgb(247, 247, 250);
    public static final int COLOR_CARD = Color.WHITE;
    public static final int COLOR_TEXT = Color.rgb(24, 25, 29);
    public static final int COLOR_SUBTEXT = Color.rgb(154, 155, 161);
    public static final int COLOR_PRIMARY = Color.rgb(247, 81, 77);
    public static final int COLOR_PRIMARY_SOFT = Color.rgb(255, 238, 237);
    public static final int COLOR_BORDER = Color.rgb(233, 233, 237);
    public static final int COLOR_SUCCESS = Color.rgb(38, 146, 88);
    public static final int COLOR_WARNING = Color.rgb(220, 86, 71);
    public static final int COLOR_DISABLED = Color.rgb(205, 206, 211);

    private Ui() {
    }

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    /**
     * Android 15+ enforces edge-to-edge for targetSdk 35. Every activity goes
     * through this method so the app bar and scroll content stay outside the
     * status/navigation bars on the target vivo device.
     */
    public static void setContentView(Activity activity, View page) {
        Window window = activity.getWindow();
        window.setStatusBarColor(COLOR_BACKGROUND);
        window.setNavigationBarColor(COLOR_BACKGROUND);
        if (Build.VERSION.SDK_INT >= 29) {
            window.setNavigationBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= 23) {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        final int initialLeft = page.getPaddingLeft();
        final int initialTop = page.getPaddingTop();
        final int initialRight = page.getPaddingRight();
        final int initialBottom = page.getPaddingBottom();
        page.setOnApplyWindowInsetsListener((view, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars()
                                | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(initialLeft + left, initialTop + top,
                    initialRight + right, initialBottom + bottom);
            return insets;
        });
        activity.setContentView(page);
        page.requestApplyInsets();
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
        view.setTypeface(view.getTypeface(), Typeface.BOLD);
        return view;
    }

    public static Button button(Context context, String label) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextSize(15);
        button.setMinHeight(dp(context, 48));
        button.setMinWidth(dp(context, 48));
        button.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        button.setBackground(roundedBackground(context,
                Color.rgb(242, 242, 245), Color.TRANSPARENT, 18));
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
        bar.setPadding(dp(activity, 4), dp(activity, 6),
                dp(activity, 4), dp(activity, 6));
        bar.setMinimumHeight(dp(activity, 68));

        Button leading = textButton(activity, left == null ? "" : left);
        leading.setContentDescription(left == null || left.isEmpty()
                ? "" : ("‹".equals(left) ? "返回" : left));
        if ("‹".equals(left)) leading.setTextSize(34);
        leading.setEnabled(leftClick != null);
        if (leftClick != null) leading.setOnClickListener(leftClick);
        bar.addView(leading, new LinearLayout.LayoutParams(
                dp(activity, 82), dp(activity, 56)));

        TextView titleView = heading(activity, title, 22);
        titleView.setGravity(Gravity.CENTER);
        titleView.setSingleLine(true);
        bar.addView(titleView, new LinearLayout.LayoutParams(
                0, dp(activity, 56), 1f));

        Button trailing = textButton(activity, right == null ? "" : right);
        trailing.setContentDescription(right == null ? "" : right);
        trailing.setEnabled(rightClick != null);
        if (rightClick != null) trailing.setOnClickListener(rightClick);
        bar.addView(trailing, new LinearLayout.LayoutParams(
                dp(activity, 82), dp(activity, 56)));
        return bar;
    }

    public static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(context, 16), 0, dp(context, 16), 0);
        card.setBackground(roundedBackground(context,
                COLOR_CARD, Color.TRANSPARENT, 28));
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
        row.setMinimumHeight(dp(context, summary == null ? 68 : 80));
        row.setPadding(dp(context, 4), dp(context, 10),
                dp(context, 4), dp(context, 10));
        LinearLayout textColumn = new LinearLayout(context);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(context, title, 17, COLOR_TEXT);
        textColumn.addView(titleView);
        if (summary != null && !summary.isEmpty()) {
            TextView summaryView = text(context, summary, 13, COLOR_SUBTEXT);
            summaryView.setPadding(0, dp(context, 5), 0, 0);
            textColumn.addView(summaryView);
        }
        row.addView(textColumn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (arrow) row.addView(chevron(context));
        makeClickable(row, listener);
        return row;
    }

    public static LinearLayout navigationRow(Context context, String title,
            String value, boolean enabled, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(context, 72));
        row.setPadding(dp(context, 4), dp(context, 8),
                dp(context, 2), dp(context, 8));
        TextView titleView = text(context, title, 17,
                enabled ? COLOR_TEXT : COLOR_DISABLED);
        row.addView(titleView, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (value != null && !value.isEmpty()) {
            TextView valueView = text(context, value, 14,
                    enabled ? COLOR_SUBTEXT : COLOR_DISABLED);
            valueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            valueView.setMaxLines(2);
            row.addView(valueView, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        if (enabled) row.addView(chevron(context));
        row.setAlpha(enabled ? 1f : 0.68f);
        makeClickable(row, enabled ? listener : null);
        return row;
    }

    public static LinearLayout switchRow(Context context, String title,
            String summary, boolean checked,
            CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = row(context, title, summary, false, null);
        Switch toggle = new Switch(context);
        toggle.setChecked(checked);
        toggle.setContentDescription(title);
        toggle.setShowText(false);
        toggle.setOnCheckedChangeListener(listener);
        row.addView(toggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(context, 56)));
        return row;
    }

    public static LinearLayout radioRow(Context context, String title,
            String summary, boolean selected, View.OnClickListener listener) {
        LinearLayout row = row(context, title, summary, false, listener);
        TextView radio = text(context, "", 1, Color.TRANSPARENT);
        radio.setGravity(Gravity.CENTER);
        radio.setContentDescription(selected ? "已选择" : "未选择");
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(Color.WHITE);
        ring.setStroke(dp(context, selected ? 4 : 2),
                selected ? COLOR_PRIMARY : COLOR_DISABLED);
        radio.setBackground(ring);
        LinearLayout holder = new LinearLayout(context);
        holder.setGravity(Gravity.CENTER);
        holder.addView(radio, new LinearLayout.LayoutParams(
                dp(context, 23), dp(context, 23)));
        row.addView(holder, new LinearLayout.LayoutParams(
                dp(context, 52), dp(context, 52)));
        return row;
    }

    public static TextView fab(Context context, String contentDescription) {
        TextView fab = text(context, "+", 36, Color.WHITE);
        fab.setGravity(Gravity.CENTER);
        fab.setContentDescription(contentDescription);
        fab.setClickable(true);
        fab.setFocusable(true);
        fab.setElevation(dp(context, 12));
        fab.setBackground(roundedBackground(context,
                COLOR_PRIMARY, Color.TRANSPARENT, 32));
        return fab;
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
        LinearLayout.LayoutParams params = matchWrap(dp(context, 16));
        params.setMarginStart(dp(context, 20));
        params.setMarginEnd(dp(context, 20));
        return params;
    }

    public static FrameLayout.LayoutParams fabParams(Context context) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(context, 64), dp(context, 64),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.bottomMargin = dp(context, 28);
        return params;
    }

    private static TextView chevron(Context context) {
        TextView arrow = text(context, "›", 32, Color.rgb(185, 186, 191));
        arrow.setGravity(Gravity.CENTER);
        arrow.setContentDescription("进入");
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        arrow.setLayoutParams(new LinearLayout.LayoutParams(
                dp(context, 32), dp(context, 48)));
        return arrow;
    }

    private static void makeClickable(View view, View.OnClickListener listener) {
        if (listener == null) return;
        view.setClickable(true);
        view.setFocusable(true);
        view.setOnClickListener(listener);
    }
}
