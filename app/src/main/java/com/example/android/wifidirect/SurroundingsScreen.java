package com.example.android.wifidirect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class SurroundingsScreen extends View {
    private final Paint userPaint;

    public SurroundingsScreen(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        userPaint = new Paint();
        userPaint.setColor(Color.RED);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, 100, userPaint);
    }
}
