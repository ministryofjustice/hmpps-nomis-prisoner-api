package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributes
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPhysicalAttributesRepository

@DslMarker
annotation class OffenderPhysicalAttributesDslMarker

@NomisDataDslMarker
interface OffenderPhysicalAttributesDsl

@Component
class OffenderPhysicalAttributesBuilderFactory(
  private val repository: OffenderPhysicalAttributesRepository,
) {
  fun builder(): OffenderPhysicalAttributesBuilder {
    return OffenderPhysicalAttributesBuilder(repository)
  }
}

class OffenderPhysicalAttributesBuilder(
  private val repository: OffenderPhysicalAttributesRepository,
) : OffenderPhysicalAttributesDsl {

  fun build(
    offenderBooking: OffenderBooking,
    sequence: Long? = null,
    heightCentimetres: Int?,
    heightFeet: Int?,
    heightInches: Int?,
    weightKilograms: Int?,
    weightPounds: Int?,
  ): OffenderPhysicalAttributes =
    OffenderPhysicalAttributes(
      id = OffenderPhysicalAttributeId(
        offenderBooking = offenderBooking,
        sequence = sequence ?: ((offenderBooking.physicalAttributes.maxByOrNull { it.id.sequence }?.id?.sequence ?: 0) + 1),
      ),
      heightCentimetres = heightCentimetres,
      heightFeet = heightFeet,
      heightInches = heightInches,
      weightKilograms = weightKilograms,
      weightPounds = weightPounds,
    )
      .let { repository.save(it) }
}
