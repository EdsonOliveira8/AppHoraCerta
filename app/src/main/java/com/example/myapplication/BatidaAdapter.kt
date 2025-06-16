package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class RegistroPonto(
    val id: Int,
    val usuarioId: Int,
    val data: String,
    val entrada: String?,
    val saida: String?,
    val latitudeEntrada: Double?,
    val longitudeEntrada: Double?,
    val latitudeSaida: Double?,
    val longitudeSaida: Double?
)

class BatidaAdapter(
    private val context: Context,
    private var registros: List<RegistroPonto>
) : RecyclerView.Adapter<BatidaAdapter.BatidaViewHolder>() {

    inner class BatidaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvData: TextView = itemView.findViewById(R.id.tvData)
        val tvEntrada: TextView = itemView.findViewById(R.id.tvEntrada)
        val tvSaida: TextView = itemView.findViewById(R.id.tvSaida)
        val btnVerLocalizacao: Button = itemView.findViewById(R.id.btnVerLocalizacao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BatidaViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_batida, parent, false)
        return BatidaViewHolder(view)
    }

    override fun onBindViewHolder(holder: BatidaViewHolder, position: Int) {
        val registro = registros[position]

        holder.tvData.text = "📅 Data: ${registro.data}"
        holder.tvEntrada.text = "⏰ Entrada: ${registro.entrada ?: "-"}"
        holder.tvSaida.text = "⏰ Saída: ${registro.saida ?: "-"}"

        if (registro.latitudeEntrada != null && registro.longitudeEntrada != null) {
            holder.btnVerLocalizacao.visibility = View.VISIBLE
            holder.btnVerLocalizacao.setOnClickListener {
                val geoUri = Uri.parse("geo:${registro.latitudeEntrada},${registro.longitudeEntrada}?q=${registro.latitudeEntrada},${registro.longitudeEntrada}(Entrada)")
                val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(mapIntent)
                } else {
                    // fallback para abrir no navegador
                    val webUri = Uri.parse("https://maps.google.com/?q=${registro.latitudeEntrada},${registro.longitudeEntrada}")
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                    context.startActivity(webIntent)
                }
            }
        } else {
            holder.btnVerLocalizacao.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = registros.size

    // Função para atualizar a lista de registros
    fun updateRegistros(novosRegistros: List<RegistroPonto>) {
        registros = novosRegistros
        notifyDataSetChanged()
    }
}
