package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetailId
import java.time.LocalDate

@DslMarker
annotation class NonAssociationDetailDslMarker

@NomisDataDslMarker
interface NonAssociationDetailDsl

@Component
class NonAssociationDetailBuilderFactory {
  fun builder() = NonAssociationDetailBuilder()
}

class NonAssociationDetailBuilder : NonAssociationDetailDsl {
  fun build(
    offenderId: Long,
    nsOffenderId: Long,
    offenderBookingId: Long,
    nsOffenderBookingId: Long,
    typeSeq: Int,
    nonAssociationReason: NonAssociationReason,
    recipNonAssociationReason: NonAssociationReason?,
    nonAssociationType: NonAssociationType,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
    authorisedBy: String?,
    modifiedBy: String?,
    comment: String?,
    nonAssociation: OffenderNonAssociation,
  ): OffenderNonAssociationDetail = OffenderNonAssociationDetail(
    id = OffenderNonAssociationDetailId(
      offenderId = offenderId,
      nsOffenderId = nsOffenderId,
      typeSeq,
    ),
    offenderBookingId = offenderBookingId,
    nsOffenderBookingId = nsOffenderBookingId,
    nonAssociationReason = nonAssociationReason,
    recipNonAssociationReason = recipNonAssociationReason,
    nonAssociationType = nonAssociationType,
    effectiveDate = effectiveDate,
    expiryDate = expiryDate,
    authorisedBy = authorisedBy,
    modifiedBy = modifiedBy,
    comment = comment,
    nonAssociation = nonAssociation,
  )
}
