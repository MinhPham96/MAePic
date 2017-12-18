package com.example.hi.maepic;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import static java.security.AccessController.getContext;

//this adapter is inherited from the class Array Adapter
//which means it has all the features in this class
public class CommentAdapter extends ArrayAdapter<Comment> {
    //Context: the activity that this adapter is applied
    //resource: the xml that is used to modify the list cell
    //objects: the (array) list that is used to store the object data to display
    public CommentAdapter(Context context, int resource, List<Comment> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //get the layout from the xml of the cell
        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_comment, parent, false);
        }

        //get each component from the layout
        TextView commentText = (TextView) convertView.findViewById(R.id.commentTextView);
        TextView nameText = (TextView) convertView.findViewById(R.id.nameTextView);
        TextView dateText = (TextView) convertView.findViewById(R.id.dateTextView);

        //this will access to each cell of the list
        Comment comment = getItem(position);

        //change the cell components depending on the current object in the array list
        commentText.setText(comment.getText());
        nameText.setText(comment.getAuthor());
        dateText.setText(comment.printDate());

        return convertView;         //return the list cell to display
    }

}
