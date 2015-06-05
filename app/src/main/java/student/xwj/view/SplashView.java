package student.xwj.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import student.xwj.R;


/**
 * Created by admin on 2015/6/2.
 */
public class SplashView extends View {
    // 大圆半径
    private float mRotationRadius = 90;
    // 每个小圆半径
    private float mCircleRadius = 18;
    //小圆的颜色
    private int[] mCircleColors;

    //大圆、小圆旋转的动画时间
    private int mRotationDuration = 1800;
    // 聚散动画执行时间
    private int mMergingTime = 400;
    // 扩散动画执行时间
    private int mExpandingTime = 2000;

    //整体的背景颜色
    private int mSplashBgColor = Color.WHITE;
    //扩散时的背景颜色
    private int mExpandingBackgroundColor = Color.BLUE;

    //空心圆初始半径
    private float mHoleRadius = 0f;
    //当前大圆旋转角度（弧度）
    private float mCurrentRotationAngle = 0f;

    //当前大圆半径
    private float mCurrentRotationRadius = mRotationRadius;

    // 绘制圆的画笔
    private Paint mPaint = new Paint();
    // 绘制背景的画笔
    private Paint mPaintBackground = new Paint();

    //屏幕正中心点的坐标
    private float mCurrentX;
    private float mCurrentY;
    //屏幕对角线的一半
    private float mDiagonalDist;
    // 保存当前动画状态-当前执行哪种动画
    private SplashState mState = null;

    private abstract class SplashState {
        public abstract void drawState(Canvas canvas);
    }

    public SplashView(Context context) {
        this(context, null);
    }

    public SplashView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SplashView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SplashView);

        mRotationRadius = a.getDimensionPixelSize(R.styleable.SplashView_svRotationRadius, (int) mRotationRadius);
        mCircleRadius = a.getDimensionPixelSize(R.styleable.SplashView_svCircleRadius, (int) mCircleRadius);
        mExpandingBackgroundColor = a.getColor(R.styleable.SplashView_svExpandingBackgroundColor, mExpandingBackgroundColor);
        mExpandingTime = a.getInteger(R.styleable.SplashView_svExpandingTime, mExpandingTime);
        mMergingTime = a.getInteger(R.styleable.SplashView_svMergingTime, mMergingTime);
        mRotationDuration = a.getInteger(R.styleable.SplashView_svRotationDuration, mRotationDuration);

        mCircleColors = context.getResources().getIntArray(R.array.color_array);
        mPaint.setAntiAlias(true);
        mPaintBackground.setAntiAlias(true);
        mPaintBackground.setStyle(Paint.Style.STROKE);
        mPaintBackground.setColor(mSplashBgColor);
    }

    // 等数据加载完毕后，显示加载后面的两个动画
    public void splashAndDisapper() {
        if (mState != null && mState instanceof RotationState) {
            RotationState rs = (RotationState) mState;
            rs.cancle();
            post(new Runnable() {
                @Override
                public void run() {
                    mState = new MergingState();
                }
            });
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCurrentX = w / 2f;
        mCurrentY = h / 2f;
        // Math.sqrt(w * w + h * h)  平方根
        mDiagonalDist = (float) (Math.sqrt(w * w + h * h) / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mState == null) {
            //第一次执行动画
            mState = new RotationState();
        }
        mState.drawState(canvas);
        super.onDraw(canvas);
    }

    private class RotationState extends SplashState {
        ValueAnimator valueAnimator;

        public RotationState() {
            //小圆的坐标--》大圆的半径  大圆当前旋转了多少角度
            // 估值器(0-2π)
            valueAnimator = ValueAnimator.ofFloat(0, (float) Math.PI * 2);
            // 线性插值器
            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.setDuration(mRotationDuration);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRotationAngle = (float) animation.getAnimatedValue();
                    //view重绘
                    invalidate();
                }
            });
            // 旋转次数 无穷-->重复旋转
            valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
            valueAnimator.start();
        }

        @Override
        public void drawState(Canvas canvas) {
            drawBackground(canvas);
            drawCircles(canvas);
        }

        public void cancle() {
            valueAnimator.cancel();
        }
    }

    /**
     * 聚合动画 --->大圆半径
     */
    private class MergingState extends SplashState {
        ValueAnimator valueAnimator;

        public MergingState() {
            // 估值器(大圆半径：r-0)
            valueAnimator = ValueAnimator.ofFloat(0, mRotationRadius);
            // 弹性插值器--弹射效果
            valueAnimator.setInterpolator(new OvershootInterpolator(8f));
            valueAnimator.setDuration(mMergingTime);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRotationRadius = (float) animation.getAnimatedValue();
                    //view重绘
                    invalidate();
                }
            });
            valueAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mState = new ExpandingState();
                }
            });
            //反转
            valueAnimator.reverse();
        }

        @Override
        public void drawState(Canvas canvas) {
            drawBackground(canvas);
            // 绘制小圆的位置
            drawCircles(canvas);
        }
    }

    /**
     * 扩散动画
     */
    private class ExpandingState extends SplashState {
        ValueAnimator valueAnimator;

        public ExpandingState() {
            // 估值器(空心圆半径：0-对角线的一半)
            valueAnimator = ValueAnimator.ofFloat(0, mDiagonalDist);
            // 弹性插值器--弹射效果
            //valueAnimator.setInterpolator(new OvershootInterpolator(6f));
            valueAnimator.setDuration(mExpandingTime);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mHoleRadius = (float) animation.getAnimatedValue();
                    invalidate();
                }
            });
            valueAnimator.start();
        }

        @Override
        public void drawState(Canvas canvas) {
            drawBackground(canvas);
        }
    }

    /**
     * 清空画板/绘制扩散效果
     *
     * @param canvas
     */
    public void drawBackground(Canvas canvas) {
        if (mHoleRadius > 0f) {
            /**
             * 绘制空心圆的效果
             * 技巧：使用一个非常宽的画笔
             */
            // 画笔的宽度= 对角线/2 -空心部分的半径
            float storkeWidth = mDiagonalDist - mHoleRadius;
            mPaintBackground.setStrokeWidth(storkeWidth);
            mPaintBackground.setColor(mExpandingBackgroundColor);
            //空心圆半径
            float circleRadius = mHoleRadius + storkeWidth / 2;
            canvas.drawCircle(mCurrentX, mCurrentY, circleRadius, mPaintBackground);
        } else {
            canvas.drawColor(mSplashBgColor);
        }
    }

    public void drawCircles(Canvas canvas) {
        //根据坐标绘制小圆
        float rotationAngle = (float) (2 * Math.PI / mCircleColors.length);
        //  小圆的坐标-->大圆半径 当前旋转了多少角度
        for (int i = 0; i < mCircleColors.length; i++) {
            double angle = mCurrentRotationAngle + i * rotationAngle;
            mPaint.setColor(mCircleColors[i]);
            float cx = (float) (mCurrentX + mCurrentRotationRadius * Math.cos(angle));
            float cy = (float) (mCurrentY + mCurrentRotationRadius * Math.sin(angle));
            canvas.drawCircle(cx, cy, mCircleRadius, mPaint);
        }
    }

    /**
     * 设置扩散背景颜色
     *
     * @param expandingBackgroundColor defaultValue BLUE
     */
    public void setExpandingBackgroundColor(int expandingBackgroundColor) {
        this.mExpandingBackgroundColor = expandingBackgroundColor;
    }

    /**
     * 设置大圆半径
     *
     * @param rotationRadius defaultValue 90
     */
    public void setRotationRadius(float rotationRadius) {
        this.mRotationRadius = rotationRadius;
    }

    /**
     * 设置小圆半径
     *
     * @param circleRadius defaultValue 90
     */
    public void setCircleRadius(float circleRadius) {
        this.mCircleRadius = circleRadius;
    }

    /**
     * 扩散动画执行时间
     *
     * @param expandingTime defaultValue 2000ms
     */
    public void setExpandingTime(int expandingTime) {
        this.mExpandingTime = expandingTime;
    }

    /**
     * 聚合动画执行时间
     *
     * @param mergingTime defaultValue 400ms
     */
    public void setMergingTime(int mergingTime) {
        this.mMergingTime = mergingTime;
    }

    /**
     * @param rotationDuration defaultValue 1200ms
     */
    public void setRotationDuration(int rotationDuration) {
        this.mRotationDuration = rotationDuration;
    }

}
