package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NonAssociationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationDetailId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderNonAssociationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.findRootByNomisId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.NonAssociationSpecification
import java.lang.RuntimeException
import java.time.LocalDate

@Service
@Transactional
class NonAssociationService(
  private val offenderRepository: OffenderRepository,
  private val offenderNonAssociationRepository: OffenderNonAssociationRepository,
  private val reasonRepository: ReferenceCodeRepository<NonAssociationReason>,
  private val typeRepository: ReferenceCodeRepository<NonAssociationType>,
  private val telemetryClient: TelemetryClient,
) {
  fun createNonAssociation(dto: CreateNonAssociationRequest): CreateNonAssociationResponse {
    val offender = offenderRepository.findRootByNomisId(dto.offenderNo)
      ?: throw BadDataException("Offender with nomsId=${dto.offenderNo} not found")
    val nsOffender = offenderRepository.findRootByNomisId(dto.nsOffenderNo)
      ?: throw BadDataException("NS Offender with nomsId=${dto.nsOffenderNo} not found")
    val reason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.reason))
      ?: throw BadDataException("Reason with code=${dto.reason} does not exist")
    val recipReason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.recipReason))
      ?: throw BadDataException("Reciprocal reason with code=${dto.recipReason} does not exist")

    if (dto.effectiveDate.isAfter(LocalDate.now())) {
      throw BadDataException("Effective date must not be in the future")
    }

    val existing = offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(offender = offender, nsOffender = nsOffender),
    )

    var typeSequence = 1

    if (existing != null) {
      if (existing.getOpenNonAssociationDetail() != null) {
        throw BadDataException("Non-association already exists for offender=${dto.offenderNo} and nsOffender=${dto.nsOffenderNo}")
      }

      val otherExisting = offenderNonAssociationRepository.findByIdOrNull(
        OffenderNonAssociationId(offender = nsOffender, nsOffender = offender),
      )
        ?: throw BadDataException("Opposite non-association not found where offender=${dto.nsOffenderNo} and nsOffender=${dto.offenderNo}")

      if (otherExisting.getOpenNonAssociationDetail() != null) {
        throw BadDataException("Non-association already exists for offender=${dto.nsOffenderNo} and nsOffender=${dto.offenderNo}")
      }

      typeSequence = existing.nextAvailableSequence()
      existing.nonAssociationReason = recipReason
      existing.recipNonAssociationReason = recipReason
      existing.offenderNonAssociationDetails.add(mapDetails(reason, existing, dto, typeSequence))

      if (typeSequence != otherExisting.nextAvailableSequence()) {
        throw RuntimeException("Non-association type sequence mismatch for offender=${dto.offenderNo} and nsOffender=${dto.nsOffenderNo}, next values are $typeSequence and ${otherExisting.nextAvailableSequence()}")
      }

      otherExisting.nonAssociationReason = recipReason
      otherExisting.recipNonAssociationReason = reason
      otherExisting.offenderNonAssociationDetails.add(mapDetails(recipReason, otherExisting, dto, typeSequence))
    } else {
      OffenderNonAssociation(
        id = OffenderNonAssociationId(offender, nsOffender),
        offenderBooking = offender.bookings.first(),
        nsOffenderBooking = nsOffender.bookings.first(),
        nonAssociationReason = recipReason,
        recipNonAssociationReason = recipReason,
      )
        .also {
          offenderNonAssociationRepository.save(it).apply {
            offenderNonAssociationDetails.add(mapDetails(reason, this, dto, typeSequence))
          }
        }

      OffenderNonAssociation(
        id = OffenderNonAssociationId(nsOffender, offender),
        offenderBooking = nsOffender.bookings.first(),
        nsOffenderBooking = offender.bookings.first(),
        nonAssociationReason = recipReason,
        recipNonAssociationReason = reason,
      )
        .also {
          offenderNonAssociationRepository.save(it).apply {
            offenderNonAssociationDetails.add(mapDetails(recipReason, this, dto, typeSequence))
          }
        }
    }
    telemetryClient.trackEvent(
      "non-association-created",
      mapOf(
        "offender" to dto.offenderNo,
        "nsOffender" to dto.nsOffenderNo,
      ),
    )
    return CreateNonAssociationResponse(typeSequence)
  }

  fun updateNonAssociation(offenderNo: String, nsOffenderNo: String, dto: UpdateNonAssociationRequest) {
    val offender = offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")
    val nsOffender = offenderRepository.findRootByNomisId(nsOffenderNo)
      ?: throw NotFoundException("NS Offender with nomsId=$nsOffenderNo not found")
    val existing = offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(offender = offender, nsOffender = nsOffender),
    ) ?: throw NotFoundException("Non-association not found where offender=$offenderNo and nsOffender=$nsOffenderNo")
    val reason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.reason))
      ?: throw BadDataException("Reason with code=${dto.reason} does not exist")
    val recipReason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.recipReason))
      ?: throw BadDataException("Reciprocal reason with code=${dto.recipReason} does not exist")
    val type = typeRepository.findByIdOrNull(NonAssociationType.pk(dto.type))
      ?: throw BadDataException("Type with code=${dto.type} does not exist")

    if (dto.effectiveDate.isAfter(LocalDate.now())) {
      throw BadDataException("Effective date must not be in the future")
    }

    existing.nonAssociationReason = recipReason
    existing.recipNonAssociationReason = recipReason

    existing.getOpenNonAssociationDetail()?.apply {
      nonAssociationReason = reason
      nonAssociationType = type
      effectiveDate = dto.effectiveDate
      authorisedBy = dto.authorisedBy
      comment = dto.comment
    }
      ?: throw BadDataException("No open Non-association detail found for offender=$offenderNo and nsOffender=$nsOffenderNo")

    val otherExisting = offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(offender = nsOffender, nsOffender = offender),
    )
      ?: throw BadDataException("Opposite non-association not found where offender=$nsOffenderNo and nsOffender=$offenderNo")

    otherExisting.nonAssociationReason = recipReason
    otherExisting.recipNonAssociationReason = reason

    otherExisting.getOpenNonAssociationDetail()?.apply {
      nonAssociationReason = otherExisting.nonAssociationReason!!
      nonAssociationType = type
      effectiveDate = dto.effectiveDate
      authorisedBy = dto.authorisedBy
      comment = dto.comment
    }
      ?: throw BadDataException("No open Non-association detail found for offender=$offenderNo and nsOffender=$nsOffenderNo")

    telemetryClient.trackEvent(
      "non-association-updated",
      mapOf(
        "offender" to offenderNo,
        "nsOffender" to nsOffenderNo,
      ),
    )
  }

  fun closeNonAssociation(offenderNo: String, nsOffenderNo: String) {
    val offender = offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")
    val nsOffender = offenderRepository.findRootByNomisId(nsOffenderNo)
      ?: throw NotFoundException("NS Offender with nomsId=$nsOffenderNo not found")
    val existing = offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(offender = offender, nsOffender = nsOffender),
    ) ?: throw NotFoundException("Non-association not found where offender=$offenderNo and nsOffender=$nsOffenderNo")

    existing.getOpenNonAssociationDetail()?.apply {
      expiryDate = LocalDate.now()
      telemetryClient.trackEvent(
        "non-association-closed",
        mapOf(
          "bookingId" to offenderBooking.bookingId.toString(),
        ),
        null,
      )
    }
      ?: throw BadDataException("No open Non-association detail found for offender=$offenderNo and nsOffender=$nsOffenderNo")
  }

  private fun mapDetails(
    reason: NonAssociationReason,
    existing: OffenderNonAssociation,
    dto: CreateNonAssociationRequest,
    typeSequence: Int,
  ) =
    OffenderNonAssociationDetail(
      id = OffenderNonAssociationDetailId(
        offender = existing.id.offender,
        nsOffender = existing.id.nsOffender,
        typeSequence = typeSequence,
      ),
      offenderBooking = existing.offenderBooking,
      nsOffenderBooking = existing.nsOffenderBooking,
      nonAssociationReason = reason,
      nonAssociation = existing,
      effectiveDate = dto.effectiveDate,
      comment = dto.comment,
      nonAssociationType = typeRepository.findByIdOrNull(NonAssociationType.pk(dto.type))
        ?: throw BadDataException("Type with code=${dto.type} does not exist"),
      authorisedBy = dto.authorisedBy,
    )

  fun getNonAssociation(offenderNo: String, nsOffenderNo: String, getAll: Boolean): List<NonAssociationResponse> {
    val offender = offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")
    val nsOffender = offenderRepository.findRootByNomisId(nsOffenderNo)
      ?: throw NotFoundException("NS Offender with nomsId=$nsOffenderNo not found")

    return offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(
        offender = offender,
        nsOffender = nsOffender,
      ),
    )?.let {
      mapModel(it, getAll)
    }
      ?: throw NotFoundException("Open NonAssociation not found")
  }

  fun findIdsByFilter(
    pageRequest: Pageable,
    nonAssociationFilter: NonAssociationFilter,
  ): Page<NonAssociationIdResponse> =
    offenderNonAssociationRepository.findAll(NonAssociationSpecification(nonAssociationFilter), pageRequest)
      .map { NonAssociationIdResponse(it.id.offender.nomsId, it.id.nsOffender.nomsId) }

  private fun mapModel(entity: OffenderNonAssociation, getAll: Boolean): List<NonAssociationResponse> =
    entity.offenderNonAssociationDetails
      .filter { getAll || it.expiryDate == null }
      .map { detail ->
        NonAssociationResponse(
          offenderNo = entity.id.offender.nomsId,
          nsOffenderNo = entity.id.nsOffender.nomsId,
          typeSequence = detail.id.typeSequence,
          reason = detail.nonAssociationReason.code,
          recipReason = entity.recipNonAssociationReason?.code!!, // Always set in prod
          type = detail.nonAssociationType.code,
          effectiveDate = detail.effectiveDate,
          expiryDate = detail.expiryDate,
          authorisedBy = detail.authorisedBy,
          comment = detail.comment,
        )
      }
}
