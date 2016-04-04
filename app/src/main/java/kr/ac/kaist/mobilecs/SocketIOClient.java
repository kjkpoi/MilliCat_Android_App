package kr.ac.kaist.mobilecs;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

/**
 * Created by william on 07/01/15.
 */
public class SocketIOClient {

    private static Socket socket;

    public static void connect(Emitter.Listener postMessageToRoomListener) {
        try {
            socket = IO.socket(HttpManager.BASE_HTTP);
            socket.on("postMessageToRoom", postMessageToRoomListener);
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void disconnect() {
        socket.disconnect();
    }

    public static void postMessageToRoom(String roomId, String sender, CharSequence message) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("sender", sender);
            obj.put("content", message);
            obj.put("roomId", roomId);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        emit("postMessageToRoom", obj);
    }

    private static void emit(String event, JSONObject object) {
        if (socket.connected()) socket.emit(event, object);
    }
}
