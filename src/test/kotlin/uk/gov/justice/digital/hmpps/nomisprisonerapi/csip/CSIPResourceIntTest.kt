package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import java.time.LocalDate

class CSIPResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var csip1: CSIPReport
  private lateinit var csip2: CSIPReport
  private lateinit var csip3: CSIPReport

  @BeforeEach
  internal fun createCSIPReports() {
    nomisDataBuilder.build {
      offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
        booking(agencyLocationId = "MDI") {
          csip1 = csipReport {
            scs(
              "ACC", reasonForDecision = "Further help needed", outcomeCreateUsername = "JAMES",
              outcomeCreateDate = LocalDate.now(),
            )
            plan(progression = "Behaviour improved")
            investigation(
              staffInvolved = "There were numerous staff involved",
              evidenceSecured = "Account by Prisoner Officer",
              reasonOccurred = "Unsure why",
              usualBehaviour = "Helpful and polite",
              trigger = "Mental Health",
              protectiveFactors = "Supported by staff",
            )
            interview(comment = "Helping with behaviour")
          }
          csip2 = csipReport {}
          csip3 = csipReport {}
        }
      }
    }
  }

  @AfterEach
  internal fun deleteCSIPReports() {
    repository.delete(csip1)
    repository.delete(csip2)
    repository.delete(csip3)
    repository.deleteOffenders()
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
          assertThat(it).contains("Not Found: CSIP with id=999999 does not exist")
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
        .jsonPath("offender.offenderNo").isEqualTo("A1234TT")
        .jsonPath("offender.firstName").isEqualTo("Bob")
        .jsonPath("offender.lastName").isEqualTo("Smith")
        .jsonPath("type.code").isEqualTo("INT")
        .jsonPath("type.description").isEqualTo("Intimidation")
        .jsonPath("location.code").isEqualTo("LIB")
        .jsonPath("location.description").isEqualTo("Library")
        .jsonPath("areaOfWork.code").isEqualTo("EDU")
        .jsonPath("areaOfWork.description").isEqualTo("Education")
        .jsonPath("reportedBy").isEqualTo("Jane Reporter")
    }

    @Test
    fun `will return csip plan data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("plans[0].id").isEqualTo(csip1.plans[0].id)
        .jsonPath("plans[0].identifiedNeed").isEqualTo("They need help")
        .jsonPath("plans[0].intervention").isEqualTo("Support their work")
        .jsonPath("plans[0].progression").isEqualTo("Behaviour improved")
        .jsonPath("plans[0].referredBy").isEqualTo("Fred Bloggs")
        .jsonPath("plans[0].createdDate").isNotEmpty()
        .jsonPath("plans[0].targetDate").isEqualTo(LocalDate.now().toString())
    }

    @Test
    fun `will return csip safer custody screening data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("saferCustodyScreening.outcome.code").isEqualTo("ACC")
        .jsonPath("saferCustodyScreening.outcome.description").isEqualTo("ACCT Supporting")
        .jsonPath("saferCustodyScreening.recordedBy").isEqualTo("JAMES")
        .jsonPath("saferCustodyScreening.recordedDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("saferCustodyScreening.reasonForDecision").isEqualTo("Further help needed")
    }

    @Test
    fun `will return csip investigation data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("investigation.staffInvolved").isEqualTo("There were numerous staff involved")
        .jsonPath("investigation.evidenceSecured").isEqualTo("Account by Prisoner Officer")
        .jsonPath("investigation.reasonOccurred").isEqualTo("Unsure why")
        .jsonPath("investigation.usualBehaviour").isEqualTo("Helpful and polite")
        .jsonPath("investigation.trigger").isEqualTo("Mental Health")
        .jsonPath("investigation.protectiveFactors").isEqualTo("Supported by staff")
    }

    @Test
    fun `will return csip investigation interview data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("investigation.interviews[0].interviewee").isEqualTo("Jim the Interviewee")
        .jsonPath("investigation.interviews[0].date").isNotEmpty
        .jsonPath("investigation.interviews[0].role.code").isEqualTo("WITNESS")
        .jsonPath("investigation.interviews[0].role.description").isEqualTo("Witness")
        .jsonPath("investigation.interviews[0].comments").isEqualTo("Helping with behaviour")
    }
  }
}
