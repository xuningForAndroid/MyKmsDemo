package com.test.xn;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.test.xn.kmslib.Constant;
import com.test.xn.kmslib.KurentoRoomAPI;
import com.test.xn.kmslib.RoomError;
import com.test.xn.kmslib.RoomListener;
import com.test.xn.kmslib.RoomNotification;
import com.test.xn.kmslib.RoomResponse;

import fi.vtt.nubomedia.utilitiesandroid.LooperExecutor;

public class MainActivity extends AppCompatActivity implements RoomListener{

    private static final String TAG = "MainActivity";
    private LooperExecutor executor;
    private KurentoRoomAPI kurentoRoomAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        executor = new LooperExecutor();
        executor.requestStart();
        String wsUri="wss://mykurentoserver:1234/room";
        kurentoRoomAPI = new KurentoRoomAPI(executor, wsUri, this);
        kurentoRoomAPI.connectWebSocket();
    }

    @Override
    public void onRoomResponse(RoomResponse roomResponse) {
        Log.i(TAG, "onRoomResponse: ");
        switch (roomResponse.getId()){//这个id对应请求的id
            case Constant.JOIN_ROOM://连接房间
                Log.i(TAG, "成功连接到房间 ");
                kurentoRoomAPI.sendMessage("myRoonName","myUserName","Hello Room",Constant.SEND_MESSAGE);
                break;
            case Constant.SEND_MESSAGE://发送消息
                Log.i(TAG, "onRoomResponse: the server receive my message:"+roomResponse.getValues().toString());
                break;
        }
    }

    @Override
    public void onRoomError(RoomError roomError) {
        Log.i(TAG, "onRoomError: "+roomError.toString());
        if (roomError.getCode()==RoomError.Code.EXISTING_USER_IN_ROOM_ERROR_CODE.getValue()){
            Log.e(TAG, "onRoomError: "+"the user already in the room");
        }
    }

    @Override
    public void onRoomNotification(RoomNotification roomNotification) {
        Log.i(TAG, "onRoomNotification: ");
        if (roomNotification.getMethod().equals(RoomListener.METHOD_SEND_MESSAGE)){
            String userName = roomNotification.getParam("user").toString();
            String message = roomNotification.getParam("message").toString();
            Log.i(TAG, "onRoomNotification: "+userName+" send a message "+message);
        }
    }

    @Override
    public void onRoomConnected() {
        Log.i(TAG, "onRoomConnected: ");
        kurentoRoomAPI.sendJoinRoom("myUserName","myRoomName",true, Constant.JOIN_ROOM);
    }

    @Override
    public void onRoomDisconnected() {
        Log.i(TAG, "onRoomDisconnected: ");
        kurentoRoomAPI.sendLeaveRoom(123);
    }
}
