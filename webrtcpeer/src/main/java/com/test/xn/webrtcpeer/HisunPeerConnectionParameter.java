package com.test.xn.webrtcpeer;

/**
 * Created by Administrator on 2018/10/10.
 * pc连接参数类
 */

public class HisunPeerConnectionParameter {
    public boolean videoCallEnabled;//是否允许视频
    public boolean loopback;//是否回环
    public int videoWidth;//视频宽度
    public int videoHeight;//视频高度
    public int videoFps;//视频帧率
    public int videoStartBitrate;//视频开始码率
    public String videoCodec;//视频编码格式
    public  boolean videoCodecHwAcceleration;//是否开启硬件加速
    public int audioStartBitrate;//音频开始码率
    public String audioCodec;//音频编码格式
    public boolean noAudioProcessing;//无音频进程
    public boolean cpuOverUseDetection;//cpu负载过重检测

    public HisunPeerConnectionParameter(Builder builder) {
        this.cpuOverUseDetection=builder.cpuOverUseDetection;
        this.videoCodecHwAcceleration=builder.videoCodecHwAcceleration;
        this.videoCodec=builder.videoCodec;
        this.audioCodec=builder.audioCodec;
        this.audioStartBitrate=builder.audioStartBitrate;
        this.videoCallEnabled=builder.videoCallEnabled;
        this.videoCodec=builder.videoCodec;
        this.noAudioProcessing=builder.noAudioProcessing;
        this.videoStartBitrate=builder.videoStartBitrate;
        this.videoFps=builder.videoFps;
        this.videoWidth=builder.videoWidth;
        this.videoHeight=builder.videoHeight;
        this.loopback=builder.loopback;
    }

    public static class Builder{
        public boolean videoCallEnabled;//是否允许视频
        public boolean loopback;//是否回环
        public int videoWidth;//视频宽度
        public int videoHeight;//视频高度
        public int videoFps;//视频帧率
        public int videoStartBitrate;//视频开始码率
        public String videoCodec;//视频编码格式
        public  boolean videoCodecHwAcceleration;//是否开启硬件加速
        public int audioStartBitrate;//音频开始码率
        public String audioCodec;//音频编码格式
        public boolean noAudioProcessing;//无音频进程
        public boolean cpuOverUseDetection;//cpu负载过重检测

        public Builder audioCodec(String audioCodec){
            this.audioCodec=audioCodec;
            return this;
        }
        public Builder noAudioProcessing(boolean noAudioProcessing){
            this.noAudioProcessing=noAudioProcessing;
            return this;
        }
        public Builder cpuOverUseDetection(boolean cpuOverUseDetection){
            this.cpuOverUseDetection=cpuOverUseDetection;
            return this;
        }
        public Builder audioStartBitrate(int audioStartBitrate){
            this.audioStartBitrate=audioStartBitrate;
            return this;
        }
        public Builder videoCodecHwAcceleration(boolean videoCodecHwAcceleration){
            this.videoCodecHwAcceleration=videoCodecHwAcceleration;
            return this;
        }
        public Builder videoCodec(String videoCodec){
            this.videoCodec=videoCodec;
            return this;
        }
        public Builder videoStartBitrate(int videoStartBitrate){
            this.videoStartBitrate=videoStartBitrate;
            return this;
        }

        public Builder videoFps(int videoFps){
            this.videoFps=videoFps;
            return this;
        }

        public Builder videoCallEnabled(boolean videoCallEnabled){
            this.videoCallEnabled=videoCallEnabled;
            return this;
        }
        public Builder loopBack(boolean loopback){
            this.loopback=loopback;
            return this;
        }
        public Builder videoWidth(int videoWidth){
            this.videoWidth=videoWidth;
            return this;
        }
        public Builder videoHeight(int videoHeight){
            this.videoHeight=videoHeight;
            return this;
        }


        public HisunPeerConnectionParameter build(){
            return new HisunPeerConnectionParameter(this);
        }
    }


}
