package com.tsa.imagerecognition


import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_list_of_default_images.*
import kotlinx.android.synthetic.main.fragment_list_of_default_images.view.*
import kotlinx.android.synthetic.main.list_defualt_item.view.*

/**
 * A simple [Fragment] subclass.
 */
class ListOfDefaultImagesFragment : Fragment() {

    private lateinit var listOfImages: RecyclerView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment

        val viewImages = inflater.inflate(R.layout.fragment_list_of_default_images, container, false)

        listOfImages = viewImages.list_of_images

        listOfImages.layoutManager = GridLayoutManager(context, 2 ) as RecyclerView.LayoutManager


        var act = activity as MainActivity

        Log.d("jij", act.listImages.size.toString())

        var adapter = MyDefaultAdapter(act.listImages, act)


        listOfImages.adapter = adapter

        Log.d("jij", "Adapter setup")


        return viewImages
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        closeFragment.setOnClickListener{
            var act = activity as MainActivity
            act.dropFragment(true)
        }
    }
}
