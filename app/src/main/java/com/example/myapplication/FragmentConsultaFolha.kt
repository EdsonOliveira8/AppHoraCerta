package com.example.myapplication

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class FragmentConsultaFolha : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BatidaAdapter
    private lateinit var btnFiltroPeriodo: Button
    private lateinit var btnExportar: Button
    private lateinit var btnSair: Button
    private lateinit var tvFiltroSelecionado: TextView

    private var listaRegistros: List<RegistroPonto> = emptyList()
    private var filtroAno: Int = 0
    private var filtroMes: Int = 0
    private var usuarioId: Int = -1

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_consulta_folha, container, false)

        usuarioId = arguments?.getInt("usuario_id", -1) ?: -1

        recyclerView = view.findViewById(R.id.recyclerViewBatidas)
        btnFiltroPeriodo = view.findViewById(R.id.btnFiltrarPeriodo)
        btnExportar = view.findViewById(R.id.btnExportar)
        btnSair = view.findViewById(R.id.btnSair)
        tvFiltroSelecionado = view.findViewById(R.id.tvFiltroSelecionado)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val cal = Calendar.getInstance()
        filtroAno = cal.get(Calendar.YEAR)
        filtroMes = cal.get(Calendar.MONTH) + 1

        atualizarFiltroTexto()
        carregarRegistros()

        btnFiltroPeriodo.setOnClickListener {
            abrirDatePicker()
        }

        btnExportar.setOnClickListener {
            exportarFolha()
        }

        btnSair.setOnClickListener {
            activity?.onBackPressed()
        }

        return view
    }

    private fun abrirDatePicker() {
        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, _ ->
                filtroAno = year
                filtroMes = month + 1
                atualizarFiltroTexto()
                carregarRegistros()
            },
            filtroAno,
            filtroMes - 1,
            1
        )
        datePicker.show()
    }

    private fun atualizarFiltroTexto() {
        val meses = arrayOf(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        )
        tvFiltroSelecionado.text = "Filtro: ${meses[filtroMes - 1]} / $filtroAno"
    }

    private fun carregarRegistros() {
        val dbHelper = DatabaseHelper(requireContext())
        if (usuarioId != -1) {
            listaRegistros = dbHelper.getRegistrosPorUsuarioMesAno(usuarioId, filtroAno, filtroMes)
            adapter = BatidaAdapter(requireContext(), listaRegistros)
            recyclerView.adapter = adapter
        }
    }

    private fun exportarFolha() {
        if (listaRegistros.isEmpty()) {
            Toast.makeText(requireContext(), "Nenhum registro para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 tamanho em pontos
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 14f
        var yPosition = 40f

        canvas.drawText("Relatório de Batidas - ${filtroMes}/${filtroAno}", 180f, yPosition, paint)
        yPosition += 40f

        for (registro in listaRegistros) {
            val texto =
                "Data: ${registro.data} | Entrada: ${registro.entrada ?: "-"} | Saída: ${registro.saida ?: "-"}"
            canvas.drawText(texto, 40f, yPosition, paint)
            yPosition += 25f
            if (yPosition > 800f) {
                // Caso tenha muitos registros, só gera a primeira página por enquanto
                break
            }
        }

        document.finishPage(page)

        val fileName = "folha_ponto_${filtroMes}_${filtroAno}.pdf"
        val file = File(requireContext().getExternalFilesDir(null), fileName)

        try {
            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            Toast.makeText(requireContext(), "PDF salvo em:\n${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Erro ao salvar PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
