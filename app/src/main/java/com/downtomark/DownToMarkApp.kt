package com.downtomark

import android.app.Application
import com.downtomark.data.repository.NotesRepository

class DownToMarkApp : Application() {

    lateinit var repository: NotesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = NotesRepository(this)
    }
}
