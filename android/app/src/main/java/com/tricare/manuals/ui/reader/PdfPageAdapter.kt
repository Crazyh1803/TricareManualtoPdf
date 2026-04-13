package com.tricare.manuals.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

/**
 * Renders a PDF file page-by-page using Android's built-in PdfRenderer.
 * Each page is rendered lazily as it scrolls into view.
 */
class PdfPageAdapter(private val file: File) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    init {
        try {
            fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(fileDescriptor!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount(): Int = renderer?.pageCount ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            adjustViewBounds = true
            setPadding(8, 8, 8, 8)
        }
        return PageViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = renderer?.openPage(position) ?: return
        try {
            // Scale to screen width at 2x density for crisp text
            val displayWidth = holder.imageView.resources.displayMetrics.widthPixels - 32
            val scale = displayWidth.toFloat() / page.width
            val bitmapWidth = displayWidth
            val bitmapHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            holder.imageView.setImageBitmap(bitmap)
        } finally {
            page.close()
        }
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        // Free the bitmap memory when the view is recycled
        (holder.imageView.drawable as? android.graphics.drawable.BitmapDrawable)
            ?.bitmap?.recycle()
        holder.imageView.setImageDrawable(null)
    }

    fun release() {
        renderer?.close()
        fileDescriptor?.close()
        renderer = null
        fileDescriptor = null
    }

    class PageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}
