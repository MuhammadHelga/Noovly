package com.example.noovly

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var login: TextView
    private lateinit var daftar: TextView
    private lateinit var emailLogin: EditText
    private lateinit var passwordLogin: EditText
    private lateinit var btnMasuk: Button
    private lateinit var username: EditText
    private lateinit var regEmail: EditText
    private lateinit var regPass: EditText
    private lateinit var btnDaftar: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        initializeMainComponents()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = mAuth.currentUser
        updateUI(currentUser)
    }

    private fun initializeMainComponents() {
        login = findViewById(R.id.login)
        daftar = findViewById(R.id.daftar)

        login.setOnClickListener {
            setContentView(R.layout.login_page)
            initializeLoginComponents()
        }

        daftar.setOnClickListener {
            setContentView(R.layout.register_page)
            initializeRegisterComponents()
        }
    }

    private fun initializeLoginComponents() {
        emailLogin = findViewById(R.id.email)
        passwordLogin = findViewById(R.id.pass)
        btnMasuk = findViewById(R.id.btn_masuk)
        btnMasuk.setOnClickListener(this)
    }

    private fun initializeRegisterComponents() {
        username = findViewById(R.id.username)
        regEmail = findViewById(R.id.email_reg)
        regPass = findViewById(R.id.pass_reg)
        btnDaftar = findViewById(R.id.btn_reg)
        btnDaftar.setOnClickListener(this)
    }

    private fun login(email: String?, password: String?) {
        if (!validateForm(true)) {
            return
        }
        mAuth.signInWithEmailAndPassword(email!!, password!!)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "signInWithEmail:success")
                    val user = mAuth.currentUser
                    Toast.makeText(this@MainActivity, user.toString(), Toast.LENGTH_SHORT).show()
                    updateUI(user)
                } else {
                    Log.w(ContentValues.TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this@MainActivity, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun signUp(username: String?, email: String?, password: String?) {
        if (!validateForm(false)) {
            return
        }
        mAuth.createUserWithEmailAndPassword(email!!, password!!)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(ContentValues.TAG, "createUserWithEmail:success")
                    val user = mAuth.currentUser
                    val userId = user!!.uid

                    val database = FirebaseDatabase.getInstance("https://prakpam-ef343-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    val userRef = database.getReference("users").child(userId)
                    val userData = mapOf(
                        "email" to email,
                        "username" to username,
                        "uid" to userId
                    )

                    userRef.setValue(userData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@MainActivity, "User registered successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to register user", Toast.LENGTH_SHORT).show()
                        }
                    }
                    updateUI(user)
                } else {
                    Log.w(ContentValues.TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this@MainActivity, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    updateUI(null)
                }
            }
    }

    private fun validateForm(isLogin: Boolean): Boolean {
        var result = true
        if (isLogin) {
            if (TextUtils.isEmpty(emailLogin.text.toString())) {
                emailLogin.error = "Required"
                result = false
            } else {
                emailLogin.error = null
            }
            if (TextUtils.isEmpty(passwordLogin.text.toString())) {
                passwordLogin.error = "Required"
                result = false
            } else {
                passwordLogin.error = null
            }
        } else {
            if (TextUtils.isEmpty(regEmail.text.toString())) {
                regEmail.error = "Required"
                result = false
            } else {
                regEmail.error = null
            }
            if (TextUtils.isEmpty(regPass.text.toString())) {
                regPass.error = "Required"
                result = false
            } else {
                regPass.error = null
            }
        }
        return result
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_masuk -> login(emailLogin.text.toString(), passwordLogin.text.toString())
            R.id.btn_reg -> signUp(username.text.toString(), regEmail.text.toString(), regPass.text.toString())
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this@MainActivity, HomePage::class.java)
            startActivity(intent)
        } else {
            Toast.makeText(this@MainActivity, "Log In First", Toast.LENGTH_SHORT).show()
        }
    }
}
