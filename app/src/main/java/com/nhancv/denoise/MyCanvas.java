/*
 * MIT License
 *
 *
 * Copyright (c) 2019 Nhan Cao
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nhancv.denoise;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MyCanvas extends View {

    private static final String TAG = MyCanvas.class.getSimpleName();
    private Bitmap bm;
    private Paint paint;
    private ValueAnimator degAnimator, moveAnimator;

    private int deg = 0, moving = 0;
    private int animMaxVal = 40;
    private Timer timer;
    private TimerTask timerTask;

    private Random random = new Random();
    private int MAX_NOISE = 20;
    private Point lastP = new Point(0, 500);
    private final List<Point> movingPointList = new ArrayList<>();
    private float[] pts = new float[]{};

    //gen noise in [0, 5]
    private int noiseGenerate() {
        return random.nextInt(MAX_NOISE) - MAX_NOISE / 2;
    }

    public MyCanvas(Context context) {
        this(context, null, 0, 0);
    }

    public MyCanvas(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public MyCanvas(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MyCanvas(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        bm = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(this.getResources(), R.drawable.nerd_eye)
                , 500, 200, false);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(10f);

        degAnimator = ValueAnimator.ofInt(-animMaxVal, animMaxVal);
        degAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                deg = (int) animation.getAnimatedValue();
            }
        });
        degAnimator.setDuration(2000);
        degAnimator.setRepeatCount(ValueAnimator.INFINITE);
        degAnimator.setRepeatMode(ValueAnimator.REVERSE);

        final int movingMaxVal = 450;
        moveAnimator = ValueAnimator.ofInt(0, movingMaxVal);
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                synchronized (movingPointList) {
                    movingPointList.clear();
                }
            }

        });
        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moving = (int) animation.getAnimatedValue();
            }

        });
        moveAnimator.setDuration(2000);
        moveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        moveAnimator.setRepeatMode(ValueAnimator.REVERSE);

        timer = new Timer("timer", true);
        timerTask = new TimerTask() {
            @Override
            public void run() {

                int noise = noiseGenerate();

                int x = moving + noise;
                int y = 500 + noise;
//                if (Math.abs(lastP.x - x) > 15 || Math.abs(lastP.y - y) > 15) {
                lastP.x = x;
                lastP.y = y;
//                }

                synchronized (movingPointList) {
                    movingPointList.add(new Point(lastP.x, lastP.y));
                    pts = new float[movingPointList.size() * 2];
                    int i = 0;
                    for (Point point : movingPointList) {
                        pts[i++] = point.x;
                        pts[i++] = point.y;
                    }
                }
                postInvalidate();
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000 / 30);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        timerTask.run();
//        degAnimator.start();
        moveAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        degAnimator.cancel();
        moveAnimator.cancel();
        timerTask.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Matrix mt = new Matrix();
        Camera camera = new Camera();

        camera.save();
        camera.rotateY(deg);
        camera.getMatrix(mt);
        mt.preTranslate(-250, -100);
        mt.postTranslate(250, 100);
        camera.restore();

        mt.postTranslate(lastP.x, lastP.y);
        canvas.drawBitmap(bm, mt, null);
        canvas.drawLines(pts, paint);

    }

}
