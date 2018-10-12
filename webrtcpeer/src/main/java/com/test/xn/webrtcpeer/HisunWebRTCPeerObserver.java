package com.test.xn.webrtcpeer;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

/**
 * Created by Administrator on 2018/10/11.
 * WebRTCPeer监听器
 */

public interface HisunWebRTCPeerObserver {
    //webRctpeer 初始化完成时被调用
    void onInitialize();
    //本地sdp offer已经设置成功
    void onLocalSdpOfferGenerated(SessionDescription sdp, HisunPeerConnection connection);
    //本地sdp Answer已经设置完成
    void onLocalSdpAnswerGenerated(SessionDescription sdp,HisunPeerConnection connection);

    void onBufferedAmountChange(long l, HisunPeerConnection connection, DataChannel channel);

    void onStateChange(HisunPeerConnection connection,DataChannel channel);

    void onMessage(HisunPeerConnection connection,DataChannel channel,DataChannel.Buffer buffer);

    void onDataChannel(HisunPeerConnection connection,DataChannel dataChannel);

    void onIceConnectionChange(HisunPeerConnection connection, PeerConnection.IceConnectionState state);

    void onSignalingChange(HisunPeerConnection connection,PeerConnection.SignalingState signalingState);

    void onIceConnectionReceivingChange(HisunPeerConnection connection,boolean b);

    void onIceGatheringChange(HisunPeerConnection connection,PeerConnection.IceGatheringState iceGatheringState);
    //有新的iceCandidate找到
    void onIceCandidate(HisunPeerConnection connection, IceCandidate iceCandidate);
    //远程流添加进来
    void onRemoteStreamAdded(HisunPeerConnection connection, MediaStream mediaStream);
    //peerConnection错误
    void onPeerConnectionError(String connectionError);
    //远程流被移除
    void onRemoteStreamRemoved(HisunPeerConnection connection,MediaStream mediaStream);
    //需要重新协商
    void onRenegotiationNeeded();
}
