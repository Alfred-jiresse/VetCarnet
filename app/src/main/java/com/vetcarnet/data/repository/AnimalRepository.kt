package com.vetcarnet.data.repository

import android.content.Context
import android.net.Uri
import com.vetcarnet.data.remote.AnimalDto
import com.vetcarnet.data.remote.SupabaseClient
import com.vetcarnet.data.remote.toDto
import com.vetcarnet.domain.model.Animal
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

interface AnimalRepository {
    suspend fun getAllAnimaux(): Result<List<Animal>>
    suspend fun getAnimalById(id: String): Result<Animal>
    suspend fun createAnimal(animal: Animal): Result<Animal>
    suspend fun updateAnimal(animal: Animal): Result<Animal>
    suspend fun deleteAnimal(id: String): Result<Unit>
    suspend fun uploadPhoto(context: Context, uri: Uri): Result<String>
}

class AnimalRepositoryImpl : AnimalRepository {

    private val supabase = SupabaseClient.client
    private val TABLE = "dossiers_animaux"
    private val BUCKET = "animal-photos"

    override suspend fun getAllAnimaux(): Result<List<Animal>> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.from(TABLE)
                .select()
                .decodeList<AnimalDto>()
                .map { it.toDomain() }
        }
    }

    override suspend fun getAnimalById(id: String): Result<Animal> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.from(TABLE)
                .select(Columns.ALL) { filter { eq("id", id) } }
                .decodeSingle<AnimalDto>()
                .toDomain()
        }
    }

    override suspend fun createAnimal(animal: Animal): Result<Animal> = withContext(Dispatchers.IO) {
        runCatching {
            val dto = animal.toDto().copy(id = UUID.randomUUID().toString())
            supabase.from(TABLE)
                .insert(dto) { select() }
                .decodeSingle<AnimalDto>()
                .toDomain()
        }
    }

    override suspend fun updateAnimal(animal: Animal): Result<Animal> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.from(TABLE)
                .update(animal.toDto()) {
                    select()
                    filter { eq("id", animal.id) }
                }
                .decodeSingle<AnimalDto>()
                .toDomain()
        }
    }

    override suspend fun deleteAnimal(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            supabase.from(TABLE)
                .delete { filter { eq("id", id) } }
            Unit
        }
    }

    override suspend fun uploadPhoto(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw Exception("Impossible de lire l'image")
                val fileName = "photo_${UUID.randomUUID()}.jpg"

                supabase.storage.from(BUCKET).upload(
                    path = fileName,
                    data = bytes
                )

                // Retourner l'URL publique
                supabase.storage.from(BUCKET).publicUrl(fileName)
            }
        }
}
