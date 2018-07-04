package io.github.lunarwatcher.chatbot.bot.chat;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Class to handle the URL's for different events in the Stack Exchange network chats. Returns the URl for different
 * events.
 */
public class SEEvents {
    private SEEvents(){}

    public static String getLogin(String mainsiteUrl){
        try {
            return mainsiteUrl + "/users/login?returnurl=" + URLEncoder.encode(mainsiteUrl + "/", "UTF-8");
        }catch(UnsupportedEncodingException e){
            throw new RuntimeException(e);
        }
    }

    public static String getSELogin(String url){
        try{
            return url + "/users/signin?returnurl=" + URLEncoder.encode(url + "/", "UTF-8");
        }catch(UnsupportedEncodingException e){
            throw new RuntimeException(e);
        }
    }

    public static String getEdit(String chatUrl, long messageID){
        return chatUrl + "/messages/" + messageID;
    }

    public static String getDelete(String chatURL, long messageID){
        return chatURL + "/messages/" + messageID + "/delete";
    }

    public static String getMessage(String chatUrl, int roomId){
        return chatUrl + "/chats/" + roomId + "/events";
    }

    public static String getSendMessage(String chatUrl, int room){
        return chatUrl + "/chats/" + room + "/messages/new";
    }

    public static String getUserInfo(String chatDomain, int userId){
        return chatDomain + "/user/info";
    }

    public static String getPingableUser(String chatDomain, int roomID){
        return chatDomain + "/rooms/pingable/" + roomID;
    }

    public static String getRoomInfo(String chatDomain, int roomID){
        return chatDomain + "/rooms/thumbs/" + roomID;
    }

    public static String leaveRoom(String chatDomain, int roomID){
        return chatDomain + "/chats/leave/" + roomID;
    }

    public static String getRoom(String chatDomain, int roomID){
        return chatDomain + "/rooms/" + roomID;
    }

    public static String getWSUrl(String chatDomain){
        return chatDomain + "/ws-auth";
    }


}
