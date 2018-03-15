package io.github.lunarwatcher.chatbot.bot.chat;

/**
 * Class to handle the URL's for different events in the Stack Exchange network chats. Returns the URl for different
 * events.
 */
public class SEEvents {
    private SEEvents(){}

    /**
     * SO and MSE
     * @param chatURL
     * @return
     */
    public static String getLogin(String chatURL){
        return (chatURL.replace("chat.", "")) + "/users/login";
    }

    /**
     * SE
     * @param chatUrl
     * @return
     */
    public static String getSELogin(String chatUrl){
        return (chatUrl.replace("chat.", "")) + "/users/signin";
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
