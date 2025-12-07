package com.example.notesfileapp

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesfileapp.data.StorageHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    // Target penyimpanan: internal atau eksternal (SAF)
    enum class Target { INTERNAL, EXTERNAL_SAF }

    // Nama file (diisi user)
    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName

    // Isi catatan (diisi user atau dibaca dari file)
    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content

    // Target penyimpanan (default = internal)
    private val _target = MutableStateFlow(Target.INTERNAL)
    val target: StateFlow<Target> = _target

    // URI terakhir file eksternal (SAF) yang dipilih/dibuat
    private val _lastExternalUri = MutableStateFlow<Uri?>(null)
    val lastExternalUri: StateFlow<Uri?> = _lastExternalUri

    fun setFileName(v: String) { _fileName.value = v }
    fun setContent(v: String) { _content.value = v }
    fun setTarget(t: Target) { _target.value = t }
    fun setExternalUri(u: Uri?) { _lastExternalUri.value = u }

    // Menyimpan file ke internal storage aplikasi
    fun saveInternal() {
        val name = fileName.value.trim()
        if (name.isEmpty()) return   // mencegah error saat nama file kosong

        viewModelScope.launch {
            // Memanggil helper untuk menulis file internal
            StorageHelper.writeInternal(getApplication(), name, content.value)
        }
    }

    // Membaca file dari internal storage
    fun openInternal() {
        val name = fileName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            // Ambil teks dari internal storage
            val txt = StorageHelper.readInternal(getApplication(), name)

            // Jika file ada, isi UI dengan konten file tersebut
            txt?.let { _content.value = it }
        }
    }

    // Menghapus file dari internal storage
    fun deleteInternal() {
        val name = fileName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            StorageHelper.deleteInternal(getApplication(), name)
        }
    }

    // Menyimpan file ke lokasi eksternal menggunakan SAF
    fun saveExternal(contentResolver: ContentResolver) {
        val uri = lastExternalUri.value ?: return  // jika belum ada URI, hentikan

        viewModelScope.launch {
            StorageHelper.writeExternalSAF(contentResolver, uri, content.value)
        }
    }

    // Membuka file eksternal melalui SAF
    fun openExternal(contentResolver: ContentResolver) {
        val uri = lastExternalUri.value ?: return

        viewModelScope.launch {
            val txt = StorageHelper.readExternalSAF(contentResolver, uri)

            // Update isi catatan di UI
            txt?.let { _content.value = it }
        }
    }

    // Menghapus file eksternal melalui SAF
    fun deleteExternal(contentResolver: ContentResolver) {
        val uri = lastExternalUri.value ?: return

        viewModelScope.launch {
            StorageHelper.deleteExternalSAF(contentResolver, uri)

            // Hapus URI karena file sudah tidak ada
            _lastExternalUri.value = null
        }
    }
}
