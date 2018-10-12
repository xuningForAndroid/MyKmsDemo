package com.test.xn.webrtcpeer;

import android.content.Context;
import android.util.Log;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.util.LinkedList;

import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

/**
 * Created by Administrator on 2018/10/10.
 * 在Android端是实现webRTC peer的主 类
 */

public class HisunWebRTCPeer {
    private static final String TAG = "HisunWebRTCPeer";
    private static final String FIELD_TRIAL_VP9 = "WebRTC-SupportVP9/Enabled/";
    private static final String FIELD_TRIAL_AUTOMATIC_RESIZE = "WebRTC-MediaCodecVideoEncoder-AutomaticResize/Enabled/";
    private  LooperExecutor executor;
    private Context context;
    public HisunMediaConfiguration configuration;
    public VideoRenderer.Callbacks localRenderer;
    public VideoRenderer.Callbacks masterRenderer;
    public MediaStream activeMediaStream;
    public HisunWebRTCPeerObserver observer;
    private final HisunPeerConnectionParameter connectionParameter;
    private final LinkedList<PeerConnection.IceServer> iceServers;
    public boolean initiatized;
    public PCResourceManager pcResourceManager;
    public PeerConnectionFactory peerConnectionFactory;
    private MediaResourceManager mediaResourceManager;
    private SignalingParameters signalingParameters;

    public HisunWebRTCPeer(Context context, HisunMediaConfiguration configuration,
                           VideoRenderer.Callbacks localRenderer,
                           HisunWebRTCPeerObserver observer) {
        this.context = context;
        this.configuration = configuration;
        this.localRenderer = localRenderer;
        this.observer = observer;
        executor=new LooperExecutor();
        //looper线程在私有cctr中被启动一次，并用于所有peerConnection ApI的调用，
        // 以确保新的对等连接 peerConnectionFactory在之前销毁的peerConnectionFactory相同的线程上创建的
        executor.requestStart();

        connectionParameter = new HisunPeerConnectionParameter.Builder()
                .videoCallEnabled(true)
                .loopBack(false)
                .videoWidth(configuration.getReceiveVideoFormat().videoWidth)
                .videoHeight(configuration.getReceiveVideoFormat().videoHeight)
                .videoFps(configuration.getReceiveVideoFormat().frameRate)
                .videoStartBitrate(configuration.getVideoBandWidth())
                .videoCodec(configuration.getVideoCodeC().toString())
                .videoCodecHwAcceleration(true)
                .audioStartBitrate(configuration.getAudioBandWidth())
                .audioCodec(configuration.getAudioCodeC().toString())
                .noAudioProcessing(false)
                .cpuOverUseDetection(true)
                .build();
        iceServers = new LinkedList<>();
        addIceServer("stun:stun.l.google.com:19302");
    }

    /**
     * 添加ice服务
     * @param serverURI
     */
    private void addIceServer(String serverURI) {
        if (!initiatized){
            iceServers.add(new PeerConnection.IceServer(serverURI));
        }else {
            throw new RuntimeException("webRTC peer没有被初始化，所以不能添加ice服务");
        }
    }

    /**
     * 初始化WebRTCPeer
     */
    public void initialize(){
        executor.execute(()->{
            signalingParameters = new SignalingParameters(iceServers, true, "", null, null);
            createPeerConnectionFactoryInternal(context);
            pcResourceManager = new PCResourceManager(connectionParameter, executor, peerConnectionFactory);
            mediaResourceManager = new MediaResourceManager(executor,peerConnectionFactory,connectionParameter);
            initiatized=true;
            observer.onInitialize();
        });
    }

    /**
     * 处理接收到的offer
     * @param remoteOffer
     * @param connectionId
     */
    public void processOffer(SessionDescription remoteOffer,String connectionId){
        executor.execute(new ProcessOfferTask(remoteOffer,connectionId));



    }

    /**
     *处理Answer
     * @param remoteAnswer
     * @param connectionId
     */
    public void processAnswer(SessionDescription remoteAnswer,String connectionId){
        HisunPeerConnection hisunPeerConnection = pcResourceManager.getConnection(connectionId);

        if (hisunPeerConnection!=null){
            hisunPeerConnection.setRemoteDescription(remoteAnswer);
        }else {
            observer.onPeerConnectionError("connection for "+connectionId+" can't be found");
        }

    }

    /**
     * 添加远端IceCandidate
     * @param remoteIceCandidate
     * @param connectionId
     */
    public void addRemoteIceCandidate(IceCandidate remoteIceCandidate, String connectionId){
        HisunPeerConnection connection = pcResourceManager.getConnection(connectionId);

        if (connection != null) {
            connection.addRemoteIceCandidate(remoteIceCandidate);
        } else {
            observer.onPeerConnectionError("Connection for id " + connectionId + " cannot be found!");
        }
    }

    public void closeConnection(String connectionId){
        if (pcResourceManager.getConnection(connectionId)==null) {
            return;
        }
        pcResourceManager.getConnection(connectionId).getPc().removeStream(mediaResourceManager.getLocalMediaStream());
        pcResourceManager.closeConnection(connectionId);
    }
    
    
    public DataChannel getDataChannel(String connectionId, String dataChannelId){
        return pcResourceManager.getConnection(connectionId).getDataChannel(dataChannelId);
    }
    public DataChannel createDataChannel(String connectionId, String dataChannelId, DataChannel.Init init){
        HisunPeerConnection connection = pcResourceManager.getConnection(connectionId);
        if (connection!=null){
            return connection.createDataChannel(dataChannelId,init);
        }else {
            Log.i(TAG, "createDataChannel: 不能根据connectionId找到对应的connection");
            return null;
        }
    }
    
    public void close(){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                for(HisunPeerConnection c : pcResourceManager.getConnections()){
                    c.getPc().removeStream(mediaResourceManager.getLocalMediaStream());
                }
                pcResourceManager.closeAllConnections();
                mediaResourceManager.close();
                peerConnectionFactory.dispose();
                pcResourceManager = null;
                mediaResourceManager = null;
                peerConnectionFactory = null;
            }
        });
    }
    
    public boolean startLocalMedia(){
        if (mediaResourceManager!=null && mediaResourceManager.getLocalMediaStream()==null){
            executor.execute(()->{
                startLocalMediaSync();
            });
            return true;
        }else {
            return false;
        }
    }

    private boolean startLocalMediaSync() {
        if (mediaResourceManager != null && mediaResourceManager.getLocalMediaStream() == null) {
            mediaResourceManager.createLocalMediaStream(VideoRendererGui.getEglBaseContext(), localRenderer);
            mediaResourceManager.startVideoSource();
            mediaResourceManager.selectCameraPosition(configuration.getCameraPosition());
            return true;
        } else {
            return false;
        }
    }

    public void stopLocalMedia(){
        mediaResourceManager.stopVideoSource();
    }
    public void attachRenderToRemoteStream(VideoRenderer.Callbacks remoteRender, MediaStream remoteStream){
        mediaResourceManager.attachRenderToRemoteStream(remoteRender,remoteStream);
    }
    public void selectCameraPosition(HisunMediaConfiguration.CameraPosition position){
        mediaResourceManager.selectCameraPosition(position);
    }
    public void switchCameraPosition(){
        mediaResourceManager.switchCamera();
    }
    public boolean hasCameraPosition(HisunMediaConfiguration.CameraPosition position){
        return mediaResourceManager.hasCameraPosition(position);
    }
    
    
    public boolean isVideoEnabled(){
        return mediaResourceManager.getVideoEnabled();
    }
    public void enableVideo(boolean enabled){
        mediaResourceManager.setVideoEnabled(enabled);
    }


    private void createPeerConnectionFactoryInternal(Context context) {
        Log.i(TAG, "createPeerConnectionFactoryInternal: ");
        String field_trials = FIELD_TRIAL_AUTOMATIC_RESIZE;
        if (connectionParameter.videoCallEnabled && connectionParameter.videoCodec != null &&
                connectionParameter.videoCodec.equals(HisunMediaConfiguration.VideoCodeC.VP9.toString())) {
            field_trials += FIELD_TRIAL_VP9;
        }
        PeerConnectionFactory.initializeFieldTrials(field_trials);
        boolean b = PeerConnectionFactory.initializeAndroidGlobals(context, true, true, connectionParameter.videoCodecHwAcceleration);
        if (!b){
            observer.onPeerConnectionError("Failed to initializeAndroidGlobals");
        }
        peerConnectionFactory = new PeerConnectionFactory();
        Log.i(TAG, "createPeerConnectionFactoryInternal: success");
    }

    //offer处理任务执行器
    public class ProcessOfferTask implements Runnable{
        SessionDescription remoteOffer;
        String connectionId;

        public ProcessOfferTask(SessionDescription remoteOffer, String connectionId) {
            this.remoteOffer = remoteOffer;
            this.connectionId = connectionId;
        }

        @Override
        public void run() {
            HisunPeerConnection connection = pcResourceManager.getConnection(connectionId);
            if (connection==null){
               if (signalingParameters!=null){
                   HisunPeerConnection peerConnection = pcResourceManager.createPeerConnection(signalingParameters, mediaResourceManager.getPcConstraints(), connectionId);
                   peerConnection.addObserver(HisunWebRTCPeer.this.observer);
                   peerConnection.setRemoteDescription(remoteOffer);

                   peerConnection.createAnswer(mediaResourceManager.getSdpMediaConstraints());
               }
            }
        }
    }
}
