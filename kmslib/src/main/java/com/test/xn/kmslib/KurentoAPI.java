package com.test.xn.kmslib;

import android.net.Uri;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcNotification;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcRequest;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcResponse;
import fi.vtt.nubomedia.jsonrpcwsandroid.JsonRpcWebSocketClient;
import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

/**
 * Created by xuning on 2018/10/10.
 * websocket连接事件处理 打开、请求、响应、关闭、接到广播、错误
 *
 */

public class KurentoAPI implements JsonRpcWebSocketClient.WebSocketConnectionEvents {


    private static final String TAG = "KurentoAPI";
    protected JsonRpcWebSocketClient client=null;
    protected LooperExecutor executor=null;
    protected String wsUri=null;
    protected WebSocketClient.WebSocketClientFactory webSocketClientFactory=null;


    public KurentoAPI(LooperExecutor executor, String wsUri) {
        this.executor = executor;
        this.wsUri = wsUri;
    }


    /**
     * 连接socket
     */
    public void connectWebSocket(){
        try {
            if (isWebSocketConnected()){
                return;
            }
            URI uri = new URI(wsUri);
            client = new JsonRpcWebSocketClient(uri, this, executor);
            if (webSocketClientFactory!=null){
                client.setWebSocketFactory(webSocketClientFactory);
            }
            executor.execute(()-> client.connect());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断socket是否已经连接
     * @return
     */
    public boolean isWebSocketConnected() {
        if (client!=null){
            return client.getConnectionState().equals(JsonRpcWebSocketClient.WebSocketConnectionState.CONNECTED);
        }
        return false;
    }

    /**
     * 断开socket连接
     */
    public void disconnectWebSocket(){
        try{
            if (client!=null){
                executor.execute(()->client.disconnect(false));
            }
        }catch (Exception e){
            Log.e(TAG, "disconnectWebSocket: ",e );
        }finally {

        }
    }

    /**
     * 向服务端发送请求
     * @param method 方法名
     * @param nameParameters 参数列表
     * @param id 请求id
     */
    protected void sendRequest(String method, HashMap<String,Object> nameParameters,
                        int id){
        try{
            JsonRpcRequest request = new JsonRpcRequest();
            request.setMethod(method);
            if (nameParameters!=null) {
                request.setNamedParams(nameParameters);
            }
            if (id>0) {
                request.setId(id);
            }
            executor.execute(()->{
                client.sendRequest(request);
        });
        }catch (Exception e){
            Log.e(TAG, "sendRequest: ",e );
        }
    }

    //webSocket connection 事件
    @Override
    public void onRequest(JsonRpcRequest request) {
        Log.i(TAG, "onRequest: ");
    }

    @Override
    public void onResponse(JsonRpcResponse response) {
        Log.i(TAG, "onResponse: ");
    }

    @Override
    public void onNotification(JsonRpcNotification notification) {
        Log.i(TAG, "onNotification: ");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i(TAG, "onClose: ");
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(TAG, "onOpen: ");
    }

    @Override
    public void onError(Exception e) {
        Log.i(TAG, "onError: ");
    }
}
