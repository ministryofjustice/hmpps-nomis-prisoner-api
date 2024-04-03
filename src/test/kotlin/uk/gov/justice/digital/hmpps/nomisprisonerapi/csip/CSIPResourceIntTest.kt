package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisData
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

class CSIPResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var reportingStaff: Staff
  private lateinit var offender: Offender
  private lateinit var csip1: CSIPReport
  private lateinit var csip2: CSIPReport
  private lateinit var csip3: CSIPReport

  @BeforeEach
  internal fun createCSIPReports() {
    nomisDataBuilder.build {
      setUpStaffAndOffender()
      csip1 = csipReport(offender = offender, offenderBooking = offender.latestBooking()) {
        plan(reportingStaff = reportingStaff)
      }
      csip2 = csipReport(offender = offender, offenderBooking = offender.latestBooking())
      csip3 = csipReport(offender = offender, offenderBooking = offender.latestBooking())
    }
  }

  fun NomisData.setUpStaffAndOffender() {
    reportingStaff = staff(firstName = "FRED", lastName = "STAFF") {
      account(username = "FREDSTAFF")
    }

    offender = offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
      booking(agencyLocationId = "MDI")
    }
  }

  @AfterEach
  internal fun deleteCSIPReports() {
    repository.delete(csip1)
    repository.delete(csip2)
    repository.delete(csip3)
    repository.deleteOffenders()
    repository.delete(reportingStaff)
  }

  @Nested
  @DisplayName("GET /csip/ids")
  inner class GetCSIPReportIds {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/csip/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/csip/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/csip/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all csipReport ids - no filter specified`() {
      webTestClient.get().uri("/csip/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/csip/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/csip/ids")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("GET /csip/{id}")
  inner class GetCSIPReport {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/csip/${csip1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/csip/${csip1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/csip/${csip1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown csip report should return not found`() {
      webTestClient.get().uri("/csip/999999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          Assertions.assertThat(it).contains("Not Found: CSIP with id=999999 does not exist")
        }
    }

    @Test
    fun `will return a csip Report by Id`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("type.code").isEqualTo("INT")
        .jsonPath("type.description").isEqualTo("Intimidation")
        .jsonPath("location.code").isEqualTo("LIB")
        .jsonPath("location.description").isEqualTo("Library")
        .jsonPath("areaOfWork.code").isEqualTo("EDU")
        .jsonPath("areaOfWork.description").isEqualTo("Education")
    }

    @Test
    fun `will return csip plan data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .consumeWith(System.out::println)
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("plans[0].id").isEqualTo(1)
        .jsonPath("plans[0].identifiedNeed").isEqualTo("They need help")
        .jsonPath("plans[0].intervention").isEqualTo("Support their work")
    }
  }
}
