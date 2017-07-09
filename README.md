# RecordWaveView
### 一款漂亮的波浪录音动画，附带封装好的MP3录音控件
#### 喜欢请Star，谢谢！

----------

# 效果图（实际效果更好）

![image](https://github.com/Jay-Goo/RecordWaveView/blob/master/gif/2017-07-09%2023_10_44.gif)

----------
# Usage
## 项目依赖
```
 allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
	
	dependencies {
	        compile 'com.github.Jay-Goo:RecordWaveView:v1.0.0'
	}
```


----------


## RecordWaveView 
#### 波浪动画
```
//开始动画
mRecordWaveView.startAnim();
//结束动画
mRecordWaveView.stopAnim();

@Override
protected void onResume() {
	super.onResume();
    mRecordWaveView.onResume();
}

@Override
protected void onPause() {
    super.onPause();
    mRecordWaveView.onPause();
}
```


----------


## Mp3WaveRecorder 
#### 封装波浪动画的MP3Recorder
```
//开始动画
mMp3WaveRecorder.start();

//结束动画
mMp3WaveRecorder.stop();

//清除录音缓存，你可以在onDestroy()中做
mMp3WaveRecorder.clearCache();

//录音状态监听
mMp3WaveRecorder.setOnRecordListener(new Mp3WaveRecorder.OnRecordListener() {
            @Override
            public void onStart() {
            //开始录音
            }

            @Override
            public void onStop(File file) {
            //结束录音，返回录音文件
            }
        });
@Override
protected void onResume() {
	super.onResume();
    mMp3WaveRecorder.onResume();
}

@Override
protected void onPause() {
    super.onPause();
    mMp3WaveRecorder.onPause();
}
```
----------


##  Attributes

attr | format | description
-------- | ---|---
backgroundColor|color|View背景颜色
firstPathColor|color|上曲线颜色
secondPathColor|color|下曲线颜色
centerPathColor|color|中间曲线颜色
moveSpeed|float|曲线移动速度，数值越小，速度越快，默认500F，向右移，负数则为向左移
ballSpeed|float|小球移动速度，数值越小，速度越快，默认150F
simplingSize|integer|采样点数量，越大越精细，默认64
amplitude|dimension|振幅，默认为宽度的1/8
showBalls|boolean|是否显示小球，默认显示


----------

# 更多用法
### 1、默认启动动画

```
mRecordWaveView.onResume(boolean isAutoStartAnim);
```

### 2、Mp3WaveRecorder默认封装了点击事件，如果你不想使用，可以`setEnable(false)`屏蔽该事件即可。

### 3、关于MP3Recorder，请传送至[AndroidMP3Recorder](https://github.com/Jay-Goo/AndroidMP3Recorder)了解更多

# 致谢
[Bugly—以Tencent OS录音机波形动画为实例](https://mp.weixin.qq.com/s?__biz=MzA3NTYzODYzMg==&mid=2653577211&idx=1&sn=2619c7df79f675e45e87891b7eb17669&scene=4#wechat_redirect)

[DrkCore—以Tencent OS录音机波形为例](http://blog.csdn.net/drkcore/article/details/51822818)
