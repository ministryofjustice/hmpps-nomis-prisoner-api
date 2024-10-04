package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CSIPResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var csip1: CSIPReport
  private lateinit var csip2: CSIPReport
  private lateinit var csip3: CSIPReport
  private var document1Id: Long = 0

  @BeforeEach
  internal fun createTestCSIPReports() {
    nomisDataBuilder.build {
      staff(firstName = "FRED", lastName = "JAMES") {
        account(username = "FRED.JAMES")
      }
      val csipTemplate = template(name = "CSIPA1_FNP", description = "This is the CSIP Template 1")
      val csipTemplate2 = template(name = "CSIPA3_HMP", description = "This is the CSIP Template 2")
      val otherTemplate = template(name = "OTHER_TEMPLT", description = "This is a different Template")
      offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
        booking(agencyLocationId = "MDI") {
          document(template = csipTemplate)
          document(template = csipTemplate2)
          document(template = otherTemplate)
          csip1 = csipReport(
            incidentDate = LocalDate.parse("2024-01-25"), incidentTime = LocalTime.parse("12:34"),
            staffAssaulted = true, staffAssaultedName = "Assaulted Person",
            releaseDate = LocalDate.parse("2028-11-25"),
            involvement = "PER", concern = "It may happen again", knownReasons = "Disagreement", otherInformation = "Two other offenders involved",
            referralComplete = true, referralCompletedBy = "FRED.JAMES", referralCompletedDate = LocalDate.parse("2024-04-15"),
            caseManager = "The Case Manager", planReason = "Will help offender", firstCaseReviewDate = LocalDate.parse("2024-08-03"),
            logNumber = "MDI-1234",
          ) {
            factor()
            factor(factor = "MED", comment = "Wrong medication given")
            scs()
            investigation(
              staffInvolved = "There were numerous staff involved",
              evidenceSecured = "Account by Prisoner Officer",
              reasonOccurred = "Unsure why",
              usualBehaviour = "Helpful and polite",
              trigger = "Mental Health",
              protectiveFactors = "Supported by staff",
            )
            interview(comment = "Helping with behaviour")
            decision()
            plan(progression = "Behaviour improved")
            review {
              attendee(name = "Fred Attendee", role = "Witness", attended = true, contribution = "helped")
            }
          }
          csip2 = csipReport {}
          csip3 = csipReport {}
        }
      }
    }
    document1Id = csip1.offenderBooking.documents[0].id
  }

  @AfterEach
  internal fun deleteCSIPReports() {
    repository.deleteAllCSIPReports()
    repository.deleteOffenders()
    repository.deleteTemplates()
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
    fun `will return a csip Report by Id with minimal data`() {
      webTestClient.get().uri("/csip/${csip2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip2.id)
        .jsonPath("offender.offenderNo").isEqualTo("A1234TT")
        .jsonPath("offender.firstName").isEqualTo("Bob")
        .jsonPath("offender.lastName").isEqualTo("Smith")
        .jsonPath("originalAgencyId").isEqualTo("MDI")
        .jsonPath("logNumber").doesNotExist()
        .jsonPath("bookingId").isEqualTo(csip2.offenderBooking.bookingId)
        .jsonPath("incidentDate").isNotEmpty
        .jsonPath("incidentTime").doesNotExist()
        .jsonPath("type.code").isEqualTo("INT")
        .jsonPath("type.description").isEqualTo("Intimidation")
        .jsonPath("location.code").isEqualTo("LIB")
        .jsonPath("location.description").isEqualTo("Library")
        .jsonPath("areaOfWork.code").isEqualTo("EDU")
        .jsonPath("areaOfWork.description").isEqualTo("Education")
        .jsonPath("reportedBy").isEqualTo("Jane Reporter")
        .jsonPath("reportedDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("proActiveReferral").isEqualTo(false)
        .jsonPath("staffAssaulted").isEqualTo(false)
        .jsonPath("staffAssaultedName").doesNotExist()
        .jsonPath("reportDetails.factors").isEmpty
        .jsonPath("reportDetails.saferCustodyTeamInformed").isEqualTo(false)
        .jsonPath("reportDetails.referralComplete").isEqualTo(false)
        .jsonPath("saferCustodyScreening").isEmpty
        .jsonPath("plans").isEmpty
        .jsonPath("reviews").isEmpty
        .jsonPath("investigation.staffInvolved").doesNotExist()
        .jsonPath("investigation.interviews").isEmpty
        .jsonPath("decision.recordedBy").doesNotExist()
        .jsonPath("decision.actions.openCSIPAlert").isEqualTo(false)
        .jsonPath("decision.actions.nonAssociationsUpdated").isEqualTo(false)
        .jsonPath("decision.actions.observationBook").isEqualTo(false)
        .jsonPath("decision.actions.unitOrCellMove").isEqualTo(false)
        .jsonPath("decision.actions.csraOrRsraReview").isEqualTo(false)
        .jsonPath("decision.actions.serviceReferral").isEqualTo(false)
        .jsonPath("decision.actions.simReferral").isEqualTo(false)
        .jsonPath("createDateTime").isNotEmpty
        .jsonPath("createdBy").isEqualTo("FRED.JAMES")
        .jsonPath("createdByDisplayName").isEqualTo("FRED JAMES")
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
        .jsonPath("originalAgencyId").isEqualTo("MDI")
        .jsonPath("logNumber").isEqualTo("MDI-1234")
        .jsonPath("bookingId").isEqualTo(csip1.offenderBooking.bookingId)
        .jsonPath("incidentDate").isEqualTo("2024-01-25")
        .jsonPath("incidentTime").isEqualTo("12:34:00")
        .jsonPath("type.code").isEqualTo("INT")
        .jsonPath("type.description").isEqualTo("Intimidation")
        .jsonPath("location.code").isEqualTo("LIB")
        .jsonPath("location.description").isEqualTo("Library")
        .jsonPath("areaOfWork.code").isEqualTo("EDU")
        .jsonPath("areaOfWork.description").isEqualTo("Education")
        .jsonPath("reportedBy").isEqualTo("Jane Reporter")
        .jsonPath("reportedDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("proActiveReferral").isEqualTo(false)
        .jsonPath("staffAssaulted").isEqualTo(true)
        .jsonPath("staffAssaultedName").isEqualTo("Assaulted Person")
        .jsonPath("createDateTime").isNotEmpty
        .jsonPath("createdBy").isEqualTo("FRED.JAMES")
        .jsonPath("createdByDisplayName").isEqualTo("FRED JAMES")
    }

    @Test
    fun `will return CSIP Report Additional Details by Id`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("reportDetails.releaseDate").isEqualTo("2028-11-25")
        .jsonPath("reportDetails.involvement.code").isEqualTo("PER")
        .jsonPath("reportDetails.involvement.description").isEqualTo("Perpetrator")
        .jsonPath("reportDetails.concern").isEqualTo("It may happen again")
        .jsonPath("reportDetails.factors[0].id").isEqualTo(csip1.factors[0].id)
        .jsonPath("reportDetails.factors[0].type.code").isEqualTo("BUL")
        .jsonPath("reportDetails.factors[0].type.description").isEqualTo("Bullying")
        .jsonPath("reportDetails.factors[0].comment").doesNotExist()
        .jsonPath("reportDetails.factors[1].id").isEqualTo(csip1.factors[1].id)
        .jsonPath("reportDetails.factors[1].type.code").isEqualTo("MED")
        .jsonPath("reportDetails.factors[1].type.description").isEqualTo("Medication")
        .jsonPath("reportDetails.factors[1].comment").isEqualTo("Wrong medication given")
        .jsonPath("reportDetails.knownReasons").isEqualTo("Disagreement")
        .jsonPath("reportDetails.otherInformation").isEqualTo("Two other offenders involved")
        .jsonPath("reportDetails.saferCustodyTeamInformed").isEqualTo(false)
        .jsonPath("reportDetails.referralComplete").isEqualTo(true)
        .jsonPath("reportDetails.referralCompletedBy").isEqualTo("FRED.JAMES")
        .jsonPath("reportDetails.referralCompletedByDisplayName").isEqualTo("FRED JAMES")
        .jsonPath("reportDetails.referralCompletedDate").isEqualTo("2024-04-15")
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
        .jsonPath("saferCustodyScreening.recordedBy").isEqualTo("FRED.JAMES")
        .jsonPath("saferCustodyScreening.recordedByDisplayName").isEqualTo("FRED JAMES")
        .jsonPath("saferCustodyScreening.recordedDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("saferCustodyScreening.reasonForDecision").isEqualTo("Further help needed")
    }

    @Test
    fun `will return csip plan data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("caseManager").isEqualTo("The Case Manager")
        .jsonPath("planReason").isEqualTo("Will help offender")
        .jsonPath("firstCaseReviewDate").isEqualTo("2024-08-03")
        .jsonPath("plans[0].id").isEqualTo(csip1.plans[0].id)
        .jsonPath("plans[0].identifiedNeed").isEqualTo("They need help")
        .jsonPath("plans[0].intervention").isEqualTo("Support their work")
        .jsonPath("plans[0].progression").isEqualTo("Behaviour improved")
        .jsonPath("plans[0].referredBy").isEqualTo("Fred Bloggs")
        .jsonPath("plans[0].createdDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("plans[0].targetDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("plans[0].closedDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("plans[0].createDateTime").isNotEmpty
        .jsonPath("plans[0].createdBy").isNotEmpty
        .jsonPath("plans[0].createdByDisplayName").doesNotExist()
    }

    @Test
    fun `will return csip review data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("reviews[0].id").isEqualTo(csip1.reviews[0].id)
        .jsonPath("reviews[0].reviewSequence").isEqualTo(1)
        .jsonPath("reviews[0].remainOnCSIP").isEqualTo(true)
        .jsonPath("reviews[0].csipUpdated").isEqualTo(true)
        .jsonPath("reviews[0].caseNote").isEqualTo(false)
        .jsonPath("reviews[0].closeCSIP").isEqualTo(false)
        .jsonPath("reviews[0].peopleInformed").isEqualTo(false)
        .jsonPath("reviews[0].summary").isEqualTo("More help needed")
        .jsonPath("reviews[0].nextReviewDate").isEqualTo("2024-08-01")
        .jsonPath("reviews[0].recordedDate").isNotEmpty
        .jsonPath("reviews[0].recordedBy").isEqualTo("FRED.JAMES")
        .jsonPath("reviews[0].recordedByDisplayName").isEqualTo("FRED JAMES")
        .jsonPath("reviews[0].createDateTime").isNotEmpty
        .jsonPath("reviews[0].createdBy").isNotEmpty
        .jsonPath("reviews[0].createdByDisplayName").doesNotExist()
        .jsonPath("reviews[0].attendees[0].id").isEqualTo(csip1.reviews[0].attendees[0].id)
        .jsonPath("reviews[0].attendees[0].name").isEqualTo("Fred Attendee")
        .jsonPath("reviews[0].attendees[0].role").isEqualTo("Witness")
        .jsonPath("reviews[0].attendees[0].attended").isEqualTo(true)
        .jsonPath("reviews[0].attendees[0].contribution").isEqualTo("helped")
        .jsonPath("reviews[0].attendees[0].createDateTime").isNotEmpty
        .jsonPath("reviews[0].attendees[0].createdBy").isNotEmpty
        .jsonPath("reviews[0].attendees[0].createdByDisplayName").doesNotExist()
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
        .jsonPath("investigation.interviews[0].id").isEqualTo(csip1.interviews[0].id)
        .jsonPath("investigation.interviews[0].interviewee").isEqualTo("Jim the Interviewee")
        .jsonPath("investigation.interviews[0].date").isEqualTo(LocalDate.now().toString())
        .jsonPath("investigation.interviews[0].role.code").isEqualTo("WITNESS")
        .jsonPath("investigation.interviews[0].role.description").isEqualTo("Witness")
        .jsonPath("investigation.interviews[0].comments").isEqualTo("Helping with behaviour")
        .jsonPath("investigation.interviews[0].createDateTime").isNotEmpty
        .jsonPath("investigation.interviews[0].createdBy").isNotEmpty
        .jsonPath("investigation.interviews[0].createdByDisplayName").doesNotExist()
    }

    @Test
    fun `will return CSIP Decision & Actions data`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("decision.conclusion").isEqualTo("The end result")
        .jsonPath("decision.decisionOutcome.code").isEqualTo("NFA")
        .jsonPath("decision.decisionOutcome.description").isEqualTo("No Further Action")
        .jsonPath("decision.signedOffRole.code").isEqualTo("CUSTMAN")
        .jsonPath("decision.signedOffRole.description").isEqualTo("Custodial Manager")
        .jsonPath("decision.recordedBy").isEqualTo("FRED.JAMES")
        .jsonPath("decision.recordedByDisplayName").isEqualTo("FRED JAMES")
        .jsonPath("decision.recordedDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("decision.nextSteps").isEqualTo("provide help")
        .jsonPath("decision.otherDetails").isEqualTo("Support and assistance needed")
        .jsonPath("decision.actions.openCSIPAlert").isEqualTo(true)
        .jsonPath("decision.actions.nonAssociationsUpdated").isEqualTo(false)
        .jsonPath("decision.actions.observationBook").isEqualTo(true)
        .jsonPath("decision.actions.unitOrCellMove").isEqualTo(true)
        .jsonPath("decision.actions.csraOrRsraReview").isEqualTo(false)
        .jsonPath("decision.actions.serviceReferral").isEqualTo(true)
        .jsonPath("decision.actions.simReferral").isEqualTo(false)
    }

    @Test
    fun `will return CSIP Document information if requested`() {
      webTestClient.get().uri("/csip/${csip1.id}?includeDocumentIds=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("documents.length()").isEqualTo(2)
        .jsonPath("documents[0].documentId").isEqualTo(document1Id)
    }

    @Test
    fun `will not return CSIP Document information by default`() {
      webTestClient.get().uri("/csip/${csip1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(csip1.id)
        .jsonPath("documents").doesNotExist()
    }
  }

  @DisplayName("DELETE /csip/{csipId}")
  @Nested
  inner class DeleteCsip {
    private lateinit var csipToDelete: CSIPReport
    private lateinit var csipWithChildrenToDelete: CSIPReport

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234YY", firstName = "Jim", lastName = "Jones") {
          booking(agencyLocationId = "MDI") {
            csipToDelete = csipReport()
            csipWithChildrenToDelete =
              csipReport {
                factor()
                interview()
                decision()
                plan()
                review { attendee() }
              }
          }
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/csip/${csipToDelete.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/csip/${csipToDelete.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/csip/${csipToDelete.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class NoValidation {
      @Test
      fun `return 204 even when does not exist`() {
        webTestClient.delete().uri("/csip/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the csip`() {
        webTestClient.get().uri("/csip/${csipToDelete.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isOk
        webTestClient.delete().uri("/csip/${csipToDelete.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isNoContent
        webTestClient.get().uri("/csip/${csipToDelete.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/csip/to-migrate")
  @Nested
  inner class GetCSIPsForOffender {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender(nomsId = "Z1234AA", firstName = "Jim", lastName = "Jones") {
          booking(agencyLocationId = "SYI") {
          }
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234TT/csip/to-migrate")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234TT/csip/to-migrate")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234TT/csip/to-migrate")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when does not exist`() {
        webTestClient.get().uri("/prisoners/99999/csip/to-migrate")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will fetch the csips`() {
        webTestClient.get().uri("/prisoners/A1234TT/csip/to-migrate")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("offenderCSIPs.length()").isEqualTo(3)
          .jsonPath("offenderCSIPs[0].id").isEqualTo(csip1.id)
          .jsonPath("offenderCSIPs[1].id").isEqualTo(csip2.id)
          .jsonPath("offenderCSIPs[2].id").isEqualTo(csip3.id)
      }

      @Test
      fun `return ok when no csips for prisoner`() {
        webTestClient.get().uri("/prisoners/Z1234AA/csip/to-migrate")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderCSIPs.size()").isEqualTo(0)
      }
    }
  }

  @DisplayName("PUT /csip create")
  @Nested
  inner class CreateCSIP {

    @Nested
    inner class Security {
      private val validCSIP = createUpsertCSIPRequestMinimalData()

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/csip")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `validation fails when prisoner does not exist`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "Z1234ZZ",
                "logNumber": "WCI-1234",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `validation fails when offender No is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
             {
                "logNumber": "WCI-1234",
                "incidentDate": "2023-12-23",
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("offenderNo")
          }
      }

      @Test
      fun `validation fails when incident date is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "typeCode": "INT",
                "prisonCodeWhenRecorded": "MDI",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("incidentDate")
          }
      }

      @Test
      fun `validation fails when typeCode is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("typeCode")
          }
      }

      @Test
      fun `validation fails when locationCode is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("locationCode")
          }
      }

      @Test
      fun `validation fails when areaOfWorkCode is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "locationCode":"LIB",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("areaOfWorkCode")
          }
      }

      @Test
      fun `validation fails when reportedBy is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("reportedBy")
          }
      }

      @Test
      fun `validation fails when reportedDate is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("reportedDate")
          }
      }

      @Test
      fun `validation fails when create username is not present`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createDatetime": "2024-12-12T12:15:34"
                }
            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("createUsername")
          }
      }

      @Test
      fun `validation fails when incident type code is not valid`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "XXX",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }
            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Incident type")
          }
      }

      @Test
      fun `validation fails when location code is not valid`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "logNumber": "WCI-1234",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "locationCode":"XXX",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),

          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Location type")
          }
      }

      @Test
      fun `validation fails when areaOfWork code is not valid`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "offenderNo": "A1234TT",
                "logNumber": "WCI-1234",
                "incidentDate": "2023-12-23",
                "prisonCodeWhenRecorded": "MDI",
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"XXX",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23",
                "auditDetails": {
                  "createUsername": "FRED.JAMES",
                  "createDatetime": "2024-12-12T12:15:34"
                }            }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Area of work type")
          }
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `creating a csip with minimal data will return basic data`() {
        val validCSIP = createUpsertCSIPRequestMinimalData()

        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody()
          .jsonPath("nomisCSIPReportId").isNotEmpty
          .jsonPath("offenderNo").isEqualTo("A1234TT")
          .jsonPath("created").isEqualTo("true")
      }

      @Test
      fun `creates a csip with full data`() {
        val validCSIP = createUpsertCSIPRequest()

        val createdCsip = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody

        webTestClient.get().uri("/csip/${createdCsip.nomisCSIPReportId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offender.offenderNo").isEqualTo("A1234TT")
          .jsonPath("offender.firstName").isEqualTo("Bob")
          .jsonPath("offender.lastName").isEqualTo("Smith")
          .jsonPath("originalAgencyId").isEqualTo("RNI")
          .jsonPath("logNumber").isEqualTo("ASI-001")
          .jsonPath("bookingId").isEqualTo(csip1.offenderBooking.bookingId)
      }
    }
  }

  @DisplayName("PUT /csip update")
  @Nested
  inner class UpdateCSIP {
    @Nested
    inner class HappyPath {
      @Test
      fun `update an csip with minimal data will return basic data`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            UpsertCSIPRequest(
              id = csip1.id,
              offenderNo = "A1234TT",
              prisonCodeWhenRecorded = "MDI",
              incidentDate = LocalDate.parse("2023-12-15"),
              typeCode = "VPA",
              locationCode = "EXY",
              areaOfWorkCode = "KIT",
              reportedBy = "Jill Reporter",
              reportedDate = LocalDate.parse("2024-05-12"),
              auditDetails = auditDetailsRequest,
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody()
          .jsonPath("nomisCSIPReportId").isNotEmpty
          .jsonPath("offenderNo").isEqualTo("A1234TT")
          .jsonPath("created").isEqualTo("false")
      }
    }
  }

  private val auditDetailsRequest = AuditDetailsRequest(
    createUsername = "FRED.JAMES",
    createDatetime = LocalDateTime.now(),
  )
  private fun createUpsertCSIPRequestMinimalData(csipReportId: Long? = null) =
    UpsertCSIPRequest(
      id = csipReportId,
      offenderNo = "A1234TT",
      incidentDate = LocalDate.parse("2023-12-15"),
      prisonCodeWhenRecorded = "MDI",
      typeCode = "VPA",
      locationCode = "EXY",
      areaOfWorkCode = "KIT",
      reportedBy = "Jill Reporter",
      reportedDate = LocalDate.parse("2024-05-12"),
      auditDetails = auditDetailsRequest,
    )

  private fun createUpsertCSIPRequest() =
    UpsertCSIPRequest(
      offenderNo = "A1234TT",
      incidentDate = LocalDate.parse("2023-12-23"),
      incidentTime = LocalTime.parse("10:32:12"),
      prisonCodeWhenRecorded = "RNI",
      typeCode = "INT",
      locationCode = "LIB",
      areaOfWorkCode = "EDU",
      reportedBy = "Jane Reporter",
      reportedDate = LocalDate.now(),
      auditDetails = auditDetailsRequest,
      logNumber = "ASI-001",
      proActiveReferral = false,
      staffAssaulted = true,
      staffAssaultedName = "Assaulted Person",
      reportDetailRequest = reportDetailRequest,
      saferCustodyScreening = saferCustodyScreeningRequest,
      investigation = investigationDetailRequest,
      caseManager = "A CaseManager",
      decision = decisionRequest,
      planReason = "helper",
      firstCaseReviewDate = LocalDate.parse("2024-04-15"),
      plans = listOf(planRequest),
      reviews = listOf(reviewRequest),
    )

  private val factorRequest = CSIPFactorRequest(
    //   id = TODO(),
    typeCode = "BUL",
    comment = "Offender causes trouble",
    auditDetails = auditDetailsRequest,
  )
  private val reportDetailRequest = UpsertReportDetailsRequest(
    involvementCode = "PER",
    concern = "There was a worry about the offender",
    knownReasons = "known reasons details go in here",
    otherInformation = "other information goes in here",
    saferCustodyTeamInformed = false,
    referralComplete = true,
    referralCompletedBy = "JIM_ADM",
    referralCompletedDate = LocalDate.parse("2024-04-04"),
    factors = listOf(factorRequest),
  )
  private val interviewRequest = InterviewDetailRequest(
    // id = 3343,
    interviewee = "Bill Black",
    date = LocalDate.parse("2024-06-06"),
    roleCode = "WITNESS",
    comments = "Saw a pipe in his hand",
    auditDetails = auditDetailsRequest,
  )

  private val investigationDetailRequest = InvestigationDetailRequest(
    staffInvolved = "some people",
    evidenceSecured = "A piece of pipe",
    reasonOccurred = "bad behaviour",
    usualBehaviour = "Good person",
    trigger = "missed meal",
    protectiveFactors = "ensure taken to canteen",
    interviews = listOf(interviewRequest),
  )
  private val saferCustodyScreeningRequest = SaferCustodyScreeningRequest(
    scsOutcomeCode = "CUR",
    recordedBy = "FRED_ADM",
    recordedDate = LocalDate.parse("2024-04-08"),
    reasonForDecision = "There is a reason for the decision - it goes here",
  )

  private val actionsRequest = ActionsRequest(
    openCSIPAlert = true,
    nonAssociationsUpdated = false,
    observationBook = true,
    unitOrCellMove = true,
    csraOrRsraReview = false,
    serviceReferral = true,
    simReferral = false,
  )
  private val decisionRequest = DecisionRequest(
    conclusion = "The end result",
    decisionOutcomeCode = "OPE",
    signedOffRoleCode = "CUSTMAN",
    recordedBy = "FRED.JAMES",
    recordedDate = LocalDate.parse("2024-08-12"),
    nextSteps = "provide help",
    otherDetails = "Support and assistance needed",
    actions = actionsRequest,
  )
  private val planRequest = PlanRequest(
    // id = TODO(),
    identifiedNeed = "they need help",
    intervention = "dd",
    progression = "there was some improvement",
    referredBy = "Jason",
    createdDate = LocalDate.parse("2024-04-16"),
    targetDate = LocalDate.parse("2024-08-20"),
    closedDate = LocalDate.parse("2024-04-17"),
    auditDetails = auditDetailsRequest,
  )
  private val attendeeRequest = AttendeeRequest(
    // id = TODO(),
    name = "same jones",
    role = "person",
    attended = true,
    contribution = "talked about things",
    auditDetails = auditDetailsRequest,
  )
  private val reviewRequest = ReviewRequest(
    // id = TODO(),
    remainOnCSIP = true,
    csipUpdated = false,
    caseNote = false,
    closeCSIP = true,
    peopleInformed = false,
    summary = "More help needed",
    nextReviewDate = LocalDate.parse("2024-08-01"),
    closeDate = LocalDate.parse("2024-04-16"),
    recordedBy = "FRED.JAMES",
    // reviewSequence = TODO(),
    attendees = listOf(attendeeRequest),
    recordedDate = LocalDate.parse("2024-04-01"),
    auditDetails = auditDetailsRequest,
  )
}
