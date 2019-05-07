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
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MyCanvas extends View {

    private static final String TAG = MyCanvas.class.getSimpleName();

    private static final int DEG_MAX_VAL = 40;
    private static final int MOVING_MAX_VAL = 1000;
    private static final int MAX_NOISE = 60;

    private int deg = 0, moving = 0;
    private RectF rect = new RectF();
    private Bitmap bm;
    private Paint paint;
    private ValueAnimator degAnimator, moveAnimator;
    private TimerTask renderTimerTask, generatePointTimerTask;
    private Random random = new Random();
    private Point lastP = new Point(0, 500);
    private final List<PointF> movingPointList = new ArrayList<>();
    private float[] pts = new float[]{};
    private boolean initFrame = false;

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

        bm = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_square)
                , 200, 200, false);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(10f);

        degAnimator = ValueAnimator.ofInt(-DEG_MAX_VAL, DEG_MAX_VAL);
        degAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                deg = (int) animation.getAnimatedValue();
            }
        });
        degAnimator.setDuration(2000);
        degAnimator.setRepeatCount(ValueAnimator.INFINITE);
        degAnimator.setRepeatMode(ValueAnimator.REVERSE);

        moveAnimator = ValueAnimator.ofInt(0, MOVING_MAX_VAL);
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationRepeat(Animator animation) {
//                synchronized (movingPointList) {
//                    movingPointList.clear();
//                }
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


        ValueAnimator renderAnimator = ValueAnimator.ofInt(0, 22);
        renderAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                synchronized (pts) {
                    if (pts != null && pts.length >= 24) {
                        int pos = (int) animation.getAnimatedValue();
                        rect.set(pts[pos], 500, pts[pos] + 50, 500 + 50);
                        postInvalidate();
                    }
                }
            }

        });
        renderAnimator.setDuration(1000 / 10);
        renderAnimator.setRepeatCount(ValueAnimator.INFINITE);
//        renderAnimator.setRepeatMode(ValueAnimator.REVERSE);
        renderAnimator.start();

        Timer renderTimer = new Timer("renderTimer", true);
        Timer generatePointTimer = new Timer("generatePointTimer", true);
        renderTimerTask = new TimerTask() {
            @Override
            public void run() {
//                postInvalidate();
            }
        };

        generatePointTimerTask = new TimerTask() {
            @Override
            public void run() {
                int noise = noiseGenerate();
                int x = moving + noise;
                int y = 500 + noise;
                if (Math.abs(lastP.x - x) > MAX_NOISE || Math.abs(lastP.y - y) > MAX_NOISE) {
                    lastP.x = x;
                    lastP.y = 500;
                }

                synchronized (movingPointList) {
                    movingPointList.add(new PointF(lastP.x, lastP.y));

                    //Calculate position
                    int sectionLength = 4;
                    int sectionExpendLength = sectionLength * 3;
                    int ptsSkip = 2;
                    if (movingPointList.size() >= sectionLength) {
                        pts = new float[sectionExpendLength * ptsSkip];

                        final List<PointF> subMovingList = new ArrayList<>();
                        for (int i = 0; i < sectionLength; i++) {
                            subMovingList.add(movingPointList.get(i));
                        }

                        for (int i = 0; i < sectionExpendLength; i++) {
                            PointF point = getQuadraticBezier(subMovingList, i * 1f / sectionExpendLength);
//                            PointF pointNext = getQuadraticBezier(subMovingList, (i + 1) * 1f / sectionExpendLength);
                            pts[i * ptsSkip] = point.x;
                            pts[i * ptsSkip + 1] = point.y;
//                            pts[i * ptsSkip + 2] = pointNext.x;
//                            pts[i * ptsSkip + 3] = pointNext.y;
                        }
                        if (movingPointList.size() > sectionLength * 2) {
                            int i = 0;
                            while (i++ < sectionLength) {
                                movingPointList.remove(0);
                            }
                        }

                        Log.e(TAG, "run: pts length = " + pts.length + " movingPointList.length = " + movingPointList.size());
                    }

                }
            }
        };
        renderTimer.scheduleAtFixedRate(renderTimerTask, 0, 1000 / 10);
        generatePointTimer.scheduleAtFixedRate(generatePointTimerTask, 0, 1000 / 20);

    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        renderTimerTask.run();
        generatePointTimerTask.run();

//        degAnimator.start();
        moveAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        degAnimator.cancel();
        moveAnimator.cancel();

        renderTimerTask.cancel();
        generatePointTimerTask.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(rect, paint);
        canvas.drawPoints(pts, paint);
    }

    /**
     * Let B(P0 P1 ... Pn) denote the Bezier curve dtermined by any selection of points P0, P1, ..., Pn. Then to start:
     * B(P0[t]) = P0, and
     * B(t) = B(P0 P1 ... Pn) (t) = (1 - t) B(P0 P1 .. Pn-1) (t) + tB(P1 P2 ... Pn) (t)
     *
     * @param t where t is timer animation with float range [0..1]
     */
    private PointF getQuadraticBezier(List<PointF> offsetList, double t) {
        return getQuadraticBezier2(offsetList, t, 0, offsetList.size() - 1);
    }

    private PointF getQuadraticBezier2(List<PointF> offsetList, double t, int i, int j) {
        if (i == j) return offsetList.get(i);

        PointF b0 = getQuadraticBezier2(offsetList, t, i, j - 1);
        PointF b1 = getQuadraticBezier2(offsetList, t, i + 1, j);
        return new PointF((float) ((1f - t) * b0.x + t * b1.x), (float) ((1f - t) * b0.y + t * b1.y));
    }

    private void draw3DBitmap(Canvas canvas) {
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
    }

}
