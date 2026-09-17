package com.example.radarcamera.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ScreenModelFactory<T : ViewModel>(private val createModel: () -> T) : ViewModelProvider.Factory {
    override fun <V : ViewModel> create(modelClass: Class<V>): V = modelClass.cast(createModel())!!
}
