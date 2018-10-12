package com.test.xn.kmslib;

/**
 * Created by xuning on 2018/10/10.
 */

public interface RoomListener {

    /**
     *  Notification method names
     */
    public static final String METHOD_PARTICIPANT_JOINED = "participantJoined";
    public static final String METHOD_PARTICIPANT_PUBLISHED = "participantPublished";
    public static final String METHOD_PARTICIPANT_UNPUBLISHED = "participantUnpublished";
    public static final String METHOD_ICE_CANDIDATE = "iceCandidate";
    public static final String METHOD_PARTICIPANT_LEFT = "participantLeft";
    public static final String METHOD_SEND_MESSAGE = "sendMessage";
    public static final String METHOD_MEDIA_ERROR = "mediaError";

    //room 返回response
    void onRoomResponse(RoomResponse roomResponse);

    //当前房间发生错误
    void onRoomError(RoomError roomError);

    //接到房间中的notification
    void onRoomNotification(RoomNotification roomNotification);

    //与当前房间的连接已建立
    void onRoomConnected();

    //与当前房间的连接已断开
    void onRoomDisconnected();

}
