package com.example.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class FragmentCadastrarEmpresa : Fragment() {

    private lateinit var editEmpresa: EditText
    private lateinit var editLatitude: EditText
    private lateinit var editLongitude: EditText
    private lateinit var editRaio: EditText
    private lateinit var btnSalvarEmpresa: Button
    private lateinit var btnPaginaInicial: Button
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_casdastrar_empresa, container, false)

        // Inicializar os campos
        editEmpresa = view.findViewById(R.id.editEmpresa)
        editLatitude = view.findViewById(R.id.editLatitude)
        editLongitude = view.findViewById(R.id.editLongitude)
        editRaio = view.findViewById(R.id.editRaio)
        btnSalvarEmpresa = view.findViewById(R.id.btnSalvarEmpresa)
        btnPaginaInicial = view.findViewById(R.id.btnPaginaInicial)

        dbHelper = DatabaseHelper(requireContext())

        // Ação do botão salvar
        btnSalvarEmpresa.setOnClickListener {
            salvarEmpresa()
        }

        // Ação do botão Página Inicial
        btnPaginaInicial.setOnClickListener {
            activity?.onBackPressed()
        }

        return view
    }

    private fun salvarEmpresa() {
        val nome = editEmpresa.text.toString()
        val latitude = editLatitude.text.toString()
        val longitude = editLongitude.text.toString()
        val raio = editRaio.text.toString()

        if (nome.isNotEmpty() && latitude.isNotEmpty() && longitude.isNotEmpty() && raio.isNotEmpty()) {
            val sucesso = dbHelper.inserirEmpresa(
                nome,
                latitude.toDoubleOrNull() ?: 0.0,
                longitude.toDoubleOrNull() ?: 0.0,
                raio.toDoubleOrNull() ?: 0.0
            )

            if (sucesso) {
                Toast.makeText(requireContext(), "Empresa cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                editEmpresa.text.clear()
                editLatitude.text.clear()
                editLongitude.text.clear()
                editRaio.text.clear()
            } else {
                Toast.makeText(requireContext(), "Erro ao cadastrar empresa.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
        }
    }
}
