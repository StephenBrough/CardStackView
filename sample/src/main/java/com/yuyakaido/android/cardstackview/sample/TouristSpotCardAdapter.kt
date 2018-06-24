package com.yuyakaido.android.cardstackview.sample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

import com.bumptech.glide.Glide

class TouristSpotCardAdapter(context: Context) : ArrayAdapter<TouristSpot>(context, 0) {

    override fun getView(position: Int, contentView: View?, parent: ViewGroup): View {
        var localContentView = contentView
        val holder: ViewHolder

        if (localContentView == null) {
            val inflater = LayoutInflater.from(context)
            localContentView = inflater.inflate(R.layout.item_tourist_spot_card, parent, false)
            holder = ViewHolder(localContentView)
            localContentView.tag = holder
        } else {
            holder = localContentView.tag as ViewHolder
        }

        val spot = getItem(position)

        holder.name.text = spot!!.name
        holder.city.text = spot.city
        Glide.with(context).load(spot.url).into(holder.image)

        return localContentView!!
    }

    private class ViewHolder(view: View) {
        var name: TextView = view.findViewById(R.id.item_tourist_spot_card_name)
        var city: TextView = view.findViewById(R.id.item_tourist_spot_card_city)
        var image: ImageView = view.findViewById(R.id.item_tourist_spot_card_image)
    }

}

