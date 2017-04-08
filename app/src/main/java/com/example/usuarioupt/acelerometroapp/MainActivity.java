package com.example.usuarioupt.acelerometroapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.MessagePattern;
import android.os.PowerManager;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private VistaSimulacion mSimulationView;
    private SensorManager mSensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    private Display mDisplay;
    private PowerManager.WakeLock mWakelock;


    public class VistaSimulacion extends FrameLayout
            implements SensorEventListener {

        //Diametro de los actores
        private static final float sBallDiameter = 0.004f;
        private static final float sBallDiameter2
                = sBallDiameter * sBallDiameter;

        // alto y ancho
        private final int mDstWidth;
        private final int mDstHeight;
        private Sensor mAccelerometer;
        private long mLastT;

        //Desplazamiento
        private float mXDpi;
        private float mYDpi;
        private float mMetersToPixelsX;
        private float mMetersToPixelsY;
        private float mXOrigin;
        private float mYOrigin;
        private float mSensorX;
        private float mSensorY;
        private float mHotizontalBound;
        private float mVerticalBound;

        private final SistemaPartes mSistemaPartes;


        public VistaSimulacion(@NonNull Context context) {
            super(context);
            mAccelerometer = mSensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);

            mXDpi = metrics.xdpi;
            mYDpi = metrics.ydpi;

            mMetersToPixelsX = mXDpi / 0.0254f;
            mMetersToPixelsY = mYDpi / 0.0254f;

            //redimensionar
            mDstWidth = (int) (sBallDiameter * mMetersToPixelsX + 0.5f);
            mDstHeight = (int) (sBallDiameter * mMetersToPixelsY + 0.5f);

            mSistemaPartes = new SistemaPartes();

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inDither = true;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;


        }

        public void startSimulation() {
            mSensorManager.registerListener(this, mAccelerometer,
                    SensorManager.SENSOR_DELAY_GAME);
        }

        public void stopSimulation() {
            mSensorManager.unregisterListener(this);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            mXOrigin = (w - mDstWidth) * 0.5f;
            mYOrigin = (h - mDstHeight) * 0.5f;

            mHotizontalBound = ((w / mMetersToPixelsX) - sBallDiameter) * 0.5f;
            mVerticalBound = ((h / mMetersToPixelsY) - sBallDiameter) * 0.5f;
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                return;

            switch (mDisplay.getRotation()) {
                case Surface.ROTATION_0:
                    mSensorX = event.values[0];
                    mSensorY = event.values[1];
                    break;
                case Surface.ROTATION_90:
                    mSensorX = -event.values[1];
                    mSensorY = event.values[0];
                    break;
                case Surface.ROTATION_180:
                    mSensorX = -event.values[0];
                    mSensorY = -event.values[1];
                    break;
                case Surface.ROTATION_270:
                    mSensorX = event.values[1];
                    mSensorY = -event.values[0];
                    break;
            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }


        @Override
        protected void onDraw(Canvas canvas) {

            final SistemaPartes sistemaPartes = mSistemaPartes;
            final long now = System.currentTimeMillis();
            final float sx = mSensorX;
            final float sy = mSensorY;

            sistemaPartes.update(sx, sy, now);
            final float xc = mXOrigin;
            final float yc = mYOrigin;

            final float xs = mMetersToPixelsX;
            final float ys = mMetersToPixelsY;

            final int count = sistemaPartes.getParteCount();

            for (int i = 0; i < count; i++) {
                final float x = xc + sistemaPartes.getPosX(i) * xs;
                final float y = yc - sistemaPartes.getPosY(i) * ys;
                sistemaPartes.mBalls[i].setTranslationX(x);
                sistemaPartes.mBalls[i].setTranslationY(y);
            }
            invalidate();
        }

        //Clase Parte
        class Parte extends View {

            private float mPosX = (float) Math.random();
            private float mPosY = (float) Math.random();

            private float mVelX;
            private float mVelY;

            public Parte(Context context) {
                super(context);
            }

            public Parte(Context context, @Nullable AttributeSet attrs) {
                super(context, attrs);
            }

            public Parte(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
            }

            public Parte(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
                super(context, attrs, defStyleAttr, defStyleRes);
            }

            //Programar la fisica del movimiento
            public void computePhysics(float sx, float sy, float dT) {
                final float ax = -sx / 5;
                final float ay = -sy / 5;

                mPosX += mVelX * dT + ax * dT * dT / 2;
                mPosY += mVelY * dT + ay * dT * dT / 2;

                mVelX += ax * dT;
                mVelY += ay * dT;

            }

            //
            public void resolveCollisionWithBounds() {
                final float xmax = mHotizontalBound;
                final float ymax = mVerticalBound;

                final float x = mPosX;
                final float y = mPosY;

                if (x > xmax) {
                    mPosX = xmax;
                    mVelX = 0;
                } else if (x < -xmax) {
                    mPosX = -xmax;
                    mVelX = 0;
                }
                if (y > ymax) {
                    mPosY = ymax;
                    mVelY = 0;
                } else if (y < -ymax) {
                    mPosY = -ymax;
                    mVelY = 0;
                }
            }

        }

        class SistemaPartes {
            static final int NUM_PARTICLES = 8;
            private Parte mBalls[] = new Parte[NUM_PARTICLES];

            SistemaPartes() {
                for (int i = 0; i < mBalls.length; i++) {
                    mBalls[i] = new Parte(getContext());
                    mBalls[i].setBackgroundResource(R.drawable.pig);
                    mBalls[i].setLayerType(LAYER_TYPE_HARDWARE, null);
                    addView(mBalls[i], new
                            ViewGroup.LayoutParams(mDstWidth, mDstHeight));
                }
            }


            private void updatePositions(float sx, float sy, long timestamp) {
                final long t = timestamp;
                if (mLastT != 0) {
                    final float dT = (float) (t - mLastT) / 1000.f
                            /** (1.0f / 1000000000.0f)*/;
                    final int count = mBalls.length;
                    for (int i = 0; i < count; i++) {
                        Parte ball = mBalls[i];
                        ball.computePhysics(sx, sy, dT);
                    }
                }
                mLastT = t;
            }

            public void update(float sx, float sy, long now) {
                updatePositions(sx, sy, now);

                //fijamos iteraciones
                final int NUM_MAX_ITERATIONS = 10;

                boolean more = true;
                final int count = mBalls.length;

                for (int k = 0; k < NUM_MAX_ITERATIONS && more; k++) {
                    more = false;
                    for (int i = 0; i < count; i++) {
                        Parte curr = mBalls[i];
                        for (int j = i + 1; j < count; j++) {
                            Parte ball = mBalls[j];
                            float dx = ball.mPosX - curr.mPosX;
                            float dy = ball.mPosY - curr.mPosY;
                            float dd = dx * dx + dy * dy;

                            if (dd <= sBallDiameter2) {

                                dx += ((float) Math.random() - 0.5f) * 0.0001f;
                                dy += ((float) Math.random() - 0.5f) * 0.0001f;

                                dd = dx * dx + dy * dy;

                                final float d = (float) Math.sqrt(dd);
                                final float c = (0.5f * (sBallDiameter - d)) / d;

                                final float effectX = dx * c;
                                final float effectY = dy * c;

                                curr.mPosX -= effectX;
                                curr.mPosY -= effectY;

                                ball.mPosX += effectX;
                                ball.mPosY += effectY;
                                more = true;
                            }
                        }
                        curr.resolveCollisionWithBounds();
                    }
                }
            }

            public int getParteCount() {
                return mBalls.length;
            }

            public float getPosX(int i) {
                return mBalls[i].mPosX;
            }

            public float getPosY(int i) {
                return mBalls[i].mPosY;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Instanciamos SensorManager
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        //Instanciamos PowerManager
        mPowerManager = (PowerManager)getSystemService(POWER_SERVICE);

        //Instanciamos WindowManager
        mWindowManager = (WindowManager)getSystemService(WINDOW_SERVICE);

        mDisplay = mWindowManager.getDefaultDisplay();

        //Crear un WakeLock
        mWakelock = mPowerManager
                .newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        ,getClass().getName());

        mSimulationView = new VistaSimulacion(this);
        mSimulationView.setBackgroundResource(R.drawable.space);
        setContentView(mSimulationView);
    }

    @Override
    protected void onResume(){
        super.onResume();
        mWakelock.acquire();
        mSimulationView.startSimulation();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mSimulationView.stopSimulation();
        mWakelock.release();
    }

}



