package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

class IncidentResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var reportingStaff1: Staff
  private lateinit var reportingStaff2: Staff
  private lateinit var incident1: Incident
  private lateinit var incident2: Incident
  private lateinit var incident3: Incident

  @BeforeEach
  internal fun createIncidents() {
    nomisDataBuilder.build {
      reportingStaff1 = staff(firstName = "FRED", lastName = "STAFF") {
        account(username = "FREDSTAFF")
      }
      reportingStaff2 = staff(firstName = "JANE", lastName = "STAFF") {
        account(username = "JANESTAFF")
      }

      incident1 = incident(
        title = "Fight in the cell",
        description = "Offenders were injured and furniture was damaged.",
        reportingStaff = reportingStaff1,
      )
      incident2 = incident(reportingStaff = reportingStaff1)
      incident3 = incident(reportingStaff = reportingStaff2)
    }
  }

  @AfterEach
  internal fun deleteIncidents() {
    repository.delete(incident1)
    repository.delete(incident2)
    repository.delete(incident3)
    repository.delete(reportingStaff1)
    repository.delete(reportingStaff2)
  }

  @Nested
  inner class Security {
    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/incidents/ids")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/incidents/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access unauthorised with no auth token`() {
      webTestClient.get().uri("/incidents/ids")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Nested
  @DisplayName("GET /incidents/ids")
  inner class GetQuestionnaireIds {
    @Test
    fun `get all question ids - no filter specified`() {
      webTestClient.get().uri("/incidents/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/incidents/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
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
        it.path("/incidents/ids")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
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
  @DisplayName("GET /incidents/{incidentId}")
  inner class GetIncident {

    @Test
    fun `will return an incident by Id`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("title").isEqualTo("Fight in the cell")
        .jsonPath("description").isEqualTo("Offenders were injured and furniture was damaged.")
        .jsonPath("status").isEqualTo("AWAN")
        .jsonPath("type.code").isEqualTo("SELF HARM")
        .jsonPath("type.description").isEqualTo("DELIBERATE SELF HARM")
        .jsonPath("lockedResponse").isEqualTo(false)
        .jsonPath("incidentDateTime").isEqualTo("2023-12-30T13:45:00")
        .jsonPath("reportedStaff.staffId").isEqualTo(reportingStaff1.id)
        .jsonPath("reportedStaff.username").isEqualTo("FREDSTAFF")
        .jsonPath("reportedStaff.firstName").isEqualTo("FRED")
        .jsonPath("reportedStaff.lastName").isEqualTo("STAFF")
        .jsonPath("reportedDateTime").isEqualTo("2024-01-02T09:30:00")
    }
  }
}
