package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.CreateOffenderProgramProfileRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.EndOffenderProgramProfileRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.activities.api.OffenderProgramProfileResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val PRISON_ID = "MDI"
private const val PROGRAM_CODE = "INTTEST"

class AllocationResourceIntTest : IntegrationTestBase() {

  private val TEN_DAYS_TIME = LocalDate.now().plusDays(10)

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var courseActivityBuilderFactory: CourseActivityBuilderFactory

  private lateinit var offenderAtMoorlands: Offender
  private lateinit var offenderAtOtherPrison: Offender

  private fun callCreateActivityEndpoint(courseActivityId: Long, bookingId: Long): Long {
    val response = webTestClient.post().uri("/activities/$courseActivityId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        BodyInserters.fromValue(
          """{
            "bookingId"  : "$bookingId",
            "startDate"  : "2022-11-14",
            "endDate"    : "${TEN_DAYS_TIME.format(DateTimeFormatter.ISO_DATE)}",
            "payBandCode": "5"
          }""",
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody(OffenderProgramProfileResponse::class.java)
      .returnResult().responseBody
    assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
    return response!!.offenderProgramReferenceId
  }

  @BeforeEach
  fun setup() {
    repository.save(ProgramServiceBuilder())
    offenderAtMoorlands =
      repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID)))
    offenderAtOtherPrison =
      repository.save(OffenderBuilder(nomsId = "A1234XX").withBooking(OffenderBookingBuilder(agencyLocationId = "BXI")))
  }

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  @Nested
  inner class CreateOffenderProgramProfile {

    private lateinit var courseActivity: CourseActivity

    private val createOffenderProgramProfileRequest: () -> CreateOffenderProgramProfileRequest = {
      CreateOffenderProgramProfileRequest(
        bookingId = offenderAtMoorlands.latestBooking().bookingId,
        startDate = LocalDate.parse("2022-11-14"),
        endDate = TEN_DAYS_TIME,
        payBandCode = "5",
      )
    }

    @BeforeEach
    fun setup() {
      courseActivity = repository.save(courseActivityBuilderFactory.builder(prisonId = PRISON_ID))
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access with activity not found`() {
      val response = webTestClient.post().uri("/activities/999888")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isNotFound
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Not Found: Course activity with id=999888 does not exist")
    }

    @Test
    fun `access with booking not found`() {
      val response = webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest().copy(bookingId = 999888)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Booking with id=999888 does not exist")
    }

    @Test
    fun `allocation already exists`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isCreated
      val response = webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Offender Program Profile with courseActivityId=${courseActivity.courseActivityId} and bookingId=${offenderAtMoorlands.latestBooking().bookingId} already exists")
    }

    @Test
    fun `prisoner at different prison`() {
      val response = webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest().copy(bookingId = offenderAtOtherPrison.latestBooking().bookingId)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Prisoner is at prison=BXI, not the Course activity prison=$PRISON_ID")
    }

    @Test
    fun `activity expired`() {
      val expired = repository.save(courseActivityBuilderFactory.builder(prisonId = PRISON_ID, endDate = "2022-12-14"))
      val response = webTestClient.post().uri("/activities/${expired.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).isEqualTo("Bad request: Course activity with id=${expired.courseActivityId} has expired")
    }

    @Test
    fun `start date missing`() {
      val response = webTestClient.post().uri("/activities/4567")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "bookingId" : "1234",
            "endDate"   : "2022-11-30"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.userMessage).contains("value failed for JSON property startDate due to missing (therefore NULL) value")
    }

    @Test
    fun `will create profile with correct details`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      val id = callCreateActivityEndpoint(courseActivity.courseActivityId, bookingId)

      // Spot check that the database has been populated correctly.
      val persistedRecord = repository.lookupOffenderProgramProfile(id)
      assertThat(persistedRecord.courseActivity?.courseActivityId).isEqualTo(courseActivity.courseActivityId)
      with(persistedRecord) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(program.programCode).isEqualTo(PROGRAM_CODE)
        assertThat(startDate).isEqualTo(LocalDate.parse("2022-11-14"))
        assertThat(endDate).isEqualTo(TEN_DAYS_TIME)
        val payBand = payBands.first()
        assertThat(payBand.offenderProgramProfile.offenderProgramReferenceId).isEqualTo(offenderProgramReferenceId)
        assertThat(payBand.startDate).isEqualTo(startDate)
        assertThat(payBand.endDate).isEqualTo(endDate)
        assertThat(payBand.payBand.code).isEqualTo("5")
      }
    }
  }

  @Nested
  inner class EndOffenderProgramProfile {

    private lateinit var courseActivity: CourseActivity
    private var bookingId: Long = 0

    private val endOffenderProgramProfileRequest: () -> EndOffenderProgramProfileRequest = {
      EndOffenderProgramProfileRequest(
        endDate = LocalDate.parse("2023-01-28"),
        endReason = "REL",
        endComment = "A comment",
      )
    }

    @BeforeEach
    fun setup() {
      courseActivity = repository.save(courseActivityBuilderFactory.builder(prisonId = PRISON_ID))
      bookingId = offenderAtMoorlands.latestBooking().bookingId
      callCreateActivityEndpoint(courseActivity.courseActivityId, bookingId)
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `course activity not found`() {
      webTestClient.put().uri("/activities/999888/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Not Found: Course activity with id=999888 does not exist")
    }

    @Test
    fun `booking not found`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/999888/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Not Found: Booking with id=999888 does not exist")
    }

    @Test
    fun `the prisoner is not allocated to the course`() {
      val otherBookingId = offenderAtOtherPrison.latestBooking().bookingId

      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$otherBookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage")
        .isEqualTo("Bad request: Offender Program Profile with courseActivityId=${courseActivity.courseActivityId} and bookingId=$otherBookingId and status=ALLOC does not exist")
    }

    @Test
    fun `end date missing`() {
      val response =
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "endReason" : "REL"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.userMessage).contains("value failed for JSON property endDate due to missing (therefore NULL) value")
    }

    @Test
    fun `end date invalid`() {
      val response =
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "endDate" : "invalid"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.userMessage).contains("Text 'invalid' could not be parsed at index 0")
    }

    @Test
    fun `text too long`() {
      val response =
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(endOffenderProgramProfileRequest().copy(endReason = "ThirteenChars")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody(ErrorResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.userMessage).contains("length must be between 0 and 12")
    }

    @Test
    fun `the end reason is invalid`() {
      webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/booking-id/$bookingId/end")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(endOffenderProgramProfileRequest().copy(endReason = "DUFF")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody()
        .jsonPath("userMessage").isEqualTo("Bad request: End reason code=DUFF is invalid")
    }

    @Test
    fun `will create profile with correct details`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      val id = callEndpoint(courseActivity.courseActivityId, bookingId)

      // Spot check that the database has been populated correctly.
      val persistedRecord = repository.lookupOffenderProgramProfile(id)
      assertThat(persistedRecord.courseActivity?.courseActivityId).isEqualTo(courseActivity.courseActivityId)
      with(persistedRecord) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(endDate).isEqualTo(LocalDate.parse("2023-01-28"))
        assertThat(endReason?.code).isEqualTo("REL")
        assertThat(endComment).isEqualTo("A comment")
      }
    }

    private fun callEndpoint(courseActivityId: Long, bookingId: Long): Long {
      val response =
        webTestClient.put().uri("/activities/$courseActivityId/booking-id/$bookingId/end")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "endDate"   : "2023-01-28",
            "endReason" : "REL",
            "endComment": "A comment"
          }""",
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody(OffenderProgramProfileResponse::class.java)
          .returnResult().responseBody
      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      return response!!.offenderProgramReferenceId
    }
  }
}
