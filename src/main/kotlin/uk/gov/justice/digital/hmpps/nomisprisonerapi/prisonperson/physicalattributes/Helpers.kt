package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.physicalattributes

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributes
import kotlin.math.roundToInt

// Note that the OffenderPhysicalAttributes extension functions below haven't been added to the class as getters and setters
// because they only make sense in the context of this service. For example if JPA started using these methods then
// OffenderPhysicalAttributes wouldn't represent the values found in NOMIS.
internal fun OffenderPhysicalAttributes.getHeightInCentimetres() = // Take height in cm if it exists because the data is more accurate (being a smaller unit than inches)
  if (heightCentimetres != null) {
    heightCentimetres
  } else {
    heightFeet?.let { ((it * 12) + (heightInches ?: 0)) * 2.54 }?.roundToInt()
  }

internal fun OffenderPhysicalAttributes.setHeightInCentimetres(value: Int?) {
  heightCentimetres = value
  val inches = heightCentimetres?.div(2.54)
  heightFeet = inches?.div(12)?.toInt()
  heightInches = inches?.rem(12)?.roundToInt()
}

internal fun OffenderPhysicalAttributes.getWeightInKilograms() = // Take weight in lb and convert if it exists because the data is more accurate (being a smaller unit than kg). See the unit tests for an example explaining why.
  if (weightPounds != null) {
    weightPounds!!.let { (it * 0.453592) }.roundToInt()
  } else {
    weightKilograms
  }

internal fun OffenderPhysicalAttributes.setWeightInKilograms(value: Int?) {
  weightKilograms = value
  weightPounds = weightKilograms?.let { (it / 0.453592) }?.roundToInt()
}
