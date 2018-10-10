package de.flynamic.mobileabis

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import android.support.design.widget.Snackbar
import android.util.Log
import java.io.IOException
import android.widget.*
import android.widget.LinearLayout
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import de.flynamic.mobileabis.inference.ImageClassifier
import de.flynamic.mobileabis.inference.Labels
import kotlinx.android.synthetic.main.fragment_inference_result.view.*
import java.io.File


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [PredictFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [PredictFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class PredictFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2

    private lateinit var classifier: ImageClassifier
    private lateinit var labels: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
        classifier = (activity!! as MainActivity).classifier
        labels = Labels(activity!!).labels
    }

    private fun choose() {
        val intent = Intent()
        // Show only images, no videos or anything else
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {

            val uri = data.data

            predict(uri)
        }

        if (resultCode == RESULT_OK && requestCode == TAKE_PHOTO_REQUEST) {
            predict(mCurrentPhotoPath)
        }
    }

    private fun predict(uri: Uri?) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(activity?.contentResolver, uri)
            Log.d("INFO", bitmap.toString())
            val imageView = ImageView(activity)
            imageView.setImageBitmap(bitmap)
            imageView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

            val sv = view!!.findViewById<LinearLayout>(R.id.view_results)
            val container = LinearLayout(activity)
            container.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val inferenceResult = LayoutInflater.from(activity).inflate(R.layout.fragment_inference_result, container, false)
            inferenceResult.imageView.setImageBitmap(bitmap)

            val result = classifier.classifyFrame(bitmap)

            val top5 = result.slice(IntRange(0, endInclusive = 4))

            val texts = listOf(inferenceResult.top1, inferenceResult.top2, inferenceResult.top3, inferenceResult.top4, inferenceResult.top5)
            val probs = listOf(inferenceResult.top1_prob, inferenceResult.top2_prob, inferenceResult.top3_prob, inferenceResult.top4_prob, inferenceResult.top5_prob)

            for (i in 0 until texts.size) {
                texts[i].text = labels[top5[i].first]
                probs[i].text = "%.5f%%".format(top5[i].second * 100)
            }
            sv.addView(inferenceResult, 0)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_predict, container, false)
        view.findViewById<Button>(R.id.button_choose_image).setOnClickListener { choose() }
        view.findViewById<Button>(R.id.button_take_photo).setOnClickListener { validatePermissions() }
        return view
    }

    private fun validatePermissions() {
        Dexter.withActivity(activity)
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(object: PermissionListener {
                    override fun onPermissionGranted(
                            response: PermissionGrantedResponse?) {
                        launchCamera()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                            permission: PermissionRequest?,
                            token: PermissionToken?) {
                        AlertDialog.Builder(context)
                                .setTitle(
                                        "title")
                                .setMessage(
                                        "message")
                                .setNegativeButton(
                                        android.R.string.cancel
                                ) { dialog, _ ->
                                    dialog.dismiss()
                                    token?.cancelPermissionRequest()
                                }
                                .setPositiveButton(android.R.string.ok
                                ) { dialog, _ ->
                                    dialog.dismiss()
                                    token?.continuePermissionRequest()
                                }
                                .setOnDismissListener {
                                    token?.cancelPermissionRequest() }
                                .show()
                    }

                    override fun onPermissionDenied(
                            response: PermissionDeniedResponse?) {
                        Snackbar.make(view!!,
                                "permission denied",
                                Snackbar.LENGTH_LONG)
                                .show()
                    }
                })
                .check()
    }

    private lateinit var mCurrentPhotoPath: Uri

    private fun launchCamera() {
        val values = ContentValues(1)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
        val fileUri = activity!!.contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if(intent.resolveActivity(activity!!.packageManager) != null) {
            mCurrentPhotoPath = fileUri!!
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PredictFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                PredictFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, param2)
                    }
                }
    }
}
