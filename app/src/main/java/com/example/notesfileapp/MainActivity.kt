package com.example.notesfileapp

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Menampilkan UI Compose utama saat aplikasi dibuka
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    // Memanggil layar yang berisi seluruh logika UI
                    NotesScreen()
                }
            }
        }
    }
}

@Composable
fun NotesScreen(vm: NotesViewModel = viewModel()) {

    // Context untuk Toast, akses storage, dll.
    val ctx = LocalContext.current
    val resolver = ctx.contentResolver

    // Mengambil data dari ViewModel menggunakan StateFlow
    val fileName by vm.fileName.collectAsState()       // nama file
    val content by vm.content.collectAsState()         // isi catatan
    val target by vm.target.collectAsState()           // target penyimpanan (internal/SAF)
    val lastUri by vm.lastExternalUri.collectAsState() // URI terakhir yang dipilih melalui SAF

    // Digunakan untuk membuat file baru menggunakan Storage Access Framework
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain") // tipe file text
    ) { uri: Uri? ->
        uri?.let {
            // Menyimpan izin akses permanen (persisted permission)
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}

            // Simpan URI ke ViewModel
            vm.setExternalUri(it)

            // Menulis data ke file
            vm.saveExternal(resolver)

            Toast.makeText(ctx, "Berkas dibuat & disimpan.", Toast.LENGTH_SHORT).show()
        }
    }

    // Membuka file melalui SAF file picker
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Ambil izin persist untuk file tersebut
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}

            // simpan URI di ViewModel
            vm.setExternalUri(it)

            // baca isi file
            vm.openExternal(resolver)

            Toast.makeText(ctx, "Berkas dibuka.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier.padding(16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Judul aplikasi
        Text(
            text = "Aplikasi Penyimpanan Internal & Eksternal",
            style = MaterialTheme.typography.titleMedium
        )

        // Input nama file
        OutlinedTextField(
            value = fileName,
            onValueChange = vm::setFileName,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nama berkas") }
        )

        // Input konten catatan
        OutlinedTextField(
            value = content,
            onValueChange = vm::setContent,
            modifier = Modifier.fillMaxWidth().height(160.dp),
            label = { Text("Isi catatan") }
        )

        // Pilihan target penyimpanan (internal atau external via SAF)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Simpan ke:")

            // Penyimpanan internal aplikasi
            FilterChip(
                selected = target == NotesViewModel.Target.INTERNAL,
                onClick = { vm.setTarget(NotesViewModel.Target.INTERNAL) },
                label = { Text("Internal") }
            )

            // Penyimpanan eksternal menggunakan SAF
            FilterChip(
                selected = target == NotesViewModel.Target.EXTERNAL_SAF,
                onClick = { vm.setTarget(NotesViewModel.Target.EXTERNAL_SAF) },
                label = { Text("Eksternal (SAF)") }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                when (target) {

                    //Simpan ke Internal
                    NotesViewModel.Target.INTERNAL -> {
                        vm.saveInternal()
                        Toast.makeText(ctx, "Tersimpan di internal.", Toast.LENGTH_SHORT).show()
                    }

                    //Simpan ke External SAF
                    NotesViewModel.Target.EXTERNAL_SAF -> {
                        if (lastUri != null) {
                            // Jika sudah punya file SAF -> update isi file
                            vm.saveExternal(resolver)
                            Toast.makeText(ctx, "Tersimpan ke berkas SAF.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Jika belum, buat file baru
                            val suggested = if (fileName.isNotBlank()) fileName else "catatan.txt"
                            createDocLauncher.launch(suggested)
                        }
                    }
                }
            }) {
                Text("Simpan / Perbarui")
            }

            OutlinedButton(onClick = {
                when (target) {

                    // Buka file di internal
                    NotesViewModel.Target.INTERNAL -> {
                        vm.openInternal()
                        Toast.makeText(ctx, "Dibuka dari internal (jika ada).", Toast.LENGTH_SHORT).show()
                    }

                    // Buka file di SAF
                    NotesViewModel.Target.EXTERNAL_SAF -> {
                        if (lastUri != null) {
                            vm.openExternal(resolver)
                        } else {
                            openDocLauncher.launch(arrayOf("text/plain"))
                        }
                    }
                }
            }) {
                Text("Buka")
            }

            OutlinedButton(onClick = {
                when (target) {

                    // Hapus di Internal
                    NotesViewModel.Target.INTERNAL -> {
                        vm.deleteInternal()
                        Toast.makeText(ctx, "Dihapus dari internal (jika ada).", Toast.LENGTH_SHORT).show()
                    }

                    // Hapus di External
                    NotesViewModel.Target.EXTERNAL_SAF -> {
                        if (lastUri != null) {
                            vm.deleteExternal(resolver)
                            Toast.makeText(ctx, "Berkas SAF dihapus.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "Pilih berkas terlebih dahulu.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }) {
                Text("Hapus")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Menampilkan URI file terakhir yang dipilih dari SAF
        LazyColumn {
            item {
                Text("Uri eksternal terakhir: ${lastUri ?: "(belum dipilih)"}")
            }
        }
    }
}
