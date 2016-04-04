package kr.ac.kaist.mobilecs.model;

import java.util.Date;

/**
 * Created by william on 24/03/15.
 */
public class ChatMessage {

    public String _id;
    public String roomId;
    public String content;
    public String timestamp;
    public String sender = "";

    public ChatMessage(String roomId, String content, String sender) {
        this.roomId = roomId;
        this.content = content;
        this.sender = sender;
        this.timestamp = new Date().toString();
    }
}
