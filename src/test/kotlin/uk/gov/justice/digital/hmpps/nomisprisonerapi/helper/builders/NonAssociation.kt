package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class NonAssociationDslMarker

@NomisDataDslMarker
interface NonAssociationDsl {
  @NonAssociationDetailDslMarker
  fun nonAssociationDetail(
    nonAssociationReason: NonAssociationReason,
    recipNonAssociationReason: NonAssociationReason,
    nonAssociationType: NonAssociationType,
    effectiveDate: LocalDate,
    expiryDate: LocalDate? = null,
    authorisedBy: String? = null,
    comment: String? = null,
  ): OffenderNonAssociationDetail
}

@Component
class NonAssociationBuilderRepository(
  private val nonAssociationReasonRepository: ReferenceCodeRepository<NonAssociationReason>? = null,
  private val nonAssociationTypeRepository: ReferenceCodeRepository<NonAssociationType>? = null,
) {
  fun lookupNonAssociationReason(code: String): NonAssociationReason =
    nonAssociationReasonRepository?.findByIdOrNull(ReferenceCode.Pk(NonAssociationReason.DOMAIN, code))!!

  fun lookupNonAssociationType(code: String): NonAssociationType =
    nonAssociationTypeRepository?.findByIdOrNull(ReferenceCode.Pk(NonAssociationType.DOMAIN, code))!!
}

@Component
class NonAssociationBuilderFactory(
  private val repository: NonAssociationBuilderRepository? = null,
  private val nonAssociationDetailBuilderFactory: NonAssociationDetailBuilderFactory = NonAssociationDetailBuilderFactory(),
) {
  fun builder(): NonAssociationBuilder {
    return NonAssociationBuilder(
      repository,
      nonAssociationDetailBuilderFactory,
    )
  }
}

class NonAssociationBuilder(
  val repository: NonAssociationBuilderRepository? = null,
  val nonAssociationDetailBuilderFactory: NonAssociationDetailBuilderFactory,
) : NonAssociationDsl {

  private lateinit var nonAssociation: OffenderNonAssociation

  fun build(
    offender: Offender,
    nsOffender: Offender,
    offenderBooking: OffenderBooking,
    nsOffenderBooking: OffenderBooking,
    nonAssociationReason: NonAssociationReason,
    recipNonAssociationReason: NonAssociationReason,
  ): OffenderNonAssociation =
    OffenderNonAssociation(
      OffenderNonAssociationId(
        offender = offender,
        nsOffender = nsOffender,
      ),
      offenderBooking = offenderBooking,
      nsOffenderBooking = nsOffenderBooking,
      nonAssociationReason = nonAssociationReason,
      recipNonAssociationReason = recipNonAssociationReason,
    )
      .also { nonAssociation = it }

  override fun nonAssociationDetail(
    nonAssociationReason: NonAssociationReason,
    recipNonAssociationReason: NonAssociationReason,
    nonAssociationType: NonAssociationType,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
    authorisedBy: String?,
    comment: String?,
  ) = nonAssociationDetailBuilderFactory.builder().build(
    offender = nonAssociation.id.offender,
    nsOffender = nonAssociation.id.nsOffender,
    offenderBooking = nonAssociation.id.offender.latestBooking(),
    nsOffenderBooking = nonAssociation.id.nsOffender.latestBooking(),
    nonAssociationReason = nonAssociationReason,
    recipNonAssociationReason = recipNonAssociationReason,
    nonAssociationType = nonAssociationType,
    effectiveDate = effectiveDate,
    expiryDate = expiryDate,
    authorisedBy = authorisedBy,
    comment = comment,
    nonAssociation = nonAssociation,
  )
    .also {
      nonAssociation.offenderNonAssociationDetails += it
    }
}
