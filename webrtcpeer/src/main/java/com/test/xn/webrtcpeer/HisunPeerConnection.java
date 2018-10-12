package com.test.xn.webrtcpeer;

import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

/**
 * Created by Administrator on 2018/10/10.
 *一个由 HisunWebRtcPeer 使用的对等连接包装器，用于支持多个连接。
 */

public class HisunPeerConnection implements PeerConnection.Observer ,SdpObserver{

    private static final String TAG = "HisunPeerConnection";
    private static final String VIDEO_CODEC_PARAM_START_BITRATE = "x-google-start-bitrate";
    private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
    private final boolean preferIsac;
    private final boolean videoCallEnabled;
    private final boolean preferH264;
    private final HisunPeerConnectionParameter peerConnectionParameter;
    private  boolean isInitiator;
    private PeerConnection pc;
    private String connectionId;
    private LooperExecutor executor;
    private SessionDescription localSdp;//可能是offer也可能是answer
    private LinkedList<IceCandidate> queuedRemoteCandidates;//iceCandidate的列表
    private HashMap<String, ObserverDataChannel> observedDataChannels;//存储ObserverDataChannel的集合,键是dataChannelId


    Vector<HisunWebRTCPeerObserver> observers;//WebRtCPeer的监听器集合，vector的原因是保证线程安全
    private MediaConstraints mediaConstraints;

    public HisunPeerConnection(String connectionId,boolean preferIsac,boolean videoCallEnabled,boolean preferH264,
                               LooperExecutor executor,HisunPeerConnectionParameter parameter) {
        this.connectionId = connectionId;
        this.preferIsac=preferIsac;
        this.videoCallEnabled=videoCallEnabled;
        this.preferH264=preferH264;
        this.executor=executor;
        this.peerConnectionParameter=parameter;

        isInitiator=false;
        queuedRemoteCandidates=new LinkedList<>();
        observedDataChannels=new HashMap<>();
    }

    /**
     * 创建数据通道
     * @param label
     * @param init
     * @return
     */
    public DataChannel createDataChannel(String label, DataChannel.Init init){
        ObserverDataChannel dataChannel = new ObserverDataChannel(label, init);
        observedDataChannels.put(label,dataChannel);
        return dataChannel.getChannel();
    }

    /**
     * 获取数据通道集合
     */
    public HashMap<String,DataChannel> getDataChannels(){
        HashMap<String, DataChannel> result = new HashMap<>();
        for (HashMap.Entry<String, ObserverDataChannel> entrySet:observedDataChannels.entrySet()){
            String key = entrySet.getKey();
            ObserverDataChannel value = entrySet.getValue();
            result.put(key,value.getChannel());
        }
        return result;
    }

    /**
     * 获取connectionId 当前连接id
     * @return
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * 根据dataChannelId获取dataChannel，这个dataChannelId就作为键存储在集合observedDataChannels中
     * @param dataChannelId
     * @return
     */
    public DataChannel getDataChannel(String dataChannelId){
        DataChannel channel;
        ObserverDataChannel observerDataChannel = observedDataChannels.get(dataChannelId);
        channel = observerDataChannel.getChannel();
        return channel;
    }

    /**
     * 获取当前的peerConnection
     * @return
     */
    public PeerConnection getPc() {
        return pc;
    }

    /**
     * 设置当前的peerConnection
     * @param pc
     */
    public void setPc(PeerConnection pc) {
        this.pc = pc;
    }

    /**
     *添加webRTCPeer的监听
     * @param observer
     */
    public void addObserver(HisunWebRTCPeerObserver observer){
        observers.add(observer);
    }

    //dataChannel监听器类
    private class ObserverDataChannel implements DataChannel.Observer{
        private DataChannel channel;

        public ObserverDataChannel(String label,DataChannel.Init init){
            channel=pc.createDataChannel(label,init);
            if (channel!=null){
                channel.registerObserver(this);
                Log.i(TAG, "ObserverDataChannel:Created data channel with Id: "+label);
            }else {
                Log.i(TAG, "ObserverDataChannel: Failed to create data channel with Id:"+label);
            }
        }

        public DataChannel getChannel() {
            return channel;
        }

        //缓存数量改变
        @Override
        public void onBufferedAmountChange(long l) {
            Log.i(TAG, "onBufferedAmountChange: ");
            for (HisunWebRTCPeerObserver observer:observers){
                observer.onBufferedAmountChange(l,HisunPeerConnection.this,channel);
            }
        }

        //数据通道状态发生改变
        @Override
        public void onStateChange() {
            Log.i(TAG, "onStateChange: ");
            for (HisunWebRTCPeerObserver observer:observers){
                observer.onStateChange(HisunPeerConnection.this,channel);
            }
        }
        //收到数据通道的消息
        @Override
        public void onMessage(DataChannel.Buffer buffer) {
            Log.i(TAG, "onMessage: ");
            for (HisunWebRTCPeerObserver observer:observers){
                observer.onMessage(HisunPeerConnection.this,channel,buffer);
            }
        }
    }

    //信号状态发生改变
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange: "+signalingState.name());
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onSignalingChange(HisunPeerConnection.this,signalingState);
        }
    }

    //ice连接状态改变
    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.i(TAG, "onIceConnectionChange: "+iceConnectionState.name());
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onIceConnectionChange(HisunPeerConnection.this,iceConnectionState);
        }
    }

    //ice连接接收改变
    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.i(TAG, "onIceConnectionReceivingChange: "+b);
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onIceConnectionReceivingChange(HisunPeerConnection.this,b);
        }
    }

    //聚集状态改变
    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.i(TAG, "onIceGatheringChange: "+iceGatheringState.name());
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onIceGatheringChange(HisunPeerConnection.this,iceGatheringState);
        }
    }

    //iceCandidate可用
    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.i(TAG, "onIceCandidate: "+iceCandidate.sdp);
        executor.execute(()->{
            for (HisunWebRTCPeerObserver observer:observers){
                observer.onIceCandidate(HisunPeerConnection.this,iceCandidate);
            }
        });
    }

    //添加远端媒体流
    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.i(TAG, "onAddStream: "+mediaStream.label());
        executor.execute(()->{
            if (pc==null)
                return;
            if (mediaStream.audioTracks.size()>1 || mediaStream.videoTracks.size()>1){
                for (HisunWebRTCPeerObserver observer:observers){
                    observer.onPeerConnectionError("on addStream error"+mediaStream);
                }
                return;
            }
            for (HisunWebRTCPeerObserver observer:observers){
                observer.onRemoteStreamAdded(HisunPeerConnection.this,mediaStream);
            }
        });
    }

    //移除远端媒体流
    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.i(TAG, "onRemoveStream: "+mediaStream.label());
        executor.execute(()->{
            if (pc==null)
                return;
            if (mediaStream.videoTracks.size()>1 || mediaStream.audioTracks.size()>1){
                for (HisunWebRTCPeerObserver observer:observers){
                    observer.onPeerConnectionError("on RmoveStream error"+mediaStream);
                }
                return;
            }
            for (HisunWebRTCPeerObserver observer:observers){
                observer.onRemoteStreamRemoved(HisunPeerConnection.this,mediaStream);
            }
        });
    }

    //开启数据通道
    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.i(TAG, "onDataChannel: peer open dataChannel");//一端开放了数据通道
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onDataChannel(HisunPeerConnection.this,dataChannel);
        }
    }

    //需要重新协商
    @Override
    public void onRenegotiationNeeded() {
        Log.i(TAG, "onRenegotiationNeeded: ");
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onRenegotiationNeeded();
        }
    }


    //sdp改变监听器回调
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        Log.i(TAG, "onCreateSuccess: ");
        if (localSdp==null)
            return;
        String sdpDescription = sessionDescription.description;
        //编码特殊格式设置
        if (preferIsac){
            sdpDescription=preferCodec(sdpDescription,HisunMediaConfiguration.AudioCodeC.ISAC.toString(), true);
        }
        if (videoCallEnabled && preferH264){
            sdpDescription=preferCodec(sdpDescription, HisunMediaConfiguration.VideoCodeC.H264.toString(),false);
        }
        SessionDescription sdp = new SessionDescription(sessionDescription.type, sdpDescription);
        localSdp=sdp;
        executor.execute(()->{
            if (pc==null)
                return;
            Log.i(TAG, "onCreateSuccess: set localDescription from"+sdp.type);
            pc.setLocalDescription(HisunPeerConnection.this,sdp);
        });
    }

    @Override
    public void onSetSuccess() {
        executor.execute(()->{
            if (pc==null)
                return;
            if (isInitiator){//是发起人

                if (pc.getRemoteDescription()==null){
                    //对于视频发起人来说，首先创建offer，然后设置local Sdp,然后接到Answer，再设置remote Sdp
                    Log.i(TAG, "onSetSuccess: local sdp设置成功");
                    for (HisunWebRTCPeerObserver observer:observers){
                        observer.onLocalSdpOfferGenerated(localSdp,HisunPeerConnection.this);
                    }
                }else {
                    //我们已经设置了远端description,将链表中的iceCandidate添加到当前peerConnection,并且将链表置空
                    Log.i(TAG, "onSetSuccess: remote sdp设置成功");
                    drainCandidate();
                }
            }else {//不是视频发起人
                if (pc.getLocalDescription()!=null){
                    Log.i(TAG, "onSetSuccess: localSdp已经成功设置");
                    for (HisunWebRTCPeerObserver observer:observers){
                        observer.onLocalSdpAnswerGenerated(localSdp,HisunPeerConnection.this);
                    }
                    drainCandidate();
                }else {
                    //什么都不需要做
                    Log.i(TAG, "onSetSuccess: remote sdp 设置成功");
                }
            }
        });
    }
    //创建失败
    @Override
    public void onCreateFailure(String s) {
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onPeerConnectionError(s);
        }
    }

    //设置失败
    @Override
    public void onSetFailure(String s) {
        for (HisunWebRTCPeerObserver observer:observers){
            observer.onPeerConnectionError(s);
        }
    }

    /**
     * 编码格式特殊设置
     * @param sdpDescription
     * @param codec
     * @param isAudio
     * @return
     */
    private String preferCodec(String sdpDescription, String codec, boolean isAudio) {
        String[] lines = sdpDescription.split("\r\n");//按照行进行切割
        int mLineIndex=-1;
        String codecRtpMap=null;
        // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codeCpattern = Pattern.compile(regex);
        String mediaDescription="m=video ";
        if (isAudio){
            mediaDescription="m=audio ";
        }
        for (int i = 0; i < (lines.length) && (mLineIndex==-1 || codecRtpMap==null); i++) {
            if (lines[i].startsWith(mediaDescription)){
                mLineIndex=i;
                continue;
            }
            Matcher codeCMatcher = codeCpattern.matcher(lines[i]);
            if (codeCMatcher.matches()){
                codecRtpMap=codeCMatcher.group(1);//得到第一组匹配
                continue;
            }
        }
        if (mLineIndex==-1){
            Log.i(TAG, "preferCodec: no "+mediaDescription+" line ,so can't prefer "+codec);
            return sdpDescription;
        }
        if (codecRtpMap==null){
            Log.i(TAG, "preferCodec: no rtpMap for "+codec);
            return sdpDescription;
        }
        Log.i(TAG, "preferCodec: found "+ codec+"rtpMap "+codecRtpMap+" ,prefer at "+lines[mLineIndex]);
        String[] origMlineParts = lines[mLineIndex].split(" ");
        if (origMlineParts.length>3){
            StringBuilder newMLine = new StringBuilder();
            int origPartIndex=0;
            newMLine.append(origMlineParts[origPartIndex++]).append(" ");
            newMLine.append(origMlineParts[origPartIndex++]).append(" ");
            newMLine.append(origMlineParts[origPartIndex++]).append(" ");
            newMLine.append(codecRtpMap);

            for (; origPartIndex < origMlineParts.length; origPartIndex++) {
                if (!origMlineParts[origPartIndex].equals(codecRtpMap)) {
                    newMLine.append(" ").append(origMlineParts[origPartIndex]);
                }
            }
            lines[mLineIndex]=newMLine.toString();
            Log.i(TAG, "preferCodec: Change media description: "+lines[mLineIndex]);
        }else {
            Log.i(TAG, "preferCodec: Wrong SDP media description format: "+lines[mLineIndex]);
        }
        StringBuilder newSdpDescription = new StringBuilder();
        for (String line:lines){
            newSdpDescription.append(line).append("\r\n");
        }
        return newSdpDescription.toString();
    }
    private void drainCandidate() {
        if (queuedRemoteCandidates!=null){
            Log.i(TAG, "drainCandidate: add "+queuedRemoteCandidates.size()+" remote candidate");
            for (IceCandidate iceCandidate:queuedRemoteCandidates){
                pc.addIceCandidate(iceCandidate);
            }
            queuedRemoteCandidates=null;
        }
    }

    /**
     * 创建offer
     * @param mediaConstraints
     */
    public void createOffer(MediaConstraints mediaConstraints){
        this.mediaConstraints =mediaConstraints;
        if (pc==null)
            return;
        Log.i(TAG, "createOffer: pc 创建offer");
        isInitiator=true;
        pc.createOffer(this,mediaConstraints);
    }

    /**
     * 创建Answer
     * @param mediaConstraints
     */
    public void createAnswer(MediaConstraints mediaConstraints){
        executor.execute(()->{
            if (pc==null)
                return;
            isInitiator=false;
            pc.createAnswer(HisunPeerConnection.this,mediaConstraints);
        });
    }

    /**
     * 同步设置远端sdp
     * @param sdp
     */
    protected void  setRemoteDescriptionSync(SessionDescription sdp){
        if (pc==null)
            return;
        String sdpDescription = sdp.description;
        if (preferIsac){
            sdpDescription=preferCodec(sdpDescription, HisunMediaConfiguration.AudioCodeC.ISAC.toString(),true);
        }
        if (videoCallEnabled && preferH264){
            sdpDescription=preferCodec(sdpDescription, HisunMediaConfiguration.VideoCodeC.H264.toString(),false);
        }
        if (videoCallEnabled && peerConnectionParameter.videoStartBitrate>0){
            sdpDescription=setStartBitrate(HisunMediaConfiguration.VideoCodeC.VP8.toString(),true,sdpDescription,peerConnectionParameter.videoStartBitrate);
            sdpDescription=setStartBitrate(HisunMediaConfiguration.VideoCodeC.VP9.toString(),true,sdpDescription,peerConnectionParameter.videoStartBitrate);
            sdpDescription=setStartBitrate(HisunMediaConfiguration.VideoCodeC.H264.toString(),true,sdpDescription,peerConnectionParameter.videoStartBitrate);
        }
        if (peerConnectionParameter.audioStartBitrate>0){
            sdpDescription=setStartBitrate(HisunMediaConfiguration.AudioCodeC.OPUS.toString(),false,sdpDescription,peerConnectionParameter.audioStartBitrate);
        }
        Log.i(TAG, "setRemoteDescriptionSync: set remote sdp");

        SessionDescription sdpRemote = new SessionDescription(sdp.type, sdpDescription);
        pc.setRemoteDescription(HisunPeerConnection.this,sdpRemote);
    }

    /**
     * 设置remoteDescription
     * @param remoteDescription
     */
    public void setRemoteDescription(SessionDescription remoteDescription){
        executor.execute(()->{
            setRemoteDescriptionSync(remoteDescription);
        });
    }

    /**
     * 设置remote IceCandidate
     * @param remoteIceCandidate
     */
    public void addRemoteIceCandidate(IceCandidate remoteIceCandidate){
        executor.execute(()->{
            if (pc==null)
                return;
            if (queuedRemoteCandidates!=null){//列表不为空
                queuedRemoteCandidates.add(remoteIceCandidate);//加入列表中
            }else {
                pc.addIceCandidate(remoteIceCandidate);
            }
        });
    }

    /**
     * 关闭PeerConnection
     */
    public void closePeerConnection(){
        if (pc==null)
            return;
        Log.i(TAG, "closePeerConnection: 开始关闭peerConnection");
        pc.dispose();
        pc=null;
        Log.i(TAG, "closePeerConnection: 关闭peerConnection完成");
    }










    /**
     * 设置开始码率
     * @param codec
     * @param isVideoCodec
     * @param sdpDescription
     * @param videoStartBitrate
     * @return
     */
    private String setStartBitrate(String codec, boolean isVideoCodec, String sdpDescription, int videoStartBitrate) {
        String[] lines = sdpDescription.split("\r\n");
        int rtpmapLineIndex = -1;
        boolean sdpFormatUpdated = false;
        String codecRtpMap = null;
        String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
        Pattern codecPattern = Pattern.compile(regex);

        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                codecRtpMap = codecMatcher.group(1);
                rtpmapLineIndex = i;
                break;
            }
        }
        if (codecRtpMap == null) {
            Log.w(TAG, "No rtpmap for " + codec + " codec");
            return sdpDescription;
        }
        Log.d(TAG, "Found " + codec + " rtpmap " + codecRtpMap + " at " + lines[rtpmapLineIndex]);
        regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
        codecPattern = Pattern.compile(regex);
        for (int i = 0; i < lines.length; i++) {
            Matcher codecMatcher = codecPattern.matcher(lines[i]);
            if (codecMatcher.matches()) {
                Log.d(TAG, "Found " + codec + " " + lines[i]);
                if (isVideoCodec) {
                    lines[i] += "; " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + videoStartBitrate;
                } else {
                    lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "=" + (videoStartBitrate * 1000);
                }
                Log.d(TAG, "Update remote SDP line: " + lines[i]);
                sdpFormatUpdated = true;
                break;
            }
        }
        StringBuilder newSdpDescription = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            newSdpDescription.append(lines[i]).append("\r\n");
            // Append new a=fmtp line if no such line exist for a codec.
            if (!sdpFormatUpdated && i == rtpmapLineIndex) {
                String bitrateSet;
                if (isVideoCodec) {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " " + VIDEO_CODEC_PARAM_START_BITRATE + "=" + videoStartBitrate;
                } else {
                    bitrateSet = "a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "=" + (videoStartBitrate * 1000);
                }
                Log.d(TAG, "Add remote SDP line: " + bitrateSet);
                newSdpDescription.append(bitrateSet).append("\r\n");
            }
        }
        return newSdpDescription.toString();
    }


}
