package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateActivityResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.PayRateRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import java.math.BigDecimal
import java.time.LocalDate

private const val prisonId = "LEI"
private const val roomId: Long = -8
private const val programCode = "INTTEST"
private const val iepLevel = "STD"

private val createActivity: () -> CreateActivityRequest = {
  CreateActivityRequest(
    prisonId = prisonId,
    code = "CA",
    programCode = programCode,
    description = "test description",
    capacity = 23,
    startDate = LocalDate.parse("2022-10-31"),
    endDate = LocalDate.parse("2022-11-30"),
    minimumIncentiveLevel = iepLevel,
    internalLocation = roomId,
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

  @DisplayName("Create")
  @Nested
  inner class Create {
    @BeforeEach
    internal fun createProgramService() {
      repository.save(
        ProgramService(
          programId = 20,
          programCode = programCode,
          description = "test description",
          active = true,
        )
      )
    }

    @AfterEach
    internal fun cleanUp() {
      repository.deleteActivities()
      repository.deleteProgramServices()
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/activities")
        .body(BodyInserters.fromValue(createActivity()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createActivity()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createActivity()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access with prison not found`() {
      webTestClient.post().uri("/activities")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .body(BodyInserters.fromValue(createActivity().copy(prisonId = "ZZX")))
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
            "minimumIncentiveLevel" : "$iepLevel",
            "internalLocation" : "$roomId",
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
}
