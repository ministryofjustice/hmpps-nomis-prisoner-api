package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SWANSEA
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementsService.Companion.MAX_TAP_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.profiledetails.roundToNearestSecond
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MovementsResourceIntTest(
  @Autowired val applicationRepository: OffenderTapApplicationRepository,
  @Autowired val scheduleOutRepository: OffenderTapScheduleOutRepository,
  @Autowired val scheduleInRepository: OffenderTapScheduleInRepository,
  @Autowired val movementOutRepository: OffenderTapMovementOutRepository,
  @Autowired val movementInRepository: OffenderTapMovementInRepository,
  @Autowired val offenderExternalMovementRepository: OffenderExternalMovementRepository,
  @Autowired val corporateRepository: CorporateRepository,
  @Autowired val offenderAddressRepository: OffenderAddressRepository,
  @Autowired val agencyLocationRepository: AgencyLocationRepository,
  @Autowired val offenderRepository: OffenderRepository,
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var offenderAddress: OffenderAddress
  private lateinit var corporateAddress: CorporateAddress
  private lateinit var booking: OffenderBooking
  private lateinit var application: OffenderTapApplication
  private lateinit var scheduleOut: OffenderTapScheduleOut
  private lateinit var scheduleIn: OffenderTapScheduleIn
  private lateinit var movementOut: OffenderTapMovementOut
  private lateinit var movementIn: OffenderTapMovementIn
  private lateinit var unscheduledMovementOut: OffenderTapMovementOut
  private lateinit var unscheduledMovementIn: OffenderTapMovementIn
  private lateinit var agencyLocation: AgencyLocation

  private val offenderNo = "D6347ED"
  private val today: LocalDateTime = LocalDateTime.now().roundToNearestSecond()
  private val yesterday: LocalDateTime = today.minusDays(1)
  private val twoDaysAgo: LocalDateTime = today.minusDays(2)
  private lateinit var orphanedSchedule: OffenderTapScheduleOut
  private lateinit var orphanedScheduleReturn: OffenderTapScheduleIn

  @AfterEach
  fun `tear down`() {
    // This must be removed before the offender booking due to a foreign key constraint (Hibernate is no longer managing this entity)
    if (this::orphanedSchedule.isInitialized) {
      scheduleOutRepository.delete(orphanedSchedule)
    }
    if (this::orphanedScheduleReturn.isInitialized) {
      scheduleInRepository.delete(orphanedScheduleReturn)
    }
    offenderRepository.deleteAll()
    if (this::agencyLocation.isInitialized) {
      agencyLocationRepository.delete(agencyLocation)
    }
    corporateRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences")
  inner class GetTemporaryAbsencesAndMovements {
    private lateinit var agencyAddress: AgencyLocationAddress

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if offender not found`() {
      webTestClient.get()
        .uri("/movements/UNKNOWN/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Offender with nomsId=UNKNOWN not found")
        }
    }

    @Test
    fun `should retrieve application`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            application = tapApplication(
              eventSubType = "C5",
              applicationDate = twoDaysAgo,
              applicationTime = twoDaysAgo,
              fromDate = twoDaysAgo.toLocalDate(),
              releaseTime = twoDaysAgo,
              toDate = yesterday.toLocalDate(),
              returnTime = yesterday,
              applicationType = "SINGLE",
              applicationStatus = "APP-SCH",
              escort = "L",
              transportType = "VAN",
              comment = "Some comment application",
              prison = "LEI",
              toAgency = "HAZLWD",
              toAddress = offenderAddress,
              contactPersonName = "Derek",
              tapType = "RR",
              tapSubType = "RDR",
            )
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].bookingId").isEqualTo(booking.bookingId)
        .jsonPath("$.bookings[0].activeBooking").isEqualTo(true)
        .jsonPath("$.bookings[0].latestBooking").isEqualTo(true)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].movementApplicationId").isEqualTo(application.tapApplicationId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].applicationDate").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].fromDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].releaseTime").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].returnTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].applicationType").isEqualTo("SINGLE")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].applicationStatus").isEqualTo("APP-SCH")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].escortCode").isEqualTo("L")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].transportType").isEqualTo("VAN")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].comment").isEqualTo("Some comment application")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].prisonId").isEqualTo("LEI")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toAgencyId").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toAddressOwnerDescription").doesNotExist()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toAddressPostcode").isEqualTo("S1 1AA")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].contactPersonName").isEqualTo("Derek")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceType").isEqualTo("RR")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceSubType").isEqualTo("RDR")
    }

    @Test
    fun `should retrieve scheduled temporary absence`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                eventDate = twoDaysAgo.toLocalDate(),
                startTime = twoDaysAgo,
                eventSubType = "C5",
                eventStatus = "SCH",
                comment = "Scheduled temporary absence",
                escort = "L",
                fromPrison = "LEI",
                toAgency = "HAZLWD",
                transportType = "VAN",
                returnDate = yesterday.toLocalDate(),
                returnTime = yesterday,
                toAddress = offenderAddress,
                contactPersonName = "Derek",
              )
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventId").isEqualTo(scheduleOut.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.startTime").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventStatus").isEqualTo("SCH")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.comment").isEqualTo("Scheduled temporary absence")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.escort").isEqualTo("L")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.transportType").isEqualTo("VAN")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.returnDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.returnTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressId").isEqualTo("${offenderAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressOwnerClass").isEqualTo("OFF")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressPostcode").isEqualTo("S1 1AA")
    }

    @Test
    fun `should retrieve corporate address`() {
      lateinit var corporateAddress: CorporateAddress
      nomisDataBuilder.build {
        corporate(corporateName = "Boots") {
          corporateAddress = address(postcode = "S2 2AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                toAddress = corporateAddress,
              )
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressId").isEqualTo("${corporateAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressOwnerClass").isEqualTo("CORP")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressPostcode").isEqualTo("S2 2AA")
    }

    @Test
    fun `should retrieve agency address`() {
      lateinit var agencyAddress: AgencyLocationAddress
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(agencyLocationId = "BIGHHO", description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                toAddress = agencyAddress,
              )
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve scheduled temporary absence's external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut(
                  date = twoDaysAgo,
                  fromPrison = "LEI",
                  toAgency = "HAZLWD",
                  movementReason = "C5",
                  arrestAgency = "POL",
                  escort = "L",
                  escortText = "SE",
                  comment = "Tap OUT comment for scheduled absence",
                  toAddress = offenderAddress,
                )
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.sequence").isEqualTo(movementOut.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.movementDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.movementTime").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.arrestAgency").isEqualTo("POL")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.escort").isEqualTo("L")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.commentText").isEqualTo("Tap OUT comment for scheduled absence")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressId").isEqualTo("${offenderAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressOwnerClass").isEqualTo("OFF")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressPostcode").isEqualTo("S1 1AA")
    }

    @Test
    fun `should retrieve corporate address from external movement`() {
      lateinit var corporateAddress: CorporateAddress
      nomisDataBuilder.build {
        corporate(corporateName = "Boots") {
          corporateAddress = address(postcode = "S2 2AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut(
                  toAddress = corporateAddress,
                )
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressId").isEqualTo("${corporateAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressOwnerClass").isEqualTo("CORP")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressPostcode").isEqualTo("S2 2AA")
    }

    @Test
    fun `should retrieve agency address from external movement`() {
      lateinit var agencyAddress: AgencyLocationAddress
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(agencyLocationId = "BIGHHO", description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut(
                  toAddress = agencyAddress,
                )
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve address from schedule if not on movement`() {
      lateinit var agencyAddress: AgencyLocationAddress
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(agencyLocationId = "BIGHHO", description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                toAddress = agencyAddress,
              ) {
                movementOut = tapMovementOut(
                  toAddress = null,
                )
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve scheduled temporary absences return`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                scheduleIn = tapScheduleIn(
                  eventDate = yesterday.toLocalDate(),
                  startTime = yesterday,
                  eventSubType = "R25",
                  eventStatus = "SCH",
                  comment = "Scheduled temporary absence return",
                  escort = "U",
                  fromAgency = "HAZLWD",
                  toPrison = "LEI",
                )
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduleIn.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.startTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventSubType").isEqualTo("R25")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventStatus").isEqualTo("SCH")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.comment").isEqualTo("Scheduled temporary absence return")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.escort").isEqualTo("U")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.fromAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.toPrison").isEqualTo("LEI")
    }

    @Test
    fun `should retrieve scheduled temporary absence return's external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()

                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn(
                    date = yesterday,
                    fromAgency = "HAZLWD",
                    toPrison = "LEI",
                    movementReason = "R25",
                    escort = "U",
                    escortText = "SE",
                    comment = "Tap IN comment",
                    fromAddress = offenderAddress,
                  )
                }
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.sequence").isEqualTo(movementIn.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.movementDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.movementTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.movementReason").isEqualTo("R25")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.escort").isEqualTo("U")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.toPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.commentText").isEqualTo("Tap IN comment")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressId").isEqualTo("${offenderAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressOwnerClass").isEqualTo("OFF")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressPostcode").isEqualTo("S1 1AA")
    }

    @Test
    fun `should retrieve corporate address from return movement`() {
      lateinit var corporateAddress: CorporateAddress
      nomisDataBuilder.build {
        corporate(corporateName = "Boots") {
          corporateAddress = address(postcode = "S2 2AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()

                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn(
                    fromAddress = corporateAddress,
                  )
                }
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressId").isEqualTo("${corporateAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressOwnerClass").isEqualTo("CORP")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressPostcode").isEqualTo("S2 2AA")
    }

    @Test
    fun `should retrieve agency address from return movement`() {
      lateinit var agencyAddress: AgencyLocationAddress
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(agencyLocationId = "BIGHHO", description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()

                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn(
                    fromAddress = agencyAddress,
                  )
                }
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve address from outbound movement if not on return movement`() {
      lateinit var agencyAddress: AgencyLocationAddress
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(agencyLocationId = "BIGHHO", description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut(
                  toAddress = agencyAddress,
                )
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn(
                    fromAddress = null,
                  )
                }
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve address from scheduled outbound movement if not on either movement`() {
      lateinit var agencyAddress: AgencyLocationAddress
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(agencyLocationId = "BIGHHO", description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                toAddress = agencyAddress,
              ) {
                movementOut = tapMovementOut(
                  toAddress = null,
                )
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn(
                    fromAddress = null,
                  )
                }
              }
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve unscheduled temporary absence external movements with agency address`() {
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(
          agencyLocationId = "NGENHO",
          description = "Northern General Hospital",
          type = "HOSPITAL",
        ) {
          agencyAddress = address(
            type = "BUS",
            street = "Herries Road",
            postcode = "S5 7AU",
            city = SHEFFIELD,
            county = "S.YORKSHIRE",
            country = "ENG",
          )
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            unscheduledMovementOut = tapMovementOut(
              date = yesterday,
              movementReason = "C5",
              escort = "U",
              escortText = "SE",
              comment = "Tap OUT comment",
              fromPrison = "LEI",
              toAgency = "NGENHO",
              toCity = null,
              toAddress = null,
            )
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].sequence").isEqualTo(unscheduledMovementOut.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].escort").isEqualTo("U")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAgency").isEqualTo("NGENHO")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].commentText").isEqualTo("Tap OUT comment")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressId").isEqualTo(agencyAddress.addressId)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressDescription").isEqualTo("Northern General Hospital")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toFullAddress").isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressPostcode").isEqualTo("S5 7AU")
    }

    @Test
    fun `should retrieve city description if no address on unscheduled temporary absence`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            unscheduledMovementOut = tapMovementOut(
              toCity = SHEFFIELD,
              toAgency = null,
              toAddress = null,
            )
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressId").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressOwnerClass").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toFullAddress").isEqualTo("Sheffield")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressPostcode").doesNotExist()
    }

    @Test
    fun `should retrieve unscheduled temporary absences return external movements with agency address`() {
      nomisDataBuilder.build {
        agencyLocation = agencyLocation(
          agencyLocationId = "NGENHO",
          description = "Northern General Hospital",
          type = "HOSPITAL",
        ) {
          agencyAddress = address(
            type = "BUS",
            street = "Herries Road",
            postcode = "S5 7AU",
            city = SHEFFIELD,
            county = "S.YORKSHIRE",
            country = "ENG",
          )
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            unscheduledMovementOut = tapMovementOut()
            unscheduledMovementIn = tapMovementIn(
              date = today,
              movementReason = "C5",
              escort = "U",
              escortText = "SE",
              comment = "Tap IN comment",
              toPrison = "LEI",
              fromAgency = "NGENHO",
              fromCity = null,
              fromAddress = null,
            )
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence")
        .isEqualTo(unscheduledMovementIn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementDate")
        .isEqualTo("${today.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementTime").value<String> {
          assertThat(it).startsWith("${today.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].escort").isEqualTo("U")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAgency").isEqualTo("NGENHO")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].toPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].commentText").isEqualTo("Tap IN comment")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressId")
        .isEqualTo(agencyAddress.addressId)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressDescription").isEqualTo("Northern General Hospital")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromFullAddress").isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressPostcode").isEqualTo("S5 7AU")
    }

    @Test
    fun `should retrieve city description from unscheduled temporary absence return`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            unscheduledMovementOut = tapMovementOut()
            unscheduledMovementIn = tapMovementIn(
              fromCity = SHEFFIELD,
              fromAddress = null,
            )
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressId").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressOwnerClass").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromFullAddress").isEqualTo("Sheffield")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressPostcode").doesNotExist()
    }

    @Test
    fun `should retrieve all temporary absences and external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
            unscheduledMovementOut = tapMovementOut()
            unscheduledMovementIn = tapMovementIn()
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].bookingId").isEqualTo(booking.bookingId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].movementApplicationId").isEqualTo(application.tapApplicationId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventId").isEqualTo(scheduleOut.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.sequence").isEqualTo(movementOut.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduleIn.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.sequence").isEqualTo(movementIn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].sequence").isEqualTo(unscheduledMovementOut.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence").isEqualTo(unscheduledMovementIn.id.sequence)
    }

    @Test
    fun `should take return time from the application if not on the schedule`() {
      val tomorrow = LocalDateTime.now().plusDays(1).withNano(0)

      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication(returnTime = tomorrow) {
              scheduleOut = tapScheduleOut()
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by nulling the return time on the schedule
         */
        entityManager.createQuery(
          """
            update OffenderTapScheduleOut ost
            set ost.returnTime = null
            where eventId = ${scheduleOut.eventId}
          """.trimIndent(),
        ).executeUpdate()
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.returnTime").isEqualTo(tomorrow)
    }

    @Test
    fun `should handle movement address that does not exist any more`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            tapApplication(toAddress = offenderAddress) {
              tapScheduleOut(toAddress = offenderAddress) {
                tapScheduleIn()
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by deleting the address
         */
        entityManager.createQuery(
          """
            delete from Address where addressId = ${offenderAddress.addressId}
          """.trimIndent(),
        ).executeUpdate()
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toAddressId").doesNotExist()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressId").doesNotExist()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.fromAddressId").doesNotExist()
    }

    @Test
    fun `should format address`() {
      lateinit var corporateAddress: CorporateAddress
      nomisDataBuilder.build {
        corporate(corporateName = "Boots") {
          corporateAddress = address(
            premise = "Boots",
            street = "High Street",
            locality = "Sheffield",
            country = "ENG",
            postcode = "S2 2AA",
          )
        }
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                toAddress = corporateAddress,
              )
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("High Street, Sheffield, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressPostcode").isEqualTo("S2 2AA")
    }

    @Test
    fun `should format address with whitespace instead of postcode`() {
      lateinit var corporateAddress: CorporateAddress
      nomisDataBuilder.build {
        corporate(corporateName = "Boots") {
          corporateAddress = address(
            street = "High Street",
            locality = "Sheffield",
            country = "ENG",
            postcode = " ",
          )
        }
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                toAddress = corporateAddress,
              )
            }
          }
        }
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("41 High Street, Sheffield, England")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toAddressPostcode").isEqualTo("")
    }
  }

  @Nested
  @DisplayName("Migration with merged data")
  inner class GetTemporaryAbsencesAndMovementsForMergedData {
    lateinit var mergedBooking: OffenderBooking
    lateinit var mergedApplication: OffenderTapApplication
    lateinit var mergedScheduledTemporaryAbsence: OffenderTapScheduleOut
    lateinit var mergedScheduledTemporaryAbsenceReturn: OffenderTapScheduleIn
    lateinit var mergedTemporaryAbsence: OffenderTapMovementOut
    lateinit var mergedTemporaryAbsenceReturn: OffenderTapMovementIn

    lateinit var scheduledTemporaryAbsence2: OffenderTapScheduleOut
    lateinit var scheduledTemporaryAbsenceReturn2: OffenderTapScheduleIn
    lateinit var mergedScheduledTemporaryAbsence2: OffenderTapScheduleOut
    lateinit var mergedScheduledTemporaryAbsenceReturn2: OffenderTapScheduleIn
    lateinit var mergedTemporaryAbsence2: OffenderTapMovementOut
    lateinit var mergedTemporaryAbsenceReturn2: OffenderTapMovementIn

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    @BeforeEach
    fun setUp() {
      // Simulate a scenario where a prisoner is merged into another
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          // This booking was moved from the old prisoner during the merge
          mergedBooking = booking(bookingSequence = 2) {
            receive(twoDaysAgo)
            mergedApplication = tapApplication {
              mergedScheduledTemporaryAbsence = tapScheduleOut(eventDate = today) {
                mergedTemporaryAbsence = tapMovementOut()
                mergedScheduledTemporaryAbsenceReturn = tapScheduleIn(eventDate = today) {
                  mergedTemporaryAbsenceReturn = tapMovementIn()
                }
              }
              mergedScheduledTemporaryAbsence2 = tapScheduleOut(eventDate = tomorrow) {
                mergedTemporaryAbsence2 = tapMovementOut()
                mergedScheduledTemporaryAbsenceReturn2 = tapScheduleIn(eventDate = tomorrow) {
                  mergedTemporaryAbsenceReturn2 = tapMovementIn()
                }
              }
            }
            release(yesterday)
          }
          // This is the latest booking
          booking = booking(bookingSequence = 1) {
            receive(yesterday)
            // these are the only details copied from the merged booking during the merge
            application = tapApplication {
              // make the schedules out of order to prove that date handling works
              scheduledTemporaryAbsence2 = tapScheduleOut(eventDate = tomorrow) {
                scheduledTemporaryAbsenceReturn2 = tapScheduleIn(eventDate = tomorrow)
              }
              scheduleOut = tapScheduleOut(eventDate = today) {
                scheduleIn = tapScheduleIn(eventDate = today)
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data copied during the merge in the same way as NOMIS does
         * - pointing at the original booking's scheduled TAP instead of its own
         * - pointing at the new application, whereas the original scheduled return has null application
         */
        entityManager.createQuery(
          """
            update OffenderTapScheduleIn ostr
            set ostr.tapScheduleOut = (from OffenderTapScheduleOut where eventId = ${mergedScheduledTemporaryAbsence.eventId}),
            ostr.tapApplication = (from OffenderTapApplication  where tapApplicationId = ${application.tapApplicationId})
            where eventId = ${scheduleIn.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Also corrupt the same data for the 2nd scheduled absence from the application to test repeating applications
        entityManager.createQuery(
          """
            update OffenderTapScheduleIn ostr
            set ostr.tapScheduleOut = (from OffenderTapScheduleOut where eventId = ${mergedScheduledTemporaryAbsence2.eventId}),
            ostr.tapApplication = (from OffenderTapApplication  where tapApplicationId = ${application.tapApplicationId})
            where eventId = ${scheduledTemporaryAbsenceReturn2.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Also corrupt the merged movements by removing the link to the schedules in the same way the NOMIS merge process does
        entityManager.createQuery(
          """
            update OffenderTapMovementOut ota
            set ota.tapScheduleOut = null
            where id.offenderBooking.id = ${mergedBooking.bookingId}
            and id.sequence in (${mergedTemporaryAbsence.id.sequence}, ${mergedTemporaryAbsence2.id.sequence})
          """.trimIndent(),
        ).executeUpdate()
        entityManager.createQuery(
          """
            update OffenderTapMovementIn otar
            set otar.tapScheduleIn = null
            where id.offenderBooking.id = ${mergedBooking.bookingId} 
            and id.sequence in (${mergedTemporaryAbsenceReturn.id.sequence}, ${mergedTemporaryAbsenceReturn2.id.sequence})
          """.trimIndent(),
        ).executeUpdate()
      }
    }

    @Test
    fun `should retrieve the temporary absence from the merged booking's first application schedule`() {
      webTestClient.getTapsForMigration()
        .apply {
          val book = bookings.first()
          val application = book.temporaryAbsenceApplications.first()
          assertThat(book.bookingId).isEqualTo(mergedBooking.bookingId)
          assertThat(book.activeBooking).isEqualTo(false)
          assertThat(book.latestBooking).isEqualTo(false)
          assertThat(book.temporaryAbsenceApplications.size).isEqualTo(1)
          assertThat(application.movementApplicationId).isEqualTo(mergedApplication.tapApplicationId)
          with(application.absences.first()) {
            assertThat(scheduledTemporaryAbsence!!.eventId).isEqualTo(mergedScheduledTemporaryAbsence.eventId)
            assertThat(temporaryAbsence).isNull()
            assertThat(scheduledTemporaryAbsenceReturn!!.eventId).isEqualTo(mergedScheduledTemporaryAbsenceReturn.eventId)
            assertThat(temporaryAbsenceReturn).isNull()
          }
          // The actual movements were unlinked by the merge process so should appear as unscheduled
          assertThat(bookings.first().unscheduledTemporaryAbsences[0].sequence).isEqualTo(mergedTemporaryAbsence.id.sequence)
          assertThat(bookings.first().unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(mergedTemporaryAbsenceReturn.id.sequence)
        }
    }

    @Test
    fun `should retrieve the temporary absence from the merged booking's second application schedule`() {
      webTestClient.getTapsForMigration()
        .apply {
          with(bookings.first().temporaryAbsenceApplications.first().absences[1]) {
            assertThat(scheduledTemporaryAbsence!!.eventId).isEqualTo(mergedScheduledTemporaryAbsence2.eventId)
            assertThat(temporaryAbsence).isNull()
            assertThat(scheduledTemporaryAbsenceReturn!!.eventId).isEqualTo(mergedScheduledTemporaryAbsenceReturn2.eventId)
            assertThat(temporaryAbsenceReturn).isNull()
          }
          // The actual movements were unlinked by the merge process so should appear as unscheduled
          assertThat(bookings.first().unscheduledTemporaryAbsences[1].sequence).isEqualTo(mergedTemporaryAbsence2.id.sequence)
          assertThat(bookings.first().unscheduledTemporaryAbsenceReturns[1].sequence).isEqualTo(mergedTemporaryAbsenceReturn2.id.sequence)
        }
    }

    @Test
    fun `should retrieve the temporary absence from the TAP copied onto the latest booking`() {
      webTestClient.getTapsForMigration()
        .apply {
          val book = bookings[1]
          val application = book.temporaryAbsenceApplications.first()
          assertThat(book.bookingId).isEqualTo(booking.bookingId)
          assertThat(book.activeBooking).isEqualTo(true)
          assertThat(book.latestBooking).isEqualTo(true)
          assertThat(book.temporaryAbsenceApplications.size).isEqualTo(1)
          assertThat(application.movementApplicationId).isEqualTo(application.movementApplicationId)
          with(application.absences[1]) {
            assertThat(scheduledTemporaryAbsence!!.eventId).isEqualTo(scheduleOut.eventId)
            assertThat(temporaryAbsence).isNull()
            assertThat(scheduledTemporaryAbsenceReturn!!.eventId).isEqualTo(scheduleIn.eventId)
            assertThat(temporaryAbsenceReturn).isNull()
          }
        }
    }

    @Test
    fun `should retrieve the temporary absence from the second TAP copied onto the latest booking`() {
      webTestClient.getTapsForMigration()
        .apply {
          with(bookings[1].temporaryAbsenceApplications.first().absences[0]) {
            assertThat(scheduledTemporaryAbsence!!.eventId).isEqualTo(scheduledTemporaryAbsence2.eventId)
            assertThat(temporaryAbsence).isNull()
            assertThat(scheduledTemporaryAbsenceReturn!!.eventId).isEqualTo(scheduledTemporaryAbsenceReturn2.eventId)
            assertThat(temporaryAbsenceReturn).isNull()
          }
        }
    }

    @Test
    fun `reconciliation should reflect the merged movements correctly`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(applications.count).isEqualTo(2)
          assertThat(scheduledOutMovements.count).isEqualTo(4)
          assertThat(movements.count).isEqualTo(4)
          // The merged movements are treated as unscheduled because the merge process removes the link to the underlying scheduled movement
          assertThat(movements.scheduled.outCount).isEqualTo(0)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.outCount).isEqualTo(2)
          assertThat(movements.unscheduled.inCount).isEqualTo(2)
        }
    }

    private fun WebTestClient.getTapsForMigration() = get()
      .uri("/movements/${offender.nomsId}/temporary-absences")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBodyResponse<OffenderTemporaryAbsencesResponse>()
  }

  @Nested
  @DisplayName("Migration for deleted schedule with movements")
  inner class GetTemporaryAbsencesAndMovementsForDeletedScheduleWthMovement {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            receive(yesterday)
            // TAP for which we will remove the schedule OUT from the DB
            tapApplication {
              scheduleOut = tapScheduleOut(eventDate = today.toLocalDate()) {
                tapMovementOut()
                orphanedScheduleReturn = tapScheduleIn(eventDate = today.toLocalDate()) {
                  tapMovementIn()
                }
              }
            }
            // Another TAP that should be included in the migration
            tapApplication {
              tapScheduleOut(eventDate = yesterday.toLocalDate()) {
                tapMovementOut()
                tapScheduleIn(eventDate = yesterday.toLocalDate()) {
                  tapMovementIn()
                }
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Emulate the scenario where a schedule OUT is deleted after movements are created
         */
        entityManager.createNativeQuery(
          """
            delete from OFFENDER_IND_SCHEDULES
            where EVENT_ID = ${scheduleOut.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Reload the scheduled return to reflect the update
        orphanedScheduleReturn = scheduleInRepository.findByIdOrNull(orphanedScheduleReturn.eventId) ?: throw IllegalStateException("Failed to reload scheduled return movement - this should not happen")
      }
    }

    @Test
    fun `should include the TAP with deleted schedule OUT as unscheduled`() {
      webTestClient.getTapsForMigration()
        .apply {
          val book = bookings.first()
          assertThat(book.temporaryAbsenceApplications.size).isEqualTo(2)
          // The TAP with the deleted scheduled absence is not included in the migration as a scheduled movement
          assertThat(book.temporaryAbsenceApplications[0].absences).isEmpty()
          // The control TAP is migrated
          with(book.temporaryAbsenceApplications[1].absences.first()) {
            assertThat(scheduledTemporaryAbsence).isNotNull()
            assertThat(temporaryAbsence).isNotNull()
            assertThat(scheduledTemporaryAbsenceReturn).isNotNull()
            assertThat(temporaryAbsenceReturn).isNotNull()
          }
          // The TAP with deleted scheduled absence has both movements included as unscheduled
          assertThat(book.unscheduledTemporaryAbsences.size).isEqualTo(1)
          assertThat(book.unscheduledTemporaryAbsenceReturns.size).isEqualTo(1)
        }
    }

    @Test
    fun `reconciliation should include the TAP with deleted scheduled OUT as unscheduled`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(applications.count).isEqualTo(2)
          assertThat(scheduledOutMovements.count).isEqualTo(1)
          assertThat(movements.count).isEqualTo(4)
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(1)
          assertThat(movements.unscheduled.outCount).isEqualTo(1)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    private fun WebTestClient.getTapsForMigration() = get()
      .uri("/movements/${offender.nomsId}/temporary-absences")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBodyResponse<OffenderTemporaryAbsencesResponse>()
  }

  @Nested
  @DisplayName("Migration for multiple schedule IN movements")
  inner class GetTemporaryAbsencesAndMovementsForMultipleScheduledInMovements {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            receive(yesterday)
            tapApplication {
              scheduleOut = tapScheduleOut(eventDate = today.toLocalDate()) {
                tapMovementOut()
                // We used to pick up this scheduled IN record - we should pick up the next one with an external movement
                tapScheduleIn()
                scheduleIn = tapScheduleIn(eventDate = today.toLocalDate()) {
                  tapMovementIn()
                }
              }
            }
          }
        }
      }
    }

    @Test
    fun `should return the scheduled IN movement with an actual movement attached`() {
      webTestClient.getTapsForMigration()
        .apply {
          val book = bookings.first()
          assertThat(book.temporaryAbsenceApplications.size).isEqualTo(1)
          // The correct scheduled IN movement is chosen
          with(bookings[0].temporaryAbsenceApplications[0].absences[0]) {
            assertThat(scheduledTemporaryAbsenceReturn!!.eventId).isEqualTo(scheduleIn.eventId)
            assertThat(temporaryAbsenceReturn).isNotNull
          }
        }
    }

    @Test
    fun `reconciliation should include only one of the scheduled IN movements`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(scheduledOutMovements.count).isEqualTo(1)
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(1)
          assertThat(movements.unscheduled.outCount).isEqualTo(0)
          assertThat(movements.unscheduled.inCount).isEqualTo(0)
        }
    }

    private fun WebTestClient.getTapsForMigration() = get()
      .uri("/movements/${offender.nomsId}/temporary-absences")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBodyResponse<OffenderTemporaryAbsencesResponse>()
  }

  @Nested
  @DisplayName("Reconciliation IDs")
  inner class GetTemporaryAbsencesAndMovementIds {
    private lateinit var booking2: OffenderBooking
    private lateinit var application2: OffenderTapApplication
    private lateinit var scheduledTempAbsence2: OffenderTapScheduleOut
    private lateinit var scheduledTempAbsenceReturn2: OffenderTapScheduleIn
    private lateinit var tempAbsence2: OffenderTapMovementOut
    private lateinit var tempAbsenceReturn2: OffenderTapMovementIn
    private lateinit var unscheduledTemporaryAbsence2: OffenderTapMovementOut
    private lateinit var unscheduledTemporaryAbsenceReturn2: OffenderTapMovementIn

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
            unscheduledMovementOut = tapMovementOut()
            unscheduledMovementIn = tapMovementIn()
          }
          booking2 = booking {
            application2 = tapApplication {
              scheduledTempAbsence2 = tapScheduleOut {
                tempAbsence2 = tapMovementOut()
                scheduledTempAbsenceReturn2 = tapScheduleIn {
                  tempAbsenceReturn2 = tapMovementIn()
                }
              }
            }
            unscheduledTemporaryAbsence2 = tapMovementOut()
            unscheduledTemporaryAbsenceReturn2 = tapMovementIn()
          }
        }
      }
    }

    @Test
    fun `should return all IDs`() {
      webTestClient.getTapIds()
        .apply {
          assertThat(applicationIds).containsExactlyInAnyOrder(
            application.tapApplicationId,
            application2.tapApplicationId,
          )
          assertThat(scheduleOutIds).containsExactlyInAnyOrder(
            scheduleOut.eventId,
            scheduledTempAbsence2.eventId,
          )
          assertThat(scheduleInIds).containsExactlyInAnyOrder(
            scheduleIn.eventId,
            scheduledTempAbsenceReturn2.eventId,
          )
          assertThat(scheduledMovementOutIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(movementOut.id.offenderBooking.bookingId, movementOut.id.sequence),
            OffenderTemporaryAbsenceId(tempAbsence2.id.offenderBooking.bookingId, tempAbsence2.id.sequence),
          )
          assertThat(scheduledMovementInIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(movementIn.id.offenderBooking.bookingId, movementIn.id.sequence),
            OffenderTemporaryAbsenceId(tempAbsenceReturn2.id.offenderBooking.bookingId, tempAbsenceReturn2.id.sequence),
          )
          assertThat(unscheduledMovementOutIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(unscheduledMovementOut.id.offenderBooking.bookingId, unscheduledMovementOut.id.sequence),
            OffenderTemporaryAbsenceId(unscheduledTemporaryAbsence2.id.offenderBooking.bookingId, unscheduledTemporaryAbsence2.id.sequence),
          )
          assertThat(unscheduledMovementInIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(unscheduledMovementIn.id.offenderBooking.bookingId, unscheduledMovementIn.id.sequence),
            OffenderTemporaryAbsenceId(unscheduledTemporaryAbsenceReturn2.id.offenderBooking.bookingId, unscheduledTemporaryAbsenceReturn2.id.sequence),
          )
        }
    }

    @Test
    fun `should return correct summary counts`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(applications.count).isEqualTo(2)
          assertThat(scheduledOutMovements.count).isEqualTo(2)
          assertThat(movements.scheduled.outCount).isEqualTo(2)
          assertThat(movements.scheduled.inCount).isEqualTo(2)
          assertThat(movements.unscheduled.outCount).isEqualTo(2)
          assertThat(movements.unscheduled.inCount).isEqualTo(2)
        }
    }

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences/ids")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences/ids")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences/ids")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    private fun WebTestClient.getTapIds() = get()
      .uri("/movements/${offender.nomsId}/temporary-absences/ids")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBodyResponse<OffenderTemporaryAbsenceIdsResponse>()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/application/{applicationId}")
  inner class GetTemporaryAbsencesApplication {
    private lateinit var inactiveBooking: OffenderBooking
    private lateinit var inactiveBookingApplication: OffenderTapApplication

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking(bookingSequence = 1) {
            application = tapApplication(
              eventSubType = "C5",
              applicationDate = twoDaysAgo,
              applicationTime = twoDaysAgo,
              fromDate = twoDaysAgo.toLocalDate(),
              releaseTime = twoDaysAgo,
              toDate = yesterday.toLocalDate(),
              returnTime = yesterday,
              applicationType = "SINGLE",
              applicationStatus = "APP-SCH",
              escort = "L",
              transportType = "VAN",
              comment = "Some comment application",
              prison = "LEI",
              toAgency = "HAZLWD",
              toAddress = offenderAddress,
              contactPersonName = "Derek",
              tapType = "RR",
              tapSubType = "RDR",
            ) {
              tapScheduleOut {
                tapMovementOut()
                tapScheduleIn {
                  tapMovementIn()
                }
              }
            }
            tapMovementOut()
            tapMovementIn()
          }
          inactiveBooking = booking(bookingSequence = 2) {
            receive(twoDaysAgo)
            inactiveBookingApplication = tapApplication()
            release(yesterday)
          }
        }
      }
    }

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/application/1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/application/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/application/1")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if offender not found`() {
      webTestClient.get()
        .uri("/movements/UNKNOWN/temporary-absences/application/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("UNKNOWN").contains("not found")
        }
    }

    @Test
    fun `should return not found if application not found`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/application/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("9999").contains("not found")
        }
    }

    @Test
    fun `should retrieve application`() {
      webTestClient.get()
        .uri(
          "/movements/${offender.nomsId}/temporary-absences/application/{applicationId}",
          application.tapApplicationId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse<TemporaryAbsenceApplicationResponse>()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(activeBooking).isEqualTo(true)
          assertThat(latestBooking).isEqualTo(true)
          assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(eventSubType).isEqualTo("C5")
          assertThat(applicationDate).isEqualTo(twoDaysAgo.toLocalDate())
          assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
          assertThat(releaseTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
          assertThat(toDate).isEqualTo(yesterday.toLocalDate())
          assertThat(returnTime).isCloseTo(yesterday, within(1, ChronoUnit.MINUTES))
          assertThat(applicationType).isEqualTo("SINGLE")
          assertThat(applicationStatus).isEqualTo("APP-SCH")
          assertThat(escortCode).isEqualTo("L")
          assertThat(transportType).isEqualTo("VAN")
          assertThat(comment).isEqualTo("Some comment application")
          assertThat(prisonId).isEqualTo("LEI")
          assertThat(toAgencyId).isEqualTo("HAZLWD")
          assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
          assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
          assertThat(toAddressDescription).isNull()
          assertThat(toFullAddress).isEqualTo("41 High Street, Sheffield")
          assertThat(toAddressPostcode).isEqualTo("S1 1AA")
          assertThat(contactPersonName).isEqualTo("Derek")
          assertThat(temporaryAbsenceType).isEqualTo("RR")
          assertThat(temporaryAbsenceSubType).isEqualTo("RDR")
        }
    }

    @Test
    fun `should retrieve application on inactive booking`() {
      webTestClient.get()
        .uri(
          "/movements/${offender.nomsId}/temporary-absences/application/{applicationId}",
          inactiveBookingApplication.tapApplicationId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse<TemporaryAbsenceApplicationResponse>()
        .apply {
          assertThat(bookingId).isEqualTo(inactiveBooking.bookingId)
          assertThat(activeBooking).isEqualTo(false)
          assertThat(latestBooking).isEqualTo(false)
          assertThat(movementApplicationId).isEqualTo(inactiveBookingApplication.tapApplicationId)
        }
    }

    @Test
    fun `should retrieve latest booking even if inactive`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = "A9843AA") {
          booking = booking(bookingSequence = 1, active = false) {
            application = tapApplication()
          }
        }
      }

      webTestClient.get()
        .uri(
          "/movements/A9843AA/temporary-absences/application/{applicationId}",
          application.tapApplicationId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse<TemporaryAbsenceApplicationResponse>()
        .apply {
          assertThat(activeBooking).isEqualTo(false)
          assertThat(latestBooking).isEqualTo(true)
        }
    }
  }

  @Nested
  @DisplayName("PUT /movements/{offenderNo}/temporary-absences/application")
  inner class UpsertTemporaryAbsenceApplication {

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class Create {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking()
          }
        }
      }

      @Test
      fun `should create application`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(toAddresses = listOf(UpsertTemporaryAbsenceAddress(addressText = "some street", postalCode = "S1 9ZZ"))),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(applicationDate.toLocalDate()).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(releaseTime).isEqualTo(twoDaysAgo)
                assertThat(toDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isEqualTo(yesterday)
                assertThat(applicationType.code).isEqualTo("SINGLE")
                assertThat(applicationStatus.code).isEqualTo("APP-SCH")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(comment).isEqualTo("Some comment application")
                assertThat(prison.id).isEqualTo("LEI")
                assertThat(toAgency).isNull()
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("some street")
                assertThat(toAddress?.postalCode).isEqualTo("S1 9ZZ")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
                assertThat(contactPersonName).isEqualTo("Derek")
                assertThat(tapType?.code).isEqualTo("RR")
                assertThat(tapSubType?.code).isEqualTo("RDR")
              }
            }
          }
      }

      @Test
      fun `should create all addresses requested`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(addressText = "some street"),
              UpsertTemporaryAbsenceAddress(name = "Kwikfit", addressText = "another street", postalCode = "S1 9ZZ"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // the first address is written to the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("some street")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
              // the first address was created
              with(offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)) {
                val address = find { it.premise == "some street" }!!
                assertThat(address.street).isNull()
                assertThat(address.postalCode).isNull()
              }
              // the second address was created
              with(corporateRepository.findAllByCorporateName("Kwikfit").first()) {
                assertThat(addresses.first().premise).isEqualTo("another street")
                assertThat(addresses.first().postalCode).isEqualTo("S1 9ZZ")
                assertThat(addresses.first().street).isNull()
              }
            }
          }
      }

      @Test
      fun `should create addresses with long corporate name`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(name = "Hm Prison Service The Chief Estates Surveyor", addressText = "another street", postalCode = "S1 9ZZ"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // the first address is written to the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("another street")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              // the address and corporation were created
              with(corporateRepository.findAllByCorporateName("Hm Prison Service The Chief Estates Surv").first()) {
                assertThat(addresses.first().premise).isEqualTo("another street")
                assertThat(addresses.first().postalCode).isEqualTo("S1 9ZZ")
                assertThat(addresses.first().street).isNull()
              }
            }
          }
      }

      @Test
      fun `should not create corporate or address if already exists`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = "A9876CA") {
            booking = booking()
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // the address is used on the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("Scotland Street, Sheffield")
                assertThat(toAddress?.postalCode).isEqualTo("S1 3GG")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
              // the corporate was not duplicated
              with(corporateRepository.findAllByCorporateName("Boots")) {
                assertThat(size).isEqualTo(1)
                // The corporate address was not duplicated
                assertThat(first().addresses.size).isEqualTo(1)
              }
            }
          }
      }

      @Test
      fun `should not create corporate or address where there is already a duplicate corporate`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Different address", street = null, locality = null, postcode = "S4 4SS")
          }
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = "A9876CB") {
            booking = booking()
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // the address is used on the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("Scotland Street, Sheffield")
                assertThat(toAddress?.postalCode).isEqualTo("S1 3GG")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              // the corporate was not duplicated
              with(corporateRepository.findAllByCorporateName("Boots")) {
                assertThat(size).isEqualTo(2)
                // The corporate address was not duplicated
                assertThat(this[0].addresses.size).isEqualTo(1)
                assertThat(this[1].addresses.size).isEqualTo(1)
              }
            }
          }
      }

      @Test
      fun `should handle multiple new addresses on a corporate`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Some address", street = null, locality = null, postcode = "S1 1XX")
          }
          offender = offender(nomsId = "A9876CB") {
            booking = booking()
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(name = "Boots", addressText = "New address 1, Sheffield", postalCode = "S2 2XX"),
              UpsertTemporaryAbsenceAddress(name = "Boots", addressText = "New address 2, Sheffield", postalCode = "S3 3XX"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // one of the new addresses is used on the application
                assertThat(toAddress?.postalCode).isIn("S2 2XX", "S3 3XX")
              }
              // the new addresses have been saved on the corporates
              with(corporateRepository.findAllByCorporateName("Boots")) {
                assertThat(flatMap { it.addresses.map { it.postalCode } })
                  .containsExactlyInAnyOrder("S1 1XX", "S2 2XX", "S3 3XX")
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        // comment is 300 long
        webTestClient.upsertApplicationOk(anUpsertApplicationRequest().copy(comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
              }
            }
          }
      }

      @Test
      fun `should allow create of a pending application without an address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            applicationStatus = "PEN",
            toAddresses = listOf(UpsertTemporaryAbsenceAddress()),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(toAddress).isNull()
                assertThat(toAddressOwnerClass).isNull()
              }
            }
          }
      }

      @Test
      fun `should create a new offender address where corporate name same as address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTemporaryAbsenceAddress(name = "Boston", addressText = "Boston")),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(toAddress?.premise).isEqualTo("Boston")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }

      @Test
      fun `should handle trailing blanks in corporate address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTemporaryAbsenceAddress(name = "HSL ", addressText = "Bowness , Cumbria ", postalCode = "LA23 3AS ")),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(toAddress?.premise).isEqualTo("Bowness , Cumbria")
                assertThat(toAddress?.postalCode).isEqualTo("LA23 3AS")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              assertThat(corporateRepository.findAllByCorporateName("HSL").size).isEqualTo(1)
            }
          }
      }

      @Test
      fun `should allow corporate name in address text`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTemporaryAbsenceAddress(name = "HSL", addressText = "HSL, Bowness, Cumbria", postalCode = "LA23 3AS")),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(toAddress?.premise).isEqualTo("HSL, Bowness, Cumbria")
                assertThat(toAddress?.postalCode).isEqualTo("LA23 3AS")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
              assertThat(corporateRepository.findAllByCorporateName("HSL").size).isEqualTo(1)
            }
          }
      }

      @Nested
      inner class Validation {
        @Test
        fun `should return not found if offender unknown`() {
          webTestClient.upsertApplication(offenderNo = "UNKNOWN")
            .isNotFound
            .expectBody().jsonPath("userMessage").value<String> {
              assertThat(it).contains("UNKNOWN").contains("not found")
            }
        }

        @Test
        fun `should return not found if offender has no bookings`() {
          nomisDataBuilder.build {
            offender = offender(nomsId = "C1234DE") {
              offenderAddress = address()
            }
          }

          webTestClient.upsertApplication()
            .isNotFound
            .expectBody().jsonPath("userMessage").value<String> {
              assertThat(it).contains("C1234DE").contains("not found")
            }
        }

        @Test
        fun `should return bad request for invalid event sub type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(eventSubType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid application type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(applicationType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid application status`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(applicationStatus = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid escort code`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(escortCode = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid transport type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(transportType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid prison ID`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(prisonId = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid temporary absence type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(temporaryAbsenceType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid temporary absence sub type`() {
          webTestClient.upsertApplicationBadRequestUnknown(anUpsertApplicationRequest().copy(temporaryAbsenceSubType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for an approved application without an address`() {
          webTestClient.upsertApplicationBadRequest(
            request = anUpsertApplicationRequest().copy(
              applicationStatus = "APP-UNSCH",
              toAddresses = listOf(),
            ),
          )
        }

        @Test
        fun `should return bad request for an approved application with a null address`() {
          webTestClient.upsertApplicationBadRequest(
            request = anUpsertApplicationRequest().copy(
              applicationStatus = "APP-UNSCH",
              toAddresses = listOf(UpsertTemporaryAbsenceAddress()),
            ),
          )
        }
      }

      @Nested
      inner class Security {
        @Test
        fun `should return unauthorised for missing token`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/temporary-absences/application")
            .bodyValue(anUpsertApplicationRequest())
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `should return forbidden for missing role`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/temporary-absences/application")
            .headers(setAuthorisation(roles = listOf()))
            .bodyValue(anUpsertApplicationRequest())
        }

        @Test
        fun `should return forbidden for wrong role`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/temporary-absences/application")
            .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        }
      }
    }

    @Nested
    inner class Update {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                eventSubType = "C4",
                applicationDate = twoDaysAgo,
                applicationTime = twoDaysAgo,
                fromDate = yesterday.toLocalDate(),
                releaseTime = yesterday,
                toDate = today.toLocalDate(),
                returnTime = today,
                applicationType = "REPEATING",
                applicationStatus = "PEN",
                escort = "U",
                transportType = "TAX",
                comment = "Old comment application",
                prison = "LEI",
                toAgency = null,
                toAddress = offenderAddress,
                contactPersonName = "Adam",
                tapType = "PP",
                tapSubType = "ROR",
              ) {
                tapScheduleOut(
                  toAddress = offenderAddress,
                  toAgency = "HAZLWD",
                ) {
                  tapMovementOut()
                  tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
              tapMovementOut()
              tapMovementIn()
            }
          }
        }
      }

      @Test
      fun `should update application`() {
        webTestClient.upsertApplicationOk(request = anUpsertApplicationRequest(id = application.tapApplicationId, toAddresses = listOf(UpsertTemporaryAbsenceAddress(id = offenderAddress.addressId))))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(applicationDate.toLocalDate()).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(releaseTime).isEqualTo(twoDaysAgo)
                assertThat(toDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isEqualTo(yesterday)
                assertThat(applicationType.code).isEqualTo("SINGLE")
                assertThat(applicationStatus.code).isEqualTo("APP-SCH")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(comment).isEqualTo("Some comment application")
                assertThat(prison.id).isEqualTo("LEI")
                assertThat(toAgency?.id).isNull()
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
                assertThat(contactPersonName).isEqualTo("Derek")
                assertThat(tapType?.code).isEqualTo("RR")
                assertThat(tapSubType?.code).isEqualTo("RDR")
              }
            }
          }
      }

      @Test
      fun `should return not found if unknown application id sent`() {
        webTestClient.upsertApplication(request = anUpsertApplicationRequest(id = 9999))
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }

      @Test
      fun `should return 423 if application is locked for update`() {
        repository.runInTransaction {
          applicationRepository.findByIdOrNullForUpdate(application.tapApplicationId)

          webTestClient.upsertApplication(request = anUpsertApplicationRequest(id = application.tapApplicationId))
            .isEqualTo(423)
            .expectBody<ErrorResponse>().returnResult().responseBody!!
            .apply {
              assertThat(this.status).isEqualTo(423)
            }
        }
      }
    }

    @Nested
    inner class UpdateAddress {
      private lateinit var scheduleAddress: OffenderAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            scheduleAddress = address()
            booking = booking {
              application = tapApplication(
                toAddress = offenderAddress,
              ) {
                tapScheduleOut(
                  toAddress = scheduleAddress,
                  toAgency = "HAZLWD",
                ) {
                  tapMovementOut()
                  tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
              tapMovementOut()
              tapMovementIn()
            }
          }
        }
      }

      @Test
      fun `should update address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = application.tapApplicationId,
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(id = offenderAddress.addressId),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }

      @Test
      fun `should update address and create new addresses`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = application.tapApplicationId,
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(addressText = "some street", postalCode = "S1 9ZZ"),
              UpsertTemporaryAbsenceAddress(name = "Kwikfit", addressText = "another street", postalCode = "S2 8YY"),
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // The first new address is written to the application
                assertThat(toAddress?.addressId).isNotNull
                assertThat(toAddress?.premise).isEqualTo("some street")
                assertThat(toAddress?.postalCode).isEqualTo("S1 9ZZ")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
              // the first address was created
              with(offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)) {
                val address = find { it.premise == "some street" }!!
                assertThat(address.street).isNull()
                assertThat(address.postalCode).isEqualTo("S1 9ZZ")
              }
              // the second address was created
              with(corporateRepository.findAllByCorporateName("Kwikfit").first()) {
                assertThat(addresses.first().premise).isEqualTo("another street")
                assertThat(addresses.first().postalCode).isEqualTo("S2 8YY")
                assertThat(addresses.first().street).isNull()
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateButNotAgencyAddressAndNoSchedules {

      private lateinit var agencyAddress: AgencyLocationAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation = agencyLocation(
            agencyLocationId = "NGENHO",
            description = "Northern General Hospital",
            type = "HOSPITAL",
          ) {
            agencyAddress = address(
              type = "BUS",
              street = "Herries Road",
              postcode = "S5 7AU",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                toAgency = "NGENHO",
                toAddress = agencyAddress,
              )
            }
          }
        }
      }

      @Test
      fun `should not update address`() {
        webTestClient.upsertApplicationOk(request = anUpsertApplicationRequest(id = application.tapApplicationId, toAddresses = listOf(UpsertTemporaryAbsenceAddress(id = agencyAddress.addressId))))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("AGY")
                assertThat(toAgency?.id).isEqualTo("NGENHO")
              }
            }
          }
      }
    }

    @Nested
    inner class UpdatePendingApplicationNoAddress {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                applicationStatus = "APP-UNSCH",
                toAddress = offenderAddress,
              )
            }
          }
        }
      }

      @Test
      fun `should allow update and remove address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationStatus = "PEN",
            toAddresses = listOf(UpsertTemporaryAbsenceAddress()),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(toAddress).isNull()
                assertThat(toAddressOwnerClass).isNull()
              }
            }
          }
      }
    }

    @Nested
    inner class IgnoreSimilarNomisCorporateAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(
            corporateName = "Swansea (Town Visit)",
          ) {
            corporateAddress = address(
              type = "BUS",
              flat = null,
              premise = "Swansea",
              street = null,
              locality = null,
              postcode = null,
              city = SWANSEA,
              county = null,
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should create application with new address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(name = "Swansea (Town Visit)", addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // We didn't pick up the existing similar NOMIS address
                assertThat(toAddress?.addressId).isNotEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.premise).isEqualTo("Swansea")
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }

      @Test
      fun `should create schedule with same address as application`() {
        var applicationAddressId: Long = 0
        var applicationId: Long = 0

        // Create the application
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(name = "Swansea (Town Visit)", addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // save the application address ID
                applicationAddressId = toAddress!!.addressId
                applicationId = tapApplicationId
              }
            }
          }

        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            eventId = null,
            movementApplicationId = applicationId,
            toAddress = UpsertTemporaryAbsenceAddress(name = "Swansea (Town Visit)", addressText = "Swansea"),
          ),
        ).apply {
          repository.runInTransaction {
            with(scheduleOutRepository.findByIdOrNull(eventId)!!) {
              // check the schedule address is the same as the application address
              assertThat(toAddress?.addressId).isEqualTo(applicationAddressId)
              assertThat(toAddress?.premise).isEqualTo("Swansea")
              assertThat(toAddressOwnerClass).isEqualTo("CORP")
            }
          }
        }
      }
    }

    @Nested
    inner class IgnoreSimilarNomisOffenderAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              flat = null,
              premise = "Swansea",
              street = null,
              locality = null,
              postcode = null,
              city = SWANSEA,
              county = null,
              country = "ENG",
            )
            booking = booking()
          }
        }
      }

      @Test
      fun `should create application with new address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // We didn't pick up the existing similar NOMIS address
                assertThat(toAddress?.addressId).isNotEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.premise).isEqualTo("Swansea")
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }

      @Test
      fun `should create schedule with same address as application`() {
        var applicationAddressId: Long = 0
        var applicationId: Long = 0

        // Create the application
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(
            id = null,
            toAddresses = listOf(
              UpsertTemporaryAbsenceAddress(addressText = "Swansea"),
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                // save the application address ID
                applicationAddressId = toAddress!!.addressId
                applicationId = tapApplicationId
              }
            }
          }

        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            eventId = null,
            movementApplicationId = applicationId,
            toAddress = UpsertTemporaryAbsenceAddress(addressText = "Swansea"),
          ),
        ).apply {
          repository.runInTransaction {
            with(scheduleOutRepository.findByIdOrNull(eventId)!!) {
              // check the schedule address is the same as the application address
              assertThat(toAddress?.addressId).isEqualTo(applicationAddressId)
              assertThat(toAddress?.premise).isEqualTo("Swansea")
              assertThat(toAddressOwnerClass).isEqualTo("OFF")
            }
          }
        }
      }
    }

    @Nested
    inner class UpdateToNotApproved {
      @Test
      fun `should remove schedule for single`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication(
                applicationType = "SINGLE",
                applicationStatus = "APP-SCH",
              ) {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationStatus = "PEN",
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(tapScheduleOuts).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should not remove schedule for repeating`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication(
                applicationType = "REPEATING",
                applicationStatus = "APP-SCH",
              ) {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationType = "REPEATING",
            applicationStatus = "PEN",
          ),
        )
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(applicationStatus.code).isEqualTo("PEN")
                assertThat(tapScheduleOuts.first()).isNotNull()
              }
            }
          }
      }

      @Test
      fun `should return bad request if schedule has a movement`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication(
                applicationType = "SINGLE",
                applicationStatus = "APP-SCH",
              ) {
                scheduleOut = tapScheduleOut {
                  tapMovementOut()
                  tapScheduleIn()
                }
              }
            }
          }
        }

        webTestClient.upsertApplicationBadRequest(
          request = anUpsertApplicationRequest(id = application.tapApplicationId).copy(
            applicationStatus = "PEN",
          ),
        )
      }
    }
  }

  @Nested
  @DisplayName("DELETE /movements/{offenderNo}/temporary-absences/application/{applicationId}")
  inner class DeleteApplication {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication()
          }
        }
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `should delete application`() {
        webTestClient.deleteApplication()
          .expectStatus().isNoContent

        repository.runInTransaction {
          assertThat(applicationRepository.findByIdOrNull(application.tapApplicationId)).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return 204 if unknown application id sent`() {
        webTestClient.deleteApplication(applicationId = 9999)
          .expectStatus().isNoContent
      }

      @Test
      fun `should return conflict for unknown offender`() {
        webTestClient.deleteApplication(offenderNo = "UNKNOWN")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 for wrong offender`() {
        nomisDataBuilder.build {
          offender(nomsId = "A7897WW")
        }

        webTestClient.deleteApplication(offenderNo = "A7897WW")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if application has scheduled movements`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                tapScheduleOut()
              }
            }
          }
        }

        webTestClient.deleteApplication()
          .expectStatus().isEqualTo(409)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/temporary-absences/application/${application.tapApplicationId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/temporary-absences/application/${application.tapApplicationId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/temporary-absences/application/${application.tapApplicationId}")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteApplication(offenderNo: String = offender.nomsId, applicationId: Long = application.tapApplicationId): WebTestClient.ResponseSpec = delete()
      .uri("/movements/$offenderNo/temporary-absences/application/$applicationId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence/{eventId}")
  inner class GetScheduledTemporaryAbsence {

    @Nested
    @DisplayName("With offender address")
    inner class WithOffenderAddress {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(
                tapType = "RR",
                tapSubType = "RDR",
              ) {
                scheduleOut = tapScheduleOut(
                  eventDate = twoDaysAgo.toLocalDate(),
                  startTime = twoDaysAgo,
                  eventSubType = "C5",
                  eventStatus = "COMP",
                  comment = "Scheduled temporary absence",
                  escort = "L",
                  fromPrison = "LEI",
                  toAgency = "HAZLWD",
                  transportType = "VAN",
                  returnDate = yesterday.toLocalDate(),
                  returnTime = yesterday,
                  toAddress = offenderAddress,
                  contactPersonName = "Jeff",
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn(
                    eventStatus = "SCH",
                  ) {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve scheduled temporary absence`() {
        webTestClient.get()
          .uri(
            "/movements/${offender.nomsId}/temporary-absences/scheduled-temporary-absence/{eventId}",
            scheduleOut.eventId,
          )
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<ScheduledTemporaryAbsenceResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
            assertThat(eventId).isEqualTo(scheduleOut.eventId)
            assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
            assertThat(startTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
            assertThat(eventSubType).isEqualTo("C5")
            assertThat(eventStatus).isEqualTo("COMP")
            assertThat(inboundEventStatus).isEqualTo("SCH")
            assertThat(comment).isEqualTo("Scheduled temporary absence")
            assertThat(escort).isEqualTo("L")
            assertThat(fromPrison).isEqualTo("LEI")
            assertThat(toAgency).isEqualTo("HAZLWD")
            assertThat(transportType).isEqualTo("VAN")
            assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
            assertThat(returnTime).isCloseTo(yesterday, within(1, ChronoUnit.MINUTES))
            assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
            assertThat(contactPersonName).isEqualTo("Jeff")
            assertThat(temporaryAbsenceType).isEqualTo("RR")
            assertThat(temporaryAbsenceSubType).isEqualTo("RDR")
          }
      }
    }

    @Nested
    @DisplayName("With corporate address")
    inner class WithCorporateAddress {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(
            corporateName = "Boots",
          ) {
            corporateAddress = address(
              type = "BUS",
              flat = null,
              premise = "Boots",
              street = "Scotland Street",
              locality = "Hunters Bar",
              postcode = "S1 3GG",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  toAddress = corporateAddress,
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve corporate address`() {
        webTestClient.get()
          .uri(
            "/movements/${offender.nomsId}/temporary-absences/scheduled-temporary-absence/{eventId}",
            scheduleOut.eventId,
          )
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<ScheduledTemporaryAbsenceResponse>()
          .apply {
            assertThat(toAddressId).isEqualTo(corporateAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo("CORP")
            assertThat(toAddressDescription).isEqualTo("Boots")
            assertThat(toFullAddress).isEqualTo("Scotland Street, Hunters Bar, Sheffield, South Yorkshire, England")
            assertThat(toAddressPostcode).isEqualTo("S1 3GG")
          }
      }
    }

    @Nested
    @DisplayName("With agency address")
    inner class WithAgencyAddress {
      private lateinit var agencyAddress: AgencyLocationAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation = agencyLocation(
            agencyLocationId = "NGENHO",
            description = "Northern General Hospital",
            type = "HOSPITAL",
          ) {
            agencyAddress = address(
              type = "BUS",
              street = "Herries Road",
              postcode = "S5 7AU",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  toAddress = agencyAddress,
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve agency address`() {
        webTestClient.get()
          .uri(
            "/movements/${offender.nomsId}/temporary-absences/scheduled-temporary-absence/{eventId}",
            scheduleOut.eventId,
          )
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<ScheduledTemporaryAbsenceResponse>()
          .apply {
            assertThat(toAddressId).isEqualTo(agencyAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo("AGY")
            assertThat(toAddressDescription).isEqualTo("Northern General Hospital")
            assertThat(toFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
            assertThat(toAddressPostcode).isEqualTo("S5 7AU")
          }
      }
    }

    @Nested
    @DisplayName("With updated address")
    inner class WithUpdatedAddress {
      private val addressModifyDateTime = LocalDateTime.now().plusHours(1)
      private val addressModifyUser = "ADDRESS_MODIFY_USER"

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              whenModified = addressModifyDateTime,
              whoModified = addressModifyUser,
            )
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  toAddress = offenderAddress,
                ) {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should retrieve audit details from updated address`() {
        webTestClient.get()
          .uri(
            "/movements/${offender.nomsId}/temporary-absences/scheduled-temporary-absence/{eventId}",
            scheduleOut.eventId,
          )
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<ScheduledTemporaryAbsenceResponse>()
          .apply {
            assertThat(audit.modifyDatetime).isEqualTo(addressModifyDateTime)
            assertThat(audit.modifyUserId).isEqualTo(addressModifyUser)
          }
      }
    }

    @Nested
    inner class Validation {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/temporary-absences/scheduled-temporary-absence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if scheduled temporary absence not found`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/9999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }
    }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence-return/{eventId}")
  inner class GetScheduledTemporaryAbsenceReturn {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn(
                  eventDate = yesterday.toLocalDate(),
                  startTime = yesterday,
                  eventSubType = "C5",
                  eventStatus = "SCH",
                  comment = "Scheduled temporary absence return",
                  escort = "L",
                  fromAgency = "HAZLWD",
                  toPrison = "LEI",
                ) {
                  movementIn = tapMovementIn()
                }
              }
            }
          }
        }
      }
    }

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return/1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return/1")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if offender not found`() {
      webTestClient.get()
        .uri("/movements/UNKNOWN/temporary-absences/scheduled-temporary-absence-return/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("UNKNOWN").contains("not found")
        }
    }

    @Test
    fun `should return not found if scheduled temporary absence return not found`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("9999").contains("not found")
        }
    }

    @Test
    fun `should retrieve scheduled temporary absence return`() {
      webTestClient.get()
        .uri(
          "/movements/${offender.nomsId}/temporary-absences/scheduled-temporary-absence-return/{eventId}",
          scheduleIn.eventId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse<ScheduledTemporaryAbsenceReturnResponse>()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(eventId).isEqualTo(scheduleIn.eventId)
          assertThat(parentEventId).isEqualTo(scheduleOut.eventId)
          assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
          assertThat(startTime).isCloseTo(yesterday, within(1, ChronoUnit.MINUTES))
          assertThat(eventSubType).isEqualTo("C5")
          assertThat(eventStatus).isEqualTo("SCH")
          assertThat(comment).isEqualTo("Scheduled temporary absence return")
          assertThat(escort).isEqualTo("L")
          assertThat(fromAgency).isEqualTo("HAZLWD")
          assertThat(toPrison).isEqualTo("LEI")
        }
    }
  }

  @Nested
  @DisplayName("PUT /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence")
  inner class UpsertScheduledTemporaryAbsence {

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class CreateScheduleWithOffenderAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                assertThat(comment).isEqualTo("Some comment scheduled temporary absence")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(applicationDate).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(applicationTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(tapScheduleIns).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            booking = booking {
              application = tapApplication()
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk(
          // comment is 300 long
          anUpsertTemporaryAbsenceRequest(application.tapApplicationId, comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithOffenderAddressCreatedInNomis {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              premise = "Permanent",
              street = "16 Main Road",
              locality = null,
              city = "28710",
              postcode = "BD10 9TT",
              county = null,
              country = "ENG",
            )
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence with existing address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            toAddress = UpsertTemporaryAbsenceAddress(
              name = null,
              addressText = "Permanent, 16 Main Road, Bradford, England",
              postalCode = "BD10 9TT",
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithOffenderAddressFromDpsWithTrailingBlanks {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              premise = "RDR, Preston Town Centre , Lancashire",
              street = null,
              locality = null,
              city = null,
              postcode = null,
              county = null,
              country = null,
            )
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence with existing address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            toAddress = UpsertTemporaryAbsenceAddress(
              name = null,
              addressText = "RDR, Preston Town Centre , Lancashire ",
              postalCode = null,
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithOffenderAddressThatOverflowIntoStreet {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              // Based on a production bug where last character of premise is a comma
              premise = "Unit 4, Nantcribbau Barns, Private Street From Junction With A490 By Nantcribba Cottages To B4388 By Dol-Y-Maen, Forden, Welshpool,",
              street = "Powys",
              locality = null,
              city = null,
              postcode = "SY21 8NW",
              county = null,
              country = null,
            )
            booking = booking {
              application = tapApplication(
                toAddress = offenderAddress,
              )
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence with existing address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            toAddress = UpsertTemporaryAbsenceAddress(
              name = null,
              addressText = "Unit 4, Nantcribbau Barns, Private Street From Junction With A490 By Nantcribba Cottages To B4388 By Dol-Y-Maen, Forden, Welshpool, Powys",
              postalCode = "SY21 8NW",
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateTapScheduleOutFromDpsAddressWithTrailingCommas {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              premise = "32 Tonbridge Road,, Maidstone,, Kent,",
              street = null,
              locality = null,
              city = null,
              postcode = null,
              county = null,
              country = null,
            )
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create tap schedule out with existing address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            toAddress = UpsertTemporaryAbsenceAddress(
              name = null,
              // Another production bug where a DPS address has last character ',' but address fits into NOMIS premise
              addressText = "32 Tonbridge Road,, Maidstone,, Kent,",
              postalCode = null,
            ),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithDuplicateOffenderAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            address(premise = "1 Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
            address(premise = "1 Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = null, addressText = "1 Scotland Street, Sheffield", postalCode = "S1 3GG")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.premise).isEqualTo("1 Scotland Street, Sheffield")
                assertThat(toAddress?.postalCode).isEqualTo("S1 3GG")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
              }
              // Should not create another address
              with(offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)) {
                assertThat(filter { it.premise == "1 Scotland Street, Sheffield" }.size).isEqualTo(2)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWhereOffenderHasEmptyAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            // The existence of this address should not prevent the schedule being created
            address(
              flat = null,
              premise = null,
              street = null,
              locality = null,
              city = null,
              county = null,
              country = null,
              postcode = null,
            )
            offenderAddress = address()
            booking = booking {
              application = tapApplication(toAddress = offenderAddress)
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence despit existing empty address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            movementApplicationId = application.tapApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(addressText = "41 High Street, Sheffield"),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithCorporateAddress {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            corporateAddress = address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class ShouldFindOffenderAddressWhereCorporateNameSameAsAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address(
              premise = "Boston",
              street = null,
              locality = null,
            )
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence with offender address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = "Boston", addressText = "Boston", postalCode = null)),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.premise).isEqualTo("Boston")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
              }
            }
          }
      }
    }

    @Nested
    inner class ShouldFindCorporateAddressWhereCorporateNameInAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence with corporate address`() {
        webTestClient.upsertApplicationOk(
          request = anUpsertApplicationRequest().copy(
            toAddresses = listOf(UpsertTemporaryAbsenceAddress(name = "HSL", addressText = "HSL, Bowness, Cumbria", postalCode = "LA23 3AS")),
          ),
        ).also {
          repository.runInTransaction {
            application = applicationRepository.findByIdOrNull(it.movementApplicationId)!!
            corporateAddress = corporateRepository.findAllByCorporateName("HSL").first().addresses.first()
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = "HSL", addressText = "HSL, Bowness, Cumbria", postalCode = "LA23 3AS")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.premise).isEqualTo("HSL, Bowness, Cumbria")
                assertThat(toAddress?.postalCode).isEqualTo("LA23 3AS")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithCorporateAddressCreatedInNomis {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "Serving Thyme, HMP Ford") {
            corporateAddress = address(
              premise = "Serving Thyme, HMP Ford",
              street = "Ford Road",
              locality = null,
              city = "28389",
              county = "W.SUSSEX",
              country = "ENG",
              postcode = "BN18 0BX",
            )
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence with existing address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = "Serving Thyme, HMP Ford", addressText = "Ford Road, Arundel, West Sussex, England", postalCode = "BN18 0BX")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithCorporateAddressTrailingBlanks {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "HSL") {
            corporateAddress = address(
              premise = "Bowness , Cumbria",
              street = null,
              locality = null,
              postcode = "LA23 3AS",
            )
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should find address despite trailing blanks`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(
            toAddress = UpsertTemporaryAbsenceAddress(name = "HSL ", addressText = "Bowness , Cumbria ", postalCode = "LA23 3AS "),
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleWithDuplicatedCorporateAddress {
      private lateinit var corporateAddress: CorporateAddress

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            corporateAddress = address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = "Boots", addressText = "Scotland Street, Sheffield", postalCode = "S1 3GG")),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleAndReturn {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence and its return schedule`() {
        val request = anUpsertTemporaryAbsenceRequest().copy(
          eventStatus = "COMP",
          returnEventStatus = "SCH",
        )

        webTestClient.upsertScheduledTemporaryAbsenceOk(request)
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventStatus.code).isEqualTo("COMP")
                with(tapScheduleIns.first()) {
                  assertThat(eventStatus.code).isEqualTo("SCH")
                  assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
                  assertThat(startTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                  assertThat(eventSubType.code).isEqualTo("C5")
                  assertThat(escort?.code).isEqualTo("L")
                  assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                  assertThat(toAgency?.id).isEqualTo("LEI")
                }
              }
            }
          }
      }
    }

    @Nested
    inner class CreateScheduleValidation {
      @Test
      fun `should return conflict if adding a schedule to an unapproved application`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(applicationStatus = "PEN")
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsence()
          .isEqualTo(409)
      }

      @Test
      fun `should return conflict if adding a schedule to a single application that already has a schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(applicationType = "SINGLE") {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsence()
          .isEqualTo(409)
      }

      @Test
      fun `should allow adding a schedule to a repeating application`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication(applicationType = "REPEATING") {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk()
          .apply {
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
                assertThat(tapScheduleOuts.size).isEqualTo(2)
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateSchedule {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }
      }

      @Test
      fun `should update scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(anUpsertTemporaryAbsenceRequest(eventId = scheduleOut.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                assertThat(comment).isEqualTo("Some comment scheduled temporary absence")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(applicationDate).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                assertThat(applicationTime).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                assertThat(tapScheduleIns).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk(
          // comment is 300 long
          anUpsertTemporaryAbsenceRequest(
            movementApplicationId = application.tapApplicationId,
            eventId = scheduleOut.eventId,
            comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890",
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateScheduleAndCreateReturn {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut()
              }
            }
          }
        }
      }

      @Test
      fun `should update scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(anUpsertTemporaryAbsenceRequest(eventId = scheduleOut.eventId, eventStatus = "COMP"))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventStatus.code).isEqualTo("COMP")
                with(tapScheduleIns.first()) {
                  assertThat(eventStatus.code).isEqualTo("SCH")
                  assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
                  assertThat(startTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                  assertThat(eventSubType.code).isEqualTo("C5")
                  assertThat(escort?.code).isEqualTo("L")
                  assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                  assertThat(toAgency?.id).isEqualTo("LEI")
                }
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateScheduleAndReturn {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut {
                  scheduleIn = tapScheduleIn()
                }
              }
            }
          }
        }
      }

      @Test
      fun `should update scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(anUpsertTemporaryAbsenceRequest(eventId = scheduleOut.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduleOutRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(tapApplication.tapApplicationId).isEqualTo(application.tapApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isCloseTo(twoDaysAgo, within(5, ChronoUnit.MINUTES))
                assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                assertThat(comment).isEqualTo("Some comment scheduled temporary absence")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(applicationDate).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                assertThat(applicationTime).isCloseTo(today, within(5, ChronoUnit.MINUTES))
                with(tapScheduleIns.first()) {
                  assertThat(eventStatus.code).isEqualTo("SCH")
                  assertThat(eventDate).isEqualTo(yesterday.toLocalDate())
                  assertThat(startTime).isCloseTo(yesterday, within(5, ChronoUnit.MINUTES))
                  assertThat(eventSubType.code).isEqualTo("C5")
                  assertThat(escort?.code).isEqualTo("L")
                  assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                  assertThat(toAgency?.id).isEqualTo("LEI")
                }
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.upsertScheduledTemporaryAbsence(offenderNo = "UNKNOWN")
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if offender has no bookings`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
          }
        }

        webTestClient.upsertScheduledTemporaryAbsence()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if movement application does not exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
            booking = booking()
          }
        }

        webTestClient.upsertScheduledTemporaryAbsence(request = anUpsertTemporaryAbsenceRequest(movementApplicationId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid event sub type`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertTemporaryAbsenceRequest().copy(eventSubType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid event status`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertTemporaryAbsenceRequest().copy(eventStatus = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertTemporaryAbsenceRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from prison`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertTemporaryAbsenceRequest().copy(fromPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertTemporaryAbsenceRequest().copy(toAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid transport type`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertTemporaryAbsenceRequest().copy(transportType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(id = 9999)
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertTemporaryAbsenceRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("invalid")
          }
      }

      @Test
      fun `should fail if offender address does not exist`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(addressText = "unknown")))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Address not found")
          }
      }

      @Test
      fun `should fail if corporate entity does not exist`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = "unknown", addressText = "unknown")))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Address not found")
          }
      }

      @Test
      fun `should fail if corporate address does not exist`() {
        nomisDataBuilder.build {
          corporate(corporateName = "Boots") {
            address(premise = "Scotland Street, Sheffield", street = null, locality = null, postcode = "S1 3GG")
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(request = anUpsertTemporaryAbsenceRequest(toAddress = UpsertTemporaryAbsenceAddress(name = "Boots", addressText = "unknown")))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Address not found")
          }
      }

      @Test
      fun `should return bad request if address text not passed`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(addressText = null, name = "Business")
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertTemporaryAbsenceRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).containsIgnoringCase("address text")
          }
      }
    }

    @Nested
    inner class Security {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }
      }

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .bodyValue(anUpsertTemporaryAbsenceRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(anUpsertTemporaryAbsenceRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(anUpsertTemporaryAbsenceRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("DELETE /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence/{eventId}")
  inner class DeleteScheduledTemporaryAbsence {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(
                eventStatus = "SCH",
              )
            }
          }
        }
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `should delete schedule`() {
        webTestClient.deleteSchedule()
          .expectStatus().isNoContent

        repository.runInTransaction {
          assertThat(scheduleOutRepository.findByIdOrNull(scheduleOut.eventId)).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return 204 if unknown application id sent`() {
        webTestClient.deleteSchedule(eventId = 9999)
          .expectStatus().isNoContent
      }

      @Test
      fun `should return conflict for unknown offender`() {
        webTestClient.deleteSchedule(offenderNo = "UNKNOWN")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 for wrong offender`() {
        nomisDataBuilder.build {
          offender(nomsId = "A7897WW")
        }

        webTestClient.deleteSchedule(offenderNo = "A7897WW")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if scheduled has a movement`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  eventStatus = "SCH",
                ) {
                  tapMovementOut()
                }
              }
            }
          }
        }

        webTestClient.deleteSchedule()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if status is completed`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  eventStatus = "COMP",
                )
              }
            }
          }
        }

        webTestClient.deleteSchedule()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if there is an inbound schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  eventStatus = "SCH",
                ) {
                  tapScheduleIn()
                }
              }
            }
          }
        }

        webTestClient.deleteSchedule()
          .expectStatus().isEqualTo(409)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/${scheduleOut.eventId}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/${scheduleOut.eventId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/${scheduleOut.eventId}")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteSchedule(offenderNo: String = offender.nomsId, eventId: Long = scheduleOut.eventId): WebTestClient.ResponseSpec = delete()
      .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence/$eventId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/temporary-absence/{bookingId}/{movementSeq}")
  inner class GetTemporaryAbsence {

    @Nested
    inner class GetUnscheduledTemporaryAbsence {
      private lateinit var agencyAddress: AgencyLocationAddress
      private lateinit var cityBooking: OffenderBooking
      private lateinit var agencyBooking: OffenderBooking
      private lateinit var cityTempAbsence: OffenderTapMovementOut
      private lateinit var agencyTempAbsence: OffenderTapMovementOut

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation = agencyLocation(
            agencyLocationId = "NGENHO",
            description = "Northern General Hospital",
            type = "HOSPITAL",
          ) {
            agencyAddress = address(
              type = "BUS",
              premise = "2",
              street = "Herries Road",
              postcode = "S5 7AU",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            cityBooking = booking {
              cityTempAbsence = tapMovementOut(
                date = twoDaysAgo,
                fromPrison = "LEI",
                toAgency = null,
                movementReason = "C5",
                arrestAgency = "POL",
                escort = "L",
                escortText = "SE",
                comment = "Tap OUT comment",
                toCity = "25343",
              )
            }
            agencyBooking = booking {
              agencyTempAbsence = tapMovementOut(
                date = twoDaysAgo,
                fromPrison = "LEI",
                toAgency = "NGENHO",
                toCity = null,
              )
            }
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence/${cityBooking.bookingId}/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence/${cityBooking.bookingId}/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence/${cityBooking.bookingId}/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/temporary-absences/temporary-absence/${cityBooking.bookingId}/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if temporary absence not found`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence/9999/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }

      @Test
      fun `should retrieve unscheduled temporary absence with city for address`() {
        webTestClient.get()
          .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${cityBooking.bookingId}/${cityTempAbsence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<TemporaryAbsenceResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(cityBooking.bookingId)
            assertThat(sequence).isEqualTo(cityTempAbsence.id.sequence)
            assertThat(scheduledTemporaryAbsenceId).isNull()
            assertThat(movementApplicationId).isNull()
            assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
            assertThat(movementTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
            assertThat(movementReason).isEqualTo("C5")
            assertThat(arrestAgency).isEqualTo("POL")
            assertThat(escort).isEqualTo("L")
            assertThat(escortText).isEqualTo("SE")
            assertThat(fromPrison).isEqualTo("LEI")
            assertThat(toAgency).isNull()
            assertThat(commentText).isEqualTo("Tap OUT comment")
            assertThat(toAddressId).isNull()
            assertThat(toAddressOwnerClass).isNull()
            assertThat(toFullAddress).isEqualTo("Sheffield")
            assertThat(toAddressPostcode).isNull()
          }
      }

      @Test
      fun `should retrieve unscheduled temporary absence with agency for address`() {
        webTestClient.get()
          .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${agencyBooking.bookingId}/${agencyTempAbsence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<TemporaryAbsenceResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(agencyBooking.bookingId)
            assertThat(sequence).isEqualTo(agencyTempAbsence.id.sequence)
            assertThat(scheduledTemporaryAbsenceId).isNull()
            assertThat(movementApplicationId).isNull()
            assertThat(toAgency).isEqualTo("NGENHO")
            assertThat(toAddressId).isEqualTo(agencyAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo("AGY")
            assertThat(toFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
            assertThat(toAddressDescription).isEqualTo("Northern General Hospital")
            assertThat(toAddressPostcode).isEqualTo("S5 7AU")
          }
      }
    }

    @Nested
    inner class GetScheduledTemporaryAbsence {

      @Nested
      inner class WithoutAddress {

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            offender = offender(nomsId = offenderNo) {
              booking = booking {
                application = tapApplication {
                  scheduleOut = tapScheduleOut {
                    movementOut = tapMovementOut(
                      date = twoDaysAgo,
                      fromPrison = "LEI",
                      toAgency = "HAZLWD",
                      movementReason = "C5",
                      arrestAgency = "POL",
                      escort = "L",
                      escortText = "SE",
                      comment = "Tap OUT comment",
                      toAddress = null,
                    )
                    scheduleIn = tapScheduleIn {
                      movementIn = tapMovementIn()
                    }
                  }
                }
              }
            }
          }
        }

        @Test
        fun `should retrieve scheduled temporary absence`() {
          webTestClient.get()
            .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${movementOut.id.sequence}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse<TemporaryAbsenceResponse>()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(movementOut.id.sequence)
              assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduleOut.eventId)
              assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
              assertThat(movementDate).isEqualTo("${twoDaysAgo.toLocalDate()}")
              assertThat(movementTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
              assertThat(movementReason).isEqualTo("C5")
              assertThat(arrestAgency).isEqualTo("POL")
              assertThat(escort).isEqualTo("L")
              assertThat(escortText).isEqualTo("SE")
              assertThat(fromPrison).isEqualTo("LEI")
              assertThat(toAgency).isEqualTo("HAZLWD")
              assertThat(commentText).isEqualTo("Tap OUT comment")
              assertThat(toAddressId).isNull()
              assertThat(toAddressDescription).isNull()
              assertThat(toAddressOwnerClass).isNull()
              assertThat(toFullAddress).isNull()
              assertThat(toAddressPostcode).isNull()
            }
        }
      }

      @Nested
      inner class WithOffenderAddress {

        @Nested
        @DisplayName("With address on scheduled OUT, not movement")
        inner class WithAddressOnScheduledOutButNotMovement {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                offenderAddress = address(
                  flat = "Flat 1",
                  premise = "41",
                  street = "High Street",
                  locality = "Hillsborough",
                  city = "25343",
                  county = "S.YORKSHIRE",
                  country = "ENG",
                  postcode = "S1 1AB",
                )
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = offenderAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = null,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from scheduled temporary absence`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${movementOut.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(movementOut.id.sequence)
                assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toFullAddress).isEqualTo("Flat 1, 41 High Street, Hillsborough, Sheffield, South Yorkshire, England")
                assertThat(toAddressPostcode).isEqualTo("S1 1AB")
              }
          }
        }

        @Nested
        @DisplayName("With address on both schedule and movement")
        inner class WithAddressOnMovement {
          private lateinit var scheduleAddress: OffenderAddress
          private lateinit var movementAddress: OffenderAddress

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                scheduleAddress = address()
                movementAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = scheduleAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = movementAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should take the address from the movement`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${movementOut.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(toAddressId).isEqualTo(movementAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo(movementAddress.addressOwnerClass)
              }
          }
        }
      }

      @Nested
      @DisplayName("With corporate address")
      inner class WithCorporateAddress {
        private lateinit var corporateAddress: CorporateAddress

        @Nested
        @DisplayName("With address on scheduled OUT, not movement")
        inner class WithAddressOnScheduledOutButNotMovement {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              corporate(
                corporateName = "Boots",
              ) {
                corporateAddress = address(
                  type = "BUS",
                  flat = "3B",
                  premise = "Brown Court",
                  street = "Scotland Street",
                  locality = "Hunters Bar",
                  postcode = "S1 3GG",
                  city = SHEFFIELD,
                  county = "S.YORKSHIRE",
                  country = "ENG",
                )
              }
              offender = offender(nomsId = offenderNo) {
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = corporateAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = null,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should take corporate address from schedule`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${movementOut.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(movementOut.id.sequence)
                assertThat(toAddressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
                assertThat(toAddressDescription).isEqualTo("Boots")
                assertThat(toFullAddress).isEqualTo("Flat 3B, Brown Court, Scotland Street, Hunters Bar, Sheffield, South Yorkshire, England")
                assertThat(toAddressPostcode).isEqualTo("S1 3GG")
              }
          }
        }

        @Nested
        @DisplayName("With address on both schedule and movement")
        inner class WithAddressOnMovement {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              nomisDataBuilder.build {
                corporate(
                  corporateName = "Boots",
                ) {
                  corporateAddress = address(
                    type = "BUS",
                    flat = "3B",
                    premise = "Brown Court",
                    street = "Scotland Street",
                    locality = "Hunters Bar",
                    postcode = "S1 3GG",
                    city = SHEFFIELD,
                    county = "S.YORKSHIRE",
                    country = "ENG",
                  )
                }
                offender = offender(nomsId = offenderNo) {
                  offenderAddress = address()
                  booking = booking {
                    application = tapApplication {
                      scheduleOut = tapScheduleOut(
                        toAddress = offenderAddress,
                      ) {
                        movementOut = tapMovementOut(
                          toAddress = corporateAddress,
                        )
                        scheduleIn = tapScheduleIn {
                          movementIn = tapMovementIn()
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should take corporate address from movement`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${movementOut.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(toAddressId).isEqualTo(corporateAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("CORP")
                assertThat(toAddressDescription).isEqualTo("Boots")
                assertThat(toFullAddress).isEqualTo("Flat 3B, Brown Court, Scotland Street, Hunters Bar, Sheffield, South Yorkshire, England")
                assertThat(toAddressPostcode).isEqualTo("S1 3GG")
              }
          }
        }
      }

      @Test
      fun `should not remove corporate name from address if they are identical`() {
        nomisDataBuilder.build {
          corporate(
            corporateName = "Newcastle City Centre",
          ) {
            corporateAddress = address(
              type = "BUS",
              flat = null,
              premise = "Newcastle City Centre",
              street = null,
              locality = null,
              postcode = null,
              city = null,
              county = null,
              country = null,
            )
          }
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut(
                  toAddress = corporateAddress,
                )
              }
            }
          }
        }

        webTestClient.get()
          .uri(
            "/movements/${offender.nomsId}/temporary-absences/scheduled-temporary-absence/{eventId}",
            scheduleOut.eventId,
          )
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<ScheduledTemporaryAbsenceResponse>()
          .apply {
            assertThat(toAddressId).isEqualTo(corporateAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo("CORP")
            assertThat(toAddressDescription).isEqualTo("Newcastle City Centre")
            assertThat(toFullAddress).isEqualTo("Newcastle City Centre")
          }
      }

      @Nested
      @DisplayName("With agency address")
      inner class WithAgencyAddress {
        private lateinit var agencyAddress: AgencyLocationAddress

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            agencyLocation = agencyLocation(
              agencyLocationId = "NGENHO",
              description = "Northern General Hospital",
              type = "HOSPITAL",
            ) {
              agencyAddress = address(
                type = "BUS",
                street = "Herries Road",
                postcode = "S5 7AU",
                city = SHEFFIELD,
                county = "S.YORKSHIRE",
                country = "ENG",
              )
            }
          }
        }

        @Nested
        @DisplayName("With address on scheduled OUT, not movement")
        inner class WithAddressOnScheduledOutButNotMovement {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = agencyAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = null,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should take agency address from schedule`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${movementOut.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(movementOut.id.sequence)
                assertThat(toAddressId).isEqualTo(agencyAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("AGY")
                assertThat(toAddressDescription).isEqualTo("Northern General Hospital")
                assertThat(toFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
                assertThat(toAddressPostcode).isEqualTo("S5 7AU")
              }
          }
        }

        @Nested
        @DisplayName("With address on both schedule and movement")
        inner class WithAddressOnMovement {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                offenderAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = offenderAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = agencyAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should take agency address from movement`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${movementOut.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(movementOut.id.sequence)
                assertThat(toAddressId).isEqualTo(agencyAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("AGY")
                assertThat(toAddressDescription).isEqualTo("Northern General Hospital")
                assertThat(toFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
                assertThat(toAddressPostcode).isEqualTo("S5 7AU")
              }
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /movements/{offenderNo}/temporary-absences/temporary-absence")
  inner class CreateTemporaryAbsence {

    private fun aCreateRequest(scheduledTemporaryAbsenceId: Long? = scheduleOut.eventId) = CreateTemporaryAbsenceRequest(
      scheduledTemporaryAbsenceId = scheduledTemporaryAbsenceId,
      movementDate = twoDaysAgo.toLocalDate(),
      movementTime = twoDaysAgo,
      movementReason = "C5",
      arrestAgency = "POL",
      escort = "L",
      escortText = "SE",
      fromPrison = "LEI",
      toAgency = "HAZLWD",
      commentText = "comment temporary absence out",
      toCity = offenderAddress.city?.code,
      toAddressId = offenderAddress.addressId,
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut()
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create temporary absence`() {
        webTestClient.createTemporaryAbsenceOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            // Note there is already an admission external movement on sequence 1
            assertThat(movementSequence).isEqualTo(2)
            repository.runInTransaction {
              with(movementOutRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(tapScheduleOut?.eventId).isEqualTo(scheduleOut.eventId)
                assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(movementTime).isEqualTo(twoDaysAgo)
                assertThat(movementReason.id.reasonCode).isEqualTo("C5")
                assertThat(arrestAgency?.code).isEqualTo("POL")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(escortText).isEqualTo("SE")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(commentText).isEqualTo("comment temporary absence out")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toCity?.code).isEqualTo(offenderAddress.city?.code)
              }
              with(offenderExternalMovementRepository.findByIdOrNull(OffenderExternalMovementId(booking, 1))!!) {
                assertThat(active).isFalse
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.createTemporaryAbsence(offenderNo = "UNKNOWN")
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if offender has no bookings`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
          }
        }

        webTestClient.createTemporaryAbsence()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if schedule temporary absence does not exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }

        webTestClient.createTemporaryAbsence(request = aCreateRequest(scheduledTemporaryAbsenceId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid movement reason`() {
        webTestClient.createTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(movementReason = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid arrest agency`() {
        webTestClient.createTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(arrestAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.createTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from prison`() {
        webTestClient.createTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(fromPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency`() {
        webTestClient.createTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(toAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        webTestClient.createTemporaryAbsenceBadRequest(aCreateRequest().copy(toAddressId = 9999))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("invalid")
          }
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence")
          .bodyValue(aCreateRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class CreateUnscheduledTemporaryAbsence {
      @Test
      fun `should create unscheduled temporary absence`() {
        webTestClient.createTemporaryAbsenceOk(aCreateRequest(scheduledTemporaryAbsenceId = null))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            // Note there is already an admission external movement on sequence 1
            assertThat(movementSequence).isEqualTo(2)
            repository.runInTransaction {
              with(movementOutRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(tapScheduleOut).isNull()
                assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(movementTime).isEqualTo(twoDaysAgo)
              }
              with(offenderExternalMovementRepository.findByIdOrNull(OffenderExternalMovementId(booking, 1))!!) {
                assertThat(active).isFalse
              }
            }
          }
      }
    }

    private fun WebTestClient.createTemporaryAbsence(
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduleOut.eventId),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/temporary-absence")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createTemporaryAbsenceOk(
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduleOut.eventId),
    ) = createTemporaryAbsence(request)
      .isCreated
      .expectBodyResponse<CreateTemporaryAbsenceResponse>()

    private fun WebTestClient.createTemporaryAbsenceBadRequest(
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduleOut.eventId),
    ) = createTemporaryAbsence(request)
      .isBadRequest

    private fun WebTestClient.createTemporaryAbsenceBadRequestUnknown(
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduleOut.eventId),
    ) = createTemporaryAbsenceBadRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/temporary-absence-return/{bookingId}/{movementSeq}")
  inner class GetTemporaryAbsenceReturn {

    @Nested
    inner class GetUnscheduledTemporaryAbsenceReturn {
      private lateinit var agencyAddress: AgencyLocationAddress
      private lateinit var cityBooking: OffenderBooking
      private lateinit var agencyBooking: OffenderBooking
      private lateinit var cityTempAbsenceReturn: OffenderTapMovementIn
      private lateinit var agencyTempAbsenceReturn: OffenderTapMovementIn

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation = agencyLocation(
            agencyLocationId = "NGENHO",
            description = "Northern General Hospital",
            type = "HOSPITAL",
          ) {
            agencyAddress = address(
              type = "BUS",
              premise = "2",
              street = "Herries Road",
              postcode = "S5 7AU",
              city = SHEFFIELD,
              county = "S.YORKSHIRE",
              country = "ENG",
            )
          }
          offender = offender(nomsId = offenderNo) {
            cityBooking = booking {
              cityTempAbsenceReturn = tapMovementIn(
                date = twoDaysAgo,
                fromAgency = null,
                toPrison = "LEI",
                movementReason = "C5",
                escort = "L",
                escortText = "SE",
                comment = "Tap IN comment",
                fromCity = "25343",
              )
            }
            agencyBooking = booking {
              agencyTempAbsenceReturn = tapMovementIn(
                date = twoDaysAgo,
                toPrison = "LEI",
                fromAgency = "NGENHO",
                fromCity = null,
              )
            }
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/${cityBooking.bookingId}/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/${cityBooking.bookingId}/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/${cityBooking.bookingId}/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/temporary-absences/temporary-absence-return/${cityBooking.bookingId}/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if temporary absence return not found`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/9999/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }

      @Test
      fun `should retrieve unscheduled temporary absence return with city address`() {
        webTestClient.get()
          .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${cityBooking.bookingId}/${cityTempAbsenceReturn.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<TemporaryAbsenceReturnResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(cityBooking.bookingId)
            assertThat(sequence).isEqualTo(cityTempAbsenceReturn.id.sequence)
            assertThat(scheduledTemporaryAbsenceId).isNull()
            assertThat(scheduledTemporaryAbsenceReturnId).isNull()
            assertThat(movementApplicationId).isNull()
            assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
            assertThat(movementTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
            assertThat(movementReason).isEqualTo("C5")
            assertThat(escort).isEqualTo("L")
            assertThat(escortText).isEqualTo("SE")
            assertThat(fromAgency).isNull()
            assertThat(toPrison).isEqualTo("LEI")
            assertThat(commentText).isEqualTo("Tap IN comment")
            assertThat(fromAddressId).isNull()
            assertThat(fromAddressOwnerClass).isNull()
            assertThat(fromFullAddress).isEqualTo("Sheffield")
            assertThat(fromAddressPostcode).isNull()
          }
      }

      @Test
      fun `should retrieve unscheduled temporary absence return with agency address`() {
        webTestClient.get()
          .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${agencyBooking.bookingId}/${agencyTempAbsenceReturn.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<TemporaryAbsenceReturnResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(agencyBooking.bookingId)
            assertThat(sequence).isEqualTo(agencyTempAbsenceReturn.id.sequence)
            assertThat(scheduledTemporaryAbsenceId).isNull()
            assertThat(scheduledTemporaryAbsenceReturnId).isNull()
            assertThat(movementApplicationId).isNull()
            assertThat(fromAgency).isEqualTo("NGENHO")
            assertThat(toPrison).isEqualTo("LEI")
            assertThat(fromAddressId).isEqualTo(agencyAddress.addressId)
            assertThat(fromAddressOwnerClass).isEqualTo("AGY")
            assertThat(fromFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
            assertThat(fromAddressDescription).isEqualTo("Northern General Hospital")
            assertThat(fromAddressPostcode).isEqualTo("S5 7AU")
          }
      }
    }

    @Nested
    inner class GetScheduledTemporaryAbsenceReturn {

      @Nested
      inner class WithoutAddress {

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            offender = offender(nomsId = offenderNo) {
              booking = booking {
                application = tapApplication {
                  scheduleOut = tapScheduleOut {
                    movementOut = tapMovementOut()
                    scheduleIn = tapScheduleIn {
                      movementIn = tapMovementIn(
                        date = twoDaysAgo,
                        fromAgency = "HAZLWD",
                        toPrison = "LEI",
                        movementReason = "C5",
                        escort = "L",
                        escortText = "SE",
                        comment = "Tap IN comment",
                        fromAddress = null,
                      )
                    }
                  }
                }
              }
            }
          }
        }

        @Test
        fun `should retrieve scheduled temporary absence return`() {
          webTestClient.get()
            .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse<TemporaryAbsenceReturnResponse>()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(movementIn.id.sequence)
              assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduleOut.eventId)
              assertThat(scheduledTemporaryAbsenceReturnId).isEqualTo(scheduleIn.eventId)
              assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
              assertThat(movementDate).isEqualTo("${twoDaysAgo.toLocalDate()}")
              assertThat(movementTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
              assertThat(movementReason).isEqualTo("C5")
              assertThat(escort).isEqualTo("L")
              assertThat(escortText).isEqualTo("SE")
              assertThat(fromAgency).isEqualTo("HAZLWD")
              assertThat(toPrison).isEqualTo("LEI")
              assertThat(commentText).isEqualTo("Tap IN comment")
              assertThat(fromAddressId).isNull()
              assertThat(fromAddressOwnerClass).isNull()
              assertThat(fromFullAddress).isNull()
              assertThat(fromAddressPostcode).isNull()
            }
        }
      }

      @Nested
      inner class WithOffenderAddress {

        @Nested
        inner class WithAddressOnScheduledOutOnly {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                offenderAddress = address(
                  flat = "Flat 1",
                  premise = "41",
                  street = "High Street",
                  locality = "Hillsborough",
                  city = "25343",
                  county = "S.YORKSHIRE",
                  country = "ENG",
                  postcode = "S1 1AB",
                )
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = offenderAddress,
                    ) {
                      movementOut = tapMovementOut()
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from scheduled OUT`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(offenderAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(fromFullAddress).isEqualTo("Flat 1, 41 High Street, Hillsborough, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S1 1AB")
              }
          }
        }

        @Nested
        @DisplayName("With address on scheduled OUT and movement OUT but not movement IN")
        inner class WithAddressOnScheduledOutAndMovementOutButNotMovementIn {
          private lateinit var scheduledOutAddress: OffenderAddress
          private lateinit var movementOutAddress: OffenderAddress

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                scheduledOutAddress = address()
                movementOutAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = scheduledOutAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = movementOutAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn(
                          fromAddress = null,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from movement OUT`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(movementOutAddress.addressId)
              }
          }
        }

        @Nested
        @DisplayName("With address on scheduled OUT, movement OUT and movement IN")
        inner class WithAddressOnMovementAndSchedules {
          private lateinit var scheduledOutAddress: OffenderAddress
          private lateinit var movementOutAddress: OffenderAddress
          private lateinit var movementInAddress: OffenderAddress

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                scheduledOutAddress = address()
                movementOutAddress = address()
                movementInAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = scheduledOutAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = movementOutAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn(
                          fromAddress = movementInAddress,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from movement IN`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(movementInAddress.addressId)
              }
          }
        }
      }

      @Nested
      inner class WithCorporateAddress {
        private lateinit var corporateAddress: CorporateAddress

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            corporate(
              corporateName = "Boots",
            ) {
              corporateAddress = address(
                type = "BUS",
                flat = "3B",
                premise = "Brown Court",
                street = "Scotland Street",
                locality = "Hunters Bar",
                postcode = "S1 3GG",
                city = SHEFFIELD,
                county = "S.YORKSHIRE",
                country = "ENG",
              )
            }
          }
        }

        @Nested
        inner class WithAddressOnScheduledOutOnly {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                offenderAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = corporateAddress,
                    ) {
                      movementOut = tapMovementOut()
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve corporate address from scheduled OUT`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(corporateAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo("CORP")
                assertThat(fromAddressDescription).isEqualTo("Boots")
                assertThat(fromFullAddress).isEqualTo("Flat 3B, Brown Court, Scotland Street, Hunters Bar, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S1 3GG")
              }
          }
        }

        @Nested
        @DisplayName("With address on scheduled OUT and movement OUT but not movement IN")
        inner class WithAddressOnScheduledOutAndMovementOutButNotMovementIn {
          private lateinit var scheduledOutAddress: OffenderAddress

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                scheduledOutAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = scheduledOutAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = corporateAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn(
                          fromAddress = null,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from movement OUT`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(corporateAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo("CORP")
                assertThat(fromAddressDescription).isEqualTo("Boots")
                assertThat(fromFullAddress).isEqualTo("Flat 3B, Brown Court, Scotland Street, Hunters Bar, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S1 3GG")
              }
          }
        }

        @Nested
        @DisplayName("With address on scheduled OUT, movement OUT and movement IN")
        inner class WithAddressOnMovementAndSchedules {
          private lateinit var scheduledOutAddress: OffenderAddress
          private lateinit var movementOutAddress: OffenderAddress

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                scheduledOutAddress = address()
                movementOutAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = scheduledOutAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = movementOutAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn(
                          fromAddress = corporateAddress,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from movement IN`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(corporateAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo("CORP")
                assertThat(fromAddressDescription).isEqualTo("Boots")
                assertThat(fromFullAddress).isEqualTo("Flat 3B, Brown Court, Scotland Street, Hunters Bar, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S1 3GG")
              }
          }
        }
      }

      @Nested
      inner class WithAgencyAddress {
        private lateinit var agencyAddress: AgencyLocationAddress

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            agencyLocation = agencyLocation(
              agencyLocationId = "NGENHO",
              description = "Northern General Hospital",
              type = "HOSPITAL",
            ) {
              agencyAddress = address(
                type = "BUS",
                street = "Herries Road",
                postcode = "S5 7AU",
                city = SHEFFIELD,
                county = "S.YORKSHIRE",
                country = "ENG",
              )
            }
          }
        }

        @Nested
        inner class WithAddressOnScheduledOutOnly {

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                offenderAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = agencyAddress,
                    ) {
                      movementOut = tapMovementOut()
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn()
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from scheduled OUT`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(agencyAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo("AGY")
                assertThat(fromAddressDescription).isEqualTo("Northern General Hospital")
                assertThat(fromFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S5 7AU")
              }
          }
        }

        @Nested
        @DisplayName("With address on scheduled OUT and movement OUT but not movement IN")
        inner class WithAddressOnScheduledOutAndMovementOutButNotMovementIn {
          private lateinit var scheduledOutAddress: OffenderAddress

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                scheduledOutAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = scheduledOutAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = agencyAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn(
                          fromAddress = null,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from movement OUT`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(agencyAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo("AGY")
                assertThat(fromAddressDescription).isEqualTo("Northern General Hospital")
                assertThat(fromFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S5 7AU")
              }
          }
        }

        @Nested
        @DisplayName("With address on scheduled OUT, movement OUT and movement IN")
        inner class WithAddressOnMovementAndSchedules {
          private lateinit var scheduledOutAddress: OffenderAddress
          private lateinit var movementOutAddress: OffenderAddress

          @BeforeEach
          fun setUp() {
            nomisDataBuilder.build {
              offender = offender(nomsId = offenderNo) {
                scheduledOutAddress = address()
                movementOutAddress = address()
                booking = booking {
                  application = tapApplication {
                    scheduleOut = tapScheduleOut(
                      toAddress = scheduledOutAddress,
                    ) {
                      movementOut = tapMovementOut(
                        toAddress = movementOutAddress,
                      )
                      scheduleIn = tapScheduleIn {
                        movementIn = tapMovementIn(
                          fromAddress = agencyAddress,
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          @Test
          fun `should retrieve address from movement IN`() {
            webTestClient.get()
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceReturnResponse>()
              .apply {
                assertThat(fromAddressId).isEqualTo(agencyAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo("AGY")
                assertThat(fromAddressDescription).isEqualTo("Northern General Hospital")
                assertThat(fromFullAddress).isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S5 7AU")
              }
          }
        }
      }

      @Nested
      inner class GetScheduledReturnWithBrokenParentLink {

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            offender = offender(nomsId = offenderNo) {
              booking = booking {
                application = tapApplication {
                  scheduleOut = tapScheduleOut {
                    movementOut = tapMovementOut()
                    scheduleIn = tapScheduleIn {
                      movementIn = tapMovementIn()
                    }
                  }
                }
              }
            }

            // there's a bug in NOMIS where the external movement parent event id is null if you perform any unscheduled
            // external movements inbetween confirming the scheduled OUT/IN movements
            movementIn.tapScheduleOut = null
          }
        }

        @Test
        fun `should retrieve scheduled temporary absence return as unscheduled`() {
          webTestClient.get()
            .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${movementIn.id.sequence}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse<TemporaryAbsenceReturnResponse>()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(movementIn.id.sequence)
              assertThat(scheduledTemporaryAbsenceId).isNull()
              assertThat(scheduledTemporaryAbsenceReturnId).isNull()
              assertThat(movementApplicationId).isNull()
            }
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /movements/{offenderNo}/temporary-absences/temporary-absence-return")
  inner class CreateTemporaryAbsenceReturn {

    private fun aCreateRequest(scheduledTemporaryAbsenceReturnId: Long? = scheduleIn.eventId) = CreateTemporaryAbsenceReturnRequest(
      scheduledTemporaryAbsenceReturnId = scheduledTemporaryAbsenceReturnId,
      movementDate = twoDaysAgo.toLocalDate(),
      movementTime = twoDaysAgo,
      movementReason = "C5",
      arrestAgency = "POL",
      escort = "L",
      escortText = "SE",
      fromAgency = "HAZLWD",
      toPrison = "LEI",
      commentText = "comment temporary absence in",
      fromAddressId = offenderAddress.addressId,
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn()
              }
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create temporary absence return`() {
        webTestClient.createTemporaryAbsenceReturnOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            // Note there is already an admission external movement on sequence 1 and the temporary absence external movement on sequence 2
            assertThat(movementSequence).isEqualTo(3)
            repository.runInTransaction {
              with(movementInRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(tapScheduleOut?.eventId).isEqualTo(scheduleOut.eventId)
                assertThat(tapScheduleIn?.eventId).isEqualTo(scheduleIn.eventId)
                assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(movementTime).isEqualTo(twoDaysAgo)
                assertThat(movementReason.id.reasonCode).isEqualTo("C5")
                assertThat(arrestAgency?.code).isEqualTo("POL")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(escortText).isEqualTo("SE")
                assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                assertThat(toAgency?.id).isEqualTo("LEI")
                assertThat(commentText).isEqualTo("comment temporary absence in")
                assertThat(fromAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(fromCity?.code).isEqualTo(offenderAddress.city?.code)
              }
              with(offenderExternalMovementRepository.findByIdOrNull(OffenderExternalMovementId(booking, 1))!!) {
                assertThat(active).isFalse
              }
              with(offenderExternalMovementRepository.findByIdOrNull(OffenderExternalMovementId(booking, 2))!!) {
                assertThat(active).isFalse
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.createTemporaryAbsenceReturn(offenderNo = "UNKNOWN")
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if offender has no bookings`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
          }
        }

        webTestClient.createTemporaryAbsenceReturn()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if schedule temporary absence does not exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }

        webTestClient.createTemporaryAbsenceReturn(request = aCreateRequest(scheduledTemporaryAbsenceReturnId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid movement reason`() {
        webTestClient.createTemporaryAbsenceReturnBadRequestUnknown(aCreateRequest().copy(movementReason = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid arrest agency`() {
        webTestClient.createTemporaryAbsenceReturnBadRequestUnknown(aCreateRequest().copy(arrestAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.createTemporaryAbsenceReturnBadRequestUnknown(aCreateRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from agency`() {
        webTestClient.createTemporaryAbsenceReturnBadRequestUnknown(aCreateRequest().copy(fromAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to prison`() {
        webTestClient.createTemporaryAbsenceReturnBadRequestUnknown(aCreateRequest().copy(toPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        webTestClient.createTemporaryAbsenceReturnBadRequest(aCreateRequest().copy(fromAddressId = 9999))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("invalid")
          }
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return")
          .bodyValue(aCreateRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class CreateUnscheduledTemporaryAbsenceReturn {
      @Test
      fun `should create unscheduled temporary absence`() {
        webTestClient.createTemporaryAbsenceReturnOk(aCreateRequest(scheduledTemporaryAbsenceReturnId = null))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            // Note there is already an admission external movement on sequence 1 and the temporary absence external movement on sequence 2
            assertThat(movementSequence).isEqualTo(3)
            repository.runInTransaction {
              with(movementInRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(tapScheduleOut).isNull()
                assertThat(tapScheduleIn).isNull()
                assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(movementTime).isEqualTo(twoDaysAgo)
              }
              with(offenderExternalMovementRepository.findByIdOrNull(OffenderExternalMovementId(booking, 1))!!) {
                assertThat(active).isFalse
              }
            }
          }
      }
    }

    private fun WebTestClient.createTemporaryAbsenceReturn(
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduleIn.eventId),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createTemporaryAbsenceReturnOk(
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduleIn.eventId),
    ) = createTemporaryAbsenceReturn(request)
      .isCreated
      .expectBodyResponse<CreateTemporaryAbsenceReturnResponse>()

    private fun WebTestClient.createTemporaryAbsenceReturnBadRequest(
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduleIn.eventId),
    ) = createTemporaryAbsenceReturn(request)
      .isBadRequest

    private fun WebTestClient.createTemporaryAbsenceReturnBadRequestUnknown(
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduleIn.eventId),
    ) = createTemporaryAbsenceReturnBadRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
  }

  /*
   * The general approach where we have movements IN attached to different schedules to the correct scheduled OUT/IN
   * is to treat the movement as unscheduled.
   *
   * This test suite corrupts data to emulate the various flavours of this error that we've seen with production data.
   *
   * The goal for each scenario is that the movement is considered unscheduled when either syncing the individual movement
   * or resyncing all of the offender's movements.
   */
  @Nested
  inner class GetUnscheduledTemporaryAbsenceDueToBadData {

    @Test
    fun `scheduled OUT has been deleted`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                orphanedScheduleReturn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by deleting the scheduled OUT
         */
        entityManager.createNativeQuery(
          """
              delete from OFFENDER_IND_SCHEDULES
              where EVENT_ID = ${scheduleOut.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Reload the scheduled return to reflect the update
        orphanedScheduleReturn = scheduleInRepository.findByIdOrNull(orphanedScheduleReturn.eventId) ?: throw IllegalStateException("Failed to reload scheduled return movement - this should not happen")
      }

      // Sync movement OUT is unscheduled
      webTestClient.getTemporaryAbsence(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
        }

      // Sync the movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender, the movements are unscheduled
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].temporaryAbsenceApplications[0].absences).isEmpty()
          assertThat(bookings[0].unscheduledTemporaryAbsences[0].sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(movementIn.id.sequence)
        }

      // Reconciliation counts reflect both movements as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.scheduled.outCount).isEqualTo(0)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.outCount).isEqualTo(1)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Test
    fun `movement IN has different schedule IN to the schedule OUT`() {
      lateinit var wrongScheduledReturn: OffenderTapScheduleIn
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
            tapApplication {
              tapScheduleOut {
                wrongScheduledReturn = tapScheduleIn()
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by linking the movement IN with the wrong scheduled return
         */
        entityManager.createNativeQuery(
          """
              update OFFENDER_EXTERNAL_MOVEMENTS oem
              set EVENT_ID = ${wrongScheduledReturn.eventId}
              where OFFENDER_BOOK_ID=${booking.bookingId} and MOVEMENT_SEQ=${movementIn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement OUT is scheduled
      webTestClient.getTemporaryAbsence(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduleOut.eventId)
        }

      // Sync movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender matches the sync
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence!!.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(movementIn.id.sequence)
        }

      // Reconciliation counts the IN movement as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    /*
     * This test demonstrates that a resync will not work but a normal sync will find that the movement is unscheduled.
     */
    @Test
    fun `movement IN is unscheduled but points at schedule OUT instead of IN - FAILS`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                scheduleIn = tapScheduleIn()
              }
            }
            movementIn = tapMovementIn()
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by pointing the movement IN at the schedule OUT
         */
        entityManager.createNativeQuery(
          """
              update OFFENDER_EXTERNAL_MOVEMENTS 
              set EVENT_ID = ${scheduleOut.eventId}
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ = ${movementIn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender is the same as the sync
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(movementIn.id.sequence)
        }

      // Reconciliation counts the IN movement as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Test
    fun `movement IN does not point at schedule OUT`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by pointing the movement IN at nothing
         */
        entityManager.createNativeQuery(
          """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set PARENT_EVENT_ID = null
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ = ${movementIn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement OUT is scheduled
      webTestClient.getTemporaryAbsence(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduleOut.eventId)
        }

      // Sync movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence!!.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(movementIn.id.sequence)
        }

      // Reconciliation counts OUT as scheduled and IN as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Test
    fun `movement IN points at a schedule IN that doesn't exist`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by pointing the movement IN at a schedule that doesn't exist
         */
        entityManager.createNativeQuery(
          """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set PARENT_EVENT_ID = 99999
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ = ${movementIn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement OUT is scheduled
      webTestClient.getTemporaryAbsence(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(movementApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduleOut.eventId)
        }

      // Sync movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence!!.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(movementIn.id.sequence)
        }

      // Reconciliation counts the OUT movement as scheduled and IN as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Test
    fun `unscheduled movement IN points at a schedule IN but it shouldn't`() {
      lateinit var wrongTempAbsenceReturn: OffenderTapMovementIn
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                orphanedScheduleReturn = tapScheduleIn()
              }
            }
            wrongTempAbsenceReturn = tapMovementIn()
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by pointing the unscheduled movement IN at the schedule IN
         */
        entityManager.createNativeQuery(
          """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set EVENT_ID = ${orphanedScheduleReturn.eventId}
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ = ${wrongTempAbsenceReturn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()

        // Reload the scheduled return to reflect the update
        orphanedScheduleReturn = scheduleInRepository.findByIdOrNull(orphanedScheduleReturn.eventId) ?: throw IllegalStateException("Failed to reload scheduled return movement - this should not happen")
      }

      // Sync movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = wrongTempAbsenceReturn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(wrongTempAbsenceReturn.id.sequence)
        }

      // Reconciliation counts the movement as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Test
    fun `unscheduled movement IN points at a schedule OUT and IN but it shouldn't`() {
      lateinit var wrongTempAbsenceReturn: OffenderTapMovementIn
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn()
              }
            }
            wrongTempAbsenceReturn = tapMovementIn()
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by pointing the unscheduled movement IN at an unrelated schedule IN and a schedule OUT that doesn't exist
         */
        entityManager.createNativeQuery(
          """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set EVENT_ID = ${scheduleIn.eventId}, PARENT_EVENT_ID=99999
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ = ${wrongTempAbsenceReturn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = wrongTempAbsenceReturn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(wrongTempAbsenceReturn.id.sequence)
        }

      // Reconciliation counts the OUT movement as scheduled and IN as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Test
    fun `scheduled movements points at a schedule without an application`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
          }
        }
      }

      repository.runInTransaction {
        /*
         * Corrupt the data by removing the application from the schedules
         */
        entityManager.createNativeQuery(
          """
              update OFFENDER_IND_SCHEDULES
              set OFFENDER_MOVEMENT_APP_ID = null
              where EVENT_ID in (${scheduleOut.eventId}, ${scheduleIn.eventId})
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement OUT is unscheduled
      webTestClient.getTemporaryAbsence(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
        }

      // Sync movement IN is unscheduled
      webTestClient.getTemporaryAbsenceReturnOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(movementApplicationId).isNull()
          assertThat(scheduledTemporaryAbsenceId).isNull()
          assertThat(scheduledTemporaryAbsenceReturnId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTemporaryAbsencesOk()
        .apply {
          assertThat(bookings[0].unscheduledTemporaryAbsences[0].sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(movementIn.id.sequence)
        }

      // Reconciliation counts the movements as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.scheduled.outCount).isEqualTo(0)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.outCount).isEqualTo(1)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Nested
    inner class CorruptScheduleType {
      @AfterEach
      fun `clear schedules`() {
        // We need a special clean up here because we've changed the discriminator on the schedules so they're no longer TAPs
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              delete from OFFENDER_IND_SCHEDULES
            """.trimIndent(),
          ).executeUpdate()
        }
      }

      @Test
      fun `scheduled movements point at a schedule that is not a TAP`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = tapApplication {
                scheduleOut = tapScheduleOut {
                  movementOut = tapMovementOut()
                  scheduleIn = tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }

        repository.runInTransaction {
          /*
           * Corrupt the data by making the schedule a non-TAP
           */
          entityManager.createNativeQuery(
            """
              update OFFENDER_IND_SCHEDULES
              set EVENT_TYPE = 'TRN'
              where EVENT_ID in (${scheduleOut.eventId}, ${scheduleIn.eventId})
            """.trimIndent(),
          ).executeUpdate()
        }

        // Sync movement OUT is unscheduled
        webTestClient.getTemporaryAbsence(movementSeq = movementOut.id.sequence)
          .apply {
            assertThat(movementApplicationId).isNull()
            assertThat(scheduledTemporaryAbsenceId).isNull()
          }

        // Sync movement IN is unscheduled
        webTestClient.getTemporaryAbsenceReturnOk(movementSeq = movementIn.id.sequence)
          .apply {
            assertThat(movementApplicationId).isNull()
            assertThat(scheduledTemporaryAbsenceId).isNull()
            assertThat(scheduledTemporaryAbsenceReturnId).isNull()
          }

        // Resync offender is same as for sync
        webTestClient.getOffenderTemporaryAbsencesOk()
          .apply {
            assertThat(bookings[0].unscheduledTemporaryAbsences[0].sequence).isEqualTo(movementOut.id.sequence)
            assertThat(bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence).isEqualTo(movementIn.id.sequence)
          }

        // Reconciliation counts the movements as unscheduled
        webTestClient.getOffenderSummaryOk(offender.nomsId)
          .apply {
            assertThat(movements.scheduled.outCount).isEqualTo(0)
            assertThat(movements.scheduled.inCount).isEqualTo(0)
            assertThat(movements.unscheduled.outCount).isEqualTo(1)
            assertThat(movements.unscheduled.inCount).isEqualTo(1)
          }
      }
    }

    private fun WebTestClient.getTemporaryAbsence(offenderNo: String = offender.nomsId, bookingId: Long = booking.bookingId, movementSeq: Int) = get()
      .uri("/movements/$offenderNo/temporary-absences/temporary-absence/$bookingId/$movementSeq")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBodyResponse<TemporaryAbsenceResponse>()

    private fun WebTestClient.getTemporaryAbsenceReturnOk(offenderNo: String = offender.nomsId, bookingId: Long = booking.bookingId, movementSeq: Int) = get()
      .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/$bookingId/$movementSeq")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
      .expectStatus().isOk
      .expectBodyResponse<TemporaryAbsenceReturnResponse>()

    private fun WebTestClient.getOffenderTemporaryAbsences(offenderNo: String = offender.nomsId) = get()
      .uri("/movements/$offenderNo/temporary-absences")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()

    private fun WebTestClient.getOffenderTemporaryAbsencesOk(offenderNo: String = offender.nomsId) = getOffenderTemporaryAbsences(offenderNo)
      .expectStatus().isOk
      .expectBodyResponse<OffenderTemporaryAbsencesResponse>()
  }

  @Nested
  inner class PrisonerMovementsReconciliationCounts {

    @Nested
    inner class HappyPath {

      @Test
      fun `should return basic counts`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking {
              tapApplication {
                tapScheduleOut {
                  tapMovementOut()
                  tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
              tapMovementOut()
              tapMovementIn()
            }
          }
        }

        webTestClient.getOffenderSummaryOk(offenderNo)
          .apply {
            assertThat(applications.count).isEqualTo(1)
            assertThat(scheduledOutMovements.count).isEqualTo(1)
            assertThat(movements.count).isEqualTo(4)
            assertThat(movements.scheduled.outCount).isEqualTo(1)
            assertThat(movements.scheduled.inCount).isEqualTo(1)
            assertThat(movements.unscheduled.outCount).isEqualTo(1)
            assertThat(movements.unscheduled.inCount).isEqualTo(1)
          }
      }

      @Test
      fun `should handle zero counts`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking()
          }
        }

        webTestClient.getOffenderSummaryOk(offenderNo)
          .apply {
            assertThat(applications.count).isEqualTo(0)
            assertThat(scheduledOutMovements.count).isEqualTo(0)
            assertThat(movements.count).isEqualTo(0)
            assertThat(movements.scheduled.outCount).isEqualTo(0)
            assertThat(movements.scheduled.inCount).isEqualTo(0)
            assertThat(movements.unscheduled.outCount).isEqualTo(0)
            assertThat(movements.unscheduled.inCount).isEqualTo(0)
          }
      }

      @Test
      fun `should handle multiple bookings and movements`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking {
              tapApplication()
            }
            booking {
              tapApplication {
                tapScheduleOut {
                  tapMovementOut()
                  tapScheduleIn()
                }
              }
            }
            booking {
              tapMovementOut()
              tapMovementIn()
              tapMovementOut()
            }
            booking()
          }
          offender(nomsId = "ANY") {
            booking {
              tapApplication()
            }
          }
        }

        webTestClient.getOffenderSummaryOk(offenderNo)
          .apply {
            assertThat(applications.count).isEqualTo(2)
            assertThat(scheduledOutMovements.count).isEqualTo(1)
            assertThat(movements.count).isEqualTo(4)
            assertThat(movements.scheduled.outCount).isEqualTo(1)
            assertThat(movements.scheduled.inCount).isEqualTo(0)
            assertThat(movements.unscheduled.outCount).isEqualTo(2)
            assertThat(movements.unscheduled.inCount).isEqualTo(1)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found for unknown offender`() {
        webTestClient.getOffenderSummary(offenderNo = "UNKNOWN")
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class DeletedScheduleOut {
      @Test
      fun `should ignore scheduled movements where there is no application`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking {
              tapApplication {
                orphanedSchedule = tapScheduleOut()
              }
              tapApplication {
                tapScheduleOut {
                  tapMovementOut()
                  tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
            }
          }
        }

        repository.runInTransaction {
          /*
           * Corrupt the data by nulling one of the schedule's temporary absence application - such data exists in NOMIS but we ignore them from the migration and reconciliation
           */
          entityManager.createQuery(
            """
            update OffenderTapScheduleOut ost
            set ost.tapApplication = null
            where eventId = ${orphanedSchedule.eventId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderSummaryOk(offenderNo)
          .apply {
            assertThat(applications.count).isEqualTo(2)
            // We don't count the orphaned schedule in the reconciliation
            assertThat(scheduledOutMovements.count).isEqualTo(1)
            assertThat(movements.count).isEqualTo(2)
          }
      }
    }

    @Nested
    inner class LinkFromMovementInToScheduleOutBroken {
      @Test
      fun `should handle case where parent event ID missing from movement IN`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking {
              tapApplication {
                tapScheduleOut {
                  tapMovementOut()
                  tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
              tapApplication {
                tapScheduleOut {
                  tapMovementOut()
                  tapScheduleIn {
                    movementIn = tapMovementIn()
                  }
                }
              }
            }
          }
        }

        repository.runInTransaction {
          /*
           * Corrupt the data by nulling the movement IN parent event ID (which should point at the schedule OUT)
           */
          entityManager.createQuery(
            """
            update OffenderTapMovementIn otar
            set otar.tapScheduleOut = null
            where otar.id.offenderBooking.id = ${movementIn.id.offenderBooking.bookingId}
            and otar.id.sequence = ${movementIn.id.sequence}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderSummaryOk(offenderNo)
          .apply {
            assertThat(applications.count).isEqualTo(2)
            assertThat(scheduledOutMovements.count).isEqualTo(2)
            // Includes the movement IN that doesn't have a parent event id
            assertThat(movements.count).isEqualTo(4)
          }
      }
    }

    @Nested
    inner class LinkFromScheduleOutToApplicationMissing {
      @Test
      fun `should handle case where movement application missing from schedule OUT`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9797LL") {
            booking {
              application = tapApplication {
                orphanedSchedule = tapScheduleOut {
                  tapMovementOut()
                  orphanedScheduleReturn = tapScheduleIn {
                    tapMovementIn()
                  }
                }
              }
            }
          }
        }

        repository.runInTransaction {
          /*
           * Corrupt the data by nulling the application on the schedule OUT and deleting the application
           */
          entityManager.createNativeQuery(
            """
            update OFFENDER_IND_SCHEDULES ois set OFFENDER_MOVEMENT_APP_ID = null
            where ois.EVENT_ID = ${orphanedSchedule.eventId}
            """.trimIndent(),
          ).executeUpdate()

          entityManager.createNativeQuery(
            """
            delete from OFFENDER_MOVEMENT_APPS oma
            where oma.OFFENDER_MOVEMENT_APP_ID = ${application.tapApplicationId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderSummaryOk("A9797LL")
          .apply {
            assertThat(applications.count).isEqualTo(0)
            assertThat(scheduledOutMovements.count).isEqualTo(0)
            assertThat(movements.scheduled.outCount).isEqualTo(0)
            assertThat(movements.scheduled.inCount).isEqualTo(0)
          }
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/summary")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/summary")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/summary")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("GET /movements/booking/{bookingId}/temporary-absences")
  inner class GetTemporaryAbsencesByBooking {
    @Test
    fun `should return unauthorized for missing token`() {
      webTestClient.get()
        .uri("/movements/booking/12345/temporary-absences")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/booking/12345/temporary-absences")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/booking/12345/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found for unknown booking id`() {
      webTestClient.get()
        .uri("/movements/booking/12345/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Offender booking 12345 not found")
        }
    }

    @Test
    fun `should return only the requested booking`() {
      lateinit var secondBooking: OffenderBooking
      lateinit var firstApplication: OffenderTapApplication
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            firstApplication = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn()
                }
              }
            }
            unscheduledMovementOut = tapMovementOut()
            unscheduledMovementIn = tapMovementIn()
          }
          secondBooking = booking {
            application = tapApplication {
              tapScheduleOut()
            }
          }
        }
      }

      webTestClient.getBookingTemporaryAbsences(booking.bookingId)
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(activeBooking).isTrue()
          assertThat(latestBooking).isTrue()
          assertThat(temporaryAbsenceApplications).hasSize(1)
          assertThat(temporaryAbsenceApplications[0].movementApplicationId).isEqualTo(firstApplication.tapApplicationId)
          assertThat(temporaryAbsenceApplications[0].absences).hasSize(1)
          assertThat(temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence?.eventId).isEqualTo(scheduleOut.eventId)
          assertThat(temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn?.eventId).isEqualTo(scheduleIn.eventId)
          assertThat(temporaryAbsenceApplications[0].absences[0].temporaryAbsence?.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn?.sequence).isEqualTo(movementIn.id.sequence)
          assertThat(unscheduledTemporaryAbsences).extracting<Int> { it.sequence }.containsExactly(unscheduledMovementOut.id.sequence)
          assertThat(unscheduledTemporaryAbsenceReturns).extracting<Int> { it.sequence }.containsExactly(unscheduledMovementIn.id.sequence)
        }

      webTestClient.getBookingTemporaryAbsences(secondBooking.bookingId)
        .apply {
          assertThat(bookingId).isEqualTo(secondBooking.bookingId)
          assertThat(temporaryAbsenceApplications).hasSize(1)
          assertThat(unscheduledTemporaryAbsences).isEmpty()
          assertThat(unscheduledTemporaryAbsenceReturns).isEmpty()
        }
    }
  }

  private fun WebTestClient.getOffenderSummary(offenderNo: String) = get()
    .uri("/movements/$offenderNo/temporary-absences/summary")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getOffenderSummaryOk(offenderNo: String) = getOffenderSummary(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<OffenderTemporaryAbsenceSummaryResponse>()

  private fun anUpsertApplicationRequest(
    id: Long? = null,
    toAddresses: List<UpsertTemporaryAbsenceAddress> = listOf(UpsertTemporaryAbsenceAddress(addressText = "some street")),
  ) = UpsertTemporaryAbsenceApplicationRequest(
    movementApplicationId = id,
    eventSubType = "C5",
    applicationDate = twoDaysAgo.toLocalDate(),
    fromDate = twoDaysAgo.toLocalDate(),
    releaseTime = twoDaysAgo,
    toDate = yesterday.toLocalDate(),
    returnTime = yesterday,
    applicationType = "SINGLE",
    applicationStatus = "APP-SCH",
    escortCode = "L",
    transportType = "VAN",
    comment = "Some comment application",
    prisonId = "LEI",
    contactPersonName = "Derek",
    temporaryAbsenceType = "RR",
    temporaryAbsenceSubType = "RDR",
    toAddresses = toAddresses,
  )

  private fun WebTestClient.upsertApplicationOk(request: UpsertTemporaryAbsenceApplicationRequest = anUpsertApplicationRequest()) = upsertApplication(request)
    .isOk
    .expectBodyResponse<UpsertTemporaryAbsenceApplicationResponse>()

  private fun WebTestClient.upsertApplicationBadRequest(request: UpsertTemporaryAbsenceApplicationRequest = anUpsertApplicationRequest()) = upsertApplication(request)
    .isBadRequest

  private fun WebTestClient.upsertApplicationBadRequestUnknown(request: UpsertTemporaryAbsenceApplicationRequest = anUpsertApplicationRequest()) = upsertApplicationBadRequest(request)
    .expectBody().jsonPath("userMessage").value<String> {
      assertThat(it).contains("UNKNOWN").contains("invalid")
    }

  private fun WebTestClient.upsertApplication(
    request: UpsertTemporaryAbsenceApplicationRequest = anUpsertApplicationRequest(),
    offenderNo: String = offender.nomsId,
  ) = put()
    .uri("/movements/$offenderNo/temporary-absences/application")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()

  private fun WebTestClient.getBookingTemporaryAbsences(bookingId: Long) = get()
    .uri("/movements/booking/$bookingId/temporary-absences")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<BookingTemporaryAbsences>()

  private fun anUpsertTemporaryAbsenceRequest(
    movementApplicationId: Long? = null,
    eventId: Long? = null,
    returnEventStatus: String? = null,
    eventStatus: String = "SCH",
    toAddress: UpsertTemporaryAbsenceAddress = UpsertTemporaryAbsenceAddress(id = offenderAddress.addressId),
    comment: String = "Some comment scheduled temporary absence",
  ) = UpsertScheduledTemporaryAbsenceRequest(
    eventId = eventId,
    movementApplicationId = movementApplicationId ?: application.tapApplicationId,
    eventDate = twoDaysAgo.toLocalDate(),
    startTime = twoDaysAgo,
    eventSubType = "C5",
    eventStatus = eventStatus,
    comment = comment,
    escort = "L",
    fromPrison = "LEI",
    toAgency = "HAZLWD",
    transportType = "VAN",
    returnDate = yesterday.toLocalDate(),
    returnTime = yesterday,
    toAddress = toAddress,
    applicationDate = twoDaysAgo,
    applicationTime = twoDaysAgo,
    returnEventStatus = returnEventStatus,
  )

  private fun WebTestClient.upsertScheduledTemporaryAbsenceOk(
    request: UpsertScheduledTemporaryAbsenceRequest = anUpsertTemporaryAbsenceRequest(application.tapApplicationId),
  ) = upsertScheduledTemporaryAbsence(request)
    .isOk
    .expectBodyResponse<UpsertScheduledTemporaryAbsenceResponse>()

  private fun WebTestClient.upsertScheduledTemporaryAbsenceBadRequest(
    request: UpsertScheduledTemporaryAbsenceRequest = anUpsertTemporaryAbsenceRequest(application.tapApplicationId),
  ) = upsertScheduledTemporaryAbsence(request)
    .isBadRequest

  private fun WebTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(
    request: UpsertScheduledTemporaryAbsenceRequest = anUpsertTemporaryAbsenceRequest(application.tapApplicationId),
  ) = upsertScheduledTemporaryAbsenceBadRequest(request)
    .expectBody().jsonPath("userMessage").value<String> {
      assertThat(it).contains("UNKNOWN").contains("invalid")
    }

  private fun WebTestClient.upsertScheduledTemporaryAbsence(
    request: UpsertScheduledTemporaryAbsenceRequest = anUpsertTemporaryAbsenceRequest(application.tapApplicationId),
    offenderNo: String = offender.nomsId,
  ) = put()
    .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()
}
