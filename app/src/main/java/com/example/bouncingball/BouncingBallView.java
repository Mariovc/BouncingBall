package com.example.bouncingball;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class BouncingBallView extends SurfaceView implements SurfaceHolder.Callback {

    private static final int REDUCTION_FACTOR = 10;
    private int xPosition = getWidth() / 2;
    private int yPosition = getHeight() / 2;
    private int xDirection = 20;
    private int yDirection = 40;
    private static int radius = 20;
    private static int ballColor = Color.RED;
    private BouncingBallAnimationThread bbThread = null;


    public BouncingBallView(Context context) {
        super(context);
        build();
    }

    public BouncingBallView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        build();
    }

    public BouncingBallView(Context ctx, AttributeSet attrs, int defStyle) {
        super(ctx, attrs, defStyle);
        build();
    }

    private void build() {
        getHolder().addCallback(this);
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setColor(ballColor);
        canvas.drawCircle(xPosition, yPosition, radius, paint);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
        if (xDirection != 0 || yDirection != 0)
            xDirection = yDirection = 0;
        else {
            xDirection = (int) (event.getX() - xPosition) / REDUCTION_FACTOR;
            yDirection = (int) (event.getY() - yPosition) / REDUCTION_FACTOR;
        }
        return true;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (bbThread != null) return;
        bbThread = new BouncingBallAnimationThread(getHolder());
        bbThread.start();
    }

    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        bbThread.stop = true;
    }

    private class BouncingBallAnimationThread extends Thread {
        public boolean stop = false;
        private SurfaceHolder surfaceHolder;

        public BouncingBallAnimationThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;
        }

        public void run() {
            while (!stop) {
                xPosition += xDirection;
                yPosition += yDirection;
                if (xPosition < 0) {
                    xDirection = -xDirection;
                    xPosition = radius;
                }
                if (xPosition > getWidth() - radius) {
                    xDirection = -xDirection;
                    xPosition = getWidth() - radius;
                }
                if (yPosition < 0) {
                    yDirection = -yDirection;
                    yPosition = radius;
                }
                if (yPosition > getHeight() - radius) {
                    yDirection = -yDirection;
                    yPosition = getHeight() - radius - 1;
                }
                Canvas c = null;
                try {
                    c = surfaceHolder.lockCanvas(null);
                    synchronized (surfaceHolder) {
                        onDraw(c);
                    }
                } catch (Exception e) {
                } finally {
                    if (c != null) surfaceHolder.unlockCanvasAndPost(c);
                }
            }
        }
    }


    /**
     * Getters and setters
     */
    public int getxPosition() {
        return xPosition;
    }

    public void setxPosition(int xPosition) {
        this.xPosition = xPosition;
    }

    public int getyPosition() {
        return yPosition;
    }

    public void setyPosition(int yPosition) {
        this.yPosition = yPosition;
    }

    public int getxDirection() {
        return xDirection;
    }

    public void setxDirection(int xDirection) {
        this.xDirection = xDirection;
    }

    public int getyDirection() {
        return yDirection;
    }

    public void setyDirection(int yDirection) {
        this.yDirection = yDirection;
    }

    public static int getRadius() {
        return radius;
    }

    public static void setRadius(int radius) {
        BouncingBallView.radius = radius;
    }

    public static int getBallColor() {
        return ballColor;
    }

    public static void setBallColor(int ballColor) {
        BouncingBallView.ballColor = ballColor;
    }
}
