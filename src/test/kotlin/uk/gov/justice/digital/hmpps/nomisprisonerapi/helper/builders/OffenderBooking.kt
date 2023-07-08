package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDateTime

@DslMarker
annotation class NewBookingDslMarker

@NomisDataDslMarker
interface NewBookingDsl : CourseAllocationDslApi, NewIncentiveDslApi

interface NewBookingDslApi {
  @NewBookingDslMarker
  fun booking(
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    active: Boolean = true,
    inOutStatus: String = "IN",
    youthAdultCode: String = "N",
    agencyLocationId: String = "BXI",
    dsl: NewBookingDsl.() -> Unit = {},
  ): OffenderBooking
}

@Component
class NewBookingBuilderRepository(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(offenderBooking: OffenderBooking) = offenderBookingRepository.save(offenderBooking)
  fun lookupAgencyLocation(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
}

@Component
class NewBookingBuilderFactory(
  private val repository: NewBookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: NewIncentiveBuilderFactory,
) {
  fun builder() = NewBookingBuilder(repository, courseAllocationBuilderFactory, incentiveBuilderFactory)
}

class NewBookingBuilder(
  private val repository: NewBookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: NewIncentiveBuilderFactory,
) : NewBookingDsl {

  private lateinit var offenderBooking: OffenderBooking

  fun build(
    offender: Offender,
    bookingSequence: Int,
    agencyLocationCode: String,
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    active: Boolean,
    inOutStatus: String,
    youthAdultCode: String,
  ): OffenderBooking {
    val agencyLocation = repository.lookupAgencyLocation(agencyLocationCode)
    return OffenderBooking(
      offender = offender,
      rootOffender = offender,
      bookingSequence = bookingSequence,
      createLocation = agencyLocation,
      location = agencyLocation,
      bookingBeginDate = bookingBeginDate,
      active = active,
      inOutStatus = inOutStatus,
      youthAdultCode = youthAdultCode,
    )
      .let { repository.save(it) }
      .also { offenderBooking = it }
  }

  override fun courseAllocation(
    courseActivity: CourseActivity,
    startDate: String?,
    programStatusCode: String,
    endDate: String?,
    endReasonCode: String?,
    endComment: String?,
    dsl: CourseAllocationDsl.() -> Unit,
  ) =
    courseAllocationBuilderFactory.builder()
      .let { builder ->
        builder.build(offenderBooking, startDate, programStatusCode, endDate, endReasonCode, endComment, courseActivity)
          .also { offenderBooking.offenderProgramProfiles += it }
          .also { builder.apply(dsl) }
      }

  override fun incentive(
    iepLevelCode: String,
    userId: String?,
    sequence: Long,
    commentText: String,
    auditModuleName: String?,
    iepDateTime: LocalDateTime,
  ) =
    incentiveBuilderFactory.builder()
      .build(
        offenderBooking,
        iepLevelCode,
        userId,
        sequence,
        commentText,
        auditModuleName,
        iepDateTime,
      )
      .also { offenderBooking.incentives += it }
}
