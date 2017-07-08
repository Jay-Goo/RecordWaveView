package jaygoo.widget.rwv;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.media.MediaRecorder;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;

import java.util.Random;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/7/8
 * 描    述:录音波浪动画
 * ================================================
 */
public class RecordWaveView extends RenderView {

    //采样点的数量，越高越精细，但是高于一定限度肉眼很难分辨，越高绘制效率越低
    private int SAMPLING_SIZE = 64;
    //控制向右偏移速度，越小偏移速度越快
    private float OFFSET_SPEED = 500F;
    //圆球的速度，越小速度越快
    private float CIRCLE_SPEED = 150F;
    //小球默认半径
    private float DEFAULT_CIRCLE_RADIUS;

    private final Paint paint = new Paint();

    {
        //防抖动
        paint.setDither(true);
        //抗锯齿，降低分辨率，提高绘制效率
        paint.setAntiAlias(true);
    }

    //最上方的线
    private final Path firstPath = new Path();
    //中间的振幅很小的线
    private final Path centerPath = new Path();
    //最下方的线
    private final Path secondPath = new Path();
    //采样点X坐标
    private float[] samplingX;
    //采样点位置映射到[-2,2]之间
    private float[] mapX;
    //画布宽高
    private int width,height;
    //画布中心的高度
    private int centerHeight;
    //振幅
    private int amplitude;
    /**
     * 波峰和两条路径交叉点的记录，包括起点和终点，用于绘制渐变。
     * 其数量范围为7~9个，这里size取9。
     * 每个元素都是一个float[2]，用于保存xy值
     */
    private final float[][] crestAndCrossPints = new float[10][];

    {//直接分配内存
        for (int i = 0; i < 9; i++) {
            crestAndCrossPints[i] = new float[2];
        }
    }

    //用于处理矩形的rectF
    private final RectF rectF = new RectF();
    /**
     * 图像回合模式机制，它能够控制绘制图形与之前已经存在的图形的混合交叠模式
     * 这里通过SRC_IN模式将已经绘制好的波形图与渐变矩形取交集，绘制出渐变色
     */
    private final Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    //背景色
    private int backGroundColor;
    //中间线的颜色
    private int centerPathColor;
    //第一条曲线的颜色
    private int firstPathColor;
    //第二条曲线的颜色
    private int secondPathColor;
    //是否显示小球
    private boolean isShowBalls;
    //存储衰减系数
    private SparseArray<Double> recessionFuncs = new SparseArray<>();

    public RecordWaveView(Context context) {
        this(context,null);
    }

    public RecordWaveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordWaveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray t = getContext().obtainStyledAttributes(attrs, R.styleable.RecordWaveView);
        backGroundColor = t.getColor(R.styleable.RecordWaveView_backgroundColor,
                ContextCompat.getColor(getContext(),R.color.backgroundColor));
        firstPathColor = t.getColor(R.styleable.RecordWaveView_firstPathColor,
                ContextCompat.getColor(getContext(),R.color.firstPathColor));
        secondPathColor = t.getColor(R.styleable.RecordWaveView_secondPathColor,
                ContextCompat.getColor(getContext(),R.color.secondPathColor));
        centerPathColor = t.getColor(R.styleable.RecordWaveView_centerPathColor,
                ContextCompat.getColor(getContext(),R.color.centerPathColor));
        isShowBalls = t.getBoolean(R.styleable.RecordWaveView_showBalls, true);
        amplitude = t.getDimensionPixelSize(R.styleable.RecordWaveView_amplitude,0);
        SAMPLING_SIZE = t.getInt(R.styleable.RecordWaveView_ballSpeed,64);
        OFFSET_SPEED = t.getFloat(R.styleable.RecordWaveView_moveSpeed,500F);
        CIRCLE_SPEED = t.getFloat(R.styleable.RecordWaveView_ballSpeed,150F);
        DEFAULT_CIRCLE_RADIUS = dip2px(3);
        t.recycle();
    }

    @Override
    protected void onRender(Canvas canvas, long millisPassed) {
        super.onRender(canvas, millisPassed);
        if (null == samplingX){
            initDraw(canvas);
        }

        refreshAmplitude();

        //绘制背景
        canvas.drawColor(backGroundColor);

        //重置所有path并移动到起点
        firstPath.rewind();
        centerPath.rewind();
        secondPath.rewind();
        firstPath.moveTo(0,centerHeight);
        centerPath.moveTo(0,centerHeight);
        secondPath.moveTo(0,centerHeight);

        //当前时间的偏移量，通过该偏移量使每次绘制向右偏移，从而让曲线动起来
        float offset = millisPassed / OFFSET_SPEED;


        //波形函数的值，包括上一点，当前点，下一点
        float lastV,curV = 0, nextV = (float)(amplitude * calcValue(mapX[0], offset));
        //波形函数的绝对值，用于筛选波峰和交错点
        float absLastV, absCurV, absNextV;
        //上次的筛选点是波峰还是交错点
        boolean lastIsCrest = false;
        //筛选出的波峰和交错点的数量，包括起点和终点
        int crestAndCrossCount = 0;

        float x;
        float[] xy;
        for (int i = 0; i <= SAMPLING_SIZE; i++){
            //计算采样点的位置
            x = samplingX[i];
            lastV = curV;
            curV = nextV;
            //计算下一采样点的位置，并判断是否到终点
            nextV = i < SAMPLING_SIZE ? (float)(amplitude * calcValue(mapX[i + 1], offset)) : 0;

            //连接路径
            firstPath.lineTo(x, centerHeight + curV);
            secondPath.lineTo(x, centerHeight - curV);
            //中间曲线的振幅是上下曲线的1/5
            centerPath.lineTo(x, centerHeight - curV / 5F);

            //记录极值点
            absLastV = Math.abs(lastV);
            absCurV = Math.abs(curV);
            absNextV = Math.abs(nextV);

            if (i == 0 || i == SAMPLING_SIZE/*起点终点*/ || (lastIsCrest && absCurV < absLastV && absCurV < absNextV)/*上一个点为波峰，且该点是极小值点*/) {
                //交叉点
                xy = crestAndCrossPints[crestAndCrossCount++];
                xy[0] = x;
                xy[1] = 0;
                lastIsCrest = false;
            } else if (!lastIsCrest && absCurV > absLastV && absCurV > absNextV) {/*上一点是交叉点，且该点极大值*/
                //极大值点
                xy = crestAndCrossPints[crestAndCrossCount++];
                xy[0] = x;
                xy[1] = curV;
                lastIsCrest = true;
            }
        }

        //连接所有路径到终点
        firstPath.lineTo(width, centerHeight);
        secondPath.lineTo(width, centerHeight);
        centerPath.lineTo(width, centerHeight);

        //记录layer,将图层进行离屏缓存
        int saveCount = canvas.saveLayer(0, 0, width, height, null, Canvas.ALL_SAVE_FLAG);

        //填充上下两条正弦函数，为下一步混合交叠做准备
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1);
        canvas.drawPath(firstPath, paint);
        canvas.drawPath(secondPath, paint);

        paint.setColor(firstPathColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setXfermode(xfermode);


        float startX, crestY, endX;
        //根据上面计算的峰顶和交叉点的位置，绘制矩形
        for (int i = 2; i < crestAndCrossCount; i += 2){
            //每隔两个点绘制一个矩形
            startX = crestAndCrossPints[i - 2][0];
            crestY = crestAndCrossPints[i - 1][1];
            endX = crestAndCrossPints[i][0];

            //设置渐变矩形区域
            paint.setShader(new LinearGradient(0, centerHeight + crestY, 0,
                    centerHeight - crestY, firstPathColor, secondPathColor,
                    Shader.TileMode.CLAMP));
            rectF.set(startX, centerHeight + crestY, endX, centerHeight - crestY);
            canvas.drawRect(rectF, paint);
        }

        //释放画笔资源
        paint.setShader(null);
        paint.setXfermode(null);

        //叠加layer，因为使用了SRC_IN的模式所以只会保留波形渐变重合的地方
        canvas.restoreToCount(saveCount);

        //绘制上弦线
        paint.setStrokeWidth(3);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(firstPathColor);
        canvas.drawPath(firstPath, paint);

        //绘制下弦线
        paint.setColor(secondPathColor);
        canvas.drawPath(secondPath, paint);

        //绘制中间线
        paint.setColor(centerPathColor);
        canvas.drawPath(centerPath, paint);

        if (isShowBalls) {
            float circleOffset = millisPassed / CIRCLE_SPEED;
            drawCircleBalls(circleOffset, canvas);
        }

    }

    //根据分贝设置不同的振幅
    private int getAmplitude(int db) {
        if (db <= 40){
            return width >> 4;
        }else {
            return width >> 3;
        }
    }

    //初始化绘制参数
    private void initDraw(Canvas canvas) {
        width = canvas.getWidth();
        height = canvas.getHeight();
        centerHeight = height >> 1;
        //振幅为宽度的1/8
        //如果未设置振幅高度，则使用默认高度
        if (amplitude == 0) {
            amplitude = width >> 3;
        }

        //初始化采样点及映射

        //这里因为包括起点和终点，所以需要+1
        samplingX = new float[SAMPLING_SIZE + 1];
        mapX = new float[SAMPLING_SIZE + 1];
        //确定采样点之间的间距
        float gap = width / (float)SAMPLING_SIZE;
        //采样点的位置
        float x;
        for (int i = 0; i <= SAMPLING_SIZE; i++){
            x = i * gap;
            samplingX[i] = x;
            //将采样点映射到[-2，2]
            mapX[i] = (x / (float)width) * 4 - 2;
        }
    }


    /**
     * 计算波形函数中x对应的y值
     * 使用稀疏矩阵进行暂存计算好的衰减系数值，下次使用时直接查找，减少计算量
     * @param mapX   换算到[-2,2]之间的x值
     * @param offset 偏移量
     * @return
     */
    private double calcValue(float mapX, float offset) {
        int keyX = (int) (mapX*1000);
        offset %= 2;
        double sinFunc = Math.sin(0.75 * Math.PI * mapX - offset * Math.PI);
        double recessionFunc;
        if(recessionFuncs.indexOfKey(keyX) >=0 ){
            recessionFunc = recessionFuncs.get(keyX);
        }else {
            recessionFunc = Math.pow(4 / (4 + Math.pow(mapX, 4)), 2.5);
            recessionFuncs.put(keyX,recessionFunc);
        }
        return sinFunc * recessionFunc;
    }

    //绘制自由运动的小球
    private void drawCircleBalls(float speed, Canvas canvas){
        float x,y;
        //从左到右依次绘制

        paint.setColor(firstPathColor);
        paint.setStyle(Paint.Style.FILL);
        x = width / 6f + 40 * (float)(Math.sin(0.45 * speed - CIRCLE_SPEED * Math.PI));
        y = centerHeight + 50 * (float) Math.sin(speed);
        canvas.drawCircle(x,y,DEFAULT_CIRCLE_RADIUS,paint);

        paint.setColor(secondPathColor);
        x = 2 * width / 6f + 20 * (float) Math.sin(speed);
        y = centerHeight +(float) Math.sin(speed);
        canvas.drawCircle(x,y,DEFAULT_CIRCLE_RADIUS * 0.8f,paint);

        paint.setColor(secondPathColor);
        paint.setAlpha(60 + new Random().nextInt(40));
        x = 2.5f * width / 6f + 40 * (float)(Math.sin(0.35 * speed + CIRCLE_SPEED * Math.PI));
        y = centerHeight + 40 * (float) Math.sin(speed);
        canvas.drawCircle(x,y,DEFAULT_CIRCLE_RADIUS,paint);

        paint.setColor(firstPathColor);
        x = 3f * width / 6f + (float)(Math.cos(speed));
        y = centerHeight + 40 * (float) Math.sin(0.6f * speed);
        canvas.drawCircle(x,y,DEFAULT_CIRCLE_RADIUS * 0.7f,paint);

        paint.setColor(secondPathColor);
        x = 4 * width / 6f + 70 *(float)(Math.sin(speed));
        y = centerHeight + 10 * (float) Math.sin(speed);
        canvas.drawCircle(x,y,DEFAULT_CIRCLE_RADIUS * 0.5f,paint);

        paint.setColor(firstPathColor);
        x = 5.2f * width / 6f + 30 * (float)(Math.sin(0.21 * speed + CIRCLE_SPEED * Math.PI));
        y = centerHeight + 10 * (float) Math.cos(speed);
        canvas.drawCircle(x,y,DEFAULT_CIRCLE_RADIUS * 0.75f,paint);

        paint.setColor(secondPathColor);
        x = 5.5f * width / 6f + 60 * (float)(Math.sin(0.15 * speed - CIRCLE_SPEED * Math.PI));
        y = centerHeight + 50 * (float) Math.sin(speed);
        canvas.drawCircle(x,y,DEFAULT_CIRCLE_RADIUS * 0.7f,paint);

    }

    private int dip2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    //设置音量分贝
    protected void setVolume(int db){
        amplitude = getAmplitude(db);
    }

    //通过音量分贝更新振幅
    protected void refreshAmplitude(){
    }

}
