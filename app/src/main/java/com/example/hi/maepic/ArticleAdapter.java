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

// adapter to display the articles in the list view in Account Activity

public class ArticleAdapter extends ArrayAdapter<Article> {
    //Context: the activity that this adapter is applied
    //resource: the xml that is used to modify the list cell
    //objects: the (array) list that is used to store the object data to display
    public ArticleAdapter(Context context, int resource, List<Article> objects) {
        super(context, resource, objects);
    }

    private class ViewHolder {
        TextView commentText, nameText, dateText;
        ImageView photoImageView, imageView;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //get the layout from the xml of the cell
        ViewHolder holder;

        if (convertView == null) {
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_status, parent, false);

            holder = new ViewHolder();
            //get each respective component from the layout
            holder.commentText = (TextView) convertView.findViewById(R.id.commentTextView);
            holder.nameText = (TextView) convertView.findViewById(R.id.nameTextView);
            holder.dateText = (TextView) convertView.findViewById(R.id.dateTextView);
            holder.photoImageView = (ImageView) convertView.findViewById(R.id.photoImageView);
            holder.imageView = (ImageView) convertView.findViewById(R.id.imageView);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }

        //this will access to each cell of the list
        Article article = getItem(position);

        //if there is a photo URL
        boolean isPhoto = article.getPhotoURL() != null;
        if(isPhoto) {
            // set the image view to be visible
            holder.photoImageView.setVisibility(View.VISIBLE);
            // and then display the image
            Glide.with(holder.photoImageView.getContext())
                    .load(article.getPhotoURL())
                    .into(holder.photoImageView);
        }
        else {
            // if there is no photo URL, do not display the image view
            holder.photoImageView.setVisibility(View.GONE);
        }

        //change the cell components depending on the current object in the array list
        holder.commentText.setText(article.getText());
        holder.nameText.setText(article.getOwner());
        holder.imageView.setImageResource(article.getIconURL());
        if (article.getDate() != null) holder.dateText.setText(article.printDate());

        return convertView;         //return the list cell to display
    }

}