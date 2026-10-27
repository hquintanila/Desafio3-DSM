package com.example.desafio3dsm.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.desafio3dsm.R
import com.example.desafio3dsm.model.Recurso

/**
 * Adapter de RecyclerView para renderizar las tarjetas de Recursos Educativos.
 */
class RecursosAdapter(
    private var listaRecursos: List<Recurso> = emptyList(),
    private val onItemClick: ((Recurso) -> Unit)? = null
) : RecyclerView.Adapter<RecursosAdapter.RecursoViewHolder>() {

    fun actualizarLista(nuevaLista: List<Recurso>) {
        listaRecursos = nuevaLista
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecursoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recurso, parent, false)
        return RecursoViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecursoViewHolder, position: Int) {
        holder.bind(listaRecursos[position])
    }

    override fun getItemCount(): Int = listaRecursos.size

    inner class RecursoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImagenRecurso: ImageView = itemView.findViewById(R.id.ivImagenRecurso)
        private val tvTipoBadge: TextView = itemView.findViewById(R.id.tvTipoBadge)
        private val tvCalificacion: TextView = itemView.findViewById(R.id.tvCalificacion)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        private val tvUrl: TextView = itemView.findViewById(R.id.tvUrl)
        private val btnAbrirEnlace: Button = itemView.findViewById(R.id.btnAbrirEnlace)

        fun bind(recurso: Recurso) {
            val context = itemView.context

            tvTitulo.text = recurso.titulo ?: context.getString(R.string.text_no_title)
            tvDescripcion.text = recurso.descripcion ?: context.getString(R.string.text_no_description)
            tvTipoBadge.text = recurso.tipo ?: recurso.categoria ?: context.getString(R.string.filter_all)
            tvUrl.text = recurso.url ?: context.getString(R.string.text_no_url)

            val calif = recurso.calificacion ?: 4.5
            tvCalificacion.text = context.getString(R.string.rating_format, calif)

            // Cargar imagen de recurso con Glide
            val imgUrl = recurso.imagen
            if (!imgUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(imgUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(ivImagenRecurso)
            } else {
                ivImagenRecurso.setImageResource(R.drawable.ic_launcher_background)
            }

            val openUrlAction = View.OnClickListener {
                val urlStr = recurso.url
                if (!urlStr.isNullOrEmpty()) {
                    try {
                        val validUrl = if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                            "https://$urlStr"
                        } else {
                            urlStr
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(validUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
                    }
                }
            }

            btnAbrirEnlace.setOnClickListener(openUrlAction)
            tvUrl.setOnClickListener(openUrlAction)

            itemView.setOnClickListener {
                onItemClick?.invoke(recurso)
            }
        }
    }
}
