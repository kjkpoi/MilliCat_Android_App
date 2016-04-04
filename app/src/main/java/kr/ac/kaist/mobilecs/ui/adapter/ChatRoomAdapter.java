package kr.ac.kaist.mobilecs.ui.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import kr.ac.kaist.mobilecs.Markup;
import kr.ac.kaist.mobilecs.R;
import kr.ac.kaist.mobilecs.model.ChatMessage;

/**
 * Created by william on 20/04/15.
 */
public class ChatRoomAdapter extends ArrayAdapter<ChatMessage> {

    private final SimpleDateFormat dateFormat;
    private LayoutInflater inflater;

    public ChatRoomAdapter(Context context, List<ChatMessage> objects) {
        super(context, R.layout.chatroom_list_item, R.id.tv_text, objects);
        inflater = LayoutInflater.from(context);

        // Setup date formatter
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.chatroom_list_item, parent, false);
        }
        TextView tv_sender = (TextView) convertView.findViewById(R.id.tv_sender);
        TextView tv_timestamp = (TextView) convertView.findViewById(R.id.tv_timestamp);
        final TextView tv_text = (TextView) convertView.findViewById(R.id.tv_text);

        final ChatMessage chatMessage = getItem(position);

        Date timestamp;
        try {
            timestamp = dateFormat.parse(chatMessage.timestamp);
        } catch (ParseException e) {
            timestamp = new Date(chatMessage.timestamp);
            //e.printStackTrace();
        }

        //String timestampString = SimpleDateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(timestamp);
        String timestampString = DateFormat.getTimeFormat(getContext()).format(timestamp);

        tv_sender.setText(chatMessage.sender);
        tv_timestamp.setText("[" + timestampString + "]");

        Markup.inflate(getContext().getResources(), tv_text, chatMessage.content);


        return convertView;
    }
}
