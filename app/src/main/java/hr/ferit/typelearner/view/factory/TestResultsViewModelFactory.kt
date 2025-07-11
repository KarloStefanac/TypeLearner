package hr.ferit.typelearner.view.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import hr.ferit.typelearner.model.repository.ModelRepository
import hr.ferit.typelearner.viewmodel.TestResultsViewModel

class TestResultsViewModelFactory(private val repository: ModelRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TestResultsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TestResultsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}