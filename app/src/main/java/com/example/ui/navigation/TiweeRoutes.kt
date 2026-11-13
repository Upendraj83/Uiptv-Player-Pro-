package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object MenuRoute

@Serializable
data object CountryBrowseRoute

@Serializable
data class CountryChannelsRoute(
  val code: String,
  val name: String,
  val flag: String,
)

@Serializable
data object CategoryBrowseRoute

@Serializable
data class CategoryChannelsRoute(
  val id: String,
  val name: String,
)

@Serializable
data object AllChannelsRoute

@Serializable
data object SavedChannelsRoute

@Serializable
data object SettingsRoute
