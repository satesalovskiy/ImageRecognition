package com.tsa.imagerecognition

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_list_of_default_images.*
import kotlinx.android.synthetic.main.fragment_list_of_default_images.view.*

class ListOfDefaultImagesFragment : Fragment() {

    private lateinit var listOfImages: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val viewImages = inflater.inflate(R.layout.fragment_list_of_default_images, container, false)

        listOfImages = viewImages.list_of_images
        listOfImages.layoutManager = GridLayoutManager(context, 2 )

        val act = activity as MainActivity

        val adapter = MyDefaultAdapter(act.listImages, act)
        listOfImages.adapter = adapter

        return viewImages
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        closeFragment.setOnClickListener{
            val act = activity as MainActivity
            act.dropFragment(true)
        }
    }
}
