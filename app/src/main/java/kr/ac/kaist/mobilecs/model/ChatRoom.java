package kr.ac.kaist.mobilecs.model;

import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * Created by william on 24/03/15.
 */
public class ChatRoom {

    public String _id;
    public String name;
    private LinkedHashSet<ChatParticipant> chatParticipants;
    private LinkedList<ChatMessage> incomingMessages;
    private LinkedList<ChatMessage> readMessages;
    public String timestamp;

}
