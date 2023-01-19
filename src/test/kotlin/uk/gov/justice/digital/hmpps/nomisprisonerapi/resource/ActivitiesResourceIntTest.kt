package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateOffenderProgramProfileRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateOffenderProgramProfileResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.PayRateRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import java.math.BigDecimal
import java.time.LocalDate

private const val prisonId = "LEI"
private const val roomId: Long = -8 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val programCode = "INTTEST"
private const val iepLevel = "STD"

private val createActivityRequest: () -> CreateActivityRequest = {
  CreateActivityRequest(
    prisonId = prisonId,
    code = "CA",
    programCode = programCode,
    description = "test description",
    capacity = 23,
    startDate = LocalDate.parse("2022-10-31"),
    endDate = LocalDate.parse("2022-11-30"),
    minimumIncentiveLevelCode = iepLevel,
    internalLocationId = roomId,
    payRates = listOf(
      PayRateRequest(
        incentiveLevel = "BAS",
        payBand = "5",
        rate = BigDecimal(3.2),
      )
    )
  )
}

class ActivitiesResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  lateinit var offenderAtMoorlands: Offender

  @BeforeEach
  internal fun setup() {
    repository.save(
      ProgramService(
        programId = 20,
        programCode = programCode,
        description = "test description",
        active = true,
      )
    )
    offenderAtMoorlands =
      repository.save(OffenderBuilder(nomsId = "A1234TT").withBooking(OffenderBookingBuilder(agencyLocationId = "MDI")))
  }

  @AfterEach
  internal fun cleanUp() {
    repository.delete(offenderAtMoorlands)
    repository.deleteActivities()
    repository.deleteProgramServices()
  }

  @Nested
  inner class CreateActivity {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/activities")
        .body(BodyInserters.fromValue(createActivityRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createActivityRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createActivityRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access with prison not found`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createActivityRequest().copy(prisonId = "ZZX")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `will create activity with correct details`() {

      val id = callCreateEndpoint()

      // Spot check that the database has been populated.
      val courseActivity = repository.lookupActivity(id)

      assertThat(courseActivity.courseActivityId).isEqualTo(id)
      assertThat(courseActivity.capacity).isEqualTo(23)
      assertThat(courseActivity.prison.id).isEqualTo(prisonId)
      assertThat(courseActivity.payRates?.first()?.halfDayRate).isCloseTo(
        BigDecimal(0.4),
        within(BigDecimal("0.001"))
      )
    }

    private fun callCreateEndpoint(): Long {
      val response = webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "prisonId" : "$prisonId",
            "code" : "CA",
            "programCode" : "$programCode",
            "description" : "test description",
            "capacity" : 23,
            "startDate" : "2022-10-31",
            "endDate" : "2022-11-30",
            "minimumIncentiveLevelCode" : "$iepLevel",
            "internalLocationId" : "$roomId",
            "payRates" : [ {
                "incentiveLevel" : "BAS",
                "payBand" : "5",
                "rate" : 0.4
                } ]
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateActivityResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.courseActivityId).isGreaterThan(0)
      return response!!.courseActivityId
    }
  }

  @Nested
  inner class CreateOffenderProgramProfile {

    lateinit var courseActivity: CourseActivity

    private val createOffenderProgramProfileRequest: () -> CreateOffenderProgramProfileRequest = {
      CreateOffenderProgramProfileRequest(
        bookingId = repository.lookupOffender("A1234TT")?.latestBooking()?.bookingId!!,
        startDate = LocalDate.parse("2022-10-31"),
        endDate = LocalDate.parse("2022-11-30"),
      )
    }

    @BeforeEach
    internal fun setup() {

      courseActivity = repository.save(
        CourseActivity(
          code = "CA",
          program = repository.lookupProgramService(20),
          caseloadId = "LEI",
          prison = repository.lookupAgency("LEI"),
          description = "test description",
          capacity = 23,
          active = true,
          scheduleStartDate = LocalDate.parse("2022-10-31"),
          scheduleEndDate = LocalDate.parse("2022-11-30"),
          iepLevel = repository.lookupIepLevel("STD"),
          internalLocation = repository.lookupAgencyInternalLocationByDescription("LEI-A-1-7"),
        )
      )
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
      webTestClient.post().uri("/activities/999888")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `access with booking not found`() {
      webTestClient.post().uri("/activities/${courseActivity.courseActivityId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createOffenderProgramProfileRequest().copy(bookingId = 999888)))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `will create profile with correct details`() {

      val bookingId = repository.lookupOffender("A1234TT")?.latestBooking()?.bookingId!!
      val id = callCreateEndpoint(courseActivity.courseActivityId, bookingId)

      // Spot check that the database has been populated correctly.
      val persistedRecord = repository.lookupOffenderProgramProfile(id)
      assertThat(persistedRecord.courseActivity?.courseActivityId).isEqualTo(courseActivity.courseActivityId)
      with(persistedRecord) {
        assertThat(offenderBooking.bookingId).isEqualTo(bookingId)
        assertThat(program.programCode).isEqualTo(programCode)
        assertThat(startDate).isEqualTo(LocalDate.parse("2022-10-31"))
        assertThat(endDate).isEqualTo(LocalDate.parse("2022-11-30"))
      }
    }

    private fun callCreateEndpoint(courseActivityId: Long, bookingId: Long): Long {
      val response = webTestClient.post().uri("/activities/$courseActivityId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "bookingId" : "$bookingId",
            "startDate" : "2022-10-31",
            "endDate" : "2022-11-30"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateOffenderProgramProfileResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.offenderProgramReferenceId).isGreaterThan(0)
      return response!!.offenderProgramReferenceId
    }
  }
}
