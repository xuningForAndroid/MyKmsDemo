package com.test.xn.kmslib;

import android.util.Log;

import net.minidev.json.JSONObject;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Vector;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcNotification;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcResponse;
import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

/**
 * Created by xuning on 2018/10/10.
 *
 */

public class KurentoRoomAPI extends KurentoAPI {

    private static final String TAG = "KurentoRoomAPI";
    private KeyStore keyStore;
    private boolean usingSelfSigned;
    private Vector<RoomListener> roomListeners;

    public KurentoRoomAPI(LooperExecutor executor, String wsUri,RoomListener listener) {
        super(executor, wsUri);

        roomListeners=new Vector<>();
        roomListeners.add(listener);
        try {
            keyStore=KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null,null);
        } catch (KeyStoreException |CertificateException|NoSuchAlgorithmException|IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发起加入房间的请求
     * @param userId
     * @param roomId
     * @param dataChannelEnabled
     * @param id
     */
    public void sendJoinRoom(String userId,String roomId,boolean dataChannelEnabled,int id){
        HashMap<String, Object> nameParameters = new HashMap<>();
        nameParameters.put("user",userId);
        nameParameters.put("room",roomId);
        nameParameters.put("dataChannels",dataChannelEnabled);
        sendRequest("joinRoom",nameParameters,id);
    }

    /**
     * 发送离开房间的请求
     * @param id
     */
    public void sendLeaveRoom(int id){
        sendRequest("leaveRoom",null,id);
    }

    /**
     * 发送发布视频的请求
     * 它的响应将包含sdpAnswer
     */
    public void sendPublishVideo(String sdpOffer,boolean doLoopback,int id){
        HashMap<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("sdpOffer", sdpOffer);
        namedParameters.put("doLoopback", doLoopback);
        sendRequest("publishVideo", namedParameters, id);
    }

    /**
     * 发送结束视频的请求
     */
    public void sendUnpublishVideo(int id){
        sendRequest("unpublishVideo",null,id);
    }

    /**
     *客户端从发布其媒体的房间中的参与者接收媒体的请求，它的响应将包含sdpAnswer
     * @param sender
     * @param streamId
     * @param sdpOffer
     * @param id
     */
    public void senReceiveVideoFrom(String sender,String streamId,String sdpOffer,int id){
        HashMap<String, Object> nameParameters = new HashMap<>();
        nameParameters.put("sdpOffer",sdpOffer);
        nameParameters.put("sender",sender+"_"+streamId);
        sendRequest("receiveVideoFrom",nameParameters,id);
    }

    /**
     * 客户端发起停止接收某个媒体提供者的视频的请求
     * @param userId
     * @param streamId
     * @param id
     */
    public void sendUnsubscribeFromVideo(String userId,String streamId,int id){
        String sender = userId + "_" + streamId;
        HashMap<String, Object> nameParameters = new HashMap<>();
        nameParameters.put("sender",sender);
        sendRequest("unsubscribeFromVideo",nameParameters,id);
    }

    /**
     *方法携带在客户端收集的关于ICE候选者的信息。实现涓流ICE机制需要这些信息。
     *
     * @param endPointName  ice候选者被找到的peer端userName
     * @param candidate     包含候选者的信息
     * @param sdpMid   视频或音频的媒体流识别，与候选者信息关联
     * @param sdpMLineIndex sdp 中mLine的索引，从0开始
     * @param id
     */
    public void sendOnIceCandidate(String endPointName,String candidate,String sdpMid,String sdpMLineIndex, int id){
        HashMap<String, Object> namedParameters = new HashMap<>();
        namedParameters.put("endpointName", endPointName);
        namedParameters.put("candidate", candidate);
        namedParameters.put("sdpMid", sdpMid);
        namedParameters.put("sdpMLineIndex", sdpMLineIndex);
        sendRequest("onIceCandidate",namedParameters,id);
    }

    /**
     * 当前用户给房间中的所有参与者发送一个消息
     * @param roomId 当前房间的名称
     * @param userId  发送消息的用户的名称
     * @param message 被发送到房间中的消息
     * @param id
     */
    public void sendMessage(String roomId,String userId,String message,int id){
        HashMap<String, Object> nameParameters = new HashMap<>();
        nameParameters.put("message",message);
        nameParameters.put("userMessage",userId);
        nameParameters.put("roomMessage",roomId);
        sendRequest("sendMessage",nameParameters,id);
    }

    /**
     * 方法发送不由房间服务器直接实现的任何自定义请求。
     *
     * @param names  参数名称数组
     * @param values 是一个参数值数组，其中索引与名称数组中的可用名称值相对应。
     * @param id
     */
    public void sendCustomRequest(String[] names,String[] values,int id){
        if (names==null || values==null|| names.length!=values.length)
            return;
        HashMap<String, Object> nameParameters = new HashMap<>();
        for (int i = 0; i < names.length; i++) {
            nameParameters.put(names[i],values[i]);
        }
        sendRequest("customRequest",nameParameters,id);
    }

    public void addTrustedCertificate(String alias, Certificate certificate){
        try {
            keyStore.setCertificateEntry(alias,certificate);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    public void setusingSelfSigned(boolean usingSelfSigned) {
        this.usingSelfSigned = usingSelfSigned;
    }
    
    public void connectWebSocket(){
        if (isWebSocketConnected()){
            return;
        }
        String scheme;
        try {
            scheme= new URI(wsUri).getScheme();
            if (scheme.equals("https") || scheme.equals("wss")){
                SSLContext sslContext = SSLContext.getInstance("TLS");
                if (usingSelfSigned) {
                    // Create a TrustManager that trusts the CAs in our KeyStore
                    String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                    tmf.init(keyStore);
                    //第一个参数是授权的密钥管理器，用来授权验证
                    //第二个是被授权的证书管理器，用来验证服务器端的证书
                    //第三个参数是一个随机数值，可以填写null
                    sslContext.init(null, tmf.getTrustManagers(), null);
                } else {
                    sslContext.init(null, null, null);
                }
                webSocketClientFactory=new DefaultSSLWebSocketClientFactory(sslContext);
            }
        } catch (URISyntaxException | NoSuchAlgorithmException |KeyStoreException |KeyManagementException e) {
            e.printStackTrace();
        }
    }

    //webSocket连接事件
    @Override
    public void onOpen(ServerHandshake handshakedata) {
        super.onOpen(handshakedata);
        synchronized (roomListeners){
            for (RoomListener rl:roomListeners){
                rl.onRoomConnected();
            }
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        super.onClose(code, reason, remote);
        synchronized (roomListeners){
            for (RoomListener rl:roomListeners){
                rl.onRoomDisconnected();
            }
        }
    }

    @Override
    public void onResponse(JsonRpcResponse response) {
        super.onResponse(response);
        if (response.isSuccessful()){//请求的返回结果是成功
            JSONObject jsonObject= (JSONObject) response.getResult();
            RoomResponse roomResponse = new RoomResponse(response.getId().toString(), jsonObject);

            synchronized (roomListeners){
                for (RoomListener rl:roomListeners){
                    rl.onRoomResponse(roomResponse);
                }
            }
        }else {//请求的返回结果是失败
            RoomError roomError = new RoomError(response.getError());
            synchronized (roomListeners){
                for (RoomListener rl:roomListeners){
                    rl.onRoomError(roomError);
                }
            }
        }
    }

    @Override
    public void onNotification(JsonRpcNotification notification) {
        super.onNotification(notification);
        RoomNotification roomNotification = new RoomNotification(notification);
        synchronized (roomListeners){
            for (RoomListener rl:roomListeners){
                rl.onRoomNotification(roomNotification);
            }
        }
    }
    public void addObserver(RoomListener listener){
        synchronized (roomListeners){
            roomListeners.add(listener);
        }
    }
    public void removeObserver(RoomListener listener){
        synchronized (roomListeners){
            roomListeners.remove(listener);
        }
    }

    @Override
    public void onError(Exception e) {
        super.onError(e);
        Log.e(TAG, "onError: "+e.getMessage(),e);
    }
}
