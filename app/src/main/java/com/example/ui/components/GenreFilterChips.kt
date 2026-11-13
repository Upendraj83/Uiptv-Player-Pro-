package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Category
import com.example.data.model.Channel
import com.example.ui.theme.LocalTiweeColors
import com.example.ui.theme.TiweePurple

@Composable
fun GenreFilterChips(
  categories: List<Category>,
  selectedCategoryId: String?,
  onSelectCategory: (String?) -> Unit,
  modifier: Modifier = Modifier,
  channels: List<Channel> = emptyList(),
  contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
  showCounts: Boolean = channels.isNotEmpty(),
) {
  val extColors = LocalTiweeColors.current

  val countsByCategory = remember(channels) {
    if (channels.isEmpty()) {
      emptyMap()
    } else {
      val map = HashMap<String, Int>()
      channels.forEach { channel ->
        channel.categories.forEach { categoryId ->
          val key = categoryId.lowercase()
          map[key] = (map[key] ?: 0) + 1
        }
      }
      map
    }
  }

  val displayedCategories = remember(categories, countsByCategory) {
    if (countsByCategory.isEmpty()) {
      categories
    } else {
      categories
        .filter { (countsByCategory[it.id.lowercase()] ?: 0) > 0 }
        .sortedByDescending { countsByCategory[it.id.lowercase()] ?: 0 }
    }
  }

  val isAllSelected = selectedCategoryId == null

  LazyRow(
    state = rememberLazyListState(),
    contentPadding = contentPadding,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier.testTag("genre_filter_chips_row"),
  ) {
    item(key = "genre_chip_all", contentType = "genre_chip") {
      val allLabel = if (showCounts && channels.isNotEmpty()) {
        "All (${channels.size})"
      } else {
        "All"
      }
      FilterChip(
        selected = isAllSelected,
        onClick = { onSelectCategory(null) },
        label = {
          Text(
            text = allLabel,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
            ),
          )
        },
        leadingIcon = {
          Icon(
            imageVector = if (isAllSelected) Icons.Rounded.Check else Icons.Rounded.LiveTv,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
          containerColor = MaterialTheme.colorScheme.surface,
          labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
          iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          selectedContainerColor = TiweePurple,
          selectedLabelColor = Color.White,
          selectedLeadingIconColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isAllSelected,
          borderColor = extColors.cardBorder,
          selectedBorderColor = TiweePurple,
        ),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("genre_chip_all"),
      )
    }

    items(
      items = displayedCategories,
      key = { "genre_${it.id}" },
      contentType = { "genre_chip" },
    ) { category ->
      val isSelected = selectedCategoryId.equals(category.id, ignoreCase = true)
      val visual = GenreStyleRegistry.visualForCategory(category.id)
      val count = countsByCategory[category.id.lowercase()] ?: 0
      val chipLabel = if (showCounts && count > 0) {
        "${category.name} · $count"
      } else {
        category.name
      }

      FilterChip(
        selected = isSelected,
        onClick = {
          onSelectCategory(if (isSelected) null else category.id)
        },
        label = {
          Text(
            text = chipLabel,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
          )
        },
        leadingIcon = {
          Icon(
            imageVector = if (isSelected) Icons.Rounded.Check else visual.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
          containerColor = MaterialTheme.colorScheme.surface,
          labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
          iconColor = visual.accent,
          selectedContainerColor = TiweePurple,
          selectedLabelColor = Color.White,
          selectedLeadingIconColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
          enabled = true,
          selected = isSelected,
          borderColor = extColors.cardBorder,
          selectedBorderColor = TiweePurple,
        ),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("genre_chip_${category.id}"),
      )
    }
  }
}
