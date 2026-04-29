package com.vetcarnet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vetcarnet.domain.model.Animal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimalCard(
    animal: Animal,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val daysUntilVaccin = animal.dateProchainVaccin?.let {
        ChronoUnit.DAYS.between(today, it)
    }
    val isVaccinUrgent = daysUntilVaccin != null && daysUntilVaccin <= 1
    val isVaccinOverdue = daysUntilVaccin != null && daysUntilVaccin < 0

    val cardColors = when {
        isVaccinOverdue -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
        isVaccinUrgent -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
        else -> CardDefaults.cardColors()
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Photo miniature circulaire
            if (animal.photoUrl != null) {
                AsyncImage(
                    model = animal.photoUrl,
                    contentDescription = "Photo de ${animal.nom}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Informations
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = animal.nom,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${animal.espece.label}${if (animal.race.isNotBlank()) " · ${animal.race}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "👤 ${animal.nomProprietaire}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Badge vaccin
                animal.dateProchainVaccin?.let { date ->
                    Spacer(Modifier.height(4.dp))
                    VaccinBadge(date = date, daysUntil = daysUntilVaccin ?: 0)
                }
            }

            // Actions
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Modifier",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Supprimer",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VaccinBadge(date: LocalDate, daysUntil: Long) {
    val (containerColor, contentColor, label) = when {
        daysUntil < 0 -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "En retard · ${date.format(DATE_FMT)}"
        )
        daysUntil == 0L -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "Vaccin AUJOURD'HUI !"
        )
        daysUntil == 1L -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            "Vaccin DEMAIN · ${date.format(DATE_FMT)}"
        )
        daysUntil <= 7 -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Vaccin dans $daysUntil j · ${date.format(DATE_FMT)}"
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Prochain vaccin · ${date.format(DATE_FMT)}"
        )
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(Icons.Outlined.Vaccines, contentDescription = null, modifier = Modifier.size(12.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun VaccinationAlertBanner(
    animaux: List<Animal>,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isToday) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = if (isToday) MaterialTheme.colorScheme.onErrorContainer
                       else MaterialTheme.colorScheme.onTertiaryContainer

    Surface(
        color = color,
        contentColor = contentColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (isToday) Icons.Default.Warning else Icons.Default.Notifications,
                contentDescription = null
            )
            Column {
                Text(
                    text = if (isToday) "🚨 Vaccination prévue AUJOURD'HUI"
                           else "⚠️ Vaccination prévue DEMAIN",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = animaux.joinToString(", ") { it.nom },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
