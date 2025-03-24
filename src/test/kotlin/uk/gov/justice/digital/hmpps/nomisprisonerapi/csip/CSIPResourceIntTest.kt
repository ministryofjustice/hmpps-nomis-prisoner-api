package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.apache.commons.lang3.StringUtils.countMatches
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.ATTENDEE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.FACTOR
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.INTERVIEW
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.PLAN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPComponent.Component.REVIEW
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit.SECONDS

class CSIPResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var csipRepository: CSIPReportRepository

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var csip1: CSIPReport
  private lateinit var csip2: CSIPReport
  private lateinit var csip3: CSIPReport
  private var document1Id: Long = 0
  private var factor31Id: Long = 0
  private var plan31Id: Long = 0
  private var review31Id: Long = 0
  private var attendee31Id: Long = 0
  private var interview31Id: Long = 0
  private var bookingId: Long = 0
  private var bookingId2: Long = 0
  private var bookingId3: Long = 0

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
            incidentDate = LocalDate.parse("2024-01-25"),
            incidentTime = LocalTime.parse("12:34"),
            staffAssaulted = true,
            staffAssaultedName = "Assaulted Person",
            releaseDate = LocalDate.parse("2028-11-25"),
            involvement = "PER",
            concern = "It may happen again",
            knownReasons = "Disagreement",
            otherInformation = "Two other offenders involved",
            referralComplete = true,
            referralCompletedBy = "FRED.JAMES",
            referralCompletedDate = LocalDate.parse("2024-04-15"),
            caseManager = "The Case Manager",
            planReason = "Will help offender",
            firstCaseReviewDate = LocalDate.parse("2024-08-03"),
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
            review {
              attendee(name = "Jim Attendee", role = "Witness", attended = false, contribution = "information")
            }
          }
          csip2 = csipReport {}
          csip3 = csipReport {
            factor()
            plan()
            review {
              attendee()
            }
            interview()
          }
        }
      }
    }
    bookingId = csip1.offenderBooking.bookingId
    factor31Id = csip3.factors[0].id
    plan31Id = csip3.plans[0].id
    review31Id = csip3.reviews[0].id
    attendee31Id = csip3.reviews[0].attendees[0].id
    interview31Id = csip3.interviews[0].id

    document1Id = csip1.offenderBooking.documents[0].id
  }

  @AfterEach
  internal fun deleteCSIPReports() {
    repository.deleteAllCSIPReports()
    repository.deleteOffenders()
    repository.deleteTemplates()
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
        .jsonPath("createdBy").isEqualTo("SA")
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
        .jsonPath("createdBy").isEqualTo("SA")
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
        .jsonPath("plans[0].targetDate").isEqualTo("2024-07-06")
        .jsonPath("plans[0].closedDate").doesNotExist()
        .jsonPath("plans[0].createDateTime").isNotEmpty
        .jsonPath("plans[0].createdBy").isEqualTo("SA")
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
        .jsonPath("reviews[0].reviewSequence").isEqualTo(csip1.reviews[0].reviewSequence)
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
        .jsonPath("reviews[0].attendees[0].id").isEqualTo(csip1.reviews[0].attendees[0].id)
        .jsonPath("reviews[0].attendees[0].name").isEqualTo("Fred Attendee")
        .jsonPath("reviews[0].attendees[0].role").isEqualTo("Witness")
        .jsonPath("reviews[0].attendees[0].attended").isEqualTo(true)
        .jsonPath("reviews[0].attendees[0].contribution").isEqualTo("helped")
        .jsonPath("reviews[0].attendees[0].createDateTime").isNotEmpty
        .jsonPath("reviews[0].attendees[0].createdBy").isNotEmpty
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

  @Nested
  @DisplayName("GET /csip/booking/{bookingId}")
  inner class GetCSIPIdsForBooking {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/csip/booking/$bookingId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/csip/booking/$bookingId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/csip/booking/$bookingId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown csip should return not found`() {
      webTestClient.get().uri("/csip/booking/999999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Prisoner with booking 999999 not found")
        }
    }

    @Test
    fun `booking with no csips should return empty list`() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234YY") {
          bookingId2 = booking().bookingId
        }
      }

      webTestClient.get().uri("/csip/booking/$bookingId2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(0)
    }

    @Test
    fun `booking with multiple csips should return csip ids list`() {
      webTestClient.get().uri("/csip/booking/$bookingId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(3)
    }

    @Test
    fun `booking will correctly identify the csip for the booking`() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234YY") {
          bookingId3 = booking {
            csipReport()
          }.bookingId
        }
      }
      webTestClient.get().uri("/csip/booking/$bookingId3")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
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

  @DisplayName("GET /prisoners/{offenderNo}/csip/reconciliation")
  @Nested
  inner class GetCSIPsForOffenderReconciliation {

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
        webTestClient.get().uri("/prisoners/A1234TT/csip/reconciliation")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234TT/csip/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234TT/csip/reconciliation")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when does not exist`() {
        webTestClient.get().uri("/prisoners/99999/csip/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will fetch the csips`() {
        webTestClient.get().uri("/prisoners/A1234TT/csip/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("offenderCSIPs.length()").isEqualTo(3)
          .jsonPath("offenderCSIPs[0].id").isEqualTo(csip1.id)
          .jsonPath("offenderCSIPs[1].id").isEqualTo(csip2.id)
          .jsonPath("offenderCSIPs[2].id").isEqualTo(csip3.id)
          .jsonPath("offenderCSIPs[0].reportDetails.factors.size()").isEqualTo(2)
          .jsonPath("offenderCSIPs[0].investigation.interviews.size()").isEqualTo(1)
          .jsonPath("offenderCSIPs[0].plans.size()").isEqualTo(1)
          .jsonPath("offenderCSIPs[0].reviews.size()").isEqualTo(2)
          .jsonPath("offenderCSIPs[0].reviews[0].attendees.size()").isEqualTo(1)
      }

      @Test
      fun `return ok when no csips for prisoner`() {
        webTestClient.get().uri("/prisoners/Z1234AA/csip/reconciliation")
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
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
                }
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
                "reportedDate": "2023-12-23"
                }
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
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
              }
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
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
              }
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
                "typeCode": "INT",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
                }
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
                "typeCode": "INT",
                "locationCode":"LIB",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
              }
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
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedDate": "2023-12-23"
              }
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
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter"
              }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("reportedDate")
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
                "typeCode": "XXX",
                "locationCode":"LIB",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
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
                "typeCode": "INT",
                "locationCode":"XXX",
                "areaOfWorkCode":"EDU",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
              }
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
                "typeCode": "INT",
                "locationCode":"LIB",
                "areaOfWorkCode":"XXX",
                "reportedBy": "Jane Reporter",
                "reportedDate": "2023-12-23"
              }
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
          .jsonPath("components.size()").isEqualTo(0)
      }

      @Test
      fun `creates a csip with full data and returns mapping data`() {
        val validCSIP = createUpsertCSIPRequest()

        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody()
          .jsonPath("nomisCSIPReportId").isNotEmpty
          .jsonPath("offenderNo").isEqualTo("A1234TT")
          .jsonPath("components.size()").isEqualTo(5)
          .jsonPath("components[0].component").isEqualTo(FACTOR.name)
          .jsonPath("components[0].nomisId").isNotEmpty
          .jsonPath("components[0].dpsId").isEqualTo("00998877")
          .jsonPath("components[1].component").isEqualTo(PLAN.name)
          .jsonPath("components[1].nomisId").isNotEmpty
          .jsonPath("components[1].dpsId").isEqualTo("00998899")
          .jsonPath("components[2].component").isEqualTo(INTERVIEW.name)
          .jsonPath("components[2].nomisId").isNotEmpty
          .jsonPath("components[2].dpsId").isEqualTo("00998888")
          .jsonPath("components[3].component").isEqualTo(REVIEW.name)
          .jsonPath("components[3].nomisId").isNotEmpty
          .jsonPath("components[3].dpsId").isEqualTo("00998811")
          .jsonPath("components[4].component").isEqualTo(ATTENDEE.name)
          .jsonPath("components[4].nomisId").isNotEmpty
          .jsonPath("components[4].dpsId").isEqualTo("00998800")
      }

      @Test
      fun `creates a csip with full data and will set all data`() {
        val validCSIP = createUpsertCSIPRequest()

        val createdCsip = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        webTestClient.get().uri("/csip/${createdCsip.nomisCSIPReportId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offender.offenderNo").isEqualTo("A1234TT")
          .jsonPath("offender.firstName").isEqualTo("Bob")
          .jsonPath("offender.lastName").isEqualTo("Smith")
          .jsonPath("originalAgencyId").isEqualTo("RNI")
          .jsonPath("logNumber").isEqualTo("RNI-001")
          .jsonPath("bookingId").isEqualTo(csip1.offenderBooking.bookingId)
      }

      @Test
      fun `creating a csip with full data will allow the data to be retrieved`() {
        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest()

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val newCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(newCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(newCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          assertThat(newCsip.originalAgencyId).isEqualTo("RNI")
          assertThat(newCsip.logNumber).isEqualTo("RNI-001")
          assertThat(newCsip.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(newCsip.incidentDate).isEqualTo(LocalDate.parse("2023-12-23"))
          assertThat(newCsip.incidentTime).isEqualTo(LocalDate.parse("2023-12-23").atTime(LocalTime.parse("10:32:12")))
          assertThat(newCsip.type.code).isEqualTo("INT")
          assertThat(newCsip.type.description).isEqualTo("Intimidation")
          assertThat(newCsip.location.code).isEqualTo("LIB")
          assertThat(newCsip.location.description).isEqualTo("Library")
          assertThat(newCsip.areaOfWork.code).isEqualTo("EDU")
          assertThat(newCsip.areaOfWork.description).isEqualTo("Education")
          assertThat(newCsip.reportedBy).isEqualTo("Jane Reporter")
          assertThat(newCsip.reportedDate).isEqualTo(LocalDate.now().toString())
          assertThat(newCsip.proActiveReferral).isEqualTo(false)
          assertThat(newCsip.staffAssaulted).isEqualTo(true)
          assertThat(newCsip.staffAssaultedName).isEqualTo("Assaulted Person")
          assertThat(newCsip.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(newCsip.createUsername).isEqualTo("SA")

          assertThat(newCsip.caseManager).isEqualTo("A CaseManager")
          assertThat(newCsip.reasonForPlan).isEqualTo("helper")
          assertThat(newCsip.firstCaseReviewDate).isEqualTo(LocalDate.parse("2024-04-15"))
          assertThat(newCsip.lastModifiedUsername).isNull()
          assertThat(newCsip.lastModifiedDateTime).isNull()

          // Request Details data
          assertThat(newCsip.involvement!!.code).isEqualTo("PER")
          assertThat(newCsip.concernDescription).isEqualTo("There was a worry about the offender")
          assertThat(newCsip.knownReasons).isEqualTo("known reasons details go in here")
          assertThat(newCsip.otherInformation).isEqualTo("other information goes in here")
          assertThat(newCsip.saferCustodyTeamInformed).isEqualTo(false)
          assertThat(newCsip.referralComplete).isEqualTo(true)
          assertThat(newCsip.referralCompletedBy).isEqualTo("JIM_ADM")
          assertThat(newCsip.referralCompletedDate).isEqualTo(LocalDate.parse("2024-04-04"))

          // Contributory factors
          assertThat(newCsip.factors.size).isEqualTo(1)
          assertThat(newCsip.factors[0].type.code).isEqualTo("BUL")
          assertThat(newCsip.factors[0].comment).isEqualTo("Offender causes trouble")
          assertThat(newCsip.factors[0].createUsername).isEqualTo("SA")

          // SaferCustodyScreening
          assertThat(newCsip.outcome!!.code).isEqualTo("CUR")
          assertThat(newCsip.outcomeCreateUsername).isEqualTo("FRED_ADM")
          assertThat(newCsip.outcomeCreateDate).isEqualTo(LocalDate.parse("2024-04-08"))
          assertThat(newCsip.reasonForDecision).isEqualTo("There is a reason for the decision - it goes here")

          // Investigation & interviews
          assertThat(newCsip.staffInvolved).isEqualTo("some people")
          assertThat(newCsip.evidenceSecured).isEqualTo("A piece of pipe")
          assertThat(newCsip.reasonOccurred).isEqualTo("bad behaviour")
          assertThat(newCsip.usualBehaviour).isEqualTo("Good person")
          assertThat(newCsip.trigger).isEqualTo("missed meal")
          assertThat(newCsip.protectiveFactors).isEqualTo("ensure taken to canteen")
          assertThat(newCsip.interviews.size).isEqualTo(1)

          assertThat(newCsip.interviews[0].interviewee).isEqualTo("Bill Black")
          assertThat(newCsip.interviews[0].interviewDate).isEqualTo("2024-06-06")
          assertThat(newCsip.interviews[0].role.code).isEqualTo("WITNESS")
          assertThat(newCsip.interviews[0].comments).isEqualTo("Saw a pipe in his hand")
          assertThat(newCsip.interviews[0].createUsername).isEqualTo("SA")

          // Decision and actions
          assertThat(newCsip.conclusion).isEqualTo("The end result")
          assertThat(newCsip.decisionOutcome!!.code).isEqualTo("OPE")
          assertThat(newCsip.signedOffRole!!.code).isEqualTo("CUSTMAN")
          assertThat(newCsip.recordedBy).isEqualTo("FRED.JAMES")
          assertThat(newCsip.recordedDate).isEqualTo(LocalDate.parse("2024-08-12"))
          assertThat(newCsip.nextSteps).isEqualTo("provide help")
          assertThat(newCsip.otherDetails).isEqualTo("Support and assistance needed")

          assertThat(newCsip.openCSIPAlert).isEqualTo(true)
          assertThat(newCsip.nonAssociationsUpdated).isEqualTo(false)
          assertThat(newCsip.observationBook).isEqualTo(true)
          assertThat(newCsip.unitOrCellMove).isEqualTo(true)
          assertThat(newCsip.csraOrRsraReview).isEqualTo(false)
          assertThat(newCsip.serviceReferral).isEqualTo(true)
          assertThat(newCsip.simReferral).isEqualTo(false)

          // Plans
          assertThat(newCsip.plans.size).isEqualTo(1)
          assertThat(newCsip.plans[0].identifiedNeed).isEqualTo("they need help")
          assertThat(newCsip.plans[0].intervention).isEqualTo("dd")
          assertThat(newCsip.plans[0].progression).isEqualTo("there was some improvement")
          assertThat(newCsip.plans[0].referredBy).isEqualTo("Jason")
          assertThat(newCsip.plans[0].targetDate).isEqualTo(LocalDate.parse("2024-08-20"))
          assertThat(newCsip.plans[0].closedDate).isEqualTo(LocalDate.parse("2024-04-17"))
          assertThat(newCsip.plans[0].createDate).isEqualTo(LocalDate.now())
          assertThat(newCsip.plans[0].createUsername).isEqualTo("SA")

          // Reviews
          assertThat(newCsip.reviews.size).isEqualTo(1)
          assertThat(newCsip.reviews[0].reviewSequence).isEqualTo(6)
          assertThat(newCsip.reviews[0].remainOnCSIP).isEqualTo(true)
          assertThat(newCsip.reviews[0].csipUpdated).isEqualTo(false)
          assertThat(newCsip.reviews[0].caseNote).isEqualTo(false)
          assertThat(newCsip.reviews[0].closeCSIP).isEqualTo(true)
          assertThat(newCsip.reviews[0].peopleInformed).isEqualTo(false)
          assertThat(newCsip.reviews[0].summary).isEqualTo("More help needed")
          assertThat(newCsip.reviews[0].nextReviewDate).isEqualTo(LocalDate.parse("2024-08-01"))
          assertThat(newCsip.reviews[0].closeDate).isEqualTo(LocalDate.parse("2024-04-16"))
          assertThat(newCsip.reviews[0].recordedUser).isEqualTo("FRED.JAMES")
          assertThat(newCsip.reviews[0].recordedDate).isEqualTo(LocalDate.parse("2024-04-01"))

          // Attendees
          assertThat(newCsip.reviews[0].attendees.size).isEqualTo(1)
          assertThat(newCsip.reviews[0].attendees[0].name).isEqualTo("sam jones")
          assertThat(newCsip.reviews[0].attendees[0].role).isEqualTo("person")
          assertThat(newCsip.reviews[0].attendees[0].attended).isEqualTo(true)
          assertThat(newCsip.reviews[0].attendees[0].contribution).isEqualTo("talked about things")
        }
      }

      @Test
      fun `Investigation usual behaviour will be truncated when over 4000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith3999Characters = "A".repeat(3999)
        val textWith3996Characters = "A".repeat(3996)
        val veryLongTextField = textWithASingle4ByteCharacter + textWith3999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest().copy(
          investigation = investigationDetailRequest.copy(usualBehaviour = veryLongTextField),
        )

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val newCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(newCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(newCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          assertThat(newCsip.usualBehaviour).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
        }
      }

      @Test
      fun `Review summary will be truncated when over 4000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith3999Characters = "A".repeat(3999)
        val textWith3996Characters = "A".repeat(3996)
        val veryLongSummary = textWithASingle4ByteCharacter + textWith3999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest().copy(reviews = listOf(reviewRequest.copy(summary = veryLongSummary)))

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val newCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(newCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(newCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          // Reviews
          assertThat(newCsip.reviews.size).isEqualTo(1)
          assertThat(newCsip.reviews[0].summary).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
        }
      }

      @Test
      fun `Plan progression and intervention text fields will be truncated when over 4000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith3999Characters = "A".repeat(3999)
        val textWith3996Characters = "A".repeat(3996)
        val veryLongTextField = textWithASingle4ByteCharacter + textWith3999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest(nomisCSIPReportId = csip2.id).copy(
          plans = listOf(planRequest.copy(progression = veryLongTextField, intervention = veryLongTextField)),
        )

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          assertThat(updatedCsip.plans.size).isEqualTo(1)
          assertThat(updatedCsip.plans[0].progression).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
          assertThat(updatedCsip.plans[0].intervention).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
        }
      }

      @Test
      fun `Plan Identified Need text field will be truncated when over 1000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith999Characters = "A".repeat(999)
        val textWith996Characters = "A".repeat(996)
        val veryLongTextField = textWithASingle4ByteCharacter + textWith999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest(nomisCSIPReportId = csip2.id).copy(plans = listOf(planRequest.copy(identifiedNeed = veryLongTextField)))

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          assertThat(updatedCsip.plans.size).isEqualTo(1)
          assertThat(updatedCsip.plans[0].identifiedNeed).isEqualTo(textWithASingle4ByteCharacter + textWith996Characters)
        }
      }

      @Test
      fun `will track telemetry event for create with minimal data`() {
        val validCSIP = createUpsertCSIPRequestMinimalData()

        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)

        verify(telemetryClient).trackEvent(
          eq("csip-created"),
          check {
            assertThat(it).containsKey("nomisCSIPReportId")
            assertThat(it).containsEntry("offenderNo", "A1234TT")
            assertThat(it).containsEntry("componentsCreated", "[]")
          },
          isNull(),
        )
      }

      @Test
      fun `will track telemetry event for create with full data`() {
        val validCSIP = createUpsertCSIPRequest()

        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)

        verify(telemetryClient).trackEvent(
          eq("csip-created"),
          check {
            assertThat(it).containsKey("nomisCSIPReportId")
            assertThat(it).containsEntry("offenderNo", "A1234TT")
            assertThat(countMatches(it["componentsCreated"], "CSIPComponent")).isEqualTo(5)
            assertThat(it["componentsCreated"]).contains("component=FACTOR")
            assertThat(it["componentsCreated"]).contains("component=PLAN")
            assertThat(it["componentsCreated"]).contains("component=INTERVIEW")
            assertThat(it["componentsCreated"]).contains("component=REVIEW")
            assertThat(it["componentsCreated"]).contains("component=ATTENDEE")
          },
          isNull(),
        )
      }

      @Nested
      inner class HappyPathMultipleFactors {
        @Test
        fun `creating a csip with minimal data and factors will return basic data`() {
          val validCSIP = createUpsertCSIPRequestMinimalData().copy(
            reportDetailRequest = reportDetailRequest.copy(
              factors =
              listOf(
                CSIPFactorRequest(
                  dpsId = "112233",
                  typeCode = "BUL",
                  comment = "Offender causes trouble3",
                ),
                CSIPFactorRequest(
                  dpsId = "223344",
                  typeCode = "BUL",
                  comment = "Offender causes trouble4",
                ),
                CSIPFactorRequest(
                  dpsId = "223355",
                  typeCode = "BUL",
                  comment = "Offender causes trouble5",
                ),
              ),
            ),
          )

          webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validCSIP)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody()
            .jsonPath("nomisCSIPReportId").isNotEmpty
            .jsonPath("offenderNo").isEqualTo("A1234TT")
            .jsonPath("components.size()").isEqualTo(3)
        }
      }

      @Nested
      inner class HappyPathMultiplePlans {
        @Test
        fun `creating a csip with minimal data and plans will return basic data`() {
          val validCSIP = createUpsertCSIPRequestMinimalData().copy(
            plans =
            listOf(
              planRequest,
              planRequest.copy(dpsId = "123456", identifiedNeed = "help 2"),
              planRequest.copy(dpsId = "345678", identifiedNeed = "help 3"),
            ),
          )

          webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validCSIP)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody()
            .jsonPath("nomisCSIPReportId").isNotEmpty
            .jsonPath("offenderNo").isEqualTo("A1234TT")
            .jsonPath("components.size()").isEqualTo(3)
        }
      }

      @Nested
      inner class HappyPathMultipleInterviews {
        @Test
        fun `creating a csip with minimal data and interviews will return basic data`() {
          val validCSIP = createUpsertCSIPRequestMinimalData().copy(
            investigation = investigationDetailRequest.copy(
              interviews =
              listOf(
                interviewRequest,
                interviewRequest.copy(dpsId = "123456", comments = "interview 2"),
                interviewRequest.copy(dpsId = "345678", comments = "interview 3"),
              ),
            ),
          )

          webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validCSIP)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody()
            .jsonPath("nomisCSIPReportId").isNotEmpty
            .jsonPath("offenderNo").isEqualTo("A1234TT")
            .jsonPath("components.size()").isEqualTo(3)
        }
      }

      @Nested
      inner class HappyPathMultipleReviews {
        @Test
        fun `creating a csip with minimal data and reviews will return basic data`() {
          val validCSIP = createUpsertCSIPRequestMinimalData().copy(
            reviews =
            listOf(
              reviewRequest.copy(attendees = null),
              reviewRequest.copy(attendees = null, dpsId = "123456", summary = "Rev Summary 2"),
              reviewRequest.copy(attendees = null, dpsId = "345678", summary = "Rev Summary 3"),
            ),
          )

          webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validCSIP)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody()
            .jsonPath("nomisCSIPReportId").isNotEmpty
            .jsonPath("offenderNo").isEqualTo("A1234TT")
            .jsonPath("components.size()").isEqualTo(3)
        }
      }

      @Nested
      inner class HappyPathMultipleAttendees {
        @Test
        fun `creating a csip with minimal data and attendees will return basic data`() {
          val validCSIP = createUpsertCSIPRequestMinimalData().copy(
            reviews =
            listOf(
              reviewRequest.copy(
                attendees =
                listOf(
                  attendeeRequest,
                  attendeeRequest.copy(dpsId = "123456", contribution = "attend 2"),
                  attendeeRequest.copy(dpsId = "345678", contribution = "attend 3"),
                ),
              ),
            ),
          )

          webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validCSIP)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody()
            .jsonPath("nomisCSIPReportId").isNotEmpty
            .jsonPath("offenderNo").isEqualTo("A1234TT")
            .jsonPath("components.size()").isEqualTo(4)
        }
      }
    }
  }

  @DisplayName("PUT /csip update")
  @Nested
  inner class UpdateCSIP {
    @Nested
    inner class HappyPath {
      @Test
      fun `update a csip with minimal data will return basic data`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            UpsertCSIPRequest(
              id = csip2.id,
              offenderNo = "A1234TT",
              incidentDate = LocalDate.parse("2023-12-15"),
              typeCode = "VPA",
              locationCode = "EXY",
              areaOfWorkCode = "KIT",
              reportedBy = "Jill Reporter",
              reportedDate = LocalDate.parse("2024-05-12"),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody()
          .jsonPath("nomisCSIPReportId").isNotEmpty
          .jsonPath("offenderNo").isEqualTo("A1234TT")
          .jsonPath("components.size()").isEqualTo(0)
      }

      @Test
      fun `updating a csip with full data will allow the data to be retrieved`() {
        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest(nomisCSIPReportId = csip2.id)

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          // Note - originalAgencyId can not be updated
          assertThat(updatedCsip.originalAgencyId).isEqualTo("MDI")
          assertThat(updatedCsip.logNumber).isEqualTo("RNI-001")
          assertThat(updatedCsip.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(updatedCsip.incidentDate).isEqualTo(LocalDate.parse("2023-12-23"))
          assertThat(updatedCsip.incidentTime).isEqualTo(
            LocalDate.parse("2023-12-23").atTime(LocalTime.parse("10:32:12")),
          )
          assertThat(updatedCsip.type.code).isEqualTo("INT")
          assertThat(updatedCsip.type.description).isEqualTo("Intimidation")
          assertThat(updatedCsip.location.code).isEqualTo("LIB")
          assertThat(updatedCsip.location.description).isEqualTo("Library")
          assertThat(updatedCsip.areaOfWork.code).isEqualTo("EDU")
          assertThat(updatedCsip.areaOfWork.description).isEqualTo("Education")
          assertThat(updatedCsip.reportedBy).isEqualTo("Jane Reporter")
          assertThat(updatedCsip.reportedDate).isEqualTo(LocalDate.now().toString())
          assertThat(updatedCsip.proActiveReferral).isEqualTo(false)
          assertThat(updatedCsip.staffAssaulted).isEqualTo(true)
          assertThat(updatedCsip.staffAssaultedName).isEqualTo("Assaulted Person")
          assertThat(updatedCsip.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(updatedCsip.createUsername).isEqualTo("SA")

          assertThat(updatedCsip.caseManager).isEqualTo("A CaseManager")
          assertThat(updatedCsip.reasonForPlan).isEqualTo("helper")
          assertThat(updatedCsip.firstCaseReviewDate).isEqualTo(LocalDate.parse("2024-04-15"))
          assertThat(updatedCsip.lastModifiedUsername).isNull()
          assertThat(updatedCsip.lastModifiedDateTime).isNull()

          // Request Details data
          assertThat(updatedCsip.involvement!!.code).isEqualTo("PER")
          assertThat(updatedCsip.concernDescription).isEqualTo("There was a worry about the offender")
          assertThat(updatedCsip.knownReasons).isEqualTo("known reasons details go in here")
          assertThat(updatedCsip.otherInformation).isEqualTo("other information goes in here")
          assertThat(updatedCsip.saferCustodyTeamInformed).isEqualTo(false)
          assertThat(updatedCsip.referralComplete).isEqualTo(true)
          assertThat(updatedCsip.referralCompletedBy).isEqualTo("JIM_ADM")
          assertThat(updatedCsip.referralCompletedDate).isEqualTo(LocalDate.parse("2024-04-04"))

          // Contributory factors
          assertThat(updatedCsip.factors.size).isEqualTo(1)
          assertThat(updatedCsip.factors[0].type.code).isEqualTo("BUL")
          assertThat(updatedCsip.factors[0].comment).isEqualTo("Offender causes trouble")
          assertThat(updatedCsip.factors[0].createUsername).isEqualTo("SA")

          // SaferCustodyScreening
          assertThat(updatedCsip.outcome!!.code).isEqualTo("CUR")
          assertThat(updatedCsip.outcomeCreateUsername).isEqualTo("FRED_ADM")
          assertThat(updatedCsip.outcomeCreateDate).isEqualTo(LocalDate.parse("2024-04-08"))
          assertThat(updatedCsip.reasonForDecision).isEqualTo("There is a reason for the decision - it goes here")

          // Investigation & interviews
          assertThat(updatedCsip.staffInvolved).isEqualTo("some people")
          assertThat(updatedCsip.evidenceSecured).isEqualTo("A piece of pipe")
          assertThat(updatedCsip.reasonOccurred).isEqualTo("bad behaviour")
          assertThat(updatedCsip.usualBehaviour).isEqualTo("Good person")
          assertThat(updatedCsip.trigger).isEqualTo("missed meal")
          assertThat(updatedCsip.protectiveFactors).isEqualTo("ensure taken to canteen")
          assertThat(updatedCsip.interviews.size).isEqualTo(1)

          assertThat(updatedCsip.interviews[0].interviewee).isEqualTo("Bill Black")
          assertThat(updatedCsip.interviews[0].interviewDate).isEqualTo("2024-06-06")
          assertThat(updatedCsip.interviews[0].role.code).isEqualTo("WITNESS")
          assertThat(updatedCsip.interviews[0].comments).isEqualTo("Saw a pipe in his hand")
          assertThat(updatedCsip.interviews[0].createUsername).isEqualTo("SA")

          // Decision and actions
          assertThat(updatedCsip.conclusion).isEqualTo("The end result")
          assertThat(updatedCsip.decisionOutcome!!.code).isEqualTo("OPE")
          assertThat(updatedCsip.signedOffRole!!.code).isEqualTo("CUSTMAN")
          assertThat(updatedCsip.recordedBy).isEqualTo("FRED.JAMES")
          assertThat(updatedCsip.recordedDate).isEqualTo(LocalDate.parse("2024-08-12"))
          assertThat(updatedCsip.nextSteps).isEqualTo("provide help")
          assertThat(updatedCsip.otherDetails).isEqualTo("Support and assistance needed")

          assertThat(updatedCsip.openCSIPAlert).isEqualTo(true)
          assertThat(updatedCsip.nonAssociationsUpdated).isEqualTo(false)
          assertThat(updatedCsip.observationBook).isEqualTo(true)
          assertThat(updatedCsip.unitOrCellMove).isEqualTo(true)
          assertThat(updatedCsip.csraOrRsraReview).isEqualTo(false)
          assertThat(updatedCsip.serviceReferral).isEqualTo(true)
          assertThat(updatedCsip.simReferral).isEqualTo(false)

          // Plans
          assertThat(updatedCsip.plans.size).isEqualTo(1)
          assertThat(updatedCsip.plans[0].identifiedNeed).isEqualTo("they need help")
          assertThat(updatedCsip.plans[0].intervention).isEqualTo("dd")
          assertThat(updatedCsip.plans[0].progression).isEqualTo("there was some improvement")
          assertThat(updatedCsip.plans[0].referredBy).isEqualTo("Jason")
          assertThat(updatedCsip.plans[0].targetDate).isEqualTo(LocalDate.parse("2024-08-20"))
          assertThat(updatedCsip.plans[0].closedDate).isEqualTo(LocalDate.parse("2024-04-17"))
          assertThat(updatedCsip.plans[0].createDate).isEqualTo(LocalDate.now())
          assertThat(updatedCsip.plans[0].createUsername).isEqualTo("SA")

          // Reviews
          assertThat(updatedCsip.reviews.size).isEqualTo(1)
          assertThat(updatedCsip.reviews[0].reviewSequence).isEqualTo(6)
          assertThat(updatedCsip.reviews[0].remainOnCSIP).isEqualTo(true)
          assertThat(updatedCsip.reviews[0].csipUpdated).isEqualTo(false)
          assertThat(updatedCsip.reviews[0].caseNote).isEqualTo(false)
          assertThat(updatedCsip.reviews[0].closeCSIP).isEqualTo(true)
          assertThat(updatedCsip.reviews[0].peopleInformed).isEqualTo(false)
          assertThat(updatedCsip.reviews[0].summary).isEqualTo("More help needed")
          assertThat(updatedCsip.reviews[0].nextReviewDate).isEqualTo(LocalDate.parse("2024-08-01"))
          assertThat(updatedCsip.reviews[0].closeDate).isEqualTo(LocalDate.parse("2024-04-16"))
          assertThat(updatedCsip.reviews[0].recordedUser).isEqualTo("FRED.JAMES")
          assertThat(updatedCsip.reviews[0].recordedDate).isEqualTo(LocalDate.parse("2024-04-01"))

          // Check attendees
          assertThat(updatedCsip.reviews[0].attendees.size).isEqualTo(1)
          assertThat(updatedCsip.reviews[0].attendees[0].name).isEqualTo("sam jones")
          assertThat(updatedCsip.reviews[0].attendees[0].role).isEqualTo("person")
          assertThat(updatedCsip.reviews[0].attendees[0].attended).isEqualTo(true)
          assertThat(updatedCsip.reviews[0].attendees[0].contribution).isEqualTo("talked about things")
        }
      }

      @Test
      fun `updating a csip with fewer reviews will clear old reviews and associated attendees`() {
        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest(nomisCSIPReportId = csip1.id).copy(
          reviews = listOf(
            reviewRequest.copy(
              dpsId = "111222",
              id = csip1.reviews[1].id,
              summary = "test1",
              attendees = listOf(attendeeRequest.copy(dpsId = "7788", id = csip1.reviews[1].attendees[0].id, name = "some body")),
            ),
          ),
        )

        repository.runInTransaction {
          val originalCsip = csipRepository.findByIdOrNull(csip1.id)!!
          assertThat(originalCsip.reviews.size).isEqualTo(2)
          assertThat(originalCsip.reviews[0].attendees.size).isEqualTo(1)
          assertThat(originalCsip.reviews[1].attendees.size).isEqualTo(1)
        }

        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(csip1.id)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)

          // Reviews
          assertThat(updatedCsip.reviews.size).isEqualTo(1)
          assertThat(updatedCsip.reviews[0].reviewSequence).isEqualTo(6)
          assertThat(updatedCsip.reviews[0].remainOnCSIP).isEqualTo(true)
          assertThat(updatedCsip.reviews[0].csipUpdated).isEqualTo(false)
          assertThat(updatedCsip.reviews[0].caseNote).isEqualTo(false)
          assertThat(updatedCsip.reviews[0].closeCSIP).isEqualTo(true)
          assertThat(updatedCsip.reviews[0].peopleInformed).isEqualTo(false)
          assertThat(updatedCsip.reviews[0].summary).isEqualTo("test1")
          assertThat(updatedCsip.reviews[0].nextReviewDate).isEqualTo(LocalDate.parse("2024-08-01"))
          assertThat(updatedCsip.reviews[0].closeDate).isEqualTo(LocalDate.parse("2024-04-16"))
          assertThat(updatedCsip.reviews[0].recordedUser).isEqualTo("FRED.JAMES")

          // Check attendees
          assertThat(updatedCsip.reviews[0].attendees.size).isEqualTo(1)
          assertThat(updatedCsip.reviews[0].attendees[0].name).isEqualTo("some body")
          assertThat(updatedCsip.reviews[0].attendees[0].role).isEqualTo("person")
          assertThat(updatedCsip.reviews[0].attendees[0].attended).isEqualTo(true)
          assertThat(updatedCsip.reviews[0].attendees[0].contribution).isEqualTo("talked about things")
        }
      }

      @Test
      fun `Investigation usual behaviour will be truncated when over 4000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith3999Characters = "A".repeat(3999)
        val textWith3996Characters = "A".repeat(3996)
        val veryLongTextField = textWithASingle4ByteCharacter + textWith3999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest(nomisCSIPReportId = csip2.id).copy(
          investigation = investigationDetailRequest.copy(usualBehaviour = veryLongTextField),
        )

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          assertThat(updatedCsip.usualBehaviour).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
        }
      }

      @Test
      fun `summary will be truncated when over 4000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith3999Characters = "A".repeat(3999)
        val textWith3996Characters = "A".repeat(3996)
        val veryLongTextField = textWithASingle4ByteCharacter + textWith3999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest(nomisCSIPReportId = csip2.id).copy(reviews = listOf(reviewRequest.copy(summary = veryLongTextField)))

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          // Reviews
          assertThat(updatedCsip.reviews.size).isEqualTo(1)
          assertThat(updatedCsip.reviews[0].summary).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
        }
      }

      @Test
      fun `Plan progression and intervention text fields will be truncated when over 4000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith3999Characters = "A".repeat(3999)
        val textWith3996Characters = "A".repeat(3996)
        val veryLongTextField = textWithASingle4ByteCharacter + textWith3999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest().copy(plans = listOf(planRequest.copy(progression = veryLongTextField, intervention = veryLongTextField)))

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          assertThat(updatedCsip.plans.size).isEqualTo(1)
          assertThat(updatedCsip.plans[0].progression).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
          assertThat(updatedCsip.plans[0].intervention).isEqualTo(textWithASingle4ByteCharacter + textWith3996Characters)
        }
      }

      @Test
      fun `Plan Identified Need text field will be truncated when over 1000 bytes`() {
        val textWithASingle4ByteCharacter = "😀"
        val textWith999Characters = "A".repeat(999)
        val textWith996Characters = "A".repeat(996)
        val veryLongTextField = textWithASingle4ByteCharacter + textWith999Characters

        val booking = offenderBookingRepository.findLatestByOffenderNomsId("A1234TT")
        val validCSIP = createUpsertCSIPRequest().copy(plans = listOf(planRequest.copy(identifiedNeed = veryLongTextField)))

        val upsertResponse = webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)
          .expectBody(UpsertCSIPResponse::class.java)
          .returnResult().responseBody!!

        repository.runInTransaction {
          val updatedCsip = csipRepository.findByIdOrNull(upsertResponse.nomisCSIPReportId)
          assertThat(updatedCsip!!.offenderBooking.offender.nomsId).isEqualTo("A1234TT")
          assertThat(updatedCsip.rootOffender?.id).isEqualTo(booking!!.rootOffender?.id)
          assertThat(updatedCsip.plans.size).isEqualTo(1)
          assertThat(updatedCsip.plans[0].identifiedNeed).isEqualTo(textWithASingle4ByteCharacter + textWith996Characters)
        }
      }

      @Test
      fun `will track telemetry event for update with minimal data`() {
        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            UpsertCSIPRequest(
              id = csip1.id,
              offenderNo = "A1234TT",
              incidentDate = LocalDate.parse("2023-12-15"),
              typeCode = "VPA",
              locationCode = "EXY",
              areaOfWorkCode = "KIT",
              reportedBy = "Jill Reporter",
              reportedDate = LocalDate.parse("2024-05-12"),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(200)

        verify(telemetryClient).trackEvent(
          eq("csip-updated"),
          check {
            assertThat(it).containsEntry("nomisCSIPReportId", csip1.id.toString())
            assertThat(it).containsEntry("offenderNo", "A1234TT")
            assertThat(it).containsEntry("componentsCreated", "[]")
          },
          isNull(),
        )
      }

      @Test
      fun `will track telemetry event for update with full data`() {
        val validCSIP = createUpsertCSIPRequest(nomisCSIPReportId = csip1.id)

        webTestClient.put().uri("/csip")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCSIP)
          .exchange()
          .expectStatus().isEqualTo(200)

        verify(telemetryClient).trackEvent(
          eq("csip-updated"),
          check<MutableMap<String, String>> {
            assertThat(it).containsEntry("nomisCSIPReportId", csip1.id.toString())
            assertThat(it).containsEntry("offenderNo", "A1234TT")
            assertThat(it["componentsCreated"]).contains("component=FACTOR")
            assertThat(it["componentsCreated"]).containsPattern("component=PLAN")
            assertThat(it["componentsCreated"]).contains("component=INTERVIEW")
            assertThat(it["componentsCreated"]).contains("component=REVIEW")
            assertThat(it["componentsCreated"]).contains("component=ATTENDEE")
          },
          isNull(),
        )
      }

      @Nested
      inner class HappyPathMultipleFactors {
        private lateinit var upsertWithMultipleFactors: UpsertCSIPRequest

        @BeforeEach
        fun setUp() {
          upsertWithMultipleFactors =
            UpsertCSIPRequest(
              id = csip3.id,
              offenderNo = "A1234TT",
              incidentDate = LocalDate.parse("2023-12-15"),
              typeCode = "VPA",
              locationCode = "EXY",
              areaOfWorkCode = "KIT",
              reportedBy = "Jill Reporter",
              reportedDate = LocalDate.parse("2024-05-12"),
              reportDetailRequest = UpsertReportDetailsRequest(
                factors =
                listOf(
                  CSIPFactorRequest(
                    dpsId = "112244",
                    typeCode = "BUL",
                    comment = "Offender causes trouble1",
                  ),
                  CSIPFactorRequest(
                    id = factor31Id,
                    dpsId = "112233",
                    typeCode = "BUL",
                    comment = "Offender causes trouble3",
                  ),
                  CSIPFactorRequest(
                    dpsId = "112277",
                    typeCode = "BUL",
                    comment = "Offender causes trouble4",
                  ),
                ),
              ),
            )
        }

        @Test
        fun `Updating a csip with multiple factors will successfully save along with existing`() {
          // Update with 2 more factors
          val response = webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(upsertWithMultipleFactors)
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(UpsertCSIPResponse::class.java)
            .returnResult().responseBody!!

          assertThat(response.nomisCSIPReportId).isEqualTo(csip3.id)
          assertThat(response.components[0].component).isEqualTo(FACTOR)
          assertThat(response.components[0].dpsId).isEqualTo("112244")
          assertThat(response.components[1].component).isEqualTo(FACTOR)
          assertThat(response.components[1].dpsId).isEqualTo("112277")

          repository.runInTransaction {
            val updatedCsip = csipRepository.findByIdOrNull(csip3.id)!!
            assertThat(updatedCsip.factors[0].id).isEqualTo(factor31Id)
            assertThat(updatedCsip.factors[0].comment).isEqualTo("Offender causes trouble3")

            assertThat(updatedCsip.factors[1].id).isEqualTo(response.components[0].nomisId)
            assertThat(updatedCsip.factors[1].comment).isEqualTo("Offender causes trouble1")

            assertThat(updatedCsip.factors[2].id).isEqualTo(response.components[1].nomisId)
            assertThat(updatedCsip.factors[2].comment).isEqualTo("Offender causes trouble4")
          }
        }

        @Test
        fun `will track telemetry event for multiple factors`() {
          // Update with 2 more factors
          webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(upsertWithMultipleFactors)
            .exchange()
            .expectStatus().isEqualTo(200)

          verify(telemetryClient).trackEvent(
            eq("csip-updated"),
            check<MutableMap<String, String>> {
              assertThat(it).containsEntry("nomisCSIPReportId", csip3.id.toString())
              assertThat(it).containsEntry("offenderNo", "A1234TT")
              assertThat(countMatches(it["componentsCreated"], "CSIPComponent")).isEqualTo(2)
              assertThat(countMatches(it["componentsCreated"], "component=FACTOR")).isEqualTo(2)
            },
            isNull(),
          )
        }
      }

      @Nested
      inner class HappyPathMultiplePlans {
        private lateinit var upsertWithMultiplePlans: UpsertCSIPRequest

        @BeforeEach
        fun setUp() {
          upsertWithMultiplePlans =
            UpsertCSIPRequest(
              id = csip3.id,
              offenderNo = "A1234TT",
              incidentDate = LocalDate.parse("2023-12-15"),
              typeCode = "VPA",
              locationCode = "EXY",
              areaOfWorkCode = "KIT",
              reportedBy = "Jill Reporter",
              reportedDate = LocalDate.parse("2024-05-12"),
              plans = listOf(
                planRequest.copy(dpsId = "00998800", identifiedNeed = "Help2"),
                PlanRequest(
                  id = plan31Id,
                  dpsId = "112233",
                  identifiedNeed = "Help1",
                  intervention = "Support their work",
                  progression = null,
                  referredBy = "Fred Bloggs",
                  targetDate = LocalDate.parse("2024-07-06"),
                ),
                planRequest.copy(dpsId = "009988011", identifiedNeed = "Help3"),
              ),
            )
        }

        @Test
        fun `Updating a csip with multiple plans will successfully save along with existing`() {
          // Update with 2 more plans
          val response = webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
              upsertWithMultiplePlans,
            )
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(UpsertCSIPResponse::class.java)
            .returnResult().responseBody!!

          assertThat(response.nomisCSIPReportId).isEqualTo(csip3.id)
          assertThat(response.components[0].component).isEqualTo(PLAN)
          assertThat(response.components[0].dpsId).isEqualTo("00998800")
          assertThat(response.components[1].component).isEqualTo(PLAN)
          assertThat(response.components[1].dpsId).isEqualTo("009988011")

          repository.runInTransaction {
            val updatedCsip = csipRepository.findByIdOrNull(csip3.id)!!
            assertThat(updatedCsip.plans[0].id).isEqualTo(plan31Id)
            assertThat(updatedCsip.plans[0].identifiedNeed).isEqualTo("Help1")

            assertThat(updatedCsip.plans[1].id).isEqualTo(response.components[0].nomisId)
            assertThat(updatedCsip.plans[1].identifiedNeed).isEqualTo("Help2")

            assertThat(updatedCsip.plans[2].id).isEqualTo(response.components[1].nomisId)
            assertThat(updatedCsip.plans[2].identifiedNeed).isEqualTo("Help3")
          }
        }

        @Test
        fun `will track telemetry event for multiple plans`() {
          webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
              upsertWithMultiplePlans,
            )
            .exchange()
            .expectStatus().isEqualTo(200)

          verify(telemetryClient).trackEvent(
            eq("csip-updated"),
            check<MutableMap<String, String>> {
              assertThat(it).containsEntry("nomisCSIPReportId", csip3.id.toString())
              assertThat(it).containsEntry("offenderNo", "A1234TT")
              assertThat(countMatches(it["componentsCreated"], "CSIPComponent")).isEqualTo(2)
              assertThat(countMatches(it["componentsCreated"], "component=PLAN")).isEqualTo(2)
            },
            isNull(),
          )
        }
      }

      @Nested
      inner class HappyPathMultipleReviews {
        @Test
        fun `Updating a csip with multiple reviews will successfully save along with existing`() {
          // Update with 2 more reviews
          val response = webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
              UpsertCSIPRequest(
                id = csip3.id,
                offenderNo = "A1234TT",
                incidentDate = LocalDate.parse("2023-12-15"),
                typeCode = "VPA",
                locationCode = "EXY",
                areaOfWorkCode = "KIT",
                reportedBy = "Jill Reporter",
                reportedDate = LocalDate.parse("2024-05-12"),
                reviews = listOf(
                  reviewRequest.copy(dpsId = "0099881100", summary = "Help2"),
                  ReviewRequest(
                    id = review31Id,
                    dpsId = "112233",
                    summary = "Help1",
                    remainOnCSIP = true,
                    csipUpdated = true,
                    caseNote = false,
                    closeCSIP = false,
                    peopleInformed = false,
                    nextReviewDate = LocalDate.parse("2024-08-02"),
                    closeDate = LocalDate.parse("2024-04-18"),
                    recordedBy = "FRED.JAMES",
                    attendees = listOf(),
                    recordedDate = LocalDate.parse("2024-04-01"),
                    reviewSequence = 2,
                  ),
                  reviewRequest.copy(dpsId = "0099881111", summary = "Help3"),
                ),
              ),
            )
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(UpsertCSIPResponse::class.java)
            .returnResult().responseBody!!

          assertThat(response.nomisCSIPReportId).isEqualTo(csip3.id)
          assertThat(response.components[0].component).isEqualTo(REVIEW)
          assertThat(response.components[0].dpsId).isEqualTo("0099881100")
          assertThat(response.components[1].component).isEqualTo(REVIEW)
          assertThat(response.components[1].dpsId).isEqualTo("0099881111")

          repository.runInTransaction {
            val updatedCsip = csipRepository.findByIdOrNull(csip3.id)!!
            assertThat(updatedCsip.reviews[0].id).isEqualTo(review31Id)
            assertThat(updatedCsip.reviews[0].summary).isEqualTo("Help1")

            assertThat(updatedCsip.reviews[1].id).isEqualTo(response.components[0].nomisId)
            assertThat(updatedCsip.reviews[1].summary).isEqualTo("Help2")

            assertThat(updatedCsip.reviews[2].id).isEqualTo(response.components[1].nomisId)
            assertThat(updatedCsip.reviews[2].summary).isEqualTo("Help3")
          }
        }
      }

      @Nested
      inner class HappyPathMultipleAttendees {
        @Test
        fun `Updating a csip with multiple attendees will successfully save along with existing`() {
          // Update with 2 more reviews
          val response = webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
              UpsertCSIPRequest(
                id = csip3.id,
                offenderNo = "A1234TT",
                incidentDate = LocalDate.parse("2023-12-15"),
                typeCode = "VPA",
                locationCode = "EXY",
                areaOfWorkCode = "KIT",
                reportedBy = "Jill Reporter",
                reportedDate = LocalDate.parse("2024-05-12"),
                reviews = listOf(
                  reviewRequest.copy(
                    dpsId = "0099881100",
                    summary = "Help2",
                    attendees = listOf(attendeeRequest.copy(dpsId = "8877", contribution = "Attendee 1")),
                  ),
                  ReviewRequest(
                    id = review31Id,
                    dpsId = "112233",
                    summary = "Help1",
                    remainOnCSIP = true,
                    csipUpdated = true,
                    caseNote = false,
                    closeCSIP = false,
                    peopleInformed = false,
                    nextReviewDate = LocalDate.parse("2024-08-02"),
                    closeDate = LocalDate.parse("2024-04-18"),
                    recordedBy = "FRED.JAMES",
                    attendees = listOf(
                      attendeeRequest.copy(id = attendee31Id, contribution = "existing1"),
                      attendeeRequest.copy(dpsId = "7766", contribution = "Attendee 2"),
                    ),
                    recordedDate = LocalDate.parse("2024-04-01"),
                    reviewSequence = 4,
                  ),
                  reviewRequest.copy(dpsId = "0099881111", summary = "Help3"),
                ),
              ),
            )
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(UpsertCSIPResponse::class.java)
            .returnResult().responseBody!!

          assertThat(response.nomisCSIPReportId).isEqualTo(csip3.id)
          assertThat(response.components.size).isEqualTo(5)
          assertThat(response.components[0].component).isEqualTo(REVIEW)
          assertThat(response.components[0].dpsId).isEqualTo("0099881100")
          assertThat(response.components[1].component).isEqualTo(REVIEW)
          assertThat(response.components[1].dpsId).isEqualTo("0099881111")

          assertThat(response.components[2].component).isEqualTo(ATTENDEE)
          assertThat(response.components[2].dpsId).isEqualTo("8877")
          assertThat(response.components[3].component).isEqualTo(ATTENDEE)
          assertThat(response.components[3].dpsId).isEqualTo("7766")
          assertThat(response.components[4].component).isEqualTo(ATTENDEE)
          assertThat(response.components[4].dpsId).isEqualTo("00998800")

          repository.runInTransaction {
            val updatedCsip = csipRepository.findByIdOrNull(csip3.id)!!
            assertThat(updatedCsip.reviews[0].id).isEqualTo(review31Id)
            assertThat(updatedCsip.reviews[0].summary).isEqualTo("Help1")
            assertThat(updatedCsip.reviews[1].id).isEqualTo(response.components[0].nomisId)
            assertThat(updatedCsip.reviews[1].summary).isEqualTo("Help2")
            assertThat(updatedCsip.reviews[2].id).isEqualTo(response.components[1].nomisId)
            assertThat(updatedCsip.reviews[2].summary).isEqualTo("Help3")

            assertThat(updatedCsip.reviews[0].attendees[0].id).isEqualTo(attendee31Id)
            assertThat(updatedCsip.reviews[0].attendees[0].contribution).isEqualTo("existing1")
            assertThat(updatedCsip.reviews[0].attendees[1].id).isEqualTo(response.components[3].nomisId)
            assertThat(updatedCsip.reviews[0].attendees[1].contribution).isEqualTo("Attendee 2")
            assertThat(updatedCsip.reviews[1].attendees[0].id).isEqualTo(response.components[2].nomisId)
            assertThat(updatedCsip.reviews[1].attendees[0].contribution).isEqualTo("Attendee 1")
            assertThat(updatedCsip.reviews[2].attendees[0].id).isEqualTo(response.components[4].nomisId)
            assertThat(updatedCsip.reviews[2].attendees[0].contribution).isEqualTo("talked about things")
          }
        }
      }

      @Nested
      inner class HappyPathMultipleInterviews {

        @Test
        fun `Updating a csip with multiple interviews will successfully save along with existing`() {
          // Update with 2 more interviews
          val response = webTestClient.put().uri("/csip")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CSIP")))
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
              UpsertCSIPRequest(
                id = csip3.id,
                offenderNo = "A1234TT",
                incidentDate = LocalDate.parse("2023-12-15"),
                typeCode = "VPA",
                locationCode = "EXY",
                areaOfWorkCode = "KIT",
                reportedBy = "Jill Reporter",
                reportedDate = LocalDate.parse("2024-05-12"),
                investigation = InvestigationDetailRequest(
                  staffInvolved = "Bob",
                  interviews = listOf(
                    interviewRequest.copy(dpsId = "0099881100", comments = "Interview2"),
                    InterviewDetailRequest(
                      id = interview31Id,
                      dpsId = "445566",
                      interviewee = "Ruby Red",
                      date = LocalDate.parse("2024-08-06"),
                      roleCode = "WITNESS",
                      comments = "Interview1",
                    ),
                    interviewRequest.copy(dpsId = "0099881111", comments = "Interview3"),
                  ),
                ),
              ),
            )
            .exchange()
            .expectStatus().isEqualTo(200)
            .expectBody(UpsertCSIPResponse::class.java)
            .returnResult().responseBody!!

          assertThat(response.nomisCSIPReportId).isEqualTo(csip3.id)
          assertThat(response.components[0].component).isEqualTo(INTERVIEW)
          assertThat(response.components[0].dpsId).isEqualTo("0099881100")
          assertThat(response.components[1].component).isEqualTo(INTERVIEW)
          assertThat(response.components[1].dpsId).isEqualTo("0099881111")

          repository.runInTransaction {
            val updatedCsip = csipRepository.findByIdOrNull(csip3.id)!!
            assertThat(updatedCsip.interviews[0].id).isEqualTo(interview31Id)
            assertThat(updatedCsip.interviews[0].comments).isEqualTo("Interview1")

            assertThat(updatedCsip.interviews[1].id).isEqualTo(response.components[0].nomisId)
            assertThat(updatedCsip.interviews[1].comments).isEqualTo("Interview2")

            assertThat(updatedCsip.interviews[2].id).isEqualTo(response.components[1].nomisId)
            assertThat(updatedCsip.interviews[2].comments).isEqualTo("Interview3")
          }
        }
      }
    }
  }
}

private fun createUpsertCSIPRequestMinimalData(csipReportId: Long? = null) = UpsertCSIPRequest(
  id = csipReportId,
  offenderNo = "A1234TT",
  incidentDate = LocalDate.parse("2023-12-15"),
  typeCode = "VPA",
  locationCode = "EXY",
  areaOfWorkCode = "KIT",
  reportedBy = "Jill Reporter",
  reportedDate = LocalDate.parse("2024-05-12"),
)

private fun createUpsertCSIPRequest(nomisCSIPReportId: Long? = null) = UpsertCSIPRequest(
  id = nomisCSIPReportId,
  offenderNo = "A1234TT",
  incidentDate = LocalDate.parse("2023-12-23"),
  incidentTime = LocalTime.parse("10:32:12"),
  prisonCodeWhenRecorded = "RNI",
  typeCode = "INT",
  locationCode = "LIB",
  areaOfWorkCode = "EDU",
  reportedBy = "Jane Reporter",
  reportedDate = LocalDate.now(),
  logNumber = "RNI-001",
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
  id = null,
  dpsId = "00998877",
  typeCode = "BUL",
  comment = "Offender causes trouble",
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
  dpsId = "00998888",
  interviewee = "Bill Black",
  date = LocalDate.parse("2024-06-06"),
  roleCode = "WITNESS",
  comments = "Saw a pipe in his hand",
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
  dpsId = "00998899",
  identifiedNeed = "they need help",
  intervention = "dd",
  progression = "there was some improvement",
  referredBy = "Jason",
  targetDate = LocalDate.parse("2024-08-20"),
  closedDate = LocalDate.parse("2024-04-17"),
)

private val attendeeRequest = AttendeeRequest(
  dpsId = "00998800",
  name = "sam jones",
  role = "person",
  attended = true,
  contribution = "talked about things",
)
private val reviewRequest = ReviewRequest(
  dpsId = "00998811",
  remainOnCSIP = true,
  csipUpdated = false,
  caseNote = false,
  closeCSIP = true,
  peopleInformed = false,
  summary = "More help needed",
  nextReviewDate = LocalDate.parse("2024-08-01"),
  closeDate = LocalDate.parse("2024-04-16"),
  recordedBy = "FRED.JAMES",
  attendees = listOf(attendeeRequest),
  recordedDate = LocalDate.parse("2024-04-01"),
  reviewSequence = 6,
)
