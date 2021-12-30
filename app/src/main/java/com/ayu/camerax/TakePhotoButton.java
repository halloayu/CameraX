package com.ayu.camerax;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

public class TakePhotoButton extends androidx.appcompat.widget.AppCompatButton {

    private final Paint mPaint;
    private final int outerRadius;
    private int innerRadius;
    private final int outerCircleColor;
    private final int innerCircleColor;
    private final RectF rect;
    private int curStatus = 0; // 0-预览中 1-拍照中

    public TakePhotoButton(Context context) {
        this(context, null);
    }

    public TakePhotoButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TakePhotoButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        outerRadius = (int) context.getResources().getDimension(R.dimen.border_thickness);
        outerCircleColor = context.getResources().getColor(R.color.white);
        innerCircleColor = context.getResources().getColor(R.color.white);
        mPaint = new Paint();
        mPaint.setAntiAlias(true); //抗锯齿功能
        rect = new RectF();
        curStatus = 0;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        innerRadius = (int) (getWidth() / 2 - 2.5 * outerRadius);
        int innerSmallRadius = getWidth() / 10;
        rect.left = getWidth() / 2 - innerSmallRadius;
        rect.top = getHeight() / 2 - innerSmallRadius;
        rect.right = getWidth() / 2 + innerSmallRadius;
        rect.bottom = getHeight() / 2 + innerSmallRadius;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawOuterCircle(canvas, outerCircleColor);
        switch (curStatus) {
            case 0:
                drawInnerCircle(canvas, innerCircleColor);
                break;
            case 1:
                drawInnerCircleInTaking(canvas, innerCircleColor);
        }

    }

    private void drawOuterCircle(Canvas canvas, int color) {
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(outerRadius);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getWidth() / 2 - outerRadius, mPaint);
    }

    private void drawInnerCircle(Canvas canvas, int color) {
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, innerRadius, mPaint);
    }

    private void drawInnerCircleInTaking(Canvas canvas, int color) {
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, innerRadius - outerRadius, mPaint);
    }

    public void startTaking() {
        curStatus = 1;
        invalidate();
    }

    public void stopTaking() {
        curStatus = 0;
        invalidate();
    }
}
