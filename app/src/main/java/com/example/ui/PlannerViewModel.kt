package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Note
import com.example.data.PlannerRepository
import com.example.data.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PlannerTab {
    TASKS, NOTES
}

enum class LanguageCode {
    BN, EN
}

class PlannerViewModel(private val repository: PlannerRepository) : ViewModel() {

    // Current app-level language
    val languageState = MutableStateFlow(LanguageCode.BN) // Default to Bengali as requested

    // Selected tab
    val currentTab = MutableStateFlow(PlannerTab.TASKS)

    // Task filters
    val selectedCategoryFilter = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    // Raw data flows
    private val rawTasks = repository.allTasks
    private val rawNotes = repository.allNotes

    // Filtered tasks flow
    val tasksState: StateFlow<List<Task>> = combine(
        rawTasks,
        selectedCategoryFilter,
        searchQuery
    ) { tasks, category, query ->
        tasks.filter { task ->
            val matchesCategory = category == "All" || task.category == category
            val matchesQuery = query.isEmpty() || task.title.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered notes flow
    val notesState: StateFlow<List<Note>> = combine(
        rawNotes,
        searchQuery
    ) { notes, query ->
        notes.filter { note ->
            query.isEmpty() || 
            note.title.contains(query, ignoreCase = true) || 
            note.content.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Task completion progress (Completed vs Total)
    val taskProgress: StateFlow<Pair<Int, Int>> = rawTasks.combine(selectedCategoryFilter) { tasks, category ->
        val filteredTasks = if (category == "All") tasks else tasks.filter { it.category == category }
        val completed = filteredTasks.count { it.isCompleted }
        val total = filteredTasks.size
        Pair(completed, total)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Pair(0, 0)
    )

    // Task operations
    fun addTask(title: String, category: String, priority: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title.trim(),
                    category = category,
                    priority = priority
                )
            )
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun deleteTaskById(id: Int) {
        viewModelScope.launch {
            repository.deleteTaskById(id)
        }
    }

    // Note operations
    fun addNote(title: String, content: String, colorHex: String) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            repository.insertNote(
                Note(
                    title = title.trim(),
                    content = content.trim(),
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun deleteNoteById(id: Int) {
        viewModelScope.launch {
            repository.deleteNoteById(id)
        }
    }

    // Language operations
    fun toggleLanguage() {
        languageState.value = if (languageState.value == LanguageCode.BN) LanguageCode.EN else LanguageCode.BN
    }

    // Translation helper
    fun translate(bn: String, en: String): String {
        return if (languageState.value == LanguageCode.BN) bn else en
    }
}

class PlannerViewModelFactory(private val repository: PlannerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlannerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlannerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
