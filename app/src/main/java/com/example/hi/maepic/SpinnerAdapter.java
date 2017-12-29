package com.example.hi.maepic;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

// adapter to change each item displayed on the spinner
public class SpinnerAdapter extends ArrayAdapter<String> {

    private Context ctx;
    // array for description text
    private String[] contentArray;
    private Integer[] imageArray;

    public SpinnerAdapter(Context context, int resource, String[] objects,
                          Integer[] imageArray) {
        super(context,  R.layout.row, R.id.spinnerTextView, objects);
        this.ctx = context;             //the context activity
        this.contentArray = objects;    //text
        this.imageArray = imageArray;   //image
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //get the layout from the xml of the cell
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.row, null);

        }
        //change the text component of each cell
        TextView textView = (TextView) convertView.findViewById(R.id.spinnerTextView);
        textView.setText(contentArray[position]);

        //due to limited space, the image view is hidden in the cell
//        ImageView imageView = (ImageView)convertView.findViewById(R.id.spinnerImages);
//        imageView.setImageResource(imageArray[position]);

        return convertView;     //return the list cell to display

    }

}