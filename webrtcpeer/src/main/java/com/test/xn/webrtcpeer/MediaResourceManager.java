package com.test.xn.webrtcpeer;

import android.util.Log;

import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.EnumSet;
import java.util.HashMap;

import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

/**
 * Created by Administrator on 2018/10/11.
 * 这个类是mediaResource的管理类
 */

public class MediaResourceManager implements HisunWebRTCPeerObserver{

    private static final String TAG = "MediaResourceManager";
    private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
    public static final int HD_VIDEO_WIDTH=1280;
    public static final int HD_VIDEO_HEIGHT=720;
    private static final int MAX_VIDEO_WIDTH = 1280;
    private static final int MAX_VIDEO_HEIGHT = 1280;
    private static final int MAX_VIDEO_FPS = 30;
    private static final int numberOfCameras = CameraEnumerationAndroid.getDeviceCount();
    private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
    private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
    private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
    private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
    private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
    private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";

    private LooperExecutor executor;
    private PeerConnectionFactory factory;
    private MediaConstraints pcConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints audioConstraints;
    private MediaConstraints sdpMediaConstraints;
    private boolean videoCallEnabled;
    private boolean renderVideo;
    private boolean videoSourceStopped;
    private MediaStream localMediaStream;
    private VideoSource videoSource;
    private VideoTrack localVideoTrack;
    private HashMap<MediaStream,VideoTrack> remoteVideoTracks;
    private HashMap<VideoRenderer.Callbacks,VideoRenderer> remoteVideoRenderers;
    private HashMap<VideoRenderer,MediaStream> remoteVideoMediaStreams;

    private VideoRenderer.Callbacks localRender;
    private HisunPeerConnectionParameter peerConnectionParameters;
    private VideoCapturerAndroid videoCapturer;
    private HisunMediaConfiguration.CameraPosition currentCameraPosition;

    public MediaResourceManager(LooperExecutor executor, PeerConnectionFactory factory,
                                HisunPeerConnectionParameter peerConnectionParameters) {
        this.executor = executor;
        this.factory = factory;
        this.peerConnectionParameters = peerConnectionParameters;

        renderVideo=true;
        remoteVideoTracks=new HashMap<>();
        remoteVideoMediaStreams=new HashMap<>();
        remoteVideoRenderers=new HashMap<>();
        videoCallEnabled=peerConnectionParameters.videoCallEnabled;
    }

    void createMediaConstraint(){
        pcConstraints=new MediaConstraints();
        if (peerConnectionParameters.loopback) {
            pcConstraints.optional.add(new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "false"));
        } else {
            pcConstraints.optional.add(new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
        }
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"));
        // Check if there is a camera on device and disable video call if not.
        if (numberOfCameras == 0) {
            Log.w(TAG, "No camera on device. Switch to audio only call.");
            videoCallEnabled = false;
        }

        // Create video constraints if video call is enabled.
        if (videoCallEnabled) {
            videoConstraints = new MediaConstraints();
            int videoWidth = peerConnectionParameters.videoWidth;
            int videoHeight = peerConnectionParameters.videoHeight;
            // If VP8 HW video encoder is supported and video resolution is not
            // specified force it to HD.
            if ((videoWidth == 0 || videoHeight == 0) && peerConnectionParameters.videoCodecHwAcceleration && MediaCodecVideoEncoder.isVp8HwSupported()) {
                videoWidth = HD_VIDEO_WIDTH;
                videoHeight = HD_VIDEO_HEIGHT;
            }
            // Add video resolution constraints.
            if (videoWidth > 0 && videoHeight > 0) {
                videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
                videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MIN_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MAX_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MIN_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MAX_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
            }
            // Add fps constraints.
            int videoFps = peerConnectionParameters.videoFps;
            if (videoFps > 0) {
                videoFps = Math.min(videoFps, MAX_VIDEO_FPS);
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
                videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair(MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
            }
        }
        // Create audio constraints.
        audioConstraints = new MediaConstraints();
        // added for audio performance measurements
        if (peerConnectionParameters.noAudioProcessing) {
            Log.d(TAG, "Disabling audio processing");
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "false"));
        }
        // Create SDP constraints.
        sdpMediaConstraints = new MediaConstraints();
        sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        if (videoCallEnabled || peerConnectionParameters.loopback) {
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        } else {
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        }

        sdpMediaConstraints.optional.add(new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
        sdpMediaConstraints.optional.add(new MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"));


    }

    MediaConstraints getPcConstraints(){
        return pcConstraints;
    }

    MediaConstraints getSdpMediaConstraints(){
        return sdpMediaConstraints;
    }

    MediaStream getLocalMediaStream() {
        return localMediaStream;
    }

    //停止收集本地视频
    void stopVideoSource(){
        executor.execute(()->{
            if (videoSource!=null && !videoSourceStopped){
                Log.i(TAG, "stopVideoSource: ");
                videoSource.stop();
                videoSourceStopped=true;
            }
        });
    }

    /**
     * 开始收集视频
     */
    void startVideoSource(){
        executor.execute(()->{
            if (videoSourceStopped && videoSource!=null){
                videoSource.restart();
                videoSourceStopped=false;
            }
        });
    }

    /**
     * 创建视频轨道
     * @param capturerAndroid
     */
    public VideoTrack createCaptureVideoTrack(VideoCapturerAndroid capturerAndroid){
        videoSource=factory.createVideoSource(capturerAndroid,videoConstraints);
        localVideoTrack=factory.createVideoTrack(VIDEO_TRACK_ID,videoSource);
        localVideoTrack.setEnabled(true);
        localVideoTrack.addRenderer(new VideoRenderer(localRender));
        return localVideoTrack;
    }

    public class AttachRenderTask implements Runnable{

        private VideoRenderer.Callbacks remoteRender;
        private MediaStream remoteStream;

        public AttachRenderTask(VideoRenderer.Callbacks remoteRender, MediaStream remoteStream) {
            this.remoteRender = remoteRender;
            this.remoteStream = remoteStream;
        }

        @Override
        public void run() {
            Log.i(TAG, "Attaching VideoRenderer to remote stream (" + remoteStream + ")");
            if (remoteStream.videoTracks.size()==1){
                VideoTrack remoteVideoTrack = remoteStream.videoTracks.get(0);
                remoteVideoTrack.setEnabled(true);

                VideoRenderer videoRenderer = remoteVideoRenderers.get(remoteRender);
                if (videoRenderer!=null){
                    MediaStream mediaStream = remoteVideoMediaStreams.get(videoRenderer);
                    if (mediaStream!=null){
                        VideoTrack videoTrack = remoteVideoTracks.get(mediaStream);
                        if (videoTrack!=null){
                            videoTrack.removeRenderer(videoRenderer);
                        }
                    }
                }
                VideoRenderer newVideoRenderer = new VideoRenderer(remoteRender);
                remoteVideoTrack.addRenderer(newVideoRenderer);
                remoteVideoRenderers.put(remoteRender,newVideoRenderer);
                remoteVideoMediaStreams.put(newVideoRenderer,remoteStream);
                remoteVideoTracks.put(remoteStream,remoteVideoTrack);
                Log.i(TAG, " attachedRender ");
            }
        }
    }

    void attachRenderToRemoteStream(VideoRenderer.Callbacks remoteRender,MediaStream mediaStream){
        executor.execute(new AttachRenderTask(remoteRender,mediaStream));
    }

    public void createLocalMediaStream(Object eglContext,VideoRenderer.Callbacks localRender){
        if (factory==null){
            Log.i(TAG, "createMediaStream: peerConnectionFactory还没有创建");
            return;
        }
        this.localRender=localRender;
        factory.setVideoHwAccelerationOptions(eglContext,eglContext);
        Logging.enableTracing("logcat:", EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT), Logging.Severity.LS_INFO);

       localMediaStream= factory.createLocalMediaStream("ARDAMS");

       if (videoCallEnabled && numberOfCameras>0){
           String cameraDeviceName; // = CameraEnumerationAndroid.getDeviceName(0);
           String frontCameraDeviceName = CameraEnumerationAndroid.getNameOfFrontFacingDevice();
           String backCameraDeviceName = CameraEnumerationAndroid.getNameOfBackFacingDevice();
           // If current camera is set to front and the device has one
           if (currentCameraPosition== HisunMediaConfiguration.CameraPosition.FRONT && frontCameraDeviceName!=null) {
               cameraDeviceName = frontCameraDeviceName;
           }
           // If current camera is set to back and the device has one
           else if (currentCameraPosition==HisunMediaConfiguration.CameraPosition.BACK && backCameraDeviceName!=null) {
               cameraDeviceName = backCameraDeviceName;
           } else {
               cameraDeviceName = CameraEnumerationAndroid.getDeviceName(0);
               currentCameraPosition = HisunMediaConfiguration.CameraPosition.BACK;
           }
           Log.i(TAG, "createMediaStream: 正在打开摄像头"+cameraDeviceName);
           videoCapturer=VideoCapturerAndroid.create(cameraDeviceName,null);
           if (videoCapturer==null) {
               Log.i(TAG, "createMediaStream: 打开摄像头失败");
               return;
           }
           localMediaStream.addTrack(createCaptureVideoTrack(videoCapturer));
       }
       localMediaStream.addTrack(factory.createAudioTrack(AUDIO_TRACK_ID,factory.createAudioSource(audioConstraints)));
        Log.i(TAG, "createMediaStream: local media Stream 被创建成功");
    }

    /**
     * 选择摄像头方向
     * @param cameraPosition
     */
    public void selectCameraPosition(HisunMediaConfiguration.CameraPosition cameraPosition){
        if (!videoCallEnabled || videoCapturer==null || !hasCameraPosition(cameraPosition)){
            Log.i(TAG, "selectCameraPosition: failed");
            return;
        }
        if (cameraPosition!=currentCameraPosition){
            executor.execute(()->{
                videoCapturer.switchCamera(null);
                currentCameraPosition=cameraPosition;
            });
        }
    }
   public void switchCamera(){
        if (!videoCallEnabled || videoCapturer == null) {
            Log.e(TAG, "Failed to switch camera. Video: " + videoCallEnabled + ". . Number of cameras: " + numberOfCameras);
            return;
        }
        executor.execute(() -> {
            Log.d(TAG, "Switch camera");
            videoCapturer.switchCamera(null);
            if (currentCameraPosition== HisunMediaConfiguration.CameraPosition.BACK) {
                currentCameraPosition = HisunMediaConfiguration.CameraPosition.FRONT;
            } else {
                currentCameraPosition = HisunMediaConfiguration.CameraPosition.BACK;
            }
        });
    }


    void setVideoEnabled( boolean enable) {
        executor.execute(() -> {
            renderVideo = enable;
            if (localVideoTrack != null) {
                localVideoTrack.setEnabled(renderVideo);
            }
            for (VideoTrack videoTrack : remoteVideoTracks.values()) {
                videoTrack.setEnabled(renderVideo);
            }
        });
    }

    boolean getVideoEnabled(){
        return renderVideo;
    }


    public boolean hasCameraPosition(HisunMediaConfiguration.CameraPosition position) {
        boolean retMe = false;

        String backName = CameraEnumerationAndroid.getNameOfBackFacingDevice();
        String frontName = CameraEnumerationAndroid.getNameOfFrontFacingDevice();

        if (position == HisunMediaConfiguration.CameraPosition.ANY &&
                (backName != null || frontName != null)){
            retMe = true;
        } else if (position == HisunMediaConfiguration.CameraPosition.BACK &&
                backName != null){
            retMe = true;

        } else if (position == HisunMediaConfiguration.CameraPosition.FRONT &&
                frontName != null){
            retMe = true;
        }
        return retMe;
    }

    void close(){
        localMediaStream.dispose();
        localMediaStream=null;
    }


    @Override
    public void onInitialize() {

    }

    @Override
    public void onLocalSdpOfferGenerated(SessionDescription sdp, HisunPeerConnection connection) {

    }

    @Override
    public void onLocalSdpAnswerGenerated(SessionDescription sdp, HisunPeerConnection connection) {

    }

    @Override
    public void onBufferedAmountChange(long l, HisunPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onStateChange(HisunPeerConnection connection, DataChannel channel) {

    }

    @Override
    public void onMessage(HisunPeerConnection connection, DataChannel channel, DataChannel.Buffer buffer) {

    }

    @Override
    public void onDataChannel(HisunPeerConnection connection, DataChannel dataChannel) {

    }

    @Override
    public void onIceConnectionChange(HisunPeerConnection connection, PeerConnection.IceConnectionState state) {

    }

    @Override
    public void onSignalingChange(HisunPeerConnection connection, PeerConnection.SignalingState signalingState) {

    }

    @Override
    public void onIceConnectionReceivingChange(HisunPeerConnection connection, boolean b) {

    }

    @Override
    public void onIceGatheringChange(HisunPeerConnection connection, PeerConnection.IceGatheringState iceGatheringState) {

    }

    @Override
    public void onIceCandidate(HisunPeerConnection connection, IceCandidate iceCandidate) {

    }

    @Override
    public void onRemoteStreamAdded(HisunPeerConnection connection, MediaStream mediaStream) {

    }

    @Override
    public void onPeerConnectionError(String connectionError) {

    }

    @Override
    public void onRemoteStreamRemoved(HisunPeerConnection connection, MediaStream mediaStream) {

    }

    @Override
    public void onRenegotiationNeeded() {

    }
}
