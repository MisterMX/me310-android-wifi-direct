package com.example.android.wifidirect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

public class SurroundingsScreen extends View {
    private final Paint userPaint;
    private final Paint animationPaint;

    private Location thisDeviceLocation;
    private Map<String, Location> otherDevices = new HashMap<>();

    private long lastDrawTime;
    private float circleScale = 0f;


    public SurroundingsScreen(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        userPaint = new Paint();
        userPaint.setColor(Color.RED);
        userPaint.setShadowLayer(10.0f, 9.0f, 9.0f, 0xff0000);

        animationPaint = new Paint();
        animationPaint.setStyle(Paint.Style.STROKE);
        animationPaint.setColor(Color.GRAY);
        animationPaint.setStrokeWidth(1);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long currentTime = System.currentTimeMillis();
        float elapsedTime = (float)(currentTime - lastDrawTime) / 1000;
        lastDrawTime = currentTime;

        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;

        canvas.drawCircle(centerX, centerY, circleScale, animationPaint);
        circleScale += elapsedTime * 400;
        if (circleScale >= Math.max(getWidth(), getHeight())) {
            circleScale = 0;
        }

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, 100, userPaint);


        for (Location otherDevice : otherDevices.values()) {
            double longDiff = otherDevice.getLongitude() - thisDeviceLocation.getLongitude();
            double latDiff = otherDevice.getLatitude() - thisDeviceLocation.getLatitude();

            canvas.drawCircle(getWidth() / 2, getHeight() / 2, 100, userPaint);
        }

        postInvalidateDelayed(1000 / 120);
    }

    private double getLongDiff(Location one, Location other) {
        Location temp = new Location("");
        temp.setLatitude(one.getLatitude());
        temp.setLongitude(one.getLongitude());

        return temp.distanceTo(other);
    }

    public void setThisDeviceLocation(Location thisDeviceLocation) {
        this.thisDeviceLocation = thisDeviceLocation;

        invalidate();
    }

    public void setOtherDeviceLocation(String deviceId, Location otherDeviceLocation) {
        otherDevices.put(deviceId, otherDeviceLocation);

        invalidate();
    }
}
