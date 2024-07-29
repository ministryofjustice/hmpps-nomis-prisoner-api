package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAttendee
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReview
import java.time.LocalDate

@DslMarker
annotation class CSIPReviewDslMarker

@NomisDataDslMarker
interface CSIPReviewDsl {
  @CSIPReviewDslMarker
  fun attendee(
    name: String? = null,
    role: String? = null,
    attended: Boolean = false,
    contribution: String? = null,
    dsl: CSIPAttendeeDsl.() -> Unit = {},
  ): CSIPAttendee
}

@Component
class CSIPReviewBuilderFactory(
  private val csipAttendeeBuilderFactory: CSIPAttendeeBuilderFactory,
) {
  fun builder() = CSIPReviewBuilder(csipAttendeeBuilderFactory)
}

class CSIPReviewBuilder(
  private val attendeeBuilderFactory: CSIPAttendeeBuilderFactory,
) : CSIPReviewDsl {
  private lateinit var csipReview: CSIPReview

  fun build(
    csipReport: CSIPReport,
    reviewSequence: Int,
    remainOnCSIP: Boolean,
    csipUpdated: Boolean,
    caseNote: Boolean,
    closeCSIP: Boolean,
    peopleInformed: Boolean,
    summary: String?,
    nextReviewDate: LocalDate?,
    closeDate: LocalDate?,
    recordedBy: String?,
  ): CSIPReview =
    CSIPReview(
      csipReport = csipReport,
      reviewSequence = reviewSequence,
      remainOnCSIP = remainOnCSIP,
      csipUpdated = csipUpdated,
      caseNote = caseNote,
      closeCSIP = closeCSIP,
      peopleInformed = peopleInformed,
      summary = summary,
      nextReviewDate = nextReviewDate,
      closeDate = closeDate,
      recordedUser = recordedBy,
    )
      .also { csipReview = it }

  override fun attendee(
    name: String?,
    role: String?,
    attended: Boolean,
    contribution: String?,
    dsl: CSIPAttendeeDsl.() -> Unit,
  ): CSIPAttendee =
    attendeeBuilderFactory.builder()
      .let { builder ->
        builder.build(
          csipReview = csipReview,
          name = name,
          role = role,
          attended = attended,
          contribution = contribution,
        )
          .also { csipReview.attendees += it }
          .also { builder.apply(dsl) }
      }
}
