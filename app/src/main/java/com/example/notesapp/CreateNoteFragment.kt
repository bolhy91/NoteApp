package com.example.notesapp

import android.app.Activity.RESULT_OK
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.notesapp.database.NotesDatabase
import com.example.notesapp.databinding.FragmentCreateNoteBinding
import com.example.notesapp.entities.Notes
import com.example.notesapp.util.NoteBottomSheetFragment
import kotlinx.android.synthetic.main.fragment_create_note.*
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest
import java.util.regex.Pattern

class CreateNoteFragment : BaseFragment(), EasyPermissions.PermissionCallbacks, EasyPermissions.RationaleCallbacks {

    private lateinit var _binding: FragmentCreateNoteBinding
    private val binding get() = _binding;
    private var currentDate: String? = null
    private var READ_STORAGE_PERMISSION = 123
    private var WRITE_STORAGE_PERMISSION = 123
    private var REQUEST_CODE_IMAGE = 123
    private var selectedImagegPath = ""
    private var webLinkUrl = ""
    private var noteId = -1

    var selectedColor = "#171C26"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteId = requireArguments().getInt("noteId", -1)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCreateNoteBinding.inflate(inflater, container, false)
        return _binding?.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CreateNoteFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            CreateNoteFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (noteId != -1) {
            launch {
                context?.let {
                    var notes = NotesDatabase.getDatabase(it).noteDao().getNoteById(noteId)
                    binding.colorView.setBackgroundColor(Color.parseColor(notes.color))
                    binding.etNoteTitle.setText(notes.title)
                    binding.etNoteSubTitle.setText(notes.subTitle)
                    binding.etNoteDesc.setText(notes.noteText)
                    if (notes.imgUrl != ""){
                        selectedImagegPath = notes.imgUrl!!
                        binding.imgNote.setImageBitmap(BitmapFactory.decodeFile(notes.imgUrl))
                        binding.layoutImage.visibility = View.VISIBLE
                        binding.imgNote.visibility = View.VISIBLE
                        binding.imgDelete.visibility = View.VISIBLE
                    }else{
                        binding.layoutImage.visibility = View.GONE
                        binding.imgNote.visibility = View.GONE
                        binding.imgDelete.visibility = View.GONE
                    }
                    if (notes.webList != ""){
                        binding.tvWebLink.text = notes.webList!!
                        binding?.tvWebLink.text = notes.webList
                        binding.l2.visibility = View.VISIBLE
                        binding.etWebLink.setText(notes.webList)
                        binding.imgUrlDelete.visibility = View.VISIBLE
                    }else{
                        binding.imgUrlDelete.visibility = View.GONE
                        binding.l2.visibility = View.GONE
                    }
                }
            }
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                BroadcastReceiver, IntentFilter("bottom_sheet_action")
        )

        val sdf = SimpleDateFormat("dd/M/yyyy hh:mm:ss")
        currentDate = sdf.format(Date())
        binding?.etNoteDateTime?.text = currentDate
        binding?.colorView?.setBackgroundColor(Color.parseColor(selectedColor))

        binding?.imgDone?.setOnClickListener {
            if (noteId != -1) {
                updateNote()
            } else {
                saveNote()
            }
        }

        binding?.imgBack?.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }

        binding.imgMore.setOnClickListener {
           var noteBottomSheetFragment = NoteBottomSheetFragment.newInstance()
               noteBottomSheetFragment.show(requireActivity().supportFragmentManager, "Note Bottom sheet Fragment")
        }

        binding.btnOk.setOnClickListener {
            if (binding.etWebLink.text.toString().trim().isNotEmpty()) {
                checkWebUrl()
            } else {
                Toast.makeText(context, "Url is required", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            if (noteId != -1) {
                binding.tvWebLink.visibility = View.VISIBLE
                binding.l2.visibility = View.GONE
            } else {
                selectedImagegPath = ""
                binding.l2.visibility = View.GONE
            }
        }

        binding.tvWebLink.setOnClickListener {
            var intent = Intent(Intent.ACTION_VIEW, Uri.parse(binding.etWebLink.text.toString()))
            startActivity(intent)
        }

        binding.imgDelete.setOnClickListener {
            binding.layoutImage.visibility = View.GONE
        }

        binding.imgUrlDelete.setOnClickListener {
            binding.tvWebLink.text = ""
            binding.tvWebLink.visibility = View.GONE
            binding.tvWebLink.visibility = View.GONE
        }
    }

    private fun updateNote(){
        launch {
            context?.let {
                var notes = NotesDatabase.getDatabase(it).noteDao().getNoteById(noteId)
                notes.title = etNoteTitle.text.toString()
                notes.subTitle = etNoteSubTitle.text.toString()
                notes.noteText = etNoteDesc.text.toString()
                notes.dateTime = currentDate
                notes.color = selectedColor
                notes.imgUrl = selectedImagegPath
                notes.webList = webLinkUrl
                NotesDatabase.getDatabase(it).noteDao().updateNote(notes)
                // Clear Note
                binding.etNoteTitle.setText("")
                binding.etNoteSubTitle.setText("")
                binding.etNoteDesc.setText("")
                binding.layoutImage.visibility = View.GONE
                binding.imgNote.visibility = View.GONE
                binding.tvWebLink.visibility = View.GONE
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
    }

    private fun saveNote() {
        if (binding?.etNoteTitle?.text?.isNullOrEmpty()!!) {
            Toast.makeText(context, "Title is required", Toast.LENGTH_SHORT).show()
        }

        else if (binding.etNoteSubTitle.text.isNullOrEmpty()!!) {
            Toast.makeText(context, "SubTitle is required", Toast.LENGTH_SHORT).show()
        }

        else if (binding.etNoteDesc.text.isNullOrEmpty()!!) {
            Toast.makeText(context, "Note Description is must not be null", Toast.LENGTH_SHORT).show()
        } else {
            launch {
                val notes = Notes()
                notes.title = binding.etNoteTitle.text.toString()
                notes.subTitle = binding.etNoteSubTitle.text.toString()
                notes.noteText = binding.etNoteDesc.text.toString()
                notes.dateTime = binding.etNoteDateTime.text.toString()
                notes.color    = selectedColor
                notes.imgUrl  = selectedImagegPath
                notes.webList = webLinkUrl
                context?.let {
                    NotesDatabase.getDatabase(it).noteDao().insertNotes(notes)
                    clearFieldNote()
                }
            }
        }
    }

    private fun clearFieldNote() {
        binding.etNoteTitle.setText("")
        binding.etNoteSubTitle.setText("")
        binding.etNoteDesc.setText("")
        binding.imgNote.visibility = View.GONE
        binding.tvWebLink.visibility = View.GONE
        requireActivity().supportFragmentManager.popBackStack()
    }

    private fun checkWebUrl() {
        if (Patterns.WEB_URL.matcher(binding.etWebLink.text.toString()).matches()) {
            binding.l2.visibility = View.GONE
            binding.etWebLink.isEnabled = false
            webLinkUrl = binding.etWebLink.text.toString()
            binding.tvWebLink.visibility = View.VISIBLE
            binding.tvWebLink.text = webLinkUrl
        } else{
            Toast.makeText(requireContext(), "Url is not valid", Toast.LENGTH_LONG).show()
        }
    }

    private val BroadcastReceiver : BroadcastReceiver = object :BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {

            var actionColor = p1!!.getStringExtra("action")

            when(actionColor!!){

                "Blue" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }

                "Yellow" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Purple" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Green" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Orange" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }


                "Black" -> {
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }

                "Image" -> {
                    readStorageTask()
                    binding.l2.visibility = View.GONE
                }

                "WebUrl" -> {
                    binding.l2.visibility = View.VISIBLE
                }

                else -> {
                    binding.layoutImage.visibility = View.GONE
                    binding.imgNote.visibility = View.GONE
                    selectedColor = p1.getStringExtra("selectedColor")!!
                    binding.colorView.setBackgroundColor(Color.parseColor(selectedColor))

                }
            }
        }

    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(BroadcastReceiver)
        super.onDestroy()
    }

    private fun hasReadStoragePerm(): Boolean {
        return EasyPermissions.hasPermissions(requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun readStorageTask() {
        if (hasReadStoragePerm()) {
            pickImageFromGallery()
        }else {
            EasyPermissions.requestPermissions(
                    requireActivity(),
                    getString(R.string.read_storage_text),
                    READ_STORAGE_PERMISSION,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    private fun pickImageFromGallery() {
        var intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivityForResult(intent, REQUEST_CODE_IMAGE)
        }
    }

    private fun getPathFromUri(contentUri: Uri): String? {
        var filePath: String? = null
        var cursor = requireActivity().contentResolver.query(contentUri, null, null, null, null)
        if (cursor == null) {
            filePath = contentUri.path
        } else {
            cursor.moveToFirst()
            var index = cursor.getColumnIndex("_data")
            filePath = cursor.getString(index)
        }
        return filePath
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                var selectedImagUrl = data.data
                if (selectedImagUrl != null) {

                    try {
                        var inputStream = requireActivity().contentResolver.openInputStream(selectedImagUrl)
                        var bitmap = BitmapFactory.decodeStream(inputStream)
                        binding.imgNote.setImageBitmap(bitmap)
                        binding.imgNote.visibility = View.VISIBLE
                        binding.layoutImage.visibility = View.VISIBLE

                        selectedImagegPath = getPathFromUri(selectedImagUrl)!!

                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, requireActivity())
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(requireActivity(), perms)) {
            AppSettingsDialog.Builder(requireActivity()).build().show()
        }
    }

    override fun onRationaleAccepted(requestCode: Int) {
        TODO("Not yet implemented")
    }

    override fun onRationaleDenied(requestCode: Int) {
        TODO("Not yet implemented")
    }

}