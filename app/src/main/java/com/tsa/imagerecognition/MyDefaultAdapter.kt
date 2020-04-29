package com.tsa.imagerecognition

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.list_defualt_item.view.*

class MyDefaultAdapter(val items : ArrayList<Bitmap>, val context: Context) : RecyclerView.Adapter<MyDefaultAdapter.ViewHoled>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHoled, position: Int) {
        holder.itemImage.setImageBitmap(items[position])
       // holder.itemImage.setBackgroundColor(121212)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHoled {
        return ViewHoled(LayoutInflater.from(context).inflate(R.layout.list_defualt_item, parent, false))
    }


    class ViewHoled(view: View): RecyclerView.ViewHolder(view) {

        val itemImage = view.item_image
    }
}