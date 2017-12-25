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

/**
 * Created by Hi on 19-Dec-17.
 */

public class ArticleAdapter extends ArrayAdapter<Article> {
    //Context: the activity that this adapter is applied
    //resource: the xml that is used to modify the list cell
    //objects: the (array) list that is used to store the object data to display
    public ArticleAdapter(Context context, int resource, List<Article> objects) {
        super(context, resource, objects);
    }

    private class ViewHolder {
        TextView commentText, nameText, dateText;
        ImageView photoImageView;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //get the layout from the xml of the cell
        ViewHolder holder;

        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_status, parent, false);

            holder = new ViewHolder();
            //get each component from the layout
            holder.commentText = (TextView) convertView.findViewById(R.id.commentTextView);
            holder.nameText = (TextView) convertView.findViewById(R.id.nameTextView);
            holder.dateText = (TextView) convertView.findViewById(R.id.dateTextView);
            holder.photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        //this will access to each cell of the list
        Article article = getItem(position);

        boolean isPhoto = article.getPhotoURL() != null;
        if(isPhoto) {
            holder.photoImageView.setVisibility(View.VISIBLE);
            Glide.with(holder.photoImageView.getContext())
                    .load(article.getPhotoURL())
                    .into(holder.photoImageView);
        }
        else {
            holder.photoImageView.setVisibility(View.GONE);
        }

        //change the cell components depending on the current object in the array list
        holder.commentText.setText(article.getText());
        holder.nameText.setText(article.getOwner());
        if (article.getDate() != null) holder.dateText.setText(article.printDate());

        return convertView;         //return the list cell to display
    }

}