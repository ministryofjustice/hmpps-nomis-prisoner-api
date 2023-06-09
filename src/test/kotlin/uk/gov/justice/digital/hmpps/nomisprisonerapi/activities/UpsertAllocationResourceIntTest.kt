package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityPayRateBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderProgramProfileBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate

class UpsertAllocationResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var courseActivityBuilderFactory: CourseActivityBuilderFactory

  @Autowired
  private lateinit var payRateBuilderFactory: CourseActivityPayRateBuilderFactory

  @Autowired
  private lateinit var allocationBuilderFactory: OffenderProgramProfileBuilderFactory

  private lateinit var courseActivity: CourseActivity
  private lateinit var offender: Offender
  private var bookingId: Long = 0

  @BeforeEach
  fun setup() {
    repository.save(ProgramServiceBuilder())
    courseActivity = repository.save(courseActivityBuilderFactory.builder())
    offender =
      repository.save(OffenderBuilder(nomsId = "A1234XX").withBooking(OffenderBookingBuilder(agencyLocationId = "LEI")))
    bookingId = offender.latestBooking().bookingId
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  private fun upsertRequest() = """{
    "bookingId": $bookingId,
    "startDate": "2022-11-14",
    "payBandCode": "5"
  }
  """.trimIndent()

  private fun String.withBookingId(newBookingId: String?) =
    replace(""""bookingId": $bookingId,""", newBookingId?.let { """"bookingId": $newBookingId,""" } ?: "")

  private fun String.withStartDate(newStartDate: String?) =
    replace(""""startDate": "2022-11-14",""", newStartDate?.let { """"startDate": "$newStartDate",""" } ?: "")

  private fun String.withPayBandCode(newPayBandCode: String?) =
    replace(""""payBandCode": "5"""", newPayBandCode?.let { """"payBandCode": "$newPayBandCode"""" } ?: """"ignored": "ignored"""") // hack so we don't have to worry about trailing comma on previous line

  private fun String.withAdditionalJson(additionalJson: String) =
    replace("}", """, $additionalJson }""")

  private fun upsertAllocationIsBadRequest(request: String = upsertRequest()) =
    webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .body(BodyInserters.fromValue(request))
      .exchange()
      .expectStatus().isBadRequest

  private fun upsertAllocationIsOk(request: String = upsertRequest()) =
    webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
      .contentType(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .body(BodyInserters.fromValue(request))
      .exchange()
      .expectStatus().isOk
      .expectBody(UpsertAllocationResponse::class.java)
      .returnResult().responseBody

  @Nested
  inner class Api {

    @Test
    fun `should return unauthorised when no authority`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(upsertRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden when no role`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(upsertRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden with wrong role`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(upsertRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if course activity does not exist`() {
      webTestClient.put().uri("/activities/9999/allocation")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(upsertRequest()))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Course activity with id=9999 does not exist")
        }
    }
  }

  @Nested
  inner class Validation {
    @Test
    fun `should return bad request if booking id wrong format`() {
      val request = upsertRequest().withBookingId("INVALID")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("INVALID")
        }
    }

    @Test
    fun `should return bad request if booking id missing`() {
      val request = upsertRequest().withBookingId(null)

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Booking with id=0 does not exist")
        }
    }

    @Test
    fun `should return bad request if booking id does not exist`() {
      val request = upsertRequest().withBookingId("999999")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Booking with id=999999 does not exist")
        }
    }

    @Test
    fun `should return bad request if start date in bad format`() {
      val request = upsertRequest().withStartDate("2022-13-14")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("2022-13-14")
        }
    }

    @Test
    fun `should return bad request if start date empty`() {
      val request = upsertRequest().withStartDate("")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("startDate")
        }
    }

    @Test
    fun `should return bad request if start date missing`() {
      val request = upsertRequest().withStartDate(null)

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("startDate")
        }
    }

    @Test
    fun `should return bad request if pay band code missing`() {
      val request = upsertRequest().withPayBandCode(null)

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("payBandCode")
        }
    }

    @Test
    fun `should return bad request if pay band code empty`() {
      val request = upsertRequest().withPayBandCode("")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("payBandCode")
        }
    }

    @Test
    fun `should return bad request if pay band code does not exist`() {
      val request = upsertRequest().withPayBandCode("9999")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Pay band code 9999 does not exist")
        }
    }

    @Test
    fun `should return bad request if pay band code is not available for the course activity`() {
      val request = upsertRequest().withPayBandCode("9")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Pay band code 9 does not exist for course activity with id=${courseActivity.courseActivityId}")
        }
    }

    @Test
    fun `should return bad request if end date in bad format`() {
      val request = upsertRequest().withAdditionalJson(""""endDate": "2023-12-35"""")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("2023-12-35")
        }
    }

    @Test
    fun `should return bad request if end reason code does not exist`() {
      val request = upsertRequest().withAdditionalJson(""""endReason": "INVALID"""")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("End reason code=INVALID does not exist")
        }
    }

    @Test
    fun `should return bad request if suspended flag in bad format`() {
      val request = upsertRequest().withAdditionalJson(""""suspended": "INVALID"""")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("INVALID")
        }
    }

    @Test
    fun `should return bad request offender in wrong prison`() {
      val offenderAtWrongPrison =
        repository.save(OffenderBuilder(nomsId = "A1234YY").withBooking(OffenderBookingBuilder(agencyLocationId = "MDI")))
      val request = upsertRequest().withBookingId(offenderAtWrongPrison.latestBooking().bookingId.toString())

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Prisoner is at prison=MDI, not the Course activity prison=LEI")
        }
    }
  }

  @Nested
  inner class CreateAllocation {
    @Test
    fun `should save new allocation`() {
      val response = upsertAllocationIsOk()

      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      assertThat(response?.created).isTrue()

      val saved = repository.lookupOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(startDate).isEqualTo("2022-11-14")
        assertThat(programStatus.code).isEqualTo("ALLOC")
        assertThat(payBands[0].payBand.code).isEqualTo("5")
      }
    }

    @Test
    fun `should publish telemetry`() {
      val response = upsertAllocationIsOk()

      verify(telemetryClient).trackEvent(
        eq("activity-allocation-created"),
        check<MutableMap<String, String>> {
          assertThat(it["nomisCourseActivityId"]).isEqualTo(courseActivity.courseActivityId.toString())
          assertThat(it["nomisAllocationId"]).isEqualTo(response?.offenderProgramReferenceId.toString())
          assertThat(it["bookingId"]).isEqualTo(bookingId.toString())
          assertThat(it["offenderNo"]).isEqualTo(offender.nomsId)
        },
        isNull(),
      )
    }
  }

  @Nested
  inner class UpdateAllocation {

    @BeforeEach
    fun setUp() {
      val payRates = listOf(
        payRateBuilderFactory.builder(),
        payRateBuilderFactory.builder(payBandCode = "6"),
        payRateBuilderFactory.builder(payBandCode = "7"),
      )
      courseActivity = repository.save(courseActivityBuilderFactory.builder(payRates = payRates))

      // create an allocation to update
      upsertAllocationIsOk()
    }

    @Test
    fun `should update when allocation ended`() {
      val request = upsertRequest().withAdditionalJson(
        """
        "endDate": "${LocalDate.now()}",
        "endReason": "WDRAWN",
        "endComment": "Withdrawn due to illness"
        """.trimMargin(),
      )
      val response = upsertAllocationIsOk(request)

      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      assertThat(response?.created).isFalse()

      val saved = repository.lookupOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(endDate).isEqualTo("${LocalDate.now()}")
        assertThat(endReason?.code).isEqualTo("WDRAWN")
        assertThat(endComment).isEqualTo("Withdrawn due to illness")
        assertThat(programStatus.code).isEqualTo("END")
      }
    }

    @Test
    fun `should update when allocation suspended`() {
      val request = upsertRequest().withAdditionalJson(
        """
        "suspended": true,
        "suspendedComment": "In hospital for a week"
        """.trimMargin(),
      )
      val response = upsertAllocationIsOk(request)

      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      assertThat(response?.created).isFalse()

      val saved = repository.lookupOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(suspended).isTrue()
        assertThat(endComment).isEqualTo("In hospital for a week")
        assertThat(programStatus.code).isEqualTo("ALLOC")
      }
    }

    @Test
    fun `should update with ended details when allocation is suspended and then ended`() {
      val suspendRequest = upsertRequest().withAdditionalJson(
        """
        "suspended": true,
        "suspendedComment": "In hospital for a week"
        """.trimMargin(),
      )
      upsertAllocationIsOk(suspendRequest)

      val endRequest = upsertRequest().withAdditionalJson(
        """
        "endDate": "${LocalDate.now()}",
        "endReason": "WDRAWN",
        "endComment": "Withdrawn due to illness"
        """.trimMargin(),
      )
      val response = upsertAllocationIsOk(endRequest)

      val saved = repository.lookupOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(endDate).isEqualTo("${LocalDate.now()}")
        assertThat(endReason?.code).isEqualTo("WDRAWN")
        assertThat(endComment).isEqualTo("Withdrawn due to illness")
        assertThat(programStatus.code).isEqualTo("END")
      }
    }

    @Test
    fun `should update allocation pay band`() {
      val request = upsertRequest().withPayBandCode("6")
      val response = upsertAllocationIsOk(request)

      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      assertThat(response?.created).isFalse()

      val saved = repository.lookupOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(payBands[0].payBand.code).isEqualTo("5")
        assertThat(payBands[0].endDate).isEqualTo(LocalDate.now())
        assertThat(payBands[1].payBand.code).isEqualTo("6")
        assertThat(payBands[1].id.startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(payBands[1].endDate).isNull()
      }
    }

    @Test
    fun `should update allocation pay band twice on the same day`() {
      val firstPayBandUpdateRequest = upsertRequest().withPayBandCode("6")
      upsertAllocationIsOk(firstPayBandUpdateRequest)

      val secondPayBandUpdateRequest = upsertRequest().withPayBandCode("7")
      val response = upsertAllocationIsOk(secondPayBandUpdateRequest)

      val saved = repository.lookupOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(payBands[0].payBand.code).isEqualTo("5")
        assertThat(payBands[0].endDate).isEqualTo(LocalDate.now())
        assertThat(payBands[1].payBand.code).isEqualTo("7")
        assertThat(payBands[1].id.startDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(payBands[1].endDate).isNull()
      }
    }

    @Test
    fun `should revert allocation pay band update`() {
      val updatePayBandRequest = upsertRequest().withPayBandCode("6")
      upsertAllocationIsOk(updatePayBandRequest)

      val revertPayBandRequest = upsertRequest().withPayBandCode("5")
      val response = upsertAllocationIsOk(revertPayBandRequest)

      val saved = repository.lookupOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(startDate).isEqualTo("2022-11-14")
        assertThat(payBands[0].payBand.code).isEqualTo("5")
        assertThat(payBands[0].endDate).isNull()
      }
    }

    @Test
    fun `should publish telemetry`() {
      val response = upsertAllocationIsOk()

      verify(telemetryClient).trackEvent(
        eq("activity-allocation-updated"),
        check<MutableMap<String, String>> {
          assertThat(it["nomisCourseActivityId"]).isEqualTo(courseActivity.courseActivityId.toString())
          assertThat(it["nomisAllocationId"]).isEqualTo(response?.offenderProgramReferenceId.toString())
          assertThat(it["bookingId"]).isEqualTo(bookingId.toString())
          assertThat(it["offenderNo"]).isEqualTo(offender.nomsId)
        },
        isNull(),
      )
    }

    @Test
    fun `should allow de-allocation when not in the activity prison`() {
      // Create an allocation for a prisoner who is out (need to do this directly on the database to avoid validation)
      offender =
        repository.save(OffenderBuilder(nomsId = "A1234XX").withBooking(OffenderBookingBuilder(agencyLocationId = "OUT")))
      bookingId = offender.latestBooking().bookingId
      repository.save(allocationBuilderFactory.builder(), offender.latestBooking(), courseActivity)

      // De-allocation is allowed
      val request = upsertRequest().withAdditionalJson(
        """
        "endDate": "${LocalDate.now()}",
        "endReason": "WDRAWN",
        "endComment": "Withdrawn due to illness"
        """.trimMargin(),
      )
      upsertAllocationIsOk(request)

      // But re-allocating is not allowed
      upsertAllocationIsBadRequest(upsertRequest())
    }

    @Test
    fun `should allow suspension when not in the activity prison`() {
      // Create an allocation for a prisoner who is out (need to do this directly on the database to avoid validation)
      offender =
        repository.save(OffenderBuilder(nomsId = "A1234XX").withBooking(OffenderBookingBuilder(agencyLocationId = "OUT")))
      bookingId = offender.latestBooking().bookingId
      repository.save(allocationBuilderFactory.builder(), offender.latestBooking(), courseActivity)

      // Suspending is allowed
      val request = upsertRequest().withAdditionalJson(
        """
        "suspended": true
        """.trimMargin(),
      )
      upsertAllocationIsOk(request)

      // But un-suspending is not allowed
      val unsuspendRequest = upsertRequest().withAdditionalJson(
        """
        "suspended": false
        """.trimMargin(),
      )
      upsertAllocationIsBadRequest(unsuspendRequest)
    }
  }

  @Nested
  inner class DuplicateAllocation {
    @Test
    fun `duplicate allocations can be worked around by deleting one of them`() {
      repository.save(allocationBuilderFactory.builder(), offender.latestBooking(), courseActivity)
      val duplicate = repository.save(allocationBuilderFactory.builder(), offender.latestBooking(), courseActivity)

      // unable to update the allocation because of a duplicate
      val request = upsertRequest().withAdditionalJson(""""endDate": "${LocalDate.now()}"""")
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(request))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("query did not return a unique result")
        }

      // delete the duplicate
      webTestClient.delete().uri("/allocations/${duplicate.offenderProgramReferenceId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isNoContent

      // now able to update the allocation
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(request))
        .exchange()
        .expectStatus().isOk
    }
  }
}
