package com.downtomark.ui.graph

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.downtomark.DownToMarkApp
import com.downtomark.data.model.Highlight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GraphNode(
    val tag: String,
    val count: Int,
    val highlights: List<HighlightRef>
)

data class HighlightRef(
    val fileUri: String,
    val fileName: String,
    val blockIndex: Int,
    val highlightedText: String,
    val highlightId: String
)

data class GraphEdge(
    val tag1: String,
    val tag2: String,
    val weight: Int
)

data class GraphData(
    val nodes: List<GraphNode> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)

class GraphViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as DownToMarkApp).repository

    private val _graphData = MutableStateFlow(GraphData())
    val graphData: StateFlow<GraphData> = _graphData

    init {
        loadGraphData()
    }

    fun loadGraphData() {
        val index = repository.loadIndex()
        val tagHighlights = mutableMapOf<String, MutableList<HighlightRef>>()
        val coOccurrence = mutableMapOf<Pair<String, String>, Int>()

        for (fileEntry in index.files) {
            val notes = repository.loadFileNotes(fileEntry.notesFileName) ?: continue
            for (highlight in notes.highlights) {
                val tags = highlight.tags
                val ref = HighlightRef(
                    fileUri = fileEntry.uri,
                    fileName = fileEntry.name,
                    blockIndex = highlight.blockIndex,
                    highlightedText = highlight.highlightedText,
                    highlightId = highlight.id
                )

                for (tag in tags) {
                    tagHighlights.getOrPut(tag) { mutableListOf() }.add(ref)
                }

                // Co-occurrence: all pairs of tags on same highlight
                for (i in tags.indices) {
                    for (j in i + 1 until tags.size) {
                        val pair = if (tags[i] < tags[j]) {
                            tags[i] to tags[j]
                        } else {
                            tags[j] to tags[i]
                        }
                        coOccurrence[pair] = (coOccurrence[pair] ?: 0) + 1
                    }
                }
            }
        }

        val nodes = tagHighlights.map { (tag, refs) ->
            GraphNode(tag = tag, count = refs.size, highlights = refs)
        }
        val edges = coOccurrence.map { (pair, weight) ->
            GraphEdge(tag1 = pair.first, tag2 = pair.second, weight = weight)
        }

        _graphData.value = GraphData(nodes = nodes, edges = edges)
    }
}
