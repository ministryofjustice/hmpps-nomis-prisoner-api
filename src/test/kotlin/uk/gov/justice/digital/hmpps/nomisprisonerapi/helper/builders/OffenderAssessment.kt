package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csra.EvaluationResultCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Assessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentCommittee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentStatusType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AssessmentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentItem
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AssessmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAssessmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderAssessmentDslMarker

@NomisDataDslMarker
interface OffenderAssessmentDsl {
  @OffenderAssessmentItemDslMarker
  fun assessmentItem(
    itemSequence: Int,
    assessmentId: Long,
    dsl: OffenderAssessmentItemDsl.() -> Unit = {},
  ): OffenderAssessmentItem
}

@Component
class OffenderAssessmentBuilderRepository(
  private val offenderAssessmentRepository: OffenderAssessmentRepository,
  private val assessmentRepository: AssessmentRepository,
  private val staffUserAccountRepository: StaffUserAccountRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(offenderAssessment: OffenderAssessment): OffenderAssessment = offenderAssessmentRepository
    .saveAndFlush(offenderAssessment)

  fun lookupStaff(username: String): StaffUserAccount = staffUserAccountRepository.findByUsername(username)
    ?: throw BadDataException("Staff user account $username not found")

  fun lookupAgency(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId)
    ?: throw BadDataException("Prison $prisonId not found")

  fun lookupAssessment(id: Long): Assessment = assessmentRepository.findByIdOrNull(id)
    ?: throw BadDataException("Assessment $id not found")
}

@Component
class OffenderAssessmentBuilderFactory(
  val repository: OffenderAssessmentBuilderRepository,
  val offenderAssessmentItemBuilderFactory: OffenderAssessmentItemBuilderFactory,
) {
  fun builder() = OffenderAssessmentBuilder(repository, offenderAssessmentItemBuilderFactory)
}

class OffenderAssessmentBuilder(
  private val repository: OffenderAssessmentBuilderRepository,
  private val offenderAssessmentItemBuilderFactory: OffenderAssessmentItemBuilderFactory,
) : OffenderAssessmentDsl {
  lateinit var assessment: OffenderAssessment

  fun build(
    booking: OffenderBooking,
    sequence: Int,
    username: String,
    assessmentDate: LocalDate,
    assessmentType: AssessmentType,
    placementAgency: String? = null,
  ): OffenderAssessment = OffenderAssessment(
    id = OffenderAssessmentId(booking, sequence),
    assessmentDate = assessmentDate,
    assessmentType = assessmentType,
    score = BigDecimal.valueOf(1000),
    assessmentStatus = AssessmentStatusType.I,
    calculatedLevel = AssessmentLevel.STANDARD,
    assessmentStaff = repository.lookupStaff(username).staff,
    assessorStaff = repository.lookupStaff(username).staff,
    assessmentComment = "a-comment",
    placementAgency = placementAgency?.let { repository.lookupAgency(placementAgency) },
    assessmentCreationLocation = null,
    overrideLevel = AssessmentLevel.STANDARD,
    overrideComment = "overrideComment",
    overrideStaff = repository.lookupStaff(username).staff,
    evaluationDate = LocalDate.parse("2025-12-28"),
    nextReviewDate = LocalDate.parse("2026-12-15"),
    evaluationResultCode = EvaluationResultCode.REJ,
    reviewLevel = AssessmentLevel.PEND,
    reviewPlacementComment = null,
    reviewCommitteeCode = AssessmentCommittee.MED,
    reviewCommitteeComment = "a-reviewCommitteeComment",
    reviewPlacementAgency = null,
    reviewComment = "a-reviewComment",
    assessmentCommitteeCode = AssessmentCommittee.SECSTATE,
    creationDateTime = LocalDateTime.parse("2025-12-27T12:34:56"),
    creationUser = username,
    overrideReasonComment = "overrideReasonComment",
    overrideUserId = null,
    overrideReasonCode = null,
  )
    .let {
      assessment = repository.save(it)
      return assessment
    }

  override fun assessmentItem(
    itemSequence: Int,
    assessmentId: Long,
    dsl: OffenderAssessmentItemDsl.() -> Unit,
  ): OffenderAssessmentItem = offenderAssessmentItemBuilderFactory.builder().let { builder ->
    builder.build(
      assessment.id.offenderBooking,
      assessment.id.sequence,
      itemSequence,
      assessment,
      repository.lookupAssessment(assessmentId),
    )
      .also {
        assessment.offenderAssessmentItems.add(it)
        builder.apply(dsl)
      }
  }
}
