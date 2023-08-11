package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetailId
import java.time.LocalDate

@DslMarker
annotation class NonAssociationDetailDslMarker

@NomisDataDslMarker
interface NonAssociationDetailDsl

@Component
class NonAssociationDetailBuilderFactory() {
  fun builder() = NonAssociationDetailBuilder()
}

class NonAssociationDetailBuilder : NonAssociationDetailDsl {
  fun build(
    offender: Offender,
    nsOffender: Offender,
    offenderBooking: OffenderBooking,
    nsOffenderBooking: OffenderBooking,
    nonAssociationReason: NonAssociationReason,
    recipNonAssociationReason: NonAssociationReason,
    nonAssociationType: NonAssociationType,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
    authorisedBy: String?,
    comment: String?,
    nonAssociation: OffenderNonAssociation,
  ): OffenderNonAssociationDetail =
    OffenderNonAssociationDetail(
      id = OffenderNonAssociationDetailId(
        offender = offender,
        nsOffender = nsOffender, 1,
      ),
      offenderBooking = offenderBooking,
      nsOffenderBooking = nsOffenderBooking,
      nonAssociationReason = nonAssociationReason,
      recipNonAssociationReason = recipNonAssociationReason,
      nonAssociationType = nonAssociationType,
      effectiveDate = effectiveDate,
      expiryDate = expiryDate,
      authorisedBy = authorisedBy,
      comment = comment,
      nonAssociation = nonAssociation,
    )

}
