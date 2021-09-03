package com.github.danishjamal104.notes.util

sealed class DataState<out T> {
    data class Success<out T>(val data: T) : DataState<T>()
    data class Error(val reason: String) : DataState<Nothing>()
    object Loading : DataState<Nothing>()
}