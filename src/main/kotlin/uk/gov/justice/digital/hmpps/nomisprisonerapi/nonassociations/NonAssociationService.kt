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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderNonAssociationDetailRepository
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
  private val offenderNonAssociationDetailRepository: OffenderNonAssociationDetailRepository,
  private val reasonRepository: ReferenceCodeRepository<NonAssociationReason>,
  private val typeRepository: ReferenceCodeRepository<NonAssociationType>,
  private val telemetryClient: TelemetryClient,
) {
  fun createNonAssociation(dto: CreateNonAssociationRequest): CreateNonAssociationResponse {
    if (dto.offenderNo == dto.nsOffenderNo) {
      throw BadDataException("Offender and NS Offender cannot be the same")
    }

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

  fun updateNonAssociation(offenderNo: String, nsOffenderNo: String, typeSequence: Int, dto: UpdateNonAssociationRequest) {
    if (offenderNo == nsOffenderNo) {
      throw BadDataException("Offender and NS Offender cannot be the same")
    }

    val offender = offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")
    val nsOffender = offenderRepository.findRootByNomisId(nsOffenderNo)
      ?: throw NotFoundException("NS Offender with nomsId=$nsOffenderNo not found")

    val existing = offenderNonAssociationDetailRepository.findByIdOrNull(
      OffenderNonAssociationDetailId(offender = offender, nsOffender = nsOffender, typeSequence = typeSequence),
    ) ?: throw NotFoundException("Non-association not found where offender=$offenderNo, nsOffender=$nsOffenderNo, typeSequence=$typeSequence")
    val otherExisting = offenderNonAssociationDetailRepository.findByIdOrNull(
      OffenderNonAssociationDetailId(offender = nsOffender, nsOffender = offender, typeSequence = typeSequence),
    ) ?: throw BadDataException("Opposite non-association not found where offender=$nsOffenderNo, nsOffender=$offenderNo, typeSequence=$typeSequence")

    val reason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.reason))
      ?: throw BadDataException("Reason with code=${dto.reason} does not exist")
    val recipReason = reasonRepository.findByIdOrNull(NonAssociationReason.pk(dto.recipReason))
      ?: throw BadDataException("Reciprocal reason with code=${dto.recipReason} does not exist")
    val type = typeRepository.findByIdOrNull(NonAssociationType.pk(dto.type))
      ?: throw BadDataException("Type with code=${dto.type} does not exist")

    if (dto.effectiveDate.isAfter(LocalDate.now())) {
      throw BadDataException("Effective date must not be in the future")
    }

    existing.nonAssociation.nonAssociationReason = recipReason
    existing.nonAssociation.recipNonAssociationReason = recipReason

    existing.apply {
      nonAssociationReason = reason
      nonAssociationType = type
      effectiveDate = dto.effectiveDate
      authorisedBy = dto.authorisedBy
      comment = dto.comment
    }

    otherExisting.nonAssociation.nonAssociationReason = recipReason
    otherExisting.nonAssociation.recipNonAssociationReason = reason

    otherExisting.apply {
      nonAssociationReason = recipReason
      nonAssociationType = type
      effectiveDate = dto.effectiveDate
      authorisedBy = dto.authorisedBy
      comment = dto.comment
    }

    telemetryClient.trackEvent(
      "non-association-amended",
      mapOf(
        "offender" to offenderNo,
        "nsOffender" to nsOffenderNo,
        "typeSequence" to "$typeSequence",
      ),
    )
  }

  fun closeNonAssociation(offenderNo: String, nsOffenderNo: String, typeSequence: Int) {
    if (offenderNo == nsOffenderNo) {
      throw BadDataException("Offender and NS Offender cannot be the same")
    }
    val offender = offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")
    val nsOffender = offenderRepository.findRootByNomisId(nsOffenderNo)
      ?: throw NotFoundException("NS Offender with nomsId=$nsOffenderNo not found")

    val existing = offenderNonAssociationDetailRepository.findByIdOrNull(
      OffenderNonAssociationDetailId(offender = offender, nsOffender = nsOffender, typeSequence = typeSequence),
    ) ?: throw NotFoundException("Non-association detail not found for offender=$offenderNo, nsOffender=$nsOffenderNo, typeSequence=$typeSequence")

    val otherExisting = offenderNonAssociationDetailRepository.findByIdOrNull(
      OffenderNonAssociationDetailId(offender = nsOffender, nsOffender = offender, typeSequence = typeSequence),
    ) ?: throw NotFoundException("Non-association detail not found where offender=$nsOffenderNo, nsOffender=$offenderNo, typeSequence=$typeSequence")

    val today = LocalDate.now()
    existing.apply {
      if (expiryDate != null) throw BadDataException("Non-association already closed for offender=$offenderNo, nsOffender=$nsOffenderNo, typeSequence=$typeSequence")
      expiryDate = today
      telemetryClient.trackEvent(
        "non-association-closed",
        mapOf(
          "offender" to offenderNo,
          "nsOffender" to nsOffenderNo,
          "typeSequence" to "$typeSequence",
        ),
      )
    }
    otherExisting.apply {
      if (expiryDate != null) throw BadDataException("Non-association already closed for offender=$offenderNo, nsOffender=$nsOffenderNo, typeSequence=$typeSequence")
      expiryDate = today
      telemetryClient.trackEvent(
        "non-association-closed",
        mapOf(
          "offender" to nsOffenderNo,
          "nsOffender" to offenderNo,
          "typeSequence" to "$typeSequence",
        ),
      )
    }
  }

  fun deleteNonAssociation(offenderNo: String, nsOffenderNo: String, typeSequence: Int) {
    if (offenderNo == nsOffenderNo) {
      throw BadDataException("Offender and NS Offender cannot be the same")
    }
    val offender = offenderRepository.findRootByNomisId(offenderNo)
      ?: throw NotFoundException("Offender with nomsId=$offenderNo not found")
    val nsOffender = offenderRepository.findRootByNomisId(nsOffenderNo)
      ?: throw NotFoundException("NS Offender with nomsId=$nsOffenderNo not found")

    val existingDetail = offenderNonAssociationDetailRepository.findByIdOrNull(
      OffenderNonAssociationDetailId(offender = offender, nsOffender = nsOffender, typeSequence = typeSequence),
    ) ?: throw NotFoundException("Non-association detail not found for offender=$offenderNo, nsOffender=$nsOffenderNo, typeSequence=$typeSequence")

    val otherExistingDetail = offenderNonAssociationDetailRepository.findByIdOrNull(
      OffenderNonAssociationDetailId(offender = nsOffender, nsOffender = offender, typeSequence = typeSequence),
    ) ?: throw NotFoundException("Non-association detail not found where offender=$nsOffenderNo, nsOffender=$offenderNo, typeSequence=$typeSequence")

    val existingNonAssociation = existingDetail.nonAssociation
    val otherExistingNonAssociation = otherExistingDetail.nonAssociation
    existingNonAssociation.offenderNonAssociationDetails.remove(existingDetail)
    otherExistingNonAssociation.offenderNonAssociationDetails.remove(otherExistingDetail)

    if (existingNonAssociation.offenderNonAssociationDetails.size != otherExistingNonAssociation.offenderNonAssociationDetails.size) {
      throw RuntimeException("Non-association type sequence mismatch for offender=$offenderNo and nsOffender=$nsOffenderNo, differing number of details records")
    }

    if (existingNonAssociation.offenderNonAssociationDetails.isEmpty()) {
      offenderNonAssociationRepository.delete(existingNonAssociation)
      offenderNonAssociationRepository.delete(otherExistingNonAssociation)
    }

    telemetryClient.trackEvent(
      "non-association-deleted",
      mapOf(
        "offender" to offenderNo,
        "nsOffender" to nsOffenderNo,
        "typeSequence" to "$typeSequence",
      ),
    )
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

  fun getNonAssociation(
    offenderNo: String,
    nsOffenderNo: String,
    typeSequence: Int?,
    getAll: Boolean,
  ): List<NonAssociationResponse> {
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
      mapModel(it, typeSequence, getAll)
    }
      ?: throw if (getAll) {
        throw NotFoundException("Non-association not found")
      } else if (typeSequence == null) {
        NotFoundException("Open NonAssociation not found")
      } else {
        NotFoundException("Non-association with sequence $typeSequence not found")
      }
  }

  fun findIdsByFilter(
    pageRequest: Pageable,
    nonAssociationFilter: NonAssociationFilter,
  ): Page<NonAssociationIdResponse> =
    offenderNonAssociationRepository.findAll(NonAssociationSpecification(nonAssociationFilter), pageRequest)
      .map { NonAssociationIdResponse(it.id.offender.nomsId, it.id.nsOffender.nomsId) }

  private fun mapModel(
    entity: OffenderNonAssociation,
    typeSequence: Int?,
    getAll: Boolean,
  ): List<NonAssociationResponse> =
    entity.offenderNonAssociationDetails
      .filter {
        getAll || if (typeSequence != null) {
          it.id.typeSequence == typeSequence
        } else {
          it.expiryDate == null
        }
      }
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
          updatedBy = detail.updatedBy,
          comment = detail.comment,
        )
      }
}

private val OffenderNonAssociationDetail.updatedBy: String
  get() {
    return modifiedBy ?: createdBy
  }
