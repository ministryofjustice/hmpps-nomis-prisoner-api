package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.STAFF_CONTROL
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.STAFF_REPORTING_OFFICER
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.SUSPECT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.VICTIM
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.NO_FURTHER_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.PLACED_ON_REPORT_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.findAdjudication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.prisonerParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.victimRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.witnessRole
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val adjudicationNumber = 9000123L
const val previousAdjudicationNumber = 8000123L

class AdjudicationsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  private var aLocationInMoorland = 0L

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI").locationId
  }

  @DisplayName("GET /adjudications/charges/ids")
  @Nested
  inner class GetAdjudications {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/adjudications/charges/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/adjudications/charges/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/adjudications/charges/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed with correct role`() {
        webTestClient.get().uri("/adjudications/charges/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {
      private lateinit var staff: Staff
      private lateinit var staffVictim: Staff
      private lateinit var prisonerVictim: Offender
      private lateinit var prisonerAtMoorlandPreviouslyAtBrixton: Offender
      private lateinit var prisonerAtMoorland: Offender
      private lateinit var prisonerAtBrixton: Offender
      private lateinit var prisonerAtLeeds: Offender
      private lateinit var incidentAtBrixton: AdjudicationIncident
      private lateinit var anotherIncidentAtBrixton: AdjudicationIncident
      private lateinit var incidentAtMoorland: AdjudicationIncident
      private lateinit var anotherIncidentAtMoorland: AdjudicationIncident
      private val incidentsAtLeeds: MutableList<AdjudicationIncident> = mutableListOf()
      private val oldAdjudicationNumberAtBrixton = 9000120L
      private val newAdjudicationNumberAtBrixton = 9000121L
      private val oldAdjudicationNumberAtMoorland = 9000122L
      private val newAdjudicationNumberAtMoorland = 9000123L
      private val anotherNewAdjudicationNumberAtBrixton = 9000124L
      private val anotherNewAdjudicationNumberAtMoorland = 9000125L
      private val leedsAdjudicationNumberRange = 9000200L..9000299L

      @BeforeEach
      internal fun createPrisonerWithAdjudication() {
        nomisDataBuilder.build {
          staff = staff {
            account {}
          }
          staffVictim = staff {}
          prisonerVictim = offender { booking {} }

          prisonerAtMoorlandPreviouslyAtBrixton =
            offender(nomsId = "A1234AA") {
              booking(agencyLocationId = "MDI", active = true)
              booking(agencyLocationId = "BXI", active = false)
            }
          prisonerAtMoorland =
            offender(nomsId = "A1234AB") {
              booking(agencyLocationId = "MDI")
            }
          prisonerAtBrixton =
            offender(nomsId = "A1234AC") {
              booking(agencyLocationId = "BXI")
            }
          prisonerAtLeeds =
            offender(nomsId = "A1234AD") {
              booking(agencyLocationId = "LEI")
            }
          incidentAtBrixton = adjudicationIncident(
            reportingStaff = staff,
            prisonId = "BXI",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          ) {
            party(
              role = VICTIM,
              offenderBooking = prisonerVictim.latestBooking(),
              actionDecision = NO_FURTHER_ACTION_CODE,
            )
            party(
              role = VICTIM,
              staff = staffVictim,
              actionDecision = NO_FURTHER_ACTION_CODE,
            )
            party(
              role = SUSPECT,
              offenderBooking = prisonerAtBrixton.latestBooking(),
              adjudicationNumber = oldAdjudicationNumberAtBrixton,
              actionDecision = PLACED_ON_REPORT_ACTION_CODE,
            )
            party(
              role = SUSPECT,
              offenderBooking = prisonerAtMoorlandPreviouslyAtBrixton.latestBooking(),
              adjudicationNumber = anotherNewAdjudicationNumberAtBrixton,
              actionDecision = PLACED_ON_REPORT_ACTION_CODE,
            ) {
              charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "1234/123",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
            }
          }
          anotherIncidentAtBrixton = adjudicationIncident(
            reportingStaff = staff,
            prisonId = "BXI",
            whenCreated = LocalDateTime.parse("2023-07-01T10:00"),
          ) {
            party(
              role = SUSPECT,
              offenderBooking = prisonerAtBrixton.latestBooking(),
              adjudicationNumber = newAdjudicationNumberAtBrixton,
              actionDecision = PLACED_ON_REPORT_ACTION_CODE,
            ) {
              charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "1234/123",
                whenCreated = LocalDateTime.parse("2023-07-01T10:00"),
              )
            }
          }
          incidentAtMoorland = adjudicationIncident(
            reportingStaff = staff,
            prisonId = "MDI",
            whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
          ) {
            party(
              role = SUSPECT,
              offenderBooking = prisonerAtMoorland.latestBooking(),
              adjudicationNumber = oldAdjudicationNumberAtMoorland,
              actionDecision = PLACED_ON_REPORT_ACTION_CODE,
            ) {
              charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "1234/123",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
              charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "4321/123",
                whenCreated = LocalDateTime.parse("2020-01-01T11:00"),
              )
            }
            party(
              role = SUSPECT,
              offenderBooking = prisonerAtMoorlandPreviouslyAtBrixton.latestBooking(),
              adjudicationNumber = anotherNewAdjudicationNumberAtMoorland,
              actionDecision = PLACED_ON_REPORT_ACTION_CODE,
            ) {
              charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "1234/123",
                whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
              )
            }
          }
          anotherIncidentAtMoorland = adjudicationIncident(
            reportingStaff = staff,
            prisonId = "MDI",
            whenCreated = LocalDateTime.parse("2023-01-01T10:00"),
          ) {
            party(
              role = SUSPECT,
              offenderBooking = prisonerAtMoorland.latestBooking(),
              adjudicationNumber = newAdjudicationNumberAtMoorland,
              actionDecision = PLACED_ON_REPORT_ACTION_CODE,
            ) {
              charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "1234/123",
                whenCreated = LocalDateTime.parse("2023-01-01T10:00"),
              )
            }
          }
          leedsAdjudicationNumberRange.forEachIndexed { index, it ->
            incidentsAtLeeds.add(
              adjudicationIncident(
                reportingStaff = staff,
                prisonId = "LEI",
                whenCreated = LocalDateTime.parse("2015-01-01T10:00").plusSeconds(index.toLong()),
              ) {
                party(
                  role = SUSPECT,
                  offenderBooking = prisonerAtLeeds.latestBooking(),
                  adjudicationNumber = it,
                  actionDecision = PLACED_ON_REPORT_ACTION_CODE,
                ) {
                  charge(
                    offenceCode = "51:1N",
                    guiltyEvidence = "HOOCH",
                    reportDetail = "1234/123",
                    whenCreated = LocalDateTime.parse("2015-01-01T10:00"),
                  )
                  charge(
                    offenceCode = "51:1N",
                    guiltyEvidence = "HOOCH",
                    reportDetail = "1234/123",
                    whenCreated = LocalDateTime.parse("2015-01-01T11:00"),
                  )
                }
              },
            )
          }
        }
      }

      @Test
      fun `will return total count when size is 1`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(206)
          .jsonPath("numberOfElements").isEqualTo(1)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(206)
          .jsonPath("size").isEqualTo(1)
      }

      @Test
      fun `by default there will be a page size of 20`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(206)
          .jsonPath("numberOfElements").isEqualTo(20)
          .jsonPath("number").isEqualTo(0)
          .jsonPath("totalPages").isEqualTo(11)
          .jsonPath("size").isEqualTo(20)
      }

      @Test
      fun `will order by primary key ascending - (eg order charges are created)`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .queryParam("size", "300")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("content[0].adjudicationNumber").isEqualTo(anotherNewAdjudicationNumberAtBrixton)
          .jsonPath("content[0].offenderNo").isEqualTo(prisonerAtMoorlandPreviouslyAtBrixton.nomsId)
          .jsonPath("content[205].adjudicationNumber").isEqualTo(leedsAdjudicationNumberRange.last)
          .jsonPath("content[205].offenderNo").isEqualTo(prisonerAtLeeds.nomsId)
      }

      @Test
      fun `supplying fromDate means only adjudications created on or after that date are returned`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .queryParam("size", "200")
            .queryParam("fromDate", "2023-07-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(1)
          .jsonPath("content[0].adjudicationNumber").isEqualTo(newAdjudicationNumberAtBrixton)
      }

      @Test
      fun `supplying toDate means only adjudications created on or before that date are returned`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .queryParam("size", "200")
            .queryParam("toDate", "2015-01-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(200)
          .jsonPath("content[0].adjudicationNumber").isEqualTo(leedsAdjudicationNumberRange.first)
      }

      @Test
      fun `can filter using both from and to dates`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .queryParam("size", "200")
            .queryParam("fromDate", "2020-01-01")
            .queryParam("toDate", "2023-01-01")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(5)
      }

      @Test
      fun `can filter by prison where adjudication was created`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .queryParam("size", "200")
            .queryParam("prisonIds", "LEI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(200)
      }

      @Test
      fun `can filter by multiple prisons where adjudication was created`() {
        webTestClient.get().uri {
          it.path("/adjudications/charges/ids")
            .queryParam("size", "200")
            .queryParam("prisonIds", "MDI")
            .queryParam("prisonIds", "BXI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(6)
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(incidentAtBrixton)
        repository.delete(anotherIncidentAtBrixton)
        repository.delete(incidentAtMoorland)
        repository.delete(anotherIncidentAtMoorland)
        incidentsAtLeeds.forEach { repository.delete(it) }
        repository.delete(prisonerAtMoorlandPreviouslyAtBrixton)
        repository.delete(prisonerAtMoorland)
        repository.delete(prisonerAtBrixton)
        repository.delete(prisonerAtLeeds)
        repository.delete(prisonerVictim)
        repository.delete(staff)
        repository.delete(staffVictim)
      }
    }
  }

  @Nested
  inner class GetAdjudicationAndGetAdjudicationCharge {
    private lateinit var prisoner: Offender
    lateinit var prisonerVictim: Offender
    lateinit var prisonerWitness: Offender
    lateinit var anotherSuspect: Offender

    lateinit var incident: AdjudicationIncident
    lateinit var previousIncident: AdjudicationIncident
    lateinit var staff: Staff
    lateinit var staffInvestigator: Staff
    lateinit var staffWitness: Staff
    lateinit var staffVictim: Staff
    lateinit var staffInvolvedWithForce: Staff
    lateinit var staffIncidentReportingOfficer: Staff

    private var offenderBookingId: Long = 0

    @BeforeEach
    internal fun createPrisonerWithAdjudication() {
      nomisDataBuilder.build {
        staff = staff(firstName = "SIMON", lastName = "BROWN") {
          account(username = "S.BROWN_ADM", type = "ADMIN")
          account(username = "S.BROWN_GEN", type = "GENERAL")
        }
        staffInvestigator =
          staff(firstName = "ISLA", lastName = "INVESTIGATOR") { account(username = "I.INVESTIGATOR") }
        staffWitness = staff(firstName = "KOFI", lastName = "WITNESS") { account(username = "K.WITNESS") }
        staffVictim = staff(firstName = "KWEKU", lastName = "VICTIM") { account(username = "K.VICTIM") }
        staffInvolvedWithForce = staff(firstName = "JANE", lastName = "MUSCLES") { account(username = "J.MUSCLES") }
        staffIncidentReportingOfficer = staff(firstName = "EAGLE", lastName = "EYES") { account(username = "E.EYES") }
        prisonerVictim = offender(firstName = "CHARLIE", lastName = "VICTIM") { booking {} }
        prisonerWitness = offender(firstName = "CLIVE", lastName = "SNITCH") { booking {} }
        anotherSuspect = offender(firstName = "KILLER", lastName = "BROWN") { booking {} }
        previousIncident = adjudicationIncident(
          reportingStaff = staff,
          prisonId = "MDI",
          agencyInternalLocationId = aLocationInMoorland,
        )
        incident = adjudicationIncident(
          reportingStaff = staff,
          prisonId = "MDI",
          agencyInternalLocationId = aLocationInMoorland,
          reportedDateTime = LocalDateTime.parse("2023-01-02T15:00"),
          reportedDate = LocalDate.parse("2023-01-02"),
          incidentDateTime = LocalDateTime.parse("2023-01-01T18:00"),
          incidentDate = LocalDate.parse("2023-01-01"),
          incidentDetails = "There was a fight in the toilets",
        ) {
          repair(repairType = "PLUM", comment = "Fixed the bog", repairCost = BigDecimal("10.30"))
          repair(repairType = "CLEA")
          party(
            role = WITNESS,
            staff = staffWitness,
            partyAddedDate = LocalDate.parse("2023-01-03"),
            comment = "They saw everything",
          )
          party(role = VICTIM, staff = staffVictim)
          party(role = STAFF_CONTROL, staff = staffInvolvedWithForce)
          party(role = STAFF_REPORTING_OFFICER, staff = staffIncidentReportingOfficer)
          party(
            role = VICTIM,
            offenderBooking = prisonerVictim.latestBooking(),
            partyAddedDate = LocalDate.parse("2023-01-04"),
            comment = "Beaten up",
          )
          party(role = WITNESS, offenderBooking = prisonerWitness.latestBooking())
          party(
            role = SUSPECT,
            offenderBooking = anotherSuspect.latestBooking(),
            adjudicationNumber = 987654,
            actionDecision = PLACED_ON_REPORT_ACTION_CODE,
          )
        }
        lateinit var previousAward: AdjudicationHearingResultAward
        prisoner = offender(nomsId = "A1234TT", genderCode = "F") {
          booking(agencyLocationId = "BXI") {
            adjudicationParty(incident = previousIncident, adjudicationNumber = previousAdjudicationNumber) {
              val previousCharge = charge(offenceCode = "51:1B")
              hearing(
                internalLocationId = aLocationInMoorland,
              ) {
                result(
                  charge = previousCharge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  previousAward = award(
                    statusCode = "SUSPENDED",
                    sanctionCode = "ADA",
                    effectiveDate = LocalDate.parse("2023-01-08"),
                    statusDate = LocalDate.parse("2023-01-09"),
                    sanctionDays = 4,
                    sanctionIndex = 1,
                  )
                }
              }
            }
            adjudicationParty(incident = incident, adjudicationNumber = adjudicationNumber) {
              val hoochCharge = charge(
                offenceCode = "51:1N",
                guiltyEvidence = "HOOCH",
                reportDetail = "1234/123",
              )
              val deadSwanCharge = charge(
                offenceCode = "51:3",
                guiltyEvidence = "DEAD SWAN",
                reportDetail = null,
              )
              hearing(
                internalLocationId = aLocationInMoorland,
                scheduleDate = LocalDate.parse("2023-01-02"),
                scheduleTime = LocalDateTime.parse("2023-01-02T14:00:00"),
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingStaff = staff,
              ) {
                result(
                  charge = hoochCharge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  val firstAward = award(
                    statusCode = "SUSPENDED",
                    sanctionCode = "REMACT",
                    effectiveDate = LocalDate.parse("2023-01-03"),
                    statusDate = LocalDate.parse("2023-01-04"),
                    comment = "award comment",
                    sanctionMonths = 1,
                    sanctionDays = 2,
                    compensationAmount = BigDecimal.valueOf(12.2),
                    sanctionIndex = 2,
                  )
                  award(
                    statusCode = "SUSPEN_RED",
                    sanctionCode = "STOP_EARN",
                    effectiveDate = LocalDate.parse("2023-01-04"),
                    statusDate = LocalDate.parse("2023-01-05"),
                    comment = "award comment for second award",
                    sanctionMonths = 3,
                    sanctionDays = 4,
                    compensationAmount = BigDecimal.valueOf(14.2),
                    consecutiveHearingResultAward = firstAward,
                    sanctionIndex = 3,
                  )
                  award(
                    statusCode = "SUSPENDED",
                    sanctionCode = "TOBA",
                    effectiveDate = LocalDate.parse("2023-01-08"),
                    statusDate = LocalDate.parse("2023-01-09"),
                    comment = "award comment for third award",
                    sanctionMonths = 3,
                    sanctionDays = 4,
                    compensationAmount = BigDecimal.valueOf(14.2),
                    consecutiveHearingResultAward = previousAward,
                    sanctionIndex = 4,
                  )
                }
                result(
                  charge = deadSwanCharge,
                  pleaFindingCode = "UNFIT",
                  findingCode = "NOT_PROCEED",
                )
                notification(staff = staff, deliveryDate = LocalDate.parse("2023-01-04"), deliveryDateTime = LocalDateTime.parse("2023-01-04T10:00:00"))
                notification(staff = staffInvestigator, deliveryDate = LocalDate.parse("2023-01-05"), deliveryDateTime = LocalDateTime.parse("2023-01-05T11:30:00"), comment = "You have been issued this hearing")
              }
              investigation(
                investigator = staffInvestigator,
                comment = "Isla comment for investigation",
                assignedDate = LocalDate.parse("2023-01-02"),
              ) {
                evidence(
                  date = LocalDate.parse("2023-01-03"),
                  detail = "smashed light bulb",
                  type = "PHOTO",
                )
                evidence(
                  date = LocalDate.parse("2023-01-04"),
                  detail = "syringe",
                  type = "DRUGTEST",
                )
              }
            }
          }
        }

        offenderBookingId = prisoner.latestBooking().bookingId
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.deleteHearingByAdjudicationNumber(adjudicationNumber)
      repository.deleteHearingByAdjudicationNumber(previousAdjudicationNumber)
      repository.delete(previousIncident)
      repository.delete(incident)
      repository.delete(prisoner)
      repository.delete(prisonerVictim)
      repository.delete(prisonerWitness)
      repository.delete(anotherSuspect)
      repository.delete(staff)
      repository.delete(staffInvestigator)
      repository.delete(staffWitness)
      repository.delete(staffVictim)
      repository.delete(staffInvolvedWithForce)
      repository.delete(staffIncidentReportingOfficer)
    }

    @DisplayName("GET /adjudications/adjudication-number/{adjudicationNumber}")
    @Nested
    inner class GetAdjudication {
      @Nested
      inner class Security {
        @Test
        fun `access forbidden when no role`() {
          webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
            .headers(setAuthorisation(roles = listOf()))
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
            .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access unauthorised with no auth token`() {
          webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
            .exchange()
            .expectStatus().isUnauthorized
        }
      }

      @Nested
      inner class Validation {
        @Test
        fun `return 404 when adjudication not found`() {
          webTestClient.get().uri("/adjudications/adjudication-number/99999999")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isNotFound
        }
      }

      @Nested
      inner class SimpleAdjudication {
        @Test
        fun `returns core incident details`() {
          getAdjudicationCoreIncidentDetails("/adjudications/adjudication-number/$adjudicationNumber")
        }

        @Test
        fun `returns details about other parties involved in incident`() {
          getAdjudicationOtherPartiesTest("/adjudications/adjudication-number/$adjudicationNumber")
        }

        @Test
        fun `returns details of the charges and offences`() {
          webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
            .jsonPath("charges[0].offence.code").isEqualTo("51:1N")
            .jsonPath("charges[0].evidence").isEqualTo("HOOCH")
            .jsonPath("charges[0].reportDetail").isEqualTo("1234/123")
            .jsonPath("charges[0].offence.description")
            .isEqualTo("Commits any assault - assault on non prison officer member of staff")
            .jsonPath("charges[0].offence.type.description").isEqualTo("Prison Rule 51")
            .jsonPath("charges[0].offenceId").isEqualTo("$adjudicationNumber/1")
            .jsonPath("charges[0].evidence").isEqualTo("HOOCH")
            .jsonPath("charges[0].chargeSequence").isEqualTo("1")
            .jsonPath("charges[1].evidence").isEqualTo("DEAD SWAN")
            .jsonPath("charges[1].reportDetail").doesNotExist()
            .jsonPath("charges[1].offence.code").isEqualTo("51:3")
            .jsonPath("charges[1].offence.description")
            .isEqualTo("Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there")
            .jsonPath("charges[1].offence.type.description").isEqualTo("Prison Rule 51")
            .jsonPath("charges[1].chargeSequence").isEqualTo("2")
            .jsonPath("charges[1].offenceId").isEqualTo("$adjudicationNumber/2")
        }

        @Test
        fun `returns details of damage done during the incident`() {
          getAdjudicationDamageTest("/adjudications/adjudication-number/$adjudicationNumber")
        }

        @Test
        fun `returns details about evidence obtained about the incident`() {
          getAdjudicationEvidenceTest("/adjudications/adjudication-number/$adjudicationNumber")
        }

        @Test
        fun `returns details about the hearings for the adjudication`() {
          getAdjudicationHearingsTest("/adjudications/adjudication-number/$adjudicationNumber")
        }

        @Test
        fun `returns details of the hearing outcome and punishments (aka awards)`() {
          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
            .jsonPath("hearings[0].hearingResults[0].pleaFindingType.description").isEqualTo("Not guilty")
            .jsonPath("hearings[0].hearingResults[0].findingType.description").isEqualTo("Charge Proved")
            .jsonPath("hearings[0].hearingResults[0].charge.offence.code").isEqualTo("51:1N")
            .jsonPath("hearings[0].hearingResults[0].offence.code").isEqualTo("51:1N")
            .jsonPath("hearings[0].hearingResults[0].createdByUsername").isNotEmpty
            .jsonPath("hearings[0].hearingResults[0].createdDateTime").isNotEmpty
            .jsonPath("hearings[0].hearingResults[1].pleaFindingType.description").isEqualTo("Unfit to Plea or Attend")
            .jsonPath("hearings[0].hearingResults[1].findingType.description").isEqualTo("Charge Not Proceeded With")
            .jsonPath("hearings[0].hearingResults[1].charge.offence.code").isEqualTo("51:3")
            .jsonPath("hearings[0].hearingResults[1].offence.code").isEqualTo("51:3")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionType.description")
            .isEqualTo("Removal from Activity")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionStatus.description").isEqualTo("Suspended")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].effectiveDate").isEqualTo("2023-01-03")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].statusDate").isEqualTo("2023-01-04")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].comment").isEqualTo("award comment")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].compensationAmount").isEqualTo(12.2)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionMonths").isEqualTo(1)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionDays").isEqualTo(2)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sequence").isEqualTo(2)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].chargeSequence").isEqualTo(1)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[1].sanctionType.description")
            .isEqualTo("Stoppage of Earnings (amount)")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[1].consecutiveAward.sanctionType.description")
            .isEqualTo("Removal from Activity")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[1].consecutiveAward.chargeSequence").isEqualTo(1)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[1].sequence").isEqualTo(3)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].effectiveDate").isEqualTo("2023-01-08")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].consecutiveAward.sanctionType.description")
            .isEqualTo("Additional Days Added")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].consecutiveAward.consecutiveAward")
            .doesNotExist()
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].sequence").isEqualTo(4)
        }
      }
    }

    @DisplayName("GET /adjudications/adjudication-number/{adjudicationNumber}/charge-sequence/{chargeSequence}")
    @Nested
    inner class GetAdjudicationCharge {
      val hoochChargeSequence = 1
      val deadSwanChargeSequence = 2

      @Nested
      inner class Security {
        @Test
        fun `access forbidden when no role`() {
          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
            .headers(setAuthorisation(roles = listOf()))
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access forbidden with wrong role`() {
          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
            .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
            .exchange()
            .expectStatus().isForbidden
        }

        @Test
        fun `access unauthorised with no auth token`() {
          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
            .exchange()
            .expectStatus().isUnauthorized
        }
      }

      @Nested
      inner class Validation {
        @Test
        fun `return 404 when adjudication not found`() {
          webTestClient.get().uri("/adjudications/adjudication-number/99999999/charge-sequence/$hoochChargeSequence")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isNotFound
        }

        @Test
        fun `return 404 when adjudication charge not found`() {
          webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/444")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isNotFound
        }
      }

      @Nested
      inner class SimpleAdjudication {
        @Test
        fun `returns core incident details`() {
          getAdjudicationCoreIncidentDetails("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
        }

        @Test
        fun `returns details about other parties involved in incident`() {
          getAdjudicationOtherPartiesTest("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
        }

        @Test
        fun `returns details of the charge and offences`() {
          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
            .jsonPath("charge.offence.code").isEqualTo("51:1N")
            .jsonPath("charge.evidence").isEqualTo("HOOCH")
            .jsonPath("charge.reportDetail").isEqualTo("1234/123")
            .jsonPath("charge.offence.description")
            .isEqualTo("Commits any assault - assault on non prison officer member of staff")
            .jsonPath("charge.offence.type.description").isEqualTo("Prison Rule 51")
            .jsonPath("charge.offenceId").isEqualTo("$adjudicationNumber/1")
            .jsonPath("charge.chargeSequence").isEqualTo(hoochChargeSequence)
        }

        @Test
        fun `returns details of another charge related to the same adjudication`() {
          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$deadSwanChargeSequence")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
            .jsonPath("charge.offence.code").isEqualTo("51:3")
            .jsonPath("charge.evidence").isEqualTo("DEAD SWAN")
            .jsonPath("charge.reportDetail").doesNotExist()
            .jsonPath("charge.offence.description")
            .isEqualTo("Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there")
            .jsonPath("charge.offence.type.description").isEqualTo("Prison Rule 51")
            .jsonPath("charge.offenceId").isEqualTo("$adjudicationNumber/2")
            .jsonPath("charge.chargeSequence").isEqualTo(deadSwanChargeSequence)
        }

        @Test
        fun `returns details of damage done during the incident`() {
          getAdjudicationDamageTest("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
        }

        @Test
        fun `returns details about evidence obtained about the incident`() {
          getAdjudicationEvidenceTest("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
        }

        @Test
        fun `returns details about the hearings for the adjudication`() {
          getAdjudicationHearingsTest("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
        }

        @Test
        fun `returns details of the hearing outcome and punishments (aka awards) only for this charge`() {
          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$hoochChargeSequence")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
            .jsonPath("hearings[0].hearingResults[0].pleaFindingType.description").isEqualTo("Not guilty")
            .jsonPath("hearings[0].hearingResults[0].findingType.description").isEqualTo("Charge Proved")
            .jsonPath("hearings[0].hearingResults[0].charge.offence.code").isEqualTo("51:1N")
            .jsonPath("hearings[0].hearingResults[0].offence.code").isEqualTo("51:1N")
            .jsonPath("hearings[0].hearingResults[0].createdByUsername").isNotEmpty
            .jsonPath("hearings[0].hearingResults[0].createdDateTime").isNotEmpty
            .jsonPath("hearings[0].hearingResults[1]").doesNotExist()
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionType.description")
            .isEqualTo("Removal from Activity")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionStatus.description").isEqualTo("Suspended")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].effectiveDate").isEqualTo("2023-01-03")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].statusDate").isEqualTo("2023-01-04")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].comment").isEqualTo("award comment")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].compensationAmount").isEqualTo(12.2)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionMonths").isEqualTo(1)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sanctionDays").isEqualTo(2)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0].sequence").isEqualTo(2)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[1].sanctionType.description")
            .isEqualTo("Stoppage of Earnings (amount)")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[1].consecutiveAward.sanctionType.description")
            .isEqualTo("Removal from Activity")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[1].sequence").isEqualTo(3)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].effectiveDate").isEqualTo("2023-01-08")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].adjudicationNumber").isEqualTo(adjudicationNumber)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].consecutiveAward.sanctionType.description")
            .isEqualTo("Additional Days Added")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].consecutiveAward.adjudicationNumber")
            .isEqualTo(previousAdjudicationNumber)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].consecutiveAward.chargeSequence")
            .isEqualTo(1)
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].consecutiveAward.consecutiveAward")
            .doesNotExist()
            .jsonPath("hearings[0].hearingResults[0].resultAwards[2].sequence").isEqualTo(4)

          webTestClient.get()
            .uri("/adjudications/adjudication-number/$adjudicationNumber/charge-sequence/$deadSwanChargeSequence")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
            .jsonPath("hearings[0].hearingResults[0].pleaFindingType.description").isEqualTo("Unfit to Plea or Attend")
            .jsonPath("hearings[0].hearingResults[0].findingType.description").isEqualTo("Charge Not Proceeded With")
            .jsonPath("hearings[0].hearingResults[0].charge.offence.code").isEqualTo("51:3")
            .jsonPath("hearings[0].hearingResults[0].offence.code").isEqualTo("51:3")
            .jsonPath("hearings[0].hearingResults[0].resultAwards[0]").doesNotExist()
        }
      }
    }

    private fun getAdjudicationCoreIncidentDetails(url: String) {
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("offenderNo").isEqualTo("A1234TT")
        .jsonPath("bookingId").isEqualTo(offenderBookingId)
        .jsonPath("gender.code").isEqualTo("F")
        .jsonPath("currentPrison.code").isEqualTo("BXI")
        .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
        .jsonPath("incident.adjudicationIncidentId").isEqualTo(incident.id)
        .jsonPath("incident.reportingStaff.firstName").isEqualTo("SIMON")
        .jsonPath("incident.reportingStaff.lastName").isEqualTo("BROWN")
        .jsonPath("incident.reportingStaff.username").isEqualTo("S.BROWN_GEN")
        .jsonPath("incident.incidentDate").isEqualTo("2023-01-01")
        .jsonPath("incident.incidentTime").isEqualTo("18:00:00")
        .jsonPath("incident.reportedDate").isEqualTo("2023-01-02")
        .jsonPath("incident.reportedTime").isEqualTo("15:00:00")
        .jsonPath("incident.createdByUsername").isNotEmpty
        .jsonPath("incident.createdDateTime").isNotEmpty
        .jsonPath("incident.internalLocation.description").isEqualTo("MDI-1-1-001")
        .jsonPath("incident.internalLocation.code").isEqualTo("1")
        .jsonPath("incident.internalLocation.locationId").isEqualTo("$aLocationInMoorland")
        .jsonPath("incident.prison.code").isEqualTo("MDI")
        .jsonPath("incident.prison.description").isEqualTo("MOORLAND")
        .jsonPath("incident.details").isEqualTo("There was a fight in the toilets")
        .jsonPath("incident.incidentType.code").isEqualTo("GOV")
        .jsonPath("incident.incidentType.description").isEqualTo("Governor's Report")
    }

    private fun getAdjudicationEvidenceTest(url: String) {
      webTestClient.get().uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
        .jsonPath("investigations[0].comment").isEqualTo("Isla comment for investigation")
        .jsonPath("investigations[0].dateAssigned").isEqualTo("2023-01-02")
        .jsonPath("investigations[0].investigator.firstName").isEqualTo("ISLA")
        .jsonPath("investigations[0].investigator.lastName").isEqualTo("INVESTIGATOR")
        .jsonPath("investigations[0].investigator.staffId").isEqualTo(staffInvestigator.id)
        .jsonPath("investigations[0].investigator.username").isEqualTo("I.INVESTIGATOR")
        .jsonPath("investigations[0].evidence[0].detail").isEqualTo("smashed light bulb")
        .jsonPath("investigations[0].evidence[0].type.code").isEqualTo("PHOTO")
        .jsonPath("investigations[0].evidence[0].type.description").isEqualTo("Photographic Evidence")
        .jsonPath("investigations[0].evidence[0].date").isEqualTo("2023-01-03")
        .jsonPath("investigations[0].evidence[0].createdByUsername").isNotEmpty
        .jsonPath("investigations[0].evidence[1].detail").isEqualTo("syringe")
        .jsonPath("investigations[0].evidence[1].type.code").isEqualTo("DRUGTEST")
        .jsonPath("investigations[0].evidence[1].type.description").isEqualTo("Drug Test Report")
        .jsonPath("investigations[0].evidence[1].date").isEqualTo("2023-01-04")
        .jsonPath("investigations[0].evidence[1].createdByUsername").isNotEmpty
    }

    private fun getAdjudicationDamageTest(url: String) {
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
        .jsonPath("partyAddedDate").isEqualTo("2023-05-10")
        .jsonPath("incident.repairs[0].type.code").isEqualTo("PLUM")
        .jsonPath("incident.repairs[0].type.description").isEqualTo("Plumbing")
        .jsonPath("incident.repairs[0].comment").isEqualTo("Fixed the bog")
        .jsonPath("incident.repairs[0].cost").isEqualTo("10.3")
        .jsonPath("incident.repairs[0].createdByUsername").isNotEmpty
        .jsonPath("incident.repairs[1].type.code").isEqualTo("CLEA")
        .jsonPath("incident.repairs[1].type.description").isEqualTo("Cleaning")
        .jsonPath("incident.repairs[1].comment").doesNotExist()
        .jsonPath("incident.repairs[1].cost").doesNotExist()
        .jsonPath("incident.repairs[1].createdByUsername").isNotEmpty
    }

    private fun getAdjudicationHearingsTest(url: String) {
      webTestClient.get()
        .uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
        .jsonPath("hearings[0]").exists()
        .jsonPath("hearings[0].type.code").isEqualTo("GOV")
        .jsonPath("hearings[0].type.description").isEqualTo("Governor's Hearing")
        .jsonPath("hearings[0].scheduleDate").isEqualTo("2023-01-02")
        .jsonPath("hearings[0].scheduleTime").isEqualTo("14:00:00")
        .jsonPath("hearings[0].hearingDate").isEqualTo("2023-01-03")
        .jsonPath("hearings[0].hearingTime").isEqualTo("15:00:00")
        .jsonPath("hearings[0].comment").isEqualTo("Hearing comment")
        .jsonPath("hearings[0].representativeText").isEqualTo("rep text")
        .jsonPath("hearings[0].hearingStaff.staffId").isEqualTo(staff.id)
        .jsonPath("hearings[0].hearingStaff.username").isEqualTo("S.BROWN_GEN")
        .jsonPath("hearings[0].representativeText").isEqualTo("rep text")
        .jsonPath("hearings[0].internalLocation.description").isEqualTo("MDI-1-1-001")
        .jsonPath("hearings[0].eventStatus.code").isEqualTo("SCH")
        .jsonPath("hearings[0].eventId").isEqualTo(1)
        .jsonPath("hearings[0].createdByUsername").isNotEmpty
        .jsonPath("hearings[0].createdDateTime").isNotEmpty
        .jsonPath("hearings[0].notifications").isArray
        .jsonPath("hearings[0].notifications[0].deliveryDate").isEqualTo("2023-01-04")
        .jsonPath("hearings[0].notifications[0].deliveryTime").isEqualTo("10:00:00")
        .jsonPath("hearings[0].notifications[0].comment").doesNotExist()
        .jsonPath("hearings[0].notifications[0].notifiedStaff.username").isEqualTo("S.BROWN_GEN")
        .jsonPath("hearings[0].notifications[0].notifiedStaff.staffId").isEqualTo(staff.id)
        .jsonPath("hearings[0].notifications[1].deliveryDate").isEqualTo("2023-01-05")
        .jsonPath("hearings[0].notifications[1].deliveryTime").isEqualTo("11:30:00")
        .jsonPath("hearings[0].notifications[1].comment").isEqualTo("You have been issued this hearing")
        .jsonPath("hearings[0].notifications[1].notifiedStaff.username").isEqualTo("I.INVESTIGATOR")
        .jsonPath("hearings[0].notifications[1].notifiedStaff.staffId").isEqualTo(staffInvestigator.id)
    }

    private fun getAdjudicationOtherPartiesTest(url: String) {
      webTestClient.get().uri(url)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
        .jsonPath("incident.staffWitnesses[0].firstName").isEqualTo("KOFI")
        .jsonPath("incident.staffWitnesses[0].lastName").isEqualTo("WITNESS")
        .jsonPath("incident.staffWitnesses[0].staffId").isEqualTo(staffWitness.id)
        .jsonPath("incident.staffWitnesses[0].username").isEqualTo("K.WITNESS")
        .jsonPath("incident.staffWitnesses[0].dateAddedToIncident").isEqualTo("2023-01-03")
        .jsonPath("incident.staffWitnesses[0].comment").isEqualTo("They saw everything")
        .jsonPath("incident.staffVictims[0].staffId").isEqualTo(staffVictim.id)
        .jsonPath("incident.reportingOfficers[0].staffId").isEqualTo(staffIncidentReportingOfficer.id)
        .jsonPath("incident.reportingOfficers[0].username").isEqualTo(staffIncidentReportingOfficer.accounts[0].username)
        .jsonPath("incident.otherStaffInvolved[0].staffId").isEqualTo(staffInvolvedWithForce.id)
        .jsonPath("incident.otherStaffInvolved[0].username").isEqualTo(staffInvolvedWithForce.accounts[0].username)
        .jsonPath("incident.prisonerVictims[0].firstName").isEqualTo("CHARLIE")
        .jsonPath("incident.prisonerVictims[0].lastName").isEqualTo("VICTIM")
        .jsonPath("incident.prisonerVictims[0].offenderNo").isEqualTo(prisonerVictim.nomsId)
        .jsonPath("incident.prisonerVictims[0].dateAddedToIncident").isEqualTo("2023-01-04")
        .jsonPath("incident.prisonerVictims[0].comment").isEqualTo("Beaten up")
        .jsonPath("incident.prisonerWitnesses[0].offenderNo").isEqualTo(prisonerWitness.nomsId)
        .jsonPath("incident.otherPrisonersInvolved[0].offenderNo").isEqualTo(anotherSuspect.nomsId)
        .jsonPath("incident.otherPrisonersInvolved[1]").doesNotExist()
    }
  }

  @DisplayName("POST /prisoners/{offenderNo}/adjudications")
  @Nested
  inner class CreateAdjudication {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L

    @BeforeEach
    fun createPrisoner() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {}
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/A1965NM/adjudications")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/A1965NM/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/A1965NM/adjudications")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisonerWithNoBookings: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          prisonerWithNoBookings = offender(nomsId = "A9876AK")
        }
      }

      @Test
      fun `will return 404 if prisoner not found`() {
        webTestClient.post().uri("/prisoners/A9999ZZ/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner A9999ZZ not found")
      }

      @Test
      fun `will return 400 if prisoner number is not valid`() {
        webTestClient.post().uri("/prisoners/BANANAS/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication()))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("""createAdjudication.offenderNo: must match "[A-Z]\d{4}[A-Z]{2}"""")
      }

      @Test
      fun `will return 400 if person has no bookings yet`() {
        webTestClient.post().uri("/prisoners/${prisonerWithNoBookings.nomsId}/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication()))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prisoner ${prisonerWithNoBookings.nomsId} has no bookings")
      }

      @Test
      fun `will return 400 if reporting officer is not found`() {
        webTestClient.post().uri("/prisoners/${prisoner.nomsId}/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication(reportingStaffUsername = "BANANAS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Staff BANANAS not found")
      }

      @Test
      fun `will return 400 if offence code is not found`() {
        webTestClient.post().uri("/prisoners/${prisoner.nomsId}/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication(offenceCode = "BANANAS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Offence BANANAS not found")
      }

      @Test
      fun `will return 400 if evidence type code is not found`() {
        webTestClient.post().uri("/prisoners/${prisoner.nomsId}/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aMultiEvidenceAdjudication(
                evidence1 = EvidenceToCreate(
                  typeCode = "BANANAS",
                  detail = "A bunch of bananas",
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Evidence type BANANAS not found")
      }

      @Test
      fun `will return 400 if repair type code is not found`() {
        webTestClient.post().uri("/prisoners/${prisoner.nomsId}/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aMultiRepairAdjudication(
                repair1 = RepairToCreate(typeCode = "BANANAS", null, null),
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Repair type BANANAS not found")
      }

      @Test
      fun `will return 400 if internal location of incident code is not found`() {
        webTestClient.post().uri("/prisoners/${prisoner.nomsId}/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication(internalLocationId = 9999999)))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prison internal location 9999999 not found")
      }

      @Test
      fun `will return 400 if prison where adjudication is created code is not found`() {
        webTestClient.post().uri("/prisoners/${prisoner.nomsId}/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aSimpleAdjudication(prisonId = "BANANAS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Prison BANANAS not found")
      }

      @Test
      fun `will return 409 if adjudication already exists`() {
        assertThat(repository.getAdjudicationIncidentByAdjudicationNumber(existingAdjudicationNumber)).isNotNull()

        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aSimpleAdjudication(
                adjudicationNumber = existingAdjudicationNumber.toString(),
                reportingStaffUsername = "JANESTAFF",
              ),
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(HttpStatus.CONFLICT)
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication $existingAdjudicationNumber already exists")
      }
    }

    @Nested
    inner class HappyPath {
      private var incident: AdjudicationIncident? = null
      private lateinit var staffWitnessAdaeze: StaffUserAccount
      private lateinit var staffWitnessAbiodun: StaffUserAccount
      private lateinit var staffVictimAarav: StaffUserAccount
      private lateinit var staffVictimShivav: StaffUserAccount
      private lateinit var prisonerVictimG1234VV: Offender
      private lateinit var prisonerVictimA1234CT: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff { staffWitnessAdaeze = account(username = "ADAEZE") }
          staff { staffWitnessAbiodun = account(username = "ABIODUN") }
          staff { staffVictimAarav = account(username = "AARAV") }
          staff { staffVictimShivav = account(username = "SHIVAV") }
          prisonerVictimG1234VV = offender(nomsId = "G1234VV") { booking { } }
          prisonerVictimA1234CT = offender(nomsId = "A1234CT") { booking { } }
        }
      }

      @AfterEach
      fun tearDown() {
        incident?.run { repository.delete(this) }
        repository.deleteStaffByAccount(staffWitnessAdaeze, staffWitnessAbiodun, staffVictimAarav, staffVictimShivav)
        repository.delete(prisonerVictimG1234VV, prisonerVictimA1234CT)
      }

      @Test
      fun `create an adjudication with minimal data`() {
        assertThat(repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)).isNull()

        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aSimpleAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                reportingStaffUsername = "JANESTAFF",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          assertThat(incident!!.reportingStaff.accounts[0].username).isEqualTo("JANESTAFF")
          assertThat(incident!!.findAdjudication(adjudicationNumber).prisonerParty().nomsId).isEqualTo(offenderNo)
        }
      }

      @Test
      fun `can create adjudication with multiple charges even though this is not expected to happen`() {
        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aMultiChargeAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                offenceCode1 = "51:1N",
                offenceCode2 = "51:19A",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          assertThat(incident.findAdjudication(adjudicationNumber).charges).hasSize(2)
          with(incident.findAdjudication(adjudicationNumber).charges[0]) {
            assertThat(offence.code).isEqualTo("51:1N")
            assertThat(offenceId).isEqualTo("$adjudicationNumber/1")
            assertThat(id.chargeSequence).isEqualTo(1)
          }
          with(incident.findAdjudication(adjudicationNumber).charges[1]) {
            assertThat(offence.code).isEqualTo("51:19A")
            assertThat(offenceId).isEqualTo("$adjudicationNumber/2")
            assertThat(id.chargeSequence).isEqualTo(2)
          }
        }
      }

      @Test
      fun `core data create in an adjudication is returned and in the NOMIS database and can be queried by API`() {
        assertThat(repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)).isNull()

        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aSimpleAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                reportingStaffUsername = "JANESTAFF",
                internalLocationId = aLocationInMoorland,
                prisonId = "MDI",
                incidentDate = "2023-01-01",
                incidentTime = "10:15",
                reportedDate = "2023-02-10",
                reportedTime = "09:15",
                incidentDetails = "There was a fight in the toilets",
                offenceCode = "51:1N",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("bookingId").isEqualTo(prisoner.latestBooking().bookingId)
          .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
          .jsonPath("incident.adjudicationIncidentId").isNumber
          .jsonPath("incident.reportingStaff.firstName").isEqualTo("JANE")
          .jsonPath("incident.reportingStaff.lastName").isEqualTo("STAFF")
          .jsonPath("incident.incidentDate").isEqualTo("2023-01-01")
          .jsonPath("incident.incidentTime").isEqualTo("10:15:00")
          .jsonPath("incident.reportedDate").isEqualTo("2023-02-10")
          .jsonPath("incident.reportedTime").isEqualTo("09:15:00")
          .jsonPath("incident.internalLocation.description").isEqualTo("MDI-1-1-001")
          .jsonPath("incident.internalLocation.code").isEqualTo("1")
          .jsonPath("incident.internalLocation.locationId").isEqualTo("$aLocationInMoorland")
          .jsonPath("incident.prison.code").isEqualTo("MDI")
          .jsonPath("incident.prison.description").isEqualTo("MOORLAND")
          .jsonPath("incident.details").isEqualTo("There was a fight in the toilets")
          .jsonPath("incident.incidentType.code").isEqualTo("GOV")
          .jsonPath("incident.incidentType.description").isEqualTo("Governor's Report")
          .jsonPath("charges[0].offence.code").isEqualTo("51:1N")
          .jsonPath("charges[0].offence.description")
          .isEqualTo("Commits any assault - assault on non prison officer member of staff")
          .jsonPath("charges[0].offence.type.description").isEqualTo("Prison Rule 51")
          .jsonPath("charges[0].offenceId").isEqualTo("$adjudicationNumber/1")
          .jsonPath("charges[0].chargeSequence").isEqualTo("1")

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          assertThat(incident!!.reportingStaff.accounts[0].username).isEqualTo("JANESTAFF")
          assertThat(incident!!.createUsername).isEqualTo("JANESTAFF")
          assertThat(incident!!.agencyInternalLocation.locationId).isEqualTo(aLocationInMoorland)
          assertThat(incident!!.prison.id).isEqualTo("MDI")
          assertThat(incident!!.incidentDate).isEqualTo(LocalDate.parse("2023-01-01"))
          assertThat(incident!!.incidentDateTime).isEqualTo(LocalDateTime.parse("2023-01-01T10:15"))
          assertThat(incident!!.reportedDate).isEqualTo(LocalDate.parse("2023-02-10"))
          assertThat(incident!!.reportedDateTime).isEqualTo(LocalDateTime.parse("2023-02-10T09:15"))
          assertThat(incident!!.incidentDetails).isEqualTo("There was a fight in the toilets")
          assertThat(incident!!.findAdjudication(adjudicationNumber).prisonerParty().nomsId).isEqualTo(offenderNo)
          assertThat(incident!!.findAdjudication(adjudicationNumber).charges).hasSize(1)
            .allMatch {
              it.offence.code == "51:1N" &&
                it.offenceId == "$adjudicationNumber/1" &&
                it.lidsChargeNumber != null
            }
        }

        webTestClient.get().uri("/adjudications/adjudication-number/$adjudicationNumber")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("offenderNo").isEqualTo(offenderNo)
          .jsonPath("bookingId").isEqualTo(prisoner.latestBooking().bookingId)
          .jsonPath("adjudicationNumber").isEqualTo(adjudicationNumber)
          .jsonPath("incident.adjudicationIncidentId").isNumber
          .jsonPath("incident.reportingStaff.firstName").isEqualTo("JANE")
          .jsonPath("incident.reportingStaff.lastName").isEqualTo("STAFF")
          .jsonPath("incident.incidentDate").isEqualTo("2023-01-01")
          .jsonPath("incident.incidentTime").isEqualTo("10:15:00")
          .jsonPath("incident.reportedDate").isEqualTo("2023-02-10")
          .jsonPath("incident.reportedTime").isEqualTo("09:15:00")
          .jsonPath("incident.internalLocation.description").isEqualTo("MDI-1-1-001")
          .jsonPath("incident.internalLocation.code").isEqualTo("1")
          .jsonPath("incident.internalLocation.locationId").isEqualTo("$aLocationInMoorland")
          .jsonPath("incident.prison.code").isEqualTo("MDI")
          .jsonPath("incident.prison.description").isEqualTo("MOORLAND")
          .jsonPath("incident.details").isEqualTo("There was a fight in the toilets")
          .jsonPath("incident.incidentType.code").isEqualTo("GOV")
          .jsonPath("incident.incidentType.description").isEqualTo("Governor's Report")
          .jsonPath("charges[0].offence.code").isEqualTo("51:1N")
          .jsonPath("charges[0].offence.description")
          .isEqualTo("Commits any assault - assault on non prison officer member of staff")
          .jsonPath("charges[0].offence.type.description").isEqualTo("Prison Rule 51")
          .jsonPath("charges[0].offenceId").isEqualTo("$adjudicationNumber/1")
          .jsonPath("charges[0].chargeSequence").isEqualTo("1")
      }

      @Test
      fun `can create adjudication with evidence`() {
        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aMultiEvidenceAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                reportingStaffUsername = "JANESTAFF",
                evidence1 = EvidenceToCreate("PHOTO", "Picture on injuries"),
                evidence2 = EvidenceToCreate("EVI_BAG", "The knife used in the attack"),
                reportedDate = "2023-02-10",
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          assertThat(incident.findAdjudication(adjudicationNumber).investigations).hasSize(1)
          with(incident.findAdjudication(adjudicationNumber).investigations[0]) {
            assertThat(evidence).hasSize(2)
            assertThat(assignedDate).isEqualTo(LocalDate.parse("2023-02-10"))
            assertThat(comment).isEqualTo("Supplied by DPS")
            assertThat(investigator.id).isEqualTo(reportingStaff.id)
          }
          with(incident.findAdjudication(adjudicationNumber).investigations[0].evidence[0]) {
            assertThat(statementType.code).isEqualTo("PHOTO")
            assertThat(statementType.description).isEqualTo("Photographic Evidence")
            assertThat(statementDate).isEqualTo(LocalDate.parse("2023-02-10"))
            assertThat(statementDetail).isEqualTo("Picture on injuries")
          }
          with(incident.findAdjudication(adjudicationNumber).investigations[0].evidence[1]) {
            assertThat(statementType.code).isEqualTo("EVI_BAG")
            assertThat(statementType.description).isEqualTo("Evidence Bag")
            assertThat(statementDate).isEqualTo(LocalDate.parse("2023-02-10"))
            assertThat(statementDetail).isEqualTo("The knife used in the attack")
          }
        }
      }

      @Test
      fun `can create adjudication with damages (aka repairs)`() {
        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aMultiRepairAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                repair1 = RepairToCreate("PLUM", "Toilets need replacing", null),
                repair2 = RepairToCreate("ELEC", null, BigDecimal.valueOf(12.2)),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          assertThat(incident!!.repairs).hasSize(2)
          with(incident!!.repairs[0]) {
            assertThat(comment).isEqualTo("Toilets need replacing")
            assertThat(repairCost).isNull()
            assertThat(type.code).isEqualTo("PLUM")
            assertThat(type.description).isEqualTo("Plumbing")
          }
          with(incident!!.repairs[1]) {
            assertThat(comment).isNull()
            assertThat(repairCost).isEqualTo(BigDecimal.valueOf(12.2))
            assertThat(type.code).isEqualTo("ELEC")
            assertThat(type.description).isEqualTo("Electrical")
          }
        }
      }

      @Test
      fun `can create adjudication with witnesses`() {
        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aSimpleAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                staffWitnessesUsernames = listOf(staffWitnessAdaeze.username, staffWitnessAbiodun.username),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          val witnesses = incident!!.parties.filter { it.incidentRole == witnessRole }
          assertThat(witnesses).hasSize(2)
          assertThat(witnesses).anyMatch { it.staff?.id == staffWitnessAdaeze.staff.id }
          assertThat(witnesses).anyMatch { it.staff?.id == staffWitnessAbiodun.staff.id }
        }
      }

      @Test
      fun `can create adjudication with staff victims`() {
        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aSimpleAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                staffVictimsUsernames = listOf(staffVictimAarav.username, staffVictimShivav.username),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          val victims = incident!!.parties.filter { it.incidentRole == victimRole && it.staff != null }
          assertThat(victims).hasSize(2)
          assertThat(victims).anyMatch { it.staff?.id == staffVictimAarav.staff.id }
          assertThat(victims).anyMatch { it.staff?.id == staffVictimShivav.staff.id }
        }
      }

      @Test
      fun `can create adjudication with prisoner victims`() {
        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aSimpleAdjudication(
                adjudicationNumber = adjudicationNumber.toString(),
                prisonerVictimsOffenderNumbers = listOf(prisonerVictimG1234VV.nomsId, prisonerVictimA1234CT.nomsId),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          val victims = incident!!.parties.filter { it.incidentRole == victimRole && it.offenderBooking != null }
          assertThat(victims).hasSize(2)
          assertThat(victims).anyMatch { it.offenderBooking?.offender?.nomsId == prisonerVictimG1234VV.nomsId }
          assertThat(victims).anyMatch { it.offenderBooking?.offender?.nomsId == prisonerVictimA1234CT.nomsId }
          assertThat(victims).allMatch { it.actionDecision?.description == "No Further Action" }
        }
      }

      @Test
      fun `can create a complex adjudication with many people involved`() {
        webTestClient.post().uri("/prisoners/$offenderNo/adjudications")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjudicationNumber": $adjudicationNumber,
                    "incident": {
                      "reportingStaffUsername": "JANESTAFF",
                      "incidentDate": "2023-01-01",
                      "incidentTime": "10:15",
                      "reportedDate": "2023-02-10",
                      "reportedTime": "09:15",
                      "internalLocationId": $aLocationInMoorland,
                      "details": "There was a fight in the toilets",
                      "prisonId": "MDI",
                      "prisonerVictimsOffenderNumbers": [
                        "G1234VV",
                        "A1234CT"
                      ],
                      "staffWitnessesUsernames": [
                        "ADAEZE",
                        "ABIODUN"
                      ],
                      "staffVictimsUsernames": [
                        "AARAV",
                        "SHIVAV"
                      ],
                      "repairs": [
                        {
                          "comment": "Toilets need replacing",
                          "typeCode": "PLUM"
                        },
                        {
                          "cost": 12.2,
                          "typeCode": "ELEC"
                        }
                      ]
                    },
                    "charges": [
                      {
                        "offenceCode": "51:1N",
                        "offenceId": "$adjudicationNumber/1"
                      },
                      {
                        "offenceCode": "51:19A",
                        "offenceId": "$adjudicationNumber/2"
                      }
                    ],
                    "evidence": [
                      {
                        "typeCode": "PHOTO",
                        "detail": "Picture on injuries"
                      },
                      {
                        "typeCode": "EVI_BAG",
                        "detail": "The knife used in the attack"
                      }
                    ]
                  }
                
            """,
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          incident = repository.getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)

          assertThat(incident).isNotNull
          assertThat(incident!!.reportingStaff.accounts[0].username).isEqualTo("JANESTAFF")
          assertThat(incident!!.agencyInternalLocation.locationId).isEqualTo(aLocationInMoorland)
          assertThat(incident!!.prison.id).isEqualTo("MDI")
          assertThat(incident!!.incidentDate).isEqualTo(LocalDate.parse("2023-01-01"))
          assertThat(incident!!.incidentDateTime).isEqualTo(LocalDateTime.parse("2023-01-01T10:15"))
          assertThat(incident!!.reportedDate).isEqualTo(LocalDate.parse("2023-02-10"))
          assertThat(incident!!.reportedDateTime).isEqualTo(LocalDateTime.parse("2023-02-10T09:15"))
          assertThat(incident!!.incidentDetails).isEqualTo("There was a fight in the toilets")
          assertThat(incident!!.findAdjudication(adjudicationNumber).prisonerParty().nomsId).isEqualTo(offenderNo)
          with(incident.findAdjudication(adjudicationNumber).charges[0]) {
            assertThat(offence.code).isEqualTo("51:1N")
            assertThat(offenceId).isEqualTo("$adjudicationNumber/1")
            assertThat(id.chargeSequence).isEqualTo(1)
          }
          with(incident.findAdjudication(adjudicationNumber).charges[1]) {
            assertThat(offence.code).isEqualTo("51:19A")
            assertThat(offenceId).isEqualTo("$adjudicationNumber/2")
            assertThat(id.chargeSequence).isEqualTo(2)
          }
          assertThat(incident.findAdjudication(adjudicationNumber).investigations).hasSize(1)
          with(incident.findAdjudication(adjudicationNumber).investigations[0]) {
            assertThat(evidence).hasSize(2)
            assertThat(assignedDate).isEqualTo(LocalDate.parse("2023-02-10"))
            assertThat(comment).isEqualTo("Supplied by DPS")
            assertThat(investigator.id).isEqualTo(reportingStaff.id)
          }
          with(incident.findAdjudication(adjudicationNumber).investigations[0].evidence[0]) {
            assertThat(statementType.code).isEqualTo("PHOTO")
            assertThat(statementType.description).isEqualTo("Photographic Evidence")
            assertThat(statementDate).isEqualTo(LocalDate.parse("2023-02-10"))
            assertThat(statementDetail).isEqualTo("Picture on injuries")
          }
          with(incident.findAdjudication(adjudicationNumber).investigations[0].evidence[1]) {
            assertThat(statementType.code).isEqualTo("EVI_BAG")
            assertThat(statementType.description).isEqualTo("Evidence Bag")
            assertThat(statementDate).isEqualTo(LocalDate.parse("2023-02-10"))
            assertThat(statementDetail).isEqualTo("The knife used in the attack")
          }
          assertThat(incident!!.repairs).hasSize(2)
          with(incident!!.repairs[0]) {
            assertThat(comment).isEqualTo("Toilets need replacing")
            assertThat(repairCost).isNull()
            assertThat(type.code).isEqualTo("PLUM")
            assertThat(type.description).isEqualTo("Plumbing")
          }
          with(incident!!.repairs[1]) {
            assertThat(comment).isNull()
            assertThat(repairCost).isEqualTo(BigDecimal.valueOf(12.2))
            assertThat(type.code).isEqualTo("ELEC")
            assertThat(type.description).isEqualTo("Electrical")
          }
          val prisonerVictims =
            incident!!.parties.filter { it.incidentRole == victimRole && it.offenderBooking != null }
          assertThat(prisonerVictims).hasSize(2)
          assertThat(prisonerVictims).anyMatch { it.offenderBooking?.offender?.nomsId == prisonerVictimG1234VV.nomsId }
          assertThat(prisonerVictims).anyMatch { it.offenderBooking?.offender?.nomsId == prisonerVictimA1234CT.nomsId }
          assertThat(prisonerVictims).allMatch { it.actionDecision?.description == "No Further Action" }
          val staffVictims = incident!!.parties.filter { it.incidentRole == victimRole && it.staff != null }
          assertThat(staffVictims).hasSize(2)
          assertThat(staffVictims).anyMatch { it.staff?.id == staffVictimAarav.staff.id }
          assertThat(staffVictims).anyMatch { it.staff?.id == staffVictimShivav.staff.id }
          val witnesses = incident!!.parties.filter { it.incidentRole == witnessRole }
          assertThat(witnesses).hasSize(2)
          assertThat(witnesses).anyMatch { it.staff?.id == staffWitnessAdaeze.staff.id }
          assertThat(witnesses).anyMatch { it.staff?.id == staffWitnessAbiodun.staff.id }
        }
      }
    }

    private fun aSimpleAdjudication(
      reportingStaffUsername: String = "JANESTAFF",
      offenceCode: String = "51:1N",
      adjudicationNumber: String = "12345678",
      internalLocationId: Long = aLocationInMoorland,
      prisonId: String = "MDI",
      incidentDate: String = "2023-01-01",
      incidentTime: String = "10:15",
      reportedDate: String = "2023-02-10",
      reportedTime: String = "09:15",
      incidentDetails: String = "A fight that lead to so much blood",
      prisonerVictimsOffenderNumbers: List<String> = emptyList(),
      staffWitnessesUsernames: List<String> = emptyList(),
      staffVictimsUsernames: List<String> = emptyList(),
    ): String = """
      {
        "adjudicationNumber": $adjudicationNumber,
        "incident": {
          "reportingStaffUsername":  "$reportingStaffUsername",
          "incidentDate": "$incidentDate",
          "incidentTime": "$incidentTime",
          "reportedDate": "$reportedDate",
          "reportedTime": "$reportedTime",
          "internalLocationId": $internalLocationId,
          "details": "$incidentDetails",
          "prisonId": "$prisonId",
          "prisonerVictimsOffenderNumbers": [${prisonerVictimsOffenderNumbers.joinToString(",") { "\"$it\"" }}],
          "staffWitnessesUsernames": [${staffWitnessesUsernames.joinToString(",") { "\"$it\"" }}],
          "staffVictimsUsernames": [${staffVictimsUsernames.joinToString(",") { "\"$it\"" }}],
          "repairs": []
        },
        "charges": [
          {
            "offenceCode": "$offenceCode",
            "offenceId": "$adjudicationNumber/1"
          }
        ],
        "evidence": []
      }
    """.trimIndent()

    private fun aMultiChargeAdjudication(
      reportingStaffUsername: String = "JANESTAFF",
      offenceCode1: String = "51:1N",
      offenceCode2: String = "51:19A",
      adjudicationNumber: String = "12345678",
      internalLocationId: Long = aLocationInMoorland,
      prisonId: String = "MDI",
      incidentDate: String = "2023-01-01",
      incidentTime: String = "10:15",
      reportedDate: String = "2023-02-10",
      reportedTime: String = "09:15",
      incidentDetails: String = "A fight that lead to so much blood",
    ): String = """
      {
        "adjudicationNumber": $adjudicationNumber,
        "incident": {
          "reportingStaffUsername":  "$reportingStaffUsername",
          "incidentDate": "$incidentDate",
          "incidentTime": "$incidentTime",
          "reportedDate": "$reportedDate",
          "reportedTime": "$reportedTime",
          "internalLocationId": $internalLocationId,
          "details": "$incidentDetails",
          "prisonId": "$prisonId",
          "prisonerVictimsOffenderNumbers": [],
          "staffWitnessesUsernames": [],
          "staffVictimsUsernames": [],
          "repairs": []
        },
        "charges": [
          {
            "offenceCode": "$offenceCode1",
            "offenceId": "$adjudicationNumber/1"
          },
          {
            "offenceCode": "$offenceCode2",
            "offenceId": "$adjudicationNumber/2"
          }
        ],
        "evidence": []
      }
    """.trimIndent()

    private fun aMultiEvidenceAdjudication(
      evidence1: EvidenceToCreate = EvidenceToCreate("PHOTO", "Picture on injuries"),
      evidence2: EvidenceToCreate = EvidenceToCreate("EVI_BAG", "The knife used in the attack"),
      reportingStaffUsername: String = "JANESTAFF",
      offenceCode: String = "51:1N",
      adjudicationNumber: String = "12345678",
      internalLocationId: Long = aLocationInMoorland,
      prisonId: String = "MDI",
      incidentDate: String = "2023-01-01",
      incidentTime: String = "10:15",
      reportedDate: String = "2023-02-10",
      reportedTime: String = "09:15",
      incidentDetails: String = "A fight that lead to so much blood",
    ): String = """
      {
        "adjudicationNumber": $adjudicationNumber,
        "incident": {
          "reportingStaffUsername":  "$reportingStaffUsername",
          "incidentDate": "$incidentDate",
          "incidentTime": "$incidentTime",
          "reportedDate": "$reportedDate",
          "reportedTime": "$reportedTime",
          "internalLocationId": $internalLocationId,
          "details": "$incidentDetails",
          "prisonId": "$prisonId",
          "prisonerVictimsOffenderNumbers": [],
          "staffWitnessesUsernames": [],
          "staffVictimsUsernames": [],
          "repairs": []
        },
        "charges": [
          {
            "offenceCode": "$offenceCode",
            "offenceId": "$adjudicationNumber/1"
          }
        ],
        "evidence": [
          {
            "typeCode": "${evidence1.typeCode}",
            "detail": "${evidence1.detail}"
          },
          {
            "typeCode": "${evidence2.typeCode}",
            "detail": "${evidence2.detail}"
          }
        ]
      }
    """.trimIndent()

    private fun aMultiRepairAdjudication(
      repair1: RepairToCreate = RepairToCreate("PLUM", "Showers broken", null),
      repair2: RepairToCreate = RepairToCreate("ELEC", "Lights need replacing", BigDecimal.valueOf(12.2)),
      reportingStaffUsername: String = "JANESTAFF",
      offenceCode: String = "51:1N",
      adjudicationNumber: String = "12345678",
      internalLocationId: Long = aLocationInMoorland,
      prisonId: String = "MDI",
      incidentDate: String = "2023-01-01",
      incidentTime: String = "10:15",
      reportedDate: String = "2023-02-10",
      reportedTime: String = "09:15",
      incidentDetails: String = "A fight that lead to so much blood",
    ): String = """
      {
        "adjudicationNumber": $adjudicationNumber,
        "incident": {
          "reportingStaffUsername":  "$reportingStaffUsername",
          "incidentDate": "$incidentDate",
          "incidentTime": "$incidentTime",
          "reportedDate": "$reportedDate",
          "reportedTime": "$reportedTime",
          "internalLocationId": $internalLocationId,
          "details": "$incidentDetails",
          "prisonId": "$prisonId",
          "prisonerVictimsOffenderNumbers": [],
          "staffWitnessesUsernames": [],
          "staffVictimsUsernames": [],
          "repairs": [
            {
              ${repair1.cost?.let { """ "cost": $it,""" } ?: ""}
              ${repair1.comment?.let { """ "comment": "$it", """ } ?: ""} 
              "typeCode": "${repair1.typeCode}"
            },
            {
              ${repair2.cost?.let { """ "cost": $it,""" } ?: ""}
              ${repair2.comment?.let { """ "comment": "$it", """ } ?: ""} 
              "typeCode": "${repair2.typeCode}"
            }
          ]
        },
        "charges": [
          {
            "offenceCode": "$offenceCode",
            "offenceId": "$adjudicationNumber/1"
          }
        ],
        "evidence": []
      }
    """.trimIndent()
  }

  @DisplayName("PUT /adjudications/adjudication-number/{adjudicationNumber}/repairs")
  @Nested
  inner class UpdateAdjudicationRepairs {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L

    @BeforeEach
    fun createPrisoner() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {
          repair(repairType = "PLUM", comment = "Toilets need replacing")
        }
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {}
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"repairs": []}"""))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"repairs": []}"""))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"repairs": []}"""))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 is returned if adjudication does not exist`() {
        webTestClient.put().uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", 99999)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"repairs": []}"""))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `repair type code must be present`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "repairs": [
                {
                  "comment": "Toilets need replacing"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
      }

      @Test
      fun `repair type code must valid`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "repairs": [
                {
                  "comment": "Toilets need replacing",
                  "typeCode": "BANANAS"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Repair type BANANAS not found")
      }
    }

    @Nested
    inner class HappyPath {
      private fun Repository.repairsFor(adjudicationNumber: Long): List<AdjudicationIncidentRepair> =
        getAdjudicationIncidentByAdjudicationNumber(adjudicationNumber)?.repairs ?: emptyList()

      @Test
      fun `empty list will remove existing repairs`() {
        repository.runInTransaction {
          assertThat(repository.repairsFor(existingAdjudicationNumber)).hasSize(1)
        }

        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "repairs": []
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          assertThat(repository.repairsFor(existingAdjudicationNumber)).hasSize(0)
        }
      }

      @Test
      fun `can appear to update existing repairs`() {
        repository.runInTransaction {
          val repairs = repository.repairsFor(existingAdjudicationNumber)
          assertThat(repairs).hasSize(1)
          assertThat(repairs[0].type.code).isEqualTo("PLUM")
          assertThat(repairs[0].comment).isEqualTo("Toilets need replacing")
        }

        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "repairs": [
                {
                  "comment": "Toilets need fixing",
                  "typeCode": "PLUM"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          val repairs = repository.repairsFor(existingAdjudicationNumber)
          assertThat(repairs).hasSize(1)
          assertThat(repairs[0].type.code).isEqualTo("PLUM")
          assertThat(repairs[0].comment).isEqualTo("Toilets need fixing")
        }
      }

      @Test
      fun `can replace and add repairs`() {
        repository.runInTransaction {
          val repairs = repository.repairsFor(existingAdjudicationNumber)
          assertThat(repairs).hasSize(1)
          assertThat(repairs[0].type.code).isEqualTo("PLUM")
        }

        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "repairs": [
                {
                  "comment": "Lights need fixing",
                  "typeCode": "ELEC"
                },
                {
                  "comment": "Carpets need cleaning",
                  "typeCode": "DECO"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          val repairs = repository.repairsFor(existingAdjudicationNumber)
          assertThat(repairs).hasSize(2)
          assertThat(repairs[0].type.code).isEqualTo("ELEC")
          assertThat(repairs[0].comment).isEqualTo("Lights need fixing")
          assertThat(repairs[1].type.code).isEqualTo("DECO")
          assertThat(repairs[1].comment).isEqualTo("Carpets need cleaning")
        }
      }

      @Test
      fun `update repair list is returned`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/repairs", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "repairs": [
                {
                  "comment": "Lights need fixing",
                  "typeCode": "ELEC"
                },
                {
                  "comment": "Carpets need cleaning",
                  "typeCode": "DECO"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("repairs").isArray
          .jsonPath("repairs[0].comment").isEqualTo("Lights need fixing")
          .jsonPath("repairs[0].type.code").isEqualTo("ELEC")
          .jsonPath("repairs[1].comment").isEqualTo("Carpets need cleaning")
          .jsonPath("repairs[1].type.code").isEqualTo("DECO")
      }
    }
  }

  @DisplayName("PUT /adjudications/adjudication-number/{adjudicationNumber}/evidence")
  @Nested
  inner class UpdateAdjudicationEvidence {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L

    @BeforeEach
    fun createPrisoner() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {
        }
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              investigation(
                investigator = reportingStaff,
                comment = "Isla comment for investigation",
                assignedDate = LocalDate.parse("2023-01-02"),
              ) {
                evidence(
                  date = LocalDate.parse("2023-01-03"),
                  detail = "smashed light bulb",
                  type = "PHOTO",
                )
                evidence(
                  date = LocalDate.parse("2023-01-04"),
                  detail = "syringe",
                  type = "DRUGTEST",
                )
              }
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"evidence": []}"""))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"evidence": []}"""))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"evidence": []}"""))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 is returned if adjudication does not exist`() {
        webTestClient.put().uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", 99999)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue("""{"evidence": []}"""))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `evidence type code must be present`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "evidence": [
                {
                  "detail": "Photo of the incident"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
      }

      @Test
      fun `evidence type code must valid`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "evidence": [
                {
                  "detail": "Photo of incident",
                  "typeCode": "BANANAS"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Evidence type BANANAS not found")
      }
    }

    @Nested
    inner class HappyPath {
      private fun Repository.evidenceFor(adjudicationNumber: Long): List<AdjudicationEvidence> =
        adjudicationIncidentPartyRepository.findByAdjudicationNumber(adjudicationNumber)?.investigations?.flatMap { it.evidence } ?: emptyList()

      @Test
      fun `empty list will remove existing evidence`() {
        repository.runInTransaction {
          assertThat(repository.evidenceFor(existingAdjudicationNumber)).hasSize(2)
        }

        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "evidence": []
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          assertThat(repository.evidenceFor(existingAdjudicationNumber)).hasSize(0)
        }
      }

      @Test
      fun `can appear to update existing evidence`() {
        repository.runInTransaction {
          val evidence = repository.evidenceFor(existingAdjudicationNumber)
          assertThat(evidence).hasSize(2)
          assertThat(evidence[0].statementType.code).isEqualTo("PHOTO")
          assertThat(evidence[0].statementDetail).isEqualTo("smashed light bulb")
          assertThat(evidence[1].statementType.code).isEqualTo("DRUGTEST")
          assertThat(evidence[1].statementDetail).isEqualTo("syringe")
        }

        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "evidence": [
                {
                  "detail": "Damaged bed",
                  "typeCode": "PHOTO"
                },
                {
                  "detail": "Drugs need testing",
                  "typeCode": "DRUGTEST"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          val evidence = repository.evidenceFor(existingAdjudicationNumber)
          assertThat(evidence).hasSize(2)
          assertThat(evidence[0].statementType.code).isEqualTo("PHOTO")
          assertThat(evidence[0].statementDetail).isEqualTo("Damaged bed")
          assertThat(evidence[1].statementType.code).isEqualTo("DRUGTEST")
          assertThat(evidence[1].statementDetail).isEqualTo("Drugs need testing")
        }
      }

      @Test
      fun `can replace and add evidence`() {
        repository.runInTransaction {
          val evidence = repository.evidenceFor(existingAdjudicationNumber)
          assertThat(evidence).hasSize(2)
          assertThat(evidence[0].statementType.code).isEqualTo("PHOTO")
          assertThat(evidence[1].statementType.code).isEqualTo("DRUGTEST")
        }

        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "evidence": [
                {
                  "detail": "smashed light bulb",
                  "typeCode": "PHOTO"
                },
                {
                  "detail": "Knife used",
                  "typeCode": "EVI_BAG"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          val evidence = repository.evidenceFor(existingAdjudicationNumber)
          assertThat(evidence[0].statementType.code).isEqualTo("PHOTO")
          assertThat(evidence[0].statementDetail).isEqualTo("smashed light bulb")
          assertThat(evidence[1].statementType.code).isEqualTo("EVI_BAG")
          assertThat(evidence[1].statementDetail).isEqualTo("Knife used")
        }
      }

      @Test
      fun `updated evidence list is returned`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/{adjudicationNumber}/evidence", existingAdjudicationNumber)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ADJUDICATIONS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              //language=json
              """
            {
              "evidence": [
                {
                  "detail": "smashed light bulb",
                  "typeCode": "PHOTO"
                },
                {
                  "detail": "Knife used",
                  "typeCode": "EVI_BAG"
                }
              ]
            }
              """.trimIndent(),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("evidence").isArray
          .jsonPath("evidence[0].detail").isEqualTo("smashed light bulb")
          .jsonPath("evidence[0].date").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
          .jsonPath("evidence[0].type.code").isEqualTo("PHOTO")
          .jsonPath("evidence[1].detail").isEqualTo("Knife used")
          .jsonPath("evidence[1].date").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
          .jsonPath("evidence[1].type.code").isEqualTo("EVI_BAG")
      }
    }
  }
}
