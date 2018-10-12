package com.test.xn.webrtcpeer;

import android.util.Log;

import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;

import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

/**
 * Created by Administrator on 2018/10/11.
 * 这个类的作用是用来管理PeerConnection的
 */

public class PCResourceManager {

    private static final String TAG = "PCResourceManager";

    public HisunPeerConnectionParameter peerConnectionParameter;
    public LooperExecutor executor;
    public PeerConnectionFactory factory;
    private final boolean videoCallEnabled;
    private final boolean preferH264;
    private final boolean preferIsac;
    private final HashMap<String, HisunPeerConnection> connections;//所以得连接

    public PCResourceManager(HisunPeerConnectionParameter peerConnectionParameter,
                             LooperExecutor executor, PeerConnectionFactory factory) {
        this.peerConnectionParameter = peerConnectionParameter;
        this.executor = executor;
        this.factory = factory;
        videoCallEnabled = peerConnectionParameter.videoCallEnabled;
        preferH264 = videoCallEnabled && peerConnectionParameter.videoCodec != null && peerConnectionParameter.videoCodec.equals(HisunMediaConfiguration.VideoCodeC.H264.toString());
        preferIsac = peerConnectionParameter.audioCodec != null && peerConnectionParameter.audioCodec.equals(HisunMediaConfiguration.AudioCodeC.ISAC.toString());
        connections = new HashMap<>();
    }

    /**
     * 创建PeerConnection实例
     * @param signalingParameters
     * @param mediaConstraints
     * @param connectionId
     * @return
     */
    public HisunPeerConnection createPeerConnection(SignalingParameters signalingParameters,
                                               MediaConstraints mediaConstraints,
                                               String connectionId){

        Log.i(TAG, "createPeerConnection: ");
        Log.i(TAG, "createPeerConnection: "+mediaConstraints.toString());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(signalingParameters.iceServers);
        rtcConfig.tcpCandidatePolicy= PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
        rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

        HisunPeerConnection hisunPeerConnection = new HisunPeerConnection(connectionId, preferIsac, videoCallEnabled, preferH264, executor, peerConnectionParameter);
        PeerConnection peerConnection = factory.createPeerConnection(rtcConfig, mediaConstraints, hisunPeerConnection);
        hisunPeerConnection.setPc(peerConnection);
        connections.put(connectionId,hisunPeerConnection);

        Logging.enableTracing("logcat:", EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT), Logging.Severity.LS_INFO);

        Log.d(TAG, "Peer connection created.");
        return hisunPeerConnection;
    }

    public HisunPeerConnection getConnection(String connectionId){
        return connections.get(connectionId);
    }
    public Collection<HisunPeerConnection> getConnections(){
        return connections.values();
    }
    public void closeAllConnections(){
        for (HisunPeerConnection connection:connections.values()){
            connection.closePeerConnection();
        }
        connections.clear();
    }
    void closeConnection(String connectionId){
        HisunPeerConnection connection = connections.remove(connectionId);
        connection.closePeerConnection();
    }
}
