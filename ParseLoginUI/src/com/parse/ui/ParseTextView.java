package com.parse.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class ParseTextView extends TextView {

    private static final String FONT_NAME = "swipes.ttf";

    private Context mContext;
    private static Typeface sTypeface;

    public ParseTextView(Context context) {
        super(context);
        init(context);
    }

    public ParseTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ParseTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        if (sTypeface == null) {
            synchronized (ParseTextView.class) {
                if (sTypeface == null) {
                    sTypeface = Typeface.createFromAsset(mContext.getAssets(), FONT_NAME);
                }
            }
        }
        this.setTypeface(sTypeface);
    }
}
