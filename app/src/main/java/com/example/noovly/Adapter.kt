package com.example.noovly

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class Adapter(
    private val context: Context,
    private val noovlyList: MutableList<Noovly>,
    private val dataChangeListener: DataChangeListener? = null

): RecyclerView.Adapter<Adapter.NoovlyViewHolder>() {

    class NoovlyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val outTittle: TextView = itemView.findViewById(R.id.vJudul)
        val delItems: ImageButton = itemView.findViewById(R.id.btn_delete)
        val avatars: ImageView = itemView.findViewById(R.id.vCover)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoovlyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)
        return NoovlyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NoovlyViewHolder, position: Int) {
        val currentNoovly = noovlyList[position]
        holder.itemView.setOnClickListener {
            val intent = Intent(context, ViewDetail::class.java)
            intent.putExtra("id", currentNoovly.id)
            intent.putExtra("judul", currentNoovly.judul)
            intent.putExtra("penulis", currentNoovly.penulis)
            intent.putExtra("sinopsis", currentNoovly.sinopsis)
            intent.putExtra("covers", currentNoovly.covers)
            context.startActivity(intent)
        }

        Glide.with(context).load(currentNoovly.covers).into(holder.avatars)

        holder.delItems.setOnClickListener {
            val itemPosition = holder.adapterPosition
            if (itemPosition != RecyclerView.NO_POSITION) {
                val noteToDelete = noovlyList[position]
                CoroutineScope(Dispatchers.IO).launch {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val userId = currentUser!!.uid
                    val databaseRef = FirebaseDatabase.getInstance("https://noovly-d683a-default-rtdb.asia-southeast1.firebasedatabase.app/")
                        .getReference("users").child(userId).child("notes")
                    databaseRef.child(noteToDelete.id!!).removeValue().await()

                    val storageRef = FirebaseStorage.getInstance().reference.child("images/${noteToDelete.id}.jpeg")
                    storageRef.delete().await()
                    withContext(Dispatchers.Main) {
                        dataChangeListener?.onDataChange()
                        notifyItemRangeChanged(position, noovlyList.size)
                    }
                }
            }
        }
        holder.outTittle.text = currentNoovly.judul
    }

    override fun getItemCount(): Int {
        return noovlyList.size
    }
}

interface DataChangeListener {
    fun onDataChange()
}
