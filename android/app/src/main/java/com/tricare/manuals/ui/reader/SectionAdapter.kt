package com.tricare.manuals.ui.reader

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tricare.manuals.data.model.Section
import com.tricare.manuals.databinding.ItemSectionBinding
import io.noties.markwon.Markwon
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin

class SectionAdapter(
    private val context: Context
) : ListAdapter<Section, SectionAdapter.SectionViewHolder>(SectionDiffCallback()) {

    private val markwon: Markwon = Markwon.builder(context)
        .usePlugin(HtmlPlugin.create())
        .usePlugin(ImagesPlugin.create())
        .build()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val binding = ItemSectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SectionViewHolder(
        private val binding: ItemSectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: Section) {
            binding.tvSectionTitle.text = section.title
            val content = section.contentMd ?: ""
            if (content.isNotBlank()) {
                markwon.setMarkdown(binding.tvSectionContent, content)
            } else {
                binding.tvSectionContent.text = "(No content)"
            }
        }
    }

    class SectionDiffCallback : DiffUtil.ItemCallback<Section>() {
        override fun areItemsTheSame(oldItem: Section, newItem: Section) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Section, newItem: Section) = oldItem == newItem
    }
}
