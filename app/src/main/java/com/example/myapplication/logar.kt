package com.example.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class LogarActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var switchTheme: Switch
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Aplicar tema salvo ANTES do super.onCreate
        preferences = getSharedPreferences("config", MODE_PRIVATE)
        val darkMode = preferences.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (darkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logar)

        dbHelper = DatabaseHelper(this)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val etCpf = findViewById<EditText>(R.id.etCpf)
        val etSenha = findViewById<EditText>(R.id.etSenha)
        switchTheme = findViewById(R.id.switch1)

        // Define o estado do Switch com base no tema atual
        switchTheme.isChecked = darkMode

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // Salva o novo estado
            preferences.edit().putBoolean("dark_mode", isChecked).apply()
            // Aplica o tema e reinicia a Activity para aplicar
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        btnLogin.setOnClickListener {
            val cpf = etCpf.text.toString()
            val senha = etSenha.text.toString()

            val usuarioInfo = dbHelper.validarLogin(cpf, senha)
            if (usuarioInfo != null) {
                val (usuarioId, nomeUsuario) = usuarioInfo
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("usuario_id", usuarioId)
                intent.putExtra("nome_usuario", nomeUsuario)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "CPF ou senha inválidos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
