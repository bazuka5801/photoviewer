package com.getcapacitor.community.media.photoviewer.fragments

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.getcapacitor.JSObject
import com.getcapacitor.community.media.photoviewer.Notifications.NotificationCenter
import com.getcapacitor.community.media.photoviewer.R
import com.getcapacitor.community.media.photoviewer.adapter.Image
import com.getcapacitor.community.media.photoviewer.databinding.ImageFragmentBinding
import com.getcapacitor.community.media.photoviewer.helper.BackgroundColor
import com.getcapacitor.community.media.photoviewer.helper.GlideApp
import com.getcapacitor.community.media.photoviewer.helper.ShareImage
import com.ortiz.touchview.TouchImageView
import java.io.File

class ImageFragment : Fragment() {
    private val TAG = "ImageFragment"
    private var imageFragmentBinding: ImageFragmentBinding? = null
    private var bShare: Boolean = true
    private var maxZoomScale: Double = 3.0
    private var compressionQuality: Double = 0.8
    private var backgroundColor: String = "black"
    private lateinit var appId: String
    private lateinit var ivTouchImage: TouchImageView
    private lateinit var rlMenu: RelativeLayout
    private lateinit var rlLayout: RelativeLayout

    private lateinit var image: Image
    private var startFrom: Int = 0

    private var options = JSObject()
    var mContainer: ViewGroup? = null
    lateinit var mInflater: LayoutInflater
    lateinit var  appContext: Context

    fun setImage(image: Image) {
        this.image = image
    }

    fun setStartFrom(startFrom: Int) {
        this.startFrom = startFrom
    }


    fun setOptions(options: JSObject) {
        this.options = options
        if(this.options.has("share")) bShare = this.options.getBoolean("share")
        if(this.options.has("maxzoomscale")) maxZoomScale = this.options
            .getDouble("maxzoomscale")
        if(this.options.has("compressionquality")) compressionQuality = this.options
            .getDouble("compressionquality")
        if(this.options.has("backgroundcolor")) backgroundColor = this.options
            .getString("backgroundcolor").toString()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mInflater = inflater
        if (container != null) {
            mContainer  = container
            val view: View = initializeView()
            activity?.runOnUiThread( java.lang.Runnable {
                view.isFocusableInTouchMode = true;
                view.requestFocus();
                view.setOnKeyListener(object: View.OnKeyListener {
                    override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                        // if the event is a key down event on the enter button
                        if (event.action == KeyEvent.ACTION_DOWN &&
                            keyCode == KeyEvent.KEYCODE_BACK
                        ) {
                            backPressed()
                            return true
                        }
                        return false
                    }
                })
            })

            return view
        }
        return null
    }

    private fun backPressed() {
        postNotification()
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit();
    }
    private fun postNotification() {
        var info: MutableMap<String, Any> = mutableMapOf()
        info["result"] = true
        info["imageIndex"] = startFrom
        NotificationCenter.defaultCenter().postNotification("photoviewerExit", info);
    }
    private fun initializeView(): View {
        if (mContainer != null) {
            mContainer?.removeAllViewsInLayout()
        }
        appContext = this.requireContext()
        appId = appContext.getPackageName()

        // Inflate the layout for this fragment
        val binding = ImageFragmentBinding.inflate(mInflater, mContainer, false)
        imageFragmentBinding = binding
        val orientation: Int = resources.configuration.orientation
        if(orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "orientation Portrait")
        } else {
            Log.d(TAG, "orientation Landscape")
        }
        rlLayout = binding.rlTouchImage
        val mBackgroundColor = BackgroundColor()
        rlLayout.setBackgroundResource(mBackgroundColor.setBackColor(backgroundColor))

        rlMenu = binding.menuBtns
        ivTouchImage = binding.ivTouchImage
        val imgUrl = image.url
        if (imgUrl?.substring(0, 4).equals("http")) {
          // load image from http
          GlideApp.with(appContext)
            .load(image.url)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivTouchImage)
        }
        if (imgUrl?.substring(0, 4).equals("file")) {
          val uri: Uri = Uri.parse(imgUrl)
          val element: String? = uri.getLastPathSegment()
          var file: File? = null
          if (imgUrl?.contains("DCIM") == true) {
            file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString(), element )
          }
          if (imgUrl?.contains("Pictures") == true) {
            file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), element )
          }
          GlideApp.with(appContext)
            .asBitmap()
            .load(file)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(ivTouchImage)
        }


        val share: ImageButton = binding.shareBtn
        val close: ImageButton = binding.closeBtn
        if(!bShare) share.visibility = View.INVISIBLE
        activity?.runOnUiThread( java.lang.Runnable {
            val clickListener = View.OnClickListener { viewFS ->
                when (viewFS.getId()) {
                    R.id.shareBtn -> {
                        val mShareImage: ShareImage = ShareImage()
                        mShareImage.shareImage(image, appId, appContext, compressionQuality)
                    }
                    R.id.closeBtn -> {
                        postNotification()
                        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit();
                    }
                }
            }
            share.setOnClickListener(clickListener)
            close.setOnClickListener(clickListener)
        })
        return binding.root
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val view: View = initializeView()
        mContainer?.addView(view)
        super.onConfigurationChanged(newConfig)
    }
    override fun onDestroyView() {
        imageFragmentBinding = null
        clearCache()
        super.onDestroyView()
    }
    private fun clearCache() {
        Thread(Runnable {
            Glide.get(appContext).clearDiskCache()
        }).start()
        Glide.get(appContext).clearMemory()
    }
}
