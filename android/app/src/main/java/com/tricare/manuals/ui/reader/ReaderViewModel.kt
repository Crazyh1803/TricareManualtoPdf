package com.tricare.manuals.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tricare.manuals.data.model.Bookmark
import com.tricare.manuals.data.model.Highlight
import com.tricare.manuals.data.model.Section
import com.tricare.manuals.data.repository.ManualRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tricare.manuals.data.db.HighlightDao

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: ManualRepository,
    private val highlightDao: HighlightDao
) : ViewModel() {

    private val _sections = MutableStateFlow<List<Section>>(emptyList())
    val sections: StateFlow<List<Section>> = _sections.asStateFlow()

    private val _bookmarks = MutableStateFlow<List<Bookmark>>(emptyList())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    private var manualCode: String = ""

    fun init(code: String) {
        manualCode = code
        viewModelScope.launch {
            val manual = repository.getManual(code)
            manual?.downloadedChange?.let { change ->
                repository.getSections(code, change).collect { sections ->
                    _sections.value = sections
                }
            }
        }
        viewModelScope.launch {
            repository.getBookmarks(code).collect { bookmarks ->
                _bookmarks.value = bookmarks
            }
        }
    }

    fun addBookmark(sectionFilename: String, sectionTitle: String, scrollY: Int) {
        viewModelScope.launch {
            repository.addBookmark(
                Bookmark(
                    manualCode = manualCode,
                    sectionFilename = sectionFilename,
                    sectionTitle = sectionTitle,
                    scrollY = scrollY
                )
            )
        }
    }

    fun removeBookmark(id: Long) {
        viewModelScope.launch {
            repository.removeBookmark(id)
        }
    }

    fun addHighlight(
        sectionFilename: String,
        start: Int,
        end: Int,
        text: String,
        color: String
    ) {
        viewModelScope.launch {
            highlightDao.insertHighlight(
                Highlight(
                    manualCode = manualCode,
                    sectionFilename = sectionFilename,
                    startOffset = start,
                    endOffset = end,
                    selectedText = text,
                    color = color
                )
            )
        }
    }
}
