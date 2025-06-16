package com.example.myapplication

import FragmentCadastrarUsuario
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isFragmentVisible = false
    private var usuarioId: Int = -1
    private var nomeUsuario: String = "usuário"
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: BatidaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Recebe usuárioId e nomeUsuario do Intent
        usuarioId = intent.getIntExtra("usuario_id", -1)
        nomeUsuario = intent.getStringExtra("nome_usuario") ?: "usuário"

        Log.d("MainActivity", "ID recebido: $usuarioId")
        Log.d("MainActivity", "Nome recebido: $nomeUsuario")

        Toast.makeText(this, "Olá, $nomeUsuario", Toast.LENGTH_SHORT).show()
        binding.textViewNomeUsuario.text = "Olá, $nomeUsuario"

        // Inicializa adapter com lista vazia e configura RecyclerView (pode remover se não usar)
        adapter = BatidaAdapter(this, listOf())
        binding.recyclerUltimosRegistros.adapter = adapter

        // Carrega e exibe a última batida no card
        carregarUltimaBatida()

        // Botão: Cadastrar Usuário
        binding.btnCadastrarUsuario.setOnClickListener {
            showFragment(FragmentCadastrarUsuario())
        }

        // Botão: Consultar folha
        binding.btnConsultarFolha.setOnClickListener {
            val fragment = FragmentConsultaFolha()
            val bundle = Bundle()
            bundle.putInt("usuario_id", usuarioId)
            fragment.arguments = bundle
            showFragment(fragment)
        }

        // Botão: Cadastrar Empresa
        binding.btnCadastrarEmpresa.setOnClickListener {
            showFragment(FragmentCadastrarEmpresa())
        }

        // Botão: Sair
        binding.btnSair.setOnClickListener {
            finish()
        }

        // Botão: Registrar Ponto (Entrada/Saída único)
        binding.bntRegistraPonto.setOnClickListener {
            val sucesso = dbHelper.baterPontoUnico(usuarioId)
            if (sucesso) {
                Toast.makeText(this, "Ponto registrado com sucesso!", Toast.LENGTH_SHORT).show()
                carregarUltimaBatida() // Atualiza card após registrar ponto
            } else {
                Toast.makeText(this, "Você já bateu entrada e saída hoje!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun carregarUltimaBatida() {
        val ultimaBatida = dbHelper.getUltimaBatida(usuarioId)

        if (ultimaBatida != null) {
            // Converte string de data (yyyy-MM-dd) para Date e formata para dd/MM/yyyy
            val sdfBanco = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfExibir = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dataDate = try {
                sdfBanco.parse(ultimaBatida.data)
            } catch (e: Exception) {
                null
            }
            val dataFormatada = dataDate?.let { sdfExibir.format(it) } ?: ultimaBatida.data

            binding.tvDataUltimaBatida.text = "Data: $dataFormatada"
            binding.tvEntradaUltimaBatida.text = "Entrada: ${ultimaBatida.entrada ?: "--:--"}"
            binding.tvSaidaUltimaBatida.text = "Saída: ${ultimaBatida.saida ?: "--:--"}"
        } else {
            // Sem registro, exibe placeholders
            binding.tvDataUltimaBatida.text = "Data: --"
            binding.tvEntradaUltimaBatida.text = "Entrada: --:--"
            binding.tvSaidaUltimaBatida.text = "Saída: --:--"
        }
    }

    private fun showFragment(fragment: Fragment) {
        isFragmentVisible = true
        binding.telaPrincipalLayout.visibility = View.GONE
        binding.fragmentContainer.visibility = View.VISIBLE

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun hideFragment() {
        isFragmentVisible = false
        binding.telaPrincipalLayout.visibility = View.VISIBLE
        binding.fragmentContainer.visibility = View.GONE

        supportFragmentManager.beginTransaction()
            .remove(supportFragmentManager.findFragmentById(R.id.fragment_container) ?: return)
            .commit()
    }

    override fun onBackPressed() {
        if (isFragmentVisible) {
            hideFragment()
        } else {
            super.onBackPressed()
        }
    }
}
