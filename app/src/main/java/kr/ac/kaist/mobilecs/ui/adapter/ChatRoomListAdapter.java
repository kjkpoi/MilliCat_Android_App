package kr.ac.kaist.mobilecs.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import kr.ac.kaist.mobilecs.model.ChatRoom;

/**
 * Created by william on 20/04/15.
 */
public class ChatRoomListAdapter extends ArrayAdapter<ChatRoom> {
    public ChatRoomListAdapter(Context context, List<ChatRoom> objects) {
        super(context, android.R.layout.simple_list_item_1, android.R.id.text1, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        textView.setText(getItem(position).name);

        return convertView;
    }
}
