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
  fun createNonAssociation(dto: CreateNonAssociationRequest) {
    val offender = offenderRepository.findRootByNomisId(dto.offenderNo)
      ?: throw BadDataException("Offender with nomsId=${dto.offenderNo} not found")
    val nsOffender = offenderRepository.findRootByNomisId(dto.nsOffenderNo)
      ?: throw BadDataException("NS Offender with nomsId=${dto.nsOffenderNo} not found")
    val reason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.reason))
      ?: throw BadDataException("Reason with code=${dto.reason} does not exist")
    val recipReason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.recipReason))
      ?: throw BadDataException("Reciprocal reason with code=${dto.recipReason} does not exist")

    val existing = offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(offender = offender, nsOffender = nsOffender),
    )
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
      existing.offenderNonAssociationDetails.add(
        mapDetails(
          recipReason,
          existing,
          dto,
          existing.nextAvailableSequence(),
        ),
      )
      otherExisting.offenderNonAssociationDetails.add(
        mapDetails(
          reason,
          otherExisting,
          dto,
          otherExisting.nextAvailableSequence(),
        ),
      )
    } else {
      OffenderNonAssociation(
        id = OffenderNonAssociationId(offender, nsOffender),
        offenderBooking = offender.bookings.first(),
        nsOffenderBooking = nsOffender.bookings.first(),
        nonAssociationReason = reason,
        recipNonAssociationReason = reason,
      )
        .also {
          offenderNonAssociationRepository.save(it).apply {
            offenderNonAssociationDetails.add(mapDetails(recipReason, this, dto))
          }
        }

      OffenderNonAssociation(
        id = OffenderNonAssociationId(nsOffender, offender),
        offenderBooking = nsOffender.bookings.first(),
        nsOffenderBooking = offender.bookings.first(),
        nonAssociationReason = reason,
        recipNonAssociationReason = recipReason,
      )
        .also {
          offenderNonAssociationRepository.save(it).apply {
            offenderNonAssociationDetails.add(mapDetails(reason, this, dto))
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

    existing.nonAssociationReason = reason
    existing.recipNonAssociationReason = recipReason

    existing.getOpenNonAssociationDetail()?.apply {
      nonAssociationReason = existing.nonAssociationReason!!
      recipNonAssociationReason = existing.recipNonAssociationReason
      effectiveDate = dto.effectiveDate
      comment = dto.comment
    }
      ?: throw BadDataException("No open Non-association detail found for offender=$offenderNo and nsOffender=$nsOffenderNo")

    val otherExisting = offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(offender = nsOffender, nsOffender = offender),
    )
      ?: throw BadDataException("Opposite non-association not found where offender=$nsOffenderNo and nsOffender=$offenderNo")

    if (otherExisting.getOpenNonAssociationDetail() != null) {
      throw BadDataException("Non-association already exists for offender=$nsOffenderNo and nsOffender=$offenderNo")
    }
    otherExisting.nonAssociationReason = recipReason
    otherExisting.recipNonAssociationReason = reason

    otherExisting.getOpenNonAssociationDetail()?.apply {
      nonAssociationReason = otherExisting.nonAssociationReason!!
      recipNonAssociationReason = otherExisting.recipNonAssociationReason
      effectiveDate = dto.effectiveDate
      comment = dto.comment
      nonAssociationType = type
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
    typeSequence: Int = 1,
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
      // recipNonAssociationReason = existing.recipNonAssociationReason,
      nonAssociation = existing,
      effectiveDate = dto.effectiveDate,
      comment = dto.comment,
      nonAssociationType = typeRepository.findByIdOrNull(NonAssociationType.pk(dto.type))
        ?: throw BadDataException("Type with code=${dto.type} does not exist"),
      authorisedBy = dto.authorisedBy,
    )

  fun getNonAssociation(offenderNo: String, nsOffenderNo: String): NonAssociationResponse {
    val offender = offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")
    val nsOffender = offenderRepository.findRootByNomisId(nsOffenderNo)
      ?: throw NotFoundException("NS Offender with nomsId=$nsOffenderNo not found")

    offenderNonAssociationRepository.findByIdOrNull(
      OffenderNonAssociationId(offender = offender, nsOffender = nsOffender),
    )?.let {
      return mapModel(it)
    }
      ?: throw NotFoundException("NonAssociation not found")
  }

  fun findIdsByFilter(
    pageRequest: Pageable,
    nonAssociationFilter: NonAssociationFilter,
  ): Page<NonAssociationIdResponse> =
    offenderNonAssociationRepository.findAll(NonAssociationSpecification(nonAssociationFilter), pageRequest)
      .map { NonAssociationIdResponse(it.id.offender.nomsId, it.id.nsOffender.nomsId) }

  private fun mapModel(entity: OffenderNonAssociation) =
    NonAssociationResponse(
      offenderNo = entity.id.offender.nomsId,
      nsOffenderNo = entity.id.nsOffender.nomsId,
      // TODO - this is a bit of a guess, it's not clear what the correct behaviour should be yet
      reason = entity.nonAssociationReason?.code,
      recipReason = entity.recipNonAssociationReason?.code,
      type = entity.getOpenNonAssociationDetail()?.nonAssociationType?.code,
      effectiveDate = entity.getOpenNonAssociationDetail()?.effectiveDate,
      authorisedBy = entity.getOpenNonAssociationDetail()?.authorisedBy,
      comment = entity.getOpenNonAssociationDetail()?.comment,
    )
}
