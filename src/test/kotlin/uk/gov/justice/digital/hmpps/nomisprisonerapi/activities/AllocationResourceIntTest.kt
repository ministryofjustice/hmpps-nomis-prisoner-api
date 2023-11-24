package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.UpsertAllocationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderActivityExclusion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfilePayBand
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import java.time.LocalDate

class AllocationResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var courseActivity: CourseActivity
  private lateinit var offender: Offender
  private var bookingId: Long = 0

  private var today = LocalDate.now()
  private var yesterday = today.minusDays(1)

  @BeforeEach
  fun setup() {
    nomisDataBuilder.build {
      programService {
        courseActivity = courseActivity()
      }
      offender = offender {
        bookingId = booking().bookingId
      }
    }
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
    "payBandCode": "5",
    "programStatusCode": "ALLOC",
    "exclusions": []
  }
  """.trimIndent()

  private fun String.withBookingId(newBookingId: String?) =
    replace(""""bookingId": $bookingId,""", newBookingId?.let { """"bookingId": $newBookingId,""" } ?: "")

  private fun String.withStartDate(newStartDate: String?) =
    replace(""""startDate": "2022-11-14",""", newStartDate?.let { """"startDate": "$newStartDate",""" } ?: "")

  private fun String.withPayBandCode(newPayBandCode: String?) =
    replace(""""payBandCode": "5",""", newPayBandCode?.let { """"payBandCode": "$newPayBandCode",""" } ?: "")

  private fun String.withProgramStatusCode(programStatusCode: String?) =
    replace(""""programStatusCode": "ALLOC",""", programStatusCode?.let { """"programStatusCode": "$programStatusCode",""" } ?: "")

  private fun String.withPrisonerExclusions(exclusions: List<Pair<String, String?>>) =
    replace(
      """"exclusions": []""",
      """"exclusions": [${exclusions.joinToString(", ") { ex -> """{"day": "${ex.first}", "slot": ${ex.second?.let { "\"$it\"" } ?: "null"}}""" }}]""",
    )

  private fun String.withEndDate(newEndDate: String?) =
    replace("}", newEndDate?.let { """, "endDate": "$newEndDate" }""" } ?: "}")

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
    fun `should return bad request if program status code missing`() {
      val request = upsertRequest().withProgramStatusCode(null)

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("programStatusCode")
        }
    }

    @Test
    fun `should return bad request if program status code empty`() {
      val request = upsertRequest().withProgramStatusCode("")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("programStatusCode")
        }
    }

    @Test
    fun `should return bad request if program status code does not exist`() {
      val request = upsertRequest().withProgramStatusCode("UNKNOWN")

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Program status code=UNKNOWN does not exist")
        }
    }

    @Test
    fun `should return bad request if end date in bad format`() {
      val request = upsertRequest().withEndDate("2023-12-35")

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
    fun `should return bad request if invalid day in prisoner exclusion`() {
      val request = upsertRequest().withPrisonerExclusions(listOf("INVALID" to "AM"))

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("INVALID")
        }
    }

    @Test
    fun `should return bad request if invalid slot category in prisoner exclusion`() {
      val request = upsertRequest().withPrisonerExclusions(listOf("MON" to "INVALID"))

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("INVALID")
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

      val saved = repository.getOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(startDate).isEqualTo("2022-11-14")
        assertThat(programStatus.code).isEqualTo("ALLOC")
        assertThat(payBands[0].payBand.code).isEqualTo("5")
      }
    }

    @Test
    fun `should save a prisoner exclusion to a new allocation`() {
      val response = upsertAllocationIsOk(
        upsertRequest().withPrisonerExclusions(listOf("TUE" to "AM")),
      )

      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      assertThat(response?.created).isTrue()

      val saved = repository.getOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved.offenderExclusions[0]) {
        assertThat(id).isGreaterThan(0)
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(courseActivity.courseActivityId).isEqualTo(this@AllocationResourceIntTest.courseActivity.courseActivityId)
        assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        assertThat(excludeDay).isEqualTo(WeekDay.TUE)
      }
    }

    @Test
    fun `should save multiple prisoner exclusions`() {
      val response = upsertAllocationIsOk(
        upsertRequest().withPrisonerExclusions(listOf("MON" to null, "TUE" to "AM")),
      )

      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      assertThat(response?.created).isTrue()

      val saved = repository.getOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderExclusions).extracting("slotCategory", "excludeDay").containsExactlyInAnyOrder(
          tuple(null, WeekDay.MON),
          tuple(SlotCategory.AM, WeekDay.TUE),
        )
      }
    }

    @Test
    fun `should return bad request offender in wrong prison`() {
      lateinit var offenderAtWrongPrison: Offender
      nomisDataBuilder.build {
        offenderAtWrongPrison = offender(nomsId = "A1234YY") {
          bookingId = booking(agencyLocationId = "MDI") {
            courseAllocation(courseActivity, endDate = "2022-11-01", programStatusCode = "END")
          }.bookingId
        }
      }
      val request = upsertRequest().withBookingId(offenderAtWrongPrison.latestBooking().bookingId.toString())

      upsertAllocationIsBadRequest(request)
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Prisoner is at prison=MDI, not the Course activity prison=BXI")
        }
    }

    @Test
    fun `should create new allocation if previously ended allocation exists`() {
      nomisDataBuilder.build {
        offender = offender {
          bookingId = booking {
            courseAllocation(courseActivity, endDate = "2022-11-01", programStatusCode = "END")
          }.bookingId
        }
      }
      val request = upsertRequest()
        .withAdditionalJson(""""startDate": "2022-12-01"""")
        .withProgramStatusCode("ALLOC")

      val response = upsertAllocationIsOk(request)!!

      val saved = repository.getOffenderProgramProfiles(courseActivity, offender.latestBooking())
      with(saved[0]) {
        assertThat(startDate).isEqualTo("2022-10-31")
        assertThat(endDate).isEqualTo("2022-11-01")
        assertThat(programStatus.code).isEqualTo("END")
        assertThat(payBands[0].payBand.code).isEqualTo("5")
      }
      with(saved[1]) {
        assertThat(response.offenderProgramReferenceId).isEqualTo(offenderProgramReferenceId)
        assertThat(response.created).isTrue()
        assertThat(startDate).isEqualTo("2022-12-01")
        assertThat(endDate).isNull()
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
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            payRate(payBandCode = "5")
            payRate(payBandCode = "6")
            payRate(payBandCode = "7")
            courseSchedule()
            courseScheduleRule()
          }
        }
        offender = offender {
          bookingId = booking {
            courseAllocation(courseActivity, startDate = "2022-11-14")
          }.bookingId
        }
      }
    }

    @Test
    fun `should update when allocation ended`() {
      val request = upsertRequest()
        .withProgramStatusCode("END")
        .withEndDate("$yesterday")
        .withAdditionalJson(
          """
            "endReason": "WDRAWN",
            "endComment": "Withdrawn due to illness"
          """.trimMargin(),
        )
      val response = upsertAllocationIsOk(request)

      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      assertThat(response?.created).isFalse()

      val saved = repository.getOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(endDate).isEqualTo("$yesterday")
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

      val saved = repository.getOffenderProgramProfile(response!!.offenderProgramReferenceId)
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

      val endRequest = upsertRequest()
        .withProgramStatusCode("END")
        .withEndDate("$yesterday")
        .withAdditionalJson(
          """
            "endReason": "WDRAWN",
            "endComment": "Withdrawn due to illness"
          """.trimMargin(),
        )
      val response = upsertAllocationIsOk(endRequest)

      val saved = repository.getOffenderProgramProfile(response!!.offenderProgramReferenceId)
      with(saved) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(endDate).isEqualTo("$yesterday")
        assertThat(endReason?.code).isEqualTo("WDRAWN")
        assertThat(endComment).isEqualTo("Withdrawn due to illness")
        assertThat(programStatus.code).isEqualTo("END")
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
    fun `should allow updates if offender in wrong prison`() {
      lateinit var bookingInWrongPrison: OffenderBooking
      nomisDataBuilder.build {
        offender(nomsId = "A1234YY") {
          bookingInWrongPrison = booking(agencyLocationId = "MDI") {
            courseAllocation(courseActivity)
          }
        }
      }
      val request = upsertRequest().withBookingId(bookingInWrongPrison.bookingId.toString())

      upsertAllocationIsOk(request)
    }

    @Test
    fun `should allow de-allocation when not in the activity prison`() {
      nomisDataBuilder.build {
        offender = offender {
          bookingId = booking(agencyLocationId = "OUT") {
            courseAllocation(courseActivity)
          }.bookingId
        }
      }

      // De-allocation is allowed
      val request = upsertRequest()
        .withProgramStatusCode("END")
        .withEndDate("$today")
        .withAdditionalJson(
          """
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
      nomisDataBuilder.build {
        offender = offender {
          bookingId = booking(agencyLocationId = "OUT") {
            courseAllocation(courseActivity)
          }.bookingId
        }
      }

      // Suspending is allowed
      val request = upsertRequest().withAdditionalJson(
        """
        "suspended": true
        """.trimMargin(),
      )
      upsertAllocationIsOk(request)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PayBands {

      @ParameterizedTest(name = "{0}")
      @MethodSource("payBandTests")
      fun `pay band updates`(
        testDescription: String,
        initialPayBands: List<PayBandTestParameter>,
        requestedPayBand: PayBandTestParameter,
        newFirstPayBand: PayBandTestParameter,
        newSecondPayBand: PayBandTestParameter?,
      ) {
        lateinit var allocation: OffenderProgramProfile
        val payBands: MutableList<OffenderProgramProfilePayBand> = mutableListOf()
        nomisDataBuilder.build {
          offender = offender {
            bookingId = booking {
              allocation = courseAllocation(courseActivity, startDate = initialPayBands[0].startDate.toString(), endDate = initialPayBands[0].endDate?.toString()) {
                initialPayBands.forEach {
                  payBands += payBand(payBandCode = it.payBandCode, startDate = it.startDate.toString(), endDate = it.endDate?.toString())
                }
              }
            }.bookingId
          }
        }
        assertThat(allocation.startDate).isEqualTo(initialPayBands[0].startDate)
        assertThat(allocation.endDate).isEqualTo(initialPayBands[0].endDate)
        assertThat(payBands[0].id.startDate).isEqualTo(initialPayBands[0].startDate)
        val expectedAllocationStartDate = listOf(newFirstPayBand.startDate, newSecondPayBand?.startDate).mapNotNull { it }.min()

        val request = upsertRequest()
          .withPayBandCode(requestedPayBand.payBandCode)
          .withStartDate(requestedPayBand.startDate.toString())
          .withEndDate(requestedPayBand.endDate?.toString())
        upsertAllocationIsOk(request)

        val updated = repository.getOffenderProgramProfile(allocation.offenderProgramReferenceId)
        assertThat(updated.startDate).isEqualTo(expectedAllocationStartDate)
        assertThat(updated.payBands[0].id.startDate).isEqualTo(newFirstPayBand.startDate)
        assertThat(updated.payBands[0].endDate).isEqualTo(newFirstPayBand.endDate)
        assertThat(updated.payBands[0].payBand.code).isEqualTo(newFirstPayBand.payBandCode)
        if (newSecondPayBand != null) {
          assertThat(updated.payBands[1].id.startDate).isEqualTo(newSecondPayBand.startDate)
          assertThat(updated.payBands[1].endDate).isEqualTo(newSecondPayBand.endDate)
          assertThat(updated.payBands[1].payBand.code).isEqualTo(newSecondPayBand.payBandCode)
        } else {
          assertThat(updated.payBands.size).isEqualTo(1)
        }
      }

      inner class PayBandTestParameter(
        val payBandCode: String,
        val startDate: LocalDate,
        val endDate: LocalDate?,
      )

      fun payBandTests(): List<Arguments> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        return listOf(
          // Changing the pay band code for existing pay bands with various start/end dates
          Arguments.of(
            "A new pay band is active from today",
            listOf(PayBandTestParameter("5", yesterday, null)),
            PayBandTestParameter("6", yesterday, null),
            PayBandTestParameter("5", yesterday, today),
            PayBandTestParameter("6", tomorrow, null),
          ),
          Arguments.of(
            "A new pay band's end date is set",
            listOf(PayBandTestParameter("5", yesterday, null)),
            PayBandTestParameter("6", yesterday, tomorrow),
            PayBandTestParameter("5", yesterday, today),
            PayBandTestParameter("6", tomorrow, tomorrow),
          ),
          Arguments.of(
            "A new pay band's changed end date overrides the existing end date",
            listOf(PayBandTestParameter("5", yesterday, today)),
            PayBandTestParameter("6", yesterday, tomorrow),
            PayBandTestParameter("5", yesterday, today),
            PayBandTestParameter("6", tomorrow, tomorrow),
          ),
          Arguments.of(
            "A new pay band becomes effective today if old pay band is expired",
            listOf(PayBandTestParameter("5", yesterday, yesterday)),
            PayBandTestParameter("6", yesterday, null),
            PayBandTestParameter("5", yesterday, yesterday),
            PayBandTestParameter("6", today, null),
          ),
          Arguments.of(
            "A new pay band effective today has its end date applied",
            listOf(PayBandTestParameter("5", yesterday, yesterday)),
            PayBandTestParameter("6", yesterday, tomorrow),
            PayBandTestParameter("5", yesterday, yesterday),
            PayBandTestParameter("6", today, tomorrow),
          ),
          Arguments.of(
            "A new pay band with no end date overrides the existing end date",
            listOf(PayBandTestParameter("5", yesterday, today)),
            PayBandTestParameter("6", yesterday, null),
            PayBandTestParameter("5", yesterday, today),
            PayBandTestParameter("6", tomorrow, null),
          ),
          // Changing the pay band code for future pay bands with various start/end dates
          Arguments.of(
            "A new future pay band is simply updated",
            listOf(PayBandTestParameter("5", tomorrow, null)),
            PayBandTestParameter("6", tomorrow, null),
            PayBandTestParameter("6", tomorrow, null),
            null,
          ),
          Arguments.of(
            "A new future pay band's end date is added",
            listOf(PayBandTestParameter("5", tomorrow, null)),
            PayBandTestParameter("6", tomorrow, tomorrow),
            PayBandTestParameter("6", tomorrow, tomorrow),
            null,
          ),
          Arguments.of(
            "A new future pay band's end date is removed",
            listOf(PayBandTestParameter("5", tomorrow, tomorrow)),
            PayBandTestParameter("6", tomorrow, null),
            PayBandTestParameter("6", tomorrow, null),
            null,
          ),
          Arguments.of(
            "A new future pay band can move the start date forwards",
            listOf(PayBandTestParameter("5", tomorrow, null)),
            PayBandTestParameter("6", yesterday, tomorrow),
            PayBandTestParameter("6", yesterday, tomorrow),
            null,
          ),
          // Changing the start/end dates but keeping pay band same
          Arguments.of(
            "Pay band is unchanged",
            listOf(PayBandTestParameter("5", yesterday, null)),
            PayBandTestParameter("5", yesterday, null),
            PayBandTestParameter("5", yesterday, null),
            null,
          ),
          Arguments.of(
            "A pay band's end date can be updated",
            listOf(PayBandTestParameter("5", yesterday, null)),
            PayBandTestParameter("5", yesterday, tomorrow),
            PayBandTestParameter("5", yesterday, tomorrow),
            null,
          ),
          Arguments.of(
            "Pay band with an end date is unchanged",
            listOf(PayBandTestParameter("5", yesterday, tomorrow)),
            PayBandTestParameter("5", yesterday, tomorrow),
            PayBandTestParameter("5", yesterday, tomorrow),
            null,
          ),
          Arguments.of(
            "An active pay band's end date can be removed",
            listOf(PayBandTestParameter("5", yesterday, tomorrow)),
            PayBandTestParameter("5", yesterday, null),
            PayBandTestParameter("5", yesterday, null),
            null,
          ),
          Arguments.of(
            "An active pay band's start date cannot be changed but its end date can",
            listOf(PayBandTestParameter("5", yesterday, today)),
            PayBandTestParameter("5", tomorrow, tomorrow),
            PayBandTestParameter("5", yesterday, tomorrow),
            null,
          ),
          // Changing the start/end date of a future pay band
          Arguments.of(
            "A future pay band is unchanged",
            listOf(PayBandTestParameter("5", tomorrow, null)),
            PayBandTestParameter("5", tomorrow, null),
            PayBandTestParameter("5", tomorrow, null),
            null,
          ),
          Arguments.of(
            "A future pay band's end date can be added",
            listOf(PayBandTestParameter("5", tomorrow, null)),
            PayBandTestParameter("5", tomorrow, tomorrow),
            PayBandTestParameter("5", tomorrow, tomorrow),
            null,
          ),
          Arguments.of(
            "A future pay band's end date can be removed",
            listOf(PayBandTestParameter("5", tomorrow, tomorrow)),
            PayBandTestParameter("5", tomorrow, null),
            PayBandTestParameter("5", tomorrow, null),
            null,
          ),
          Arguments.of(
            "A future pay band's start date can be moved forward",
            listOf(PayBandTestParameter("5", tomorrow, null)),
            PayBandTestParameter("5", yesterday, null),
            PayBandTestParameter("5", yesterday, null),
            null,
          ),
          Arguments.of(
            "A future pay band's start date can be moved forward while an end date is added",
            listOf(PayBandTestParameter("5", tomorrow, null)),
            PayBandTestParameter("5", yesterday, tomorrow),
            PayBandTestParameter("5", yesterday, tomorrow),
            null,
          ),
          // Existing pay bands with an existing future pay band
          Arguments.of(
            "An expired pay band doesn't effect updates to a future pay band",
            listOf(
              PayBandTestParameter("5", yesterday, yesterday),
              PayBandTestParameter("6", tomorrow, tomorrow),
            ),
            PayBandTestParameter("7", yesterday, null),
            PayBandTestParameter("5", yesterday, yesterday),
            PayBandTestParameter("7", tomorrow, null),
          ),
          Arguments.of(
            "An active pay band doesn't effect updates to a future pay band",
            listOf(
              PayBandTestParameter("5", yesterday, today),
              PayBandTestParameter("6", tomorrow, tomorrow),
            ),
            PayBandTestParameter("7", yesterday, null),
            PayBandTestParameter("5", yesterday, today),
            PayBandTestParameter("7", tomorrow, null),
          ),
        )
      }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AllocationExclusions {
      private lateinit var allocation: OffenderProgramProfile

      @Test
      fun `should add a new exclusion to an existing allocation`() {
        nomisDataBuilder.build {
          offender = offender {
            bookingId = booking {
              allocation = courseAllocation(courseActivity)
            }.bookingId
          }
        }

        upsertAllocationIsOk(
          upsertRequest().withPrisonerExclusions(listOf("MON" to null)),
        )

        val updated = repository.getOffenderProgramProfile(allocation.offenderProgramReferenceId)
        assertThat(updated.offenderExclusions.size).isEqualTo(1)
        with(updated.offenderExclusions[0]) {
          assertThat(id).isGreaterThan(0)
          assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
          assertThat(courseActivity.courseActivityId).isEqualTo(this@AllocationResourceIntTest.courseActivity.courseActivityId)
          assertThat(slotCategory).isNull()
          assertThat(excludeDay).isEqualTo(WeekDay.MON)
        }
      }

      @Test
      fun `should add to existing exclusions`() {
        lateinit var exclusion: OffenderActivityExclusion
        nomisDataBuilder.build {
          offender = offender {
            bookingId = booking {
              allocation = courseAllocation(courseActivity) {
                exclusion = exclusion(null, WeekDay.MON)
              }
            }.bookingId
          }
        }

        upsertAllocationIsOk(
          upsertRequest().withPrisonerExclusions(listOf("TUE" to "AM", "MON" to null)),
        )

        val updated = repository.getOffenderProgramProfile(allocation.offenderProgramReferenceId)
        assertThat(updated.offenderExclusions.size).isEqualTo(2)
        with(updated.offenderExclusions.find { it.excludeDay.name == "MON" }!!) {
          assertThat(id).isEqualTo(exclusion.id)
          assertThat(slotCategory).isNull()
        }
        with(updated.offenderExclusions.find { it.excludeDay.name == "TUE" }!!) {
          assertThat(id).isGreaterThan(0).isNotEqualTo(exclusion.id)
          assertThat(slotCategory).isEqualTo(SlotCategory.AM)
        }
      }

      @Test
      fun `should replace exclusions`() {
        lateinit var exclusion: OffenderActivityExclusion
        lateinit var exclusion2: OffenderActivityExclusion
        nomisDataBuilder.build {
          offender = offender {
            bookingId = booking {
              allocation = courseAllocation(courseActivity) {
                exclusion = exclusion(SlotCategory.AM, WeekDay.WED)
                exclusion2 = exclusion(SlotCategory.PM, WeekDay.SAT)
              }
            }.bookingId
          }
        }

        upsertAllocationIsOk(
          upsertRequest().withPrisonerExclusions(listOf("THU" to "PM", "SUN" to "ED")),
        )

        val updated = repository.getOffenderProgramProfile(allocation.offenderProgramReferenceId)
        assertThat(updated.offenderExclusions.size).isEqualTo(2)
        with(updated.offenderExclusions.find { it.excludeDay.name == "THU" }!!) {
          assertThat(id).isGreaterThan(0).isNotIn(exclusion.id, exclusion2.id)
          assertThat(slotCategory).isEqualTo(SlotCategory.PM)
        }
        with(updated.offenderExclusions.find { it.excludeDay.name == "SUN" }!!) {
          assertThat(id).isGreaterThan(0).isNotIn(exclusion.id, exclusion2.id)
          assertThat(slotCategory).isEqualTo(SlotCategory.ED)
        }
      }

      @Test
      fun `should remove exclusions`() {
        nomisDataBuilder.build {
          offender = offender {
            bookingId = booking {
              allocation = courseAllocation(courseActivity) {
                exclusion(null, WeekDay.MON)
                exclusion(SlotCategory.AM, WeekDay.TUE)
              }
            }.bookingId
          }
        }

        upsertAllocationIsOk()

        val updated = repository.getOffenderProgramProfile(allocation.offenderProgramReferenceId)
        assertThat(updated.offenderExclusions.size).isEqualTo(0)
      }
    }
  }

  @Nested
  inner class DuplicateAllocation {
    @Test
    fun `duplicate allocations can be worked around by deleting one of them`() {
      lateinit var duplicate: OffenderProgramProfile
      nomisDataBuilder.build {
        offender = offender {
          bookingId = booking {
            courseAllocation(courseActivity)
            duplicate = courseAllocation(courseActivity)
          }.bookingId
        }
      }

      // unable to update the allocation because of a duplicate
      val request = upsertRequest().withEndDate("$today")
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/allocation")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(request))
        .exchange()
        .expectStatus().is5xxServerError
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Query did not return a unique result")
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
