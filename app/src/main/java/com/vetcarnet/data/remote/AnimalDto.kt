package com.vetcarnet.data.remote

import com.vetcarnet.domain.model.Animal
import com.vetcarnet.domain.model.Espece
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class AnimalDto(
    val id: String = "",
    val nom: String = "",
    val espece: String = "CHIEN",
    val race: String = "",
    @SerialName("nom_proprietaire")
    val nomProprietaire: String = "",
    @SerialName("telephone_proprietaire")
    val telephoneProprietaire: String = "",
    @SerialName("date_naissance")
    val dateNaissance: String? = null,
    @SerialName("date_prochain_vaccin")
    val dateProchainVaccin: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    val notes: String = ""
) {
    fun toDomain(): Animal = Animal(
        id = id,
        nom = nom,
        espece = Espece.entries.firstOrNull { it.name == espece } ?: Espece.CHIEN,
        race = race,
        nomProprietaire = nomProprietaire,
        telephoneProprietaire = telephoneProprietaire,
        dateNaissance = dateNaissance?.let { LocalDate.parse(it) },
        dateProchainVaccin = dateProchainVaccin?.let { LocalDate.parse(it) },
        photoUrl = photoUrl,
        notes = notes
    )
}

fun Animal.toDto(): AnimalDto = AnimalDto(
    id = id,
    nom = nom,
    espece = espece.name,
    race = race,
    nomProprietaire = nomProprietaire,
    telephoneProprietaire = telephoneProprietaire,
    dateNaissance = dateNaissance?.toString(),
    dateProchainVaccin = dateProchainVaccin?.toString(),
    photoUrl = photoUrl,
    notes = notes
)
