package com.vetcarnet.domain.model

import java.time.LocalDate

data class Animal(
    val id: String = "",
    val nom: String = "",
    val espece: Espece = Espece.CHIEN,
    val race: String = "",
    val nomProprietaire: String = "",
    val telephoneProprietaire: String = "",
    val dateNaissance: LocalDate? = null,
    val dateProchainVaccin: LocalDate? = null,
    val photoUrl: String? = null,
    val notes: String = ""
)

enum class Espece(val label: String) {
    CHIEN("Chien"),
    CHAT("Chat"),
    PORC("Porc"),
    LAPIN("Lapin"),
    OISEAU("Oiseau"),
    REPTILE("Reptile"),
    BOVINS("Bovins"),
    AUTRE("Autre")
}
