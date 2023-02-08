package com.adelawson.facedetectiontest.ui.fragments.imagePreview

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toFile
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.adelawson.facedetectiontest.R
import com.adelawson.facedetectiontest.databinding.FragmentImagePreviewBinding
import com.bumptech.glide.Glide
import java.io.File

class ImagePreviewFragment : Fragment() {

    companion object {
        fun newInstance() = ImagePreviewFragment()
    }

    private lateinit var binding: FragmentImagePreviewBinding
    private lateinit var viewModel: ImagePreviewViewModel
    private val imagePreviewFragmentArgs by navArgs<ImagePreviewFragmentArgs>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagePreviewBinding.inflate(inflater)
        loadImage()
        setButtonBehaviour()
        return binding.root
    }

    private fun setButtonBehaviour(){
        binding.retakeButton.setOnClickListener {
            this.findNavController().navigateUp()
        }
    }

    private fun loadImage(){
        Glide.with(this).load(imagePreviewFragmentArgs.imageLocationUri)
            .into(binding.pictureImageView)
    }


}