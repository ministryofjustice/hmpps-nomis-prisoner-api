package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderActivityExclusion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay

@DslMarker
annotation class OffenderActivityExclusionDslMarker

@NomisDataDslMarker
interface OffenderActivityExclusionDsl

@Component
class OffenderActivityExclusionBuilderFactory {
  fun builder() = OffenderActivityExclusionBuilder()
}

class OffenderActivityExclusionBuilder : OffenderActivityExclusionDsl {

  fun build(
    offenderBooking: OffenderBooking,
    courseAllocation: OffenderProgramProfile,
    courseActivity: CourseActivity,
    slotCategory: SlotCategory?,
    excludeDay: WeekDay,
  ): OffenderActivityExclusion =
    OffenderActivityExclusion(
      offenderBooking = offenderBooking,
      offenderProgramProfile = courseAllocation,
      courseActivity = courseActivity,
      slotCategory = slotCategory,
      excludeDay = excludeDay,
    )
}
