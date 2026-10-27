package com.example.desafio3dsm.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.desafio3dsm.R
import com.example.desafio3dsm.model.Recurso

/**
 * Adapter de RecyclerView para renderizar las tarjetas de Recursos Educativos con Favoritos,
 * Calificación y opciones CRUD exclusivas de Docentes.
 */
class RecursosAdapter(
    private var listaRecursos: List<Recurso> = emptyList(),
    private var listaFavoritosIds: List<String> = emptyList(),
    private var esDocente: Boolean = false,
    private val onFavoritoClick: ((Recurso) -> Unit)? = null,
    private val onCalificarClick: ((Recurso) -> Unit)? = null,
    private val onEditarClick: ((Recurso) -> Unit)? = null,
    private val onEliminarClick: ((Recurso) -> Unit)? = null,
    private val onItemClick: ((Recurso) -> Unit)? = null
) : RecyclerView.Adapter<RecursosAdapter.RecursoViewHolder>() {

    fun actualizarLista(nuevaLista: List<Recurso>) {
        listaRecursos = nuevaLista
        notifyDataSetChanged()
    }

    fun actualizarFavoritos(nuevosFavoritos: List<String>) {
        listaFavoritosIds = nuevosFavoritos
        notifyDataSetChanged()
    }

    fun actualizarModoDocente(isDocente: Boolean) {
        esDocente = isDocente
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
        private val btnFavorito: ImageButton = itemView.findViewById(R.id.btnFavorito)
        private val tvCategoria: TextView = itemView.findViewById(R.id.tvCategoria)
        private val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        private val btnCalificar: Button = itemView.findViewById(R.id.btnCalificar)
        private val btnAbrirEnlace: Button = itemView.findViewById(R.id.btnAbrirEnlace)

        private val btnEditarRecurso: Button = itemView.findViewById(R.id.btnEditarRecurso)
        private val btnEliminarRecurso: Button = itemView.findViewById(R.id.btnEliminarRecurso)

        fun bind(recurso: Recurso) {
            val context = itemView.context

            tvTitulo.text = recurso.titulo ?: context.getString(R.string.text_no_title)
            tvDescripcion.text = recurso.descripcion ?: context.getString(R.string.text_no_description)
            tvTipoBadge.text = recurso.tipo ?: recurso.categoria ?: context.getString(R.string.filter_all)

            val tipoRecurso = recurso.tipo ?: recurso.categoria ?: "General"
            tvCategoria.text = context.getString(R.string.label_category, tipoRecurso)

            val rating = recurso.obtenerCalificacionPromedio()
            val totalVotos = recurso.obtenerTotalVotos()
            tvCalificacion.text = context.getString(R.string.rating_format_with_count, rating, totalVotos)

            // Estado de Favorito con tintes claros: Gris si no está seleccionado, Naranja brillante si es favorito
            val recursoId = recurso.id ?: ""
            val esFavorito = listaFavoritosIds.contains(recursoId)

            if (esFavorito) {
                btnFavorito.setImageResource(android.R.drawable.btn_star_big_on)
                ImageViewCompat.setImageTintList(btnFavorito, ColorStateList.valueOf(Color.parseColor("#F57C00")))
                btnFavorito.contentDescription = context.getString(R.string.btn_favorite_remove)
            } else {
                btnFavorito.setImageResource(android.R.drawable.btn_star_big_off)
                ImageViewCompat.setImageTintList(btnFavorito, ColorStateList.valueOf(Color.parseColor("#9E9E9E")))
                btnFavorito.contentDescription = context.getString(R.string.btn_favorite_add)
            }

            // Visibilidad de Botones Docente (Editar y Eliminar)
            if (esDocente) {
                btnEditarRecurso.visibility = View.VISIBLE
                btnEliminarRecurso.visibility = View.VISIBLE
            } else {
                btnEditarRecurso.visibility = View.GONE
                btnEliminarRecurso.visibility = View.GONE
            }

            // Cargar imagen de recurso (Soporte para ad1, ap2, mv3 y URLs dinámicas)
            val rawImg = recurso.imagen ?: ""
            val imgUrl = when {
                rawImg.contains("ad1", ignoreCase = true) || recurso.id == "1" -> "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=400"
                rawImg.contains("ap2", ignoreCase = true) || recurso.id == "2" -> "https://images.unsplash.com/photo-1503676260728-1c00da094a0b?w=400"
                rawImg.contains("mv3", ignoreCase = true) || recurso.id == "3" -> "https://images.unsplash.com/photo-1524178232363-1fb2b075b655?w=400"
                else -> rawImg
            }

            if (imgUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(imgUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(ivImagenRecurso)
            } else {
                ivImagenRecurso.setImageResource(R.drawable.ic_launcher_background)
            }

            // Acción Abrir Enlace
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

            btnFavorito.setOnClickListener {
                onFavoritoClick?.invoke(recurso)
            }

            btnCalificar.setOnClickListener {
                onCalificarClick?.invoke(recurso)
            }

            btnEditarRecurso.setOnClickListener {
                onEditarClick?.invoke(recurso)
            }

            btnEliminarRecurso.setOnClickListener {
                onEliminarClick?.invoke(recurso)
            }

            itemView.setOnClickListener {
                onItemClick?.invoke(recurso)
            }
        }
    }
}
