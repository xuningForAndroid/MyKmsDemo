package com.test.xn.webrtcpeer;

import android.graphics.ImageFormat;

/**
 * Created by Administrator on 2018/10/11.
 * 媒体配置信息类，用于HisunWebRTC的构造方法中
 */

public class HisunMediaConfiguration {
    /**
     * 渲染类型
     */
    public enum  RendererType{
        NATIVE,
        OPENGLES
    }
    /**
     * 音频编码格式
     */
    public enum AudioCodeC{
        OPUS,
        ISAC
    }
    /**
     * 视频编码格式
     */
    public enum VideoCodeC{
        VP8,
        VP9,
        H264
    }

    /**
     * 摄像头位置
     */
    public enum CameraPosition{
        ANY,FRONT,BACK
    }
    /**
     * 视频格式
     */
    public static class VideoFormat{
        public int  videoWidth;//视频宽度
        public  int videoHeight;//视频高度
        public int imageFormat;//图像格式
        public int frameRate;//帧率 每秒帧数

        public VideoFormat(int videoWidth, int videoHeight, int imageFormat, int frameRate) {
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.imageFormat = imageFormat;
            this.frameRate = frameRate;
        }
    }

    public RendererType rendererType;
    public AudioCodeC audioCodeC;
    public int audioBandWidth;
    public VideoCodeC videoCodeC;
    public int videoBandWidth;
    public CameraPosition cameraPosition;
    public VideoFormat receiveVideoFormat;

    public RendererType getRendererType() {
        return rendererType;
    }

    public AudioCodeC getAudioCodeC() {
        return audioCodeC;
    }

    public int getAudioBandWidth() {
        return audioBandWidth;
    }

    public VideoCodeC getVideoCodeC() {
        return videoCodeC;
    }

    public int getVideoBandWidth() {
        return videoBandWidth;
    }

    public CameraPosition getCameraPosition() {
        return cameraPosition;
    }

    public VideoFormat getReceiveVideoFormat() {
        return receiveVideoFormat;
    }

    public HisunMediaConfiguration(){
        rendererType=RendererType.NATIVE;
        audioCodeC=AudioCodeC.OPUS;
        audioBandWidth=0;
        videoCodeC=VideoCodeC.VP8;
        videoBandWidth=0;

        //ImageFormat.NV21是Camera预览图像的默认格式
        receiveVideoFormat= new VideoFormat(640, 480, ImageFormat.NV21, 30);
        cameraPosition=CameraPosition.FRONT;
    }

    public HisunMediaConfiguration(RendererType rendererType, AudioCodeC audioCodeC,
                                   int audioBandWidth, VideoCodeC videoCodeC,
                                   int videoBandWidth, CameraPosition cameraPosition,
                                   VideoFormat receiveVideoFormat) {
        this.rendererType = rendererType;
        this.audioCodeC = audioCodeC;
        this.audioBandWidth = audioBandWidth;
        this.videoCodeC = videoCodeC;
        this.videoBandWidth = videoBandWidth;
        this.cameraPosition = cameraPosition;
        this.receiveVideoFormat = receiveVideoFormat;
    }


}
