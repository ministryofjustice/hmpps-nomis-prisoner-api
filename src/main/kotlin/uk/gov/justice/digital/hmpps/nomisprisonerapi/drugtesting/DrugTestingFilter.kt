package uk.gov.justice.digital.hmpps.nomisprisonerapi.drugtesting

data class DrugTestingFilter(
  val includedPrisonIds: Set<String>?,
  val excludedPrisonIds: Set<String>?,
) {
  val includedPrisonIdsAsList: List<String> = includedPrisonIds?.toList()?.takeIf { it.isNotEmpty() } ?: listOf("")
  val filterByIncludedPrisonIds: Int = if (includedPrisonIds.isNullOrEmpty()) 0 else 1
  val excludedPrisonIdsAsList: List<String> = (mutableSetOf("ZZGHI") + (excludedPrisonIds ?: emptySet())).toList()
}
