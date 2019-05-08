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
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MyCanvas extends View {

    private static final String TAG = MyCanvas.class.getSimpleName();

    private static final int MOVING_MAX_VAL = 1000;
    private static final int MAX_NOISE = 60;
    private int moving = 0;
    private Paint paintRed, paintBlue, paintBlack;
    private Random random = new Random();
    private TimerTask generatePointTimerTask;
    private ValueAnimator moveAnimator;

    //Noise line
    private final List<PointF> noiseList = new ArrayList<>();

    //Smooth line
    private final List<PointF> smoothList = new ArrayList<>();
    private KalmanFilter kalmanFilterY = new KalmanFilter(1f, 1f, 0.001f);

    //gen noise in [0, MAX_NOISE]
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
        paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStyle(Paint.Style.FILL);
        paintRed.setStrokeWidth(50f);
        paintBlue = new Paint();
        paintBlue.setColor(Color.BLUE);
        paintBlue.setStyle(Paint.Style.FILL);
        paintBlue.setStrokeWidth(10f);
        paintBlack = new Paint();
        paintBlack.setColor(Color.BLACK);
        paintBlack.setStyle(Paint.Style.FILL);
        paintBlack.setStrokeWidth(5f);

        moveAnimator = ValueAnimator.ofInt(0, MOVING_MAX_VAL);
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
                synchronized (noiseList) {
                    noiseList.clear();
                }
                synchronized (smoothList) {
                    smoothList.clear();
                }
            }
        });
        moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                moving = (int) animation.getAnimatedValue();
            }

        });
        moveAnimator.setDuration(3000);
        moveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        moveAnimator.setRepeatMode(ValueAnimator.REVERSE);

        Timer generatePointTimer = new Timer("generatePointTimer", true);
        generatePointTimerTask = new TimerTask() {
            @Override
            public void run() {
                int noise = noiseGenerate();
                int x = moving;
                int y = 800 + noise;
                synchronized (smoothList) {
                    smoothList.add(new PointF(x, kalmanFilterY.updateEstimate(y)));
                }
                synchronized (noiseList) {
                    noiseList.add(new PointF(x, y - 300));
                }
                postInvalidate();
            }
        };
        generatePointTimer.scheduleAtFixedRate(generatePointTimerTask, 0, 1000 / 50);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        generatePointTimerTask.run();
        moveAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        moveAnimator.cancel();
        generatePointTimerTask.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        synchronized (noiseList) {
            if (noiseList.size() > 0) {
                PointF lastP = noiseList.get(noiseList.size() - 1);
                canvas.drawPoint(lastP.x, lastP.y, paintRed);
                for (PointF pointF : noiseList) {
                    canvas.drawPoint(pointF.x, pointF.y, paintBlue);
                }
            }
        }
        synchronized (smoothList) {
            if (smoothList.size() > 0) {
                PointF lastP = smoothList.get(smoothList.size() - 1);
                canvas.drawPoint(lastP.x, lastP.y, paintRed);
                for (PointF pointF : smoothList) {
                    canvas.drawPoint(pointF.x, pointF.y, paintBlue);
                }
            }
        }
    }

    private void draw3DBitmap(Canvas canvas, Bitmap bm, PointF pointF, int deg) {
        Matrix mt = new Matrix();
        Camera camera = new Camera();
        camera.save();
        camera.rotateY(deg);
        camera.getMatrix(mt);
        mt.preTranslate(-250, -100);
        mt.postTranslate(250, 100);
        camera.restore();
        mt.postTranslate(pointF.x, pointF.y);
        canvas.drawBitmap(bm, mt, null);
    }

}
