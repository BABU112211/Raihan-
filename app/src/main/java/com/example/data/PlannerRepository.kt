package com.example.data

import kotlinx.coroutines.flow.Flow

class PlannerRepository(
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun deleteTaskById(id: Int) {
        taskDao.deleteById(id)
    }

    suspend fun insertNote(note: Note) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun deleteNoteById(id: Int) {
        noteDao.deleteById(id)
    }
}
