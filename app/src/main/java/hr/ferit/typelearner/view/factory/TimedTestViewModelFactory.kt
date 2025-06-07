package hr.ferit.typelearner.view.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.viewmodel.TestsViewModel

class TimedTestViewModelFactory(private val repository: ModelRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TestsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}