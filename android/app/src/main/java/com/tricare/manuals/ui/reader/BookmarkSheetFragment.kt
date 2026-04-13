package com.tricare.manuals.ui.reader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tricare.manuals.R
import com.tricare.manuals.data.model.Bookmark
import com.tricare.manuals.databinding.FragmentBookmarkSheetBinding
import com.tricare.manuals.databinding.ItemBookmarkBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BookmarkSheetFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "BookmarkSheetFragment"
        private const val ARG_MANUAL_CODE = "manual_code"

        fun newInstance(manualCode: String) = BookmarkSheetFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_MANUAL_CODE, manualCode)
            }
        }
    }

    private var _binding: FragmentBookmarkSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReaderViewModel by activityViewModels()
    private lateinit var adapter: BookmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookmarkSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = BookmarkAdapter(
            onItemClick = { bookmark ->
                // Navigate reader to the bookmark's section
                val activity = requireActivity() as? ReaderActivity
                val sections = viewModel.sections.value
                val sectionIndex = sections.indexOfFirst { it.filename == bookmark.sectionFilename }
                if (sectionIndex >= 0) {
                    activity?.let {
                        val layoutManager = it.binding.recyclerSections.layoutManager as? LinearLayoutManager
                        layoutManager?.scrollToPosition(sectionIndex)
                    }
                }
                dismiss()
            }
        )

        binding.recyclerBookmarks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BookmarkSheetFragment.adapter
        }

        // Swipe to delete
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val bookmark = adapter.currentList[viewHolder.adapterPosition]
                viewModel.removeBookmark(bookmark.id)
            }
        }).attachToRecyclerView(binding.recyclerBookmarks)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bookmarks.collect { bookmarks ->
                adapter.submitList(bookmarks)
                binding.tvNoBookmarks.visibility = if (bookmarks.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class BookmarkAdapter(
    private val onItemClick: (Bookmark) -> Unit
) : ListAdapter<Bookmark, BookmarkAdapter.BookmarkViewHolder>(BookmarkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemBookmarkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookmarkViewHolder(
        private val binding: ItemBookmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bookmark: Bookmark) {
            binding.tvBookmarkTitle.text = bookmark.sectionTitle
            val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            binding.tvBookmarkTimestamp.text = sdf.format(Date(bookmark.createdAt))
            binding.root.setOnClickListener { onItemClick(bookmark) }
        }
    }

    class BookmarkDiffCallback : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark) = oldItem == newItem
    }
}
