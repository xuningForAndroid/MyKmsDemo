package com.test.xn.webrtcpeer;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

/**
 * Created by Administrator on 2018/10/11.
 *  信令参数类
 */

public class SignalingParameters {
    public List<PeerConnection.IceServer> iceServers;
    public boolean initiator;
    public String clientId;
    public SessionDescription offerSdp;
    public List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> iceServers,
                               boolean initiator, String clientId,
                               SessionDescription offerSdp, List<IceCandidate> iceCandidates) {
        this.iceServers = iceServers;
        this.initiator = initiator;
        this.clientId = clientId;
        this.offerSdp = offerSdp;
        this.iceCandidates = iceCandidates;
    }
}
