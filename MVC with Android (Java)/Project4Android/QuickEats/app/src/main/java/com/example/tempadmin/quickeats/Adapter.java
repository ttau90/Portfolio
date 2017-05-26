package com.example.tempadmin.quickeats;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by Tim on 11/16/2016.
 */
public class Adapter extends ArrayAdapter<Business> {
    public Adapter(Context context, List<Business> businessList){
        super(context, 0, businessList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        Business businessRow = getItem(position);

        //Get the view if it's null
        if (convertView == null){
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.adapterlayout, parent, false);
        }

        //Get the text view resources
        TextView nameTV = (TextView) convertView.findViewById(R.id.nameTV);
        TextView ratingTV = (TextView) convertView.findViewById(R.id.ratingTV);
        TextView addressTV = (TextView) convertView.findViewById(R.id.addressTV);

        //Set the contents of the textview to the restaurant information
        nameTV.setText(businessRow.getName());
        ratingTV.setText(businessRow.getRating());
        addressTV.setText(businessRow.getAddress());

        return convertView;
    }
}
