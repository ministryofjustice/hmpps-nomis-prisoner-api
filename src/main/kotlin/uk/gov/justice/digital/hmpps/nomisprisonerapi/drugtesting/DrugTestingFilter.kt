package uk.gov.justice.digital.hmpps.nomisprisonerapi.drugtesting

data class DrugTestingFilter(
  val includedPrisonIds: Set<String>?,
  val excludedPrisonIds: Set<String>?,
) {
  val includedPrisonIdsAsList: List<String>? = includedPrisonIds?.toList()
  val excludedPrisonIdsAsList: List<String> = (mutableSetOf("ZZGHI") + (excludedPrisonIds ?: emptySet())).toList()
}
