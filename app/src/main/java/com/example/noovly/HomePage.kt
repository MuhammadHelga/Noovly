package com.example.noovly

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.storage
import java.io.ByteArrayOutputStream
import java.io.IOException

class HomePage : AppCompatActivity(), View.OnClickListener {

    private var tvUsername: TextView? = null
    private lateinit var btnLogout: Button
    private var mAuth: FirebaseAuth? = null
    private var addJudul: EditText? = null
    private var addPenulis: EditText? = null
    private var addSinopsis: EditText? = null
    private lateinit var btnAdd: Button
    private lateinit  var covers: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var noovlyAdapter: Adapter
    private lateinit var noovlyDB: DatabaseReference
    private var novelList: MutableList<Noovly> = mutableListOf()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        tvUsername = findViewById(R.id.username)
        btnLogout = findViewById(R.id.logout)
        btnLogout.setOnClickListener(this)
        mAuth = FirebaseAuth.getInstance()
        addJudul = findViewById(R.id.add_judul)
        addPenulis = findViewById(R.id.add_penulis)
        addSinopsis = findViewById(R.id.add_sinopsis)
        covers = findViewById(R.id.add_cover)
        btnAdd = findViewById(R.id.btn_add)

        recyclerView = findViewById(R.id.rv)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        noovlyAdapter = Adapter(this, novelList)
        recyclerView.adapter = noovlyAdapter

        noovlyDB = FirebaseDatabase.getInstance("https://noovly-d683a-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("noovly")
        noovlyDB.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                novelList.clear()
                for (noteSnapshot in snapshot.children) {
                    val note = noteSnapshot.getValue(Noovly::class.java)
                    note?.id = noteSnapshot.key
                    if (note != null) {
                        novelList.add(note)
                    }
                }
                noovlyAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomePage, "Failed to load notes.", Toast.LENGTH_SHORT).show()
            }
        })

        covers.setOnClickListener {
            selectimage()
        }
    }

    private fun selectimage() {
        val items = arrayOf<CharSequence>("Take Photo", "Choose from Library", "Cancel")
        val builder = AlertDialog.Builder(this)
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setItems(items) { dialog, item ->
            when {
                items[item] == "Ambil Foto" -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, 10)
                }
                items[item] == "Pilih dari File" -> {
                    val intent = Intent(Intent.ACTION_PICK)
                    intent.type = "image/*"
                    startActivityForResult(Intent.createChooser(intent, "Pilih Gambar"), 20)
                }
                items[item] == "Batal" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 20 && resultCode == RESULT_OK && data != null) {
            val path: Uri? = data.data
            val thread = Thread {
                try {
                    val inputStream = contentResolver.openInputStream(path!!)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    covers.post {
                        covers.setImageBitmap(bitmap)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            thread.start()
        }

        if (requestCode == 10 && resultCode == RESULT_OK) {
            val extras = data?.extras
            val thread = Thread {
                val bitmap = extras?.get("data") as Bitmap
                covers.post {
                    covers.setImageBitmap(bitmap)
                }
            }
            thread.start()
        }
    }

    private fun uploadImg(noteId: String) {
        covers.isDrawingCacheEnabled = true
        covers.buildDrawingCache()
        val bitmap = (covers.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val storage = Firebase.storage
        val storageRef = storage.reference.child("images/$noteId.jpeg")
        val uploadTask = storageRef.putBytes(data)
        uploadTask.addOnFailureListener { exception ->
            Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
        }.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUrl = task.result.toString()
                    saveNoteData(noteId, downloadUrl)
                } else {
                    Toast.makeText(this, "Failed to get download URL", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth!!.currentUser
        if (currentUser != null) {
            tvUsername!!.text = currentUser.email
        }
    }

    private fun logOut() {
        mAuth!!.signOut()
        val intent = Intent(this@HomePage, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.logout -> logOut()
            R.id.btn_add -> submitData()
        }
    }

    private fun submitData() {
        if (!validateForm()) {
            return
        }
        val noovlyID = noovlyDB.push().key ?: return
        uploadImg(noovlyID)
    }

    private fun validateForm(): Boolean {
        var result = true
        if (TextUtils.isEmpty(addJudul?.text.toString())) {
            addJudul?.error = "Required"
            result = false
        } else {
            addJudul?.error = null
        }
        if (TextUtils.isEmpty(addPenulis?.text.toString())) {
            addPenulis?.error = "Required"
            result = false
        } else {
            addPenulis?.error = null
        }
        if (TextUtils.isEmpty(addSinopsis?.text.toString())) {
            addSinopsis?.error = "Required"
            result = false
        } else {
            addSinopsis?.error = null
        }
        return result
    }

    private fun saveNoteData(noteId: String, imageUrl: String) {
        val judul = addJudul?.text.toString()
        val penulis = addPenulis?.text.toString()
        val sinopsis = addSinopsis?.text.toString()
        val note = Noovly(
            id = noteId,
            judul = judul,
            penulis = penulis,
            sinopsis = sinopsis,
            covers = imageUrl
        )
        noovlyDB.child(noteId).setValue(note).addOnSuccessListener {
            Toast.makeText(this@HomePage, "Note added", Toast.LENGTH_SHORT).show()
            addPenulis?.text?.clear()
            addPenulis?.text?.clear()
            addSinopsis?.text?.clear()
            covers.setImageResource(R.drawable.add_cover)
        }.addOnFailureListener {
            Toast.makeText(this@HomePage, "Failed to add note", Toast.LENGTH_SHORT).show()
        }
    }
}