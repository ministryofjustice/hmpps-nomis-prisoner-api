package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.CreateVisitResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderVisitBalanceAdjustmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import java.time.LocalDateTime
import java.time.LocalTime

private const val offenderBookingId = -10L
private const val offenderNo = "A1234AJ"
private const val prisonId = "BXI"
private val createVisitRequest = CreateVisitRequest(
  visitType = "SCON",
  startDateTime = LocalDateTime.parse("2021-11-04T12:05"),
  endTime = LocalTime.parse("13:04"),
  prisonId = prisonId,
  visitorPersonIds = listOf(-7L, -8L, -9L),
  decrementBalances = true,
  visitRoomId = "VISIT",
)

class VisitResourceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var visitRepository: VisitRepository

  @Autowired
  lateinit var offenderVisitBalanceAdjustmentRepository: OffenderVisitBalanceAdjustmentRepository

  @Autowired
  lateinit var transactionManager: PlatformTransactionManager

  @DisplayName("Create")
  @Nested
  inner class CreateVisitRequest {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .body(BodyInserters.fromValue(createVisitRequest))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createVisitRequest))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit forbidden with wrong role`() {
      webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createVisitRequest))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create visit with offender not found`() {
      webTestClient.post().uri("/prisoners/Z9999ZZ/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_NOMIS")))
        .body(BodyInserters.fromValue(createVisitRequest))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `create visit with invalid person`() {
      val error = webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_NOMIS")))
        .body(BodyInserters.fromValue(createVisitRequest.copy(visitorPersonIds = listOf(-7L, -99L))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
      assertThat(error?.userMessage).isEqualTo("Bad request: Person with id=-99 does not exist")
    }

    @Test
    fun `create visit with invalid offenderNo`() {
      webTestClient.post().uri("/prisoners/ZZ000ZZ/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_NOMIS")))
        .body(BodyInserters.fromValue(createVisitRequest))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult().responseBody
    }

    @Test
    fun `create visit success`() {
      val response = webTestClient.post().uri("/prisoners/$offenderNo/visits")
        .headers(setAuthorisation(roles = listOf("ROLE_UPDATE_NOMIS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "visitType"         : "SCON",
            "startDateTime"     : "2021-11-04T12:05",
            "endTime"           : "13:04",
            "prisonId"          : "$prisonId",
            "visitorPersonIds"  : [-7, -8, -9],
            "decrementBalances" : true,
            "visitRoomId"       : "VISIT"
          }"""
          )
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateVisitResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.visitId).isGreaterThan(0)

      // Spot check that the database has been populated.
      TransactionTemplate(transactionManager).execute {
        val visit = visitRepository.findById(response?.visitId!!).orElseThrow()
        assertThat(visit.endTime).isEqualTo(LocalDateTime.parse("2021-11-04T13:04"))
        assertThat(visit.offenderBooking?.bookingId).isEqualTo(offenderBookingId)
        assertThat(visit.visitors).extracting("person.id", "eventStatus.code").containsExactly(
          Tuple.tuple(null, "SCH"),
          Tuple.tuple(-7L, "SCH"),
          Tuple.tuple(-8L, "SCH"),
          Tuple.tuple(-9L, "SCH"),
        )

        val balanceAdjustment = offenderVisitBalanceAdjustmentRepository.findAll()

        assertThat(balanceAdjustment).extracting("offenderBooking.bookingId", "remainingVisitOrders").containsExactly(
          Tuple.tuple(-10L, -1),
        )
      }
    }
  }
}
