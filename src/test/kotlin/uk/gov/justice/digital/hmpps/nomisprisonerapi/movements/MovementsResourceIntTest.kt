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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementsService.Companion.MAX_TAP_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.profiledetails.roundToNearestSecond
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MovementsResourceIntTest(
  @Autowired val nomisDataBuilder: NomisDataBuilder,
  @Autowired val repository: Repository,
  @Autowired val applicationRepository: OffenderMovementApplicationRepository,
  @Autowired val scheduledTemporaryAbsenceRepository: OffenderScheduledTemporaryAbsenceRepository,
  @Autowired val scheduledTemporaryAbsenceReturnRepository: OffenderScheduledTemporaryAbsenceReturnRepository,
  @Autowired val temporaryAbsenceRepository: OffenderTemporaryAbsenceRepository,
  @Autowired val temporaryAbsenceReturnRepository: OffenderTemporaryAbsenceReturnRepository,
  @Autowired val offenderExternalMovementRepository: OffenderExternalMovementRepository,
  @Autowired val corporateAddressRepository: CorporateAddressRepository,
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var offenderAddress: OffenderAddress
  private lateinit var booking: OffenderBooking
  private lateinit var application: OffenderMovementApplication
  private lateinit var scheduledTempAbsence: OffenderScheduledTemporaryAbsence
  private lateinit var scheduledTempAbsenceReturn: OffenderScheduledTemporaryAbsenceReturn
  private lateinit var tempAbsence: OffenderTemporaryAbsence
  private lateinit var tempAbsenceReturn: OffenderTemporaryAbsenceReturn
  private lateinit var unscheduledTemporaryAbsence: OffenderTemporaryAbsence
  private lateinit var unscheduledTemporaryAbsenceReturn: OffenderTemporaryAbsenceReturn

  private val offenderNo = "D6347ED"
  private val today: LocalDateTime = LocalDateTime.now().roundToNearestSecond()
  private val yesterday: LocalDateTime = today.minusDays(1)
  private val twoDaysAgo: LocalDateTime = today.minusDays(2)
  private lateinit var orphanedSchedule: OffenderScheduledTemporaryAbsence
  private lateinit var orphanedScheduleReturn: OffenderScheduledTemporaryAbsenceReturn

  @AfterEach
  fun `tear down`() {
    // This must be removed before the offender booking due to a foreign key constraint (Hibernate is no longer managing this entity)
    if (this::orphanedSchedule.isInitialized) {
      scheduledTemporaryAbsenceRepository.delete(orphanedSchedule)
    }
    if (this::orphanedScheduleReturn.isInitialized) {
      scheduledTemporaryAbsenceReturnRepository.delete(orphanedScheduleReturn)
    }
    if (this::offender.isInitialized) {
      repository.delete(offender)
    }
    corporateAddressRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences")
  inner class GetTemporaryAbsencesAndMovements {

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
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication(
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
              temporaryAbsenceType = "RR",
              temporaryAbsenceSubType = "RDR",
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].movementApplicationId").isEqualTo(application.movementApplicationId)
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventId").isEqualTo(scheduledTempAbsence.eventId)
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(
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
        agencyLocation(description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement(
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.sequence").isEqualTo(tempAbsence.id.sequence)
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement(
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
        agencyLocation(description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement(
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
        agencyLocation(description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(
                toAddress = agencyAddress,
              ) {
                tempAbsence = externalMovement(
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                scheduledTempAbsenceReturn = scheduledReturn(
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduledTempAbsenceReturn.eventId)
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement()

                scheduledTempAbsenceReturn = scheduledReturn {
                  tempAbsenceReturn = externalMovement(
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.sequence").isEqualTo(tempAbsenceReturn.id.sequence)
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement()

                scheduledTempAbsenceReturn = scheduledReturn {
                  tempAbsenceReturn = externalMovement(
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
        agencyLocation(description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement()

                scheduledTempAbsenceReturn = scheduledReturn {
                  tempAbsenceReturn = externalMovement(
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
        agencyLocation(description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement(
                  toAddress = agencyAddress,
                )
                scheduledTempAbsenceReturn = scheduledReturn {
                  tempAbsenceReturn = externalMovement(
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
        agencyLocation(description = "Big Hospital") {
          agencyAddress = address(postcode = "LS3 3AA")
        }
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(
                toAddress = agencyAddress,
              ) {
                tempAbsence = externalMovement(
                  toAddress = null,
                )
                scheduledTempAbsenceReturn = scheduledReturn {
                  tempAbsenceReturn = externalMovement(
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
    fun `should retrieve unscheduled temporary absence external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            unscheduledTemporaryAbsence = temporaryAbsence(
              date = yesterday,
              movementReason = "C5",
              escort = "U",
              escortText = "SE",
              comment = "Tap OUT comment",
              fromPrison = "LEI",
              toAgency = "HAZLWD",
              toCity = SHEFFIELD,
              toAddress = offenderAddress,
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
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].sequence").isEqualTo(unscheduledTemporaryAbsence.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].escort").isEqualTo("U")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].commentText").isEqualTo("Tap OUT comment")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toAddressPostcode").isEqualTo("S1 1AA")
    }

    @Test
    fun `should retrieve city description if no address on unscheduled temporary absence`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            unscheduledTemporaryAbsence = temporaryAbsence(
              toCity = SHEFFIELD,
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
    fun `should retrieve unscheduled temporary absences return external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address(postcode = "S1 1AA")
          booking = booking {
            unscheduledTemporaryAbsence = temporaryAbsence()
            unscheduledTemporaryAbsenceReturn = temporaryAbsenceReturn(
              date = today,
              movementReason = "C5",
              escort = "U",
              escortText = "SE",
              comment = "Tap IN comment",
              toPrison = "LEI",
              fromAgency = "HAZLWD",
              fromCity = SHEFFIELD,
              fromAddress = offenderAddress,
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
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence").isEqualTo(unscheduledTemporaryAbsenceReturn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementDate").isEqualTo("${today.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementTime").value<String> {
          assertThat(it).startsWith("${today.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].escort").isEqualTo("U")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].toPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].commentText").isEqualTo("Tap IN comment")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromAddressPostcode").isEqualTo("S1 1AA")
    }

    @Test
    fun `should retrieve city description from unscheduled temporary absence return`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            unscheduledTemporaryAbsence = temporaryAbsence()
            unscheduledTemporaryAbsenceReturn = temporaryAbsenceReturn(
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement()
                scheduledTempAbsenceReturn = scheduledReturn {
                  tempAbsenceReturn = externalMovement()
                }
              }
            }
            unscheduledTemporaryAbsence = temporaryAbsence()
            unscheduledTemporaryAbsenceReturn = temporaryAbsenceReturn()
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].movementApplicationId").isEqualTo(application.movementApplicationId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventId").isEqualTo(scheduledTempAbsence.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.sequence").isEqualTo(tempAbsence.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduledTempAbsenceReturn.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.sequence").isEqualTo(tempAbsenceReturn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].sequence").isEqualTo(unscheduledTemporaryAbsence.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence").isEqualTo(unscheduledTemporaryAbsenceReturn.id.sequence)
    }

    @Test
    fun `should take return time from the application if not on the schedule`() {
      val tomorrow = LocalDateTime.now().plusDays(1).withNano(0)

      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = temporaryAbsenceApplication(returnTime = tomorrow) {
              scheduledTempAbsence = scheduledTemporaryAbsence()
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
            update OffenderScheduledTemporaryAbsence ost
            set ost.returnTime = null
            where eventId = ${scheduledTempAbsence.eventId}
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
            temporaryAbsenceApplication(toAddress = offenderAddress) {
              scheduledTemporaryAbsence(toAddress = offenderAddress) {
                scheduledReturn()
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(
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
    lateinit var mergedApplication: OffenderMovementApplication
    lateinit var mergedScheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence
    lateinit var mergedScheduledTemporaryAbsenceReturn: OffenderScheduledTemporaryAbsenceReturn
    lateinit var mergedTemporaryAbsence: OffenderTemporaryAbsence
    lateinit var mergedTemporaryAbsenceReturn: OffenderTemporaryAbsenceReturn

    lateinit var scheduledTemporaryAbsence2: OffenderScheduledTemporaryAbsence
    lateinit var scheduledTemporaryAbsenceReturn2: OffenderScheduledTemporaryAbsenceReturn
    lateinit var mergedScheduledTemporaryAbsence2: OffenderScheduledTemporaryAbsence
    lateinit var mergedScheduledTemporaryAbsenceReturn2: OffenderScheduledTemporaryAbsenceReturn
    lateinit var mergedTemporaryAbsence2: OffenderTemporaryAbsence
    lateinit var mergedTemporaryAbsenceReturn2: OffenderTemporaryAbsenceReturn

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
            mergedApplication = temporaryAbsenceApplication {
              mergedScheduledTemporaryAbsence = scheduledTemporaryAbsence(eventDate = today) {
                mergedTemporaryAbsence = externalMovement()
                mergedScheduledTemporaryAbsenceReturn = scheduledReturn(eventDate = today) {
                  mergedTemporaryAbsenceReturn = externalMovement()
                }
              }
              mergedScheduledTemporaryAbsence2 = scheduledTemporaryAbsence(eventDate = tomorrow) {
                mergedTemporaryAbsence2 = externalMovement()
                mergedScheduledTemporaryAbsenceReturn2 = scheduledReturn(eventDate = tomorrow) {
                  mergedTemporaryAbsenceReturn2 = externalMovement()
                }
              }
            }
            release(yesterday)
          }
          // This is the latest booking
          booking = booking(bookingSequence = 1) {
            receive(yesterday)
            // these are the only details copied from the merged booking during the merge
            application = temporaryAbsenceApplication {
              // make the schedules out of order to prove that date handling works
              scheduledTemporaryAbsence2 = scheduledTemporaryAbsence(eventDate = tomorrow) {
                scheduledTemporaryAbsenceReturn2 = scheduledReturn(eventDate = tomorrow)
              }
              scheduledTempAbsence = scheduledTemporaryAbsence(eventDate = today) {
                scheduledTempAbsenceReturn = scheduledReturn(eventDate = today)
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
            update OffenderScheduledTemporaryAbsenceReturn ostr
            set ostr.scheduledTemporaryAbsence = (from OffenderScheduledTemporaryAbsence where eventId = ${mergedScheduledTemporaryAbsence.eventId}),
            ostr.temporaryAbsenceApplication = (from OffenderMovementApplication  where movementApplicationId = ${application.movementApplicationId})
            where eventId = ${scheduledTempAbsenceReturn.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Also corrupt the same data for the 2nd scheduled absence from the application to test repeating applications
        entityManager.createQuery(
          """
            update OffenderScheduledTemporaryAbsenceReturn ostr
            set ostr.scheduledTemporaryAbsence = (from OffenderScheduledTemporaryAbsence where eventId = ${mergedScheduledTemporaryAbsence2.eventId}),
            ostr.temporaryAbsenceApplication = (from OffenderMovementApplication  where movementApplicationId = ${application.movementApplicationId})
            where eventId = ${scheduledTemporaryAbsenceReturn2.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Also corrupt the merged movements by removing the link to the schedules in the same way the NOMIS merge process does
        entityManager.createQuery(
          """
            update OffenderTemporaryAbsence ota
            set ota.scheduledTemporaryAbsence = null
            where id.offenderBooking.id = ${mergedBooking.bookingId}
            and id.sequence in (${mergedTemporaryAbsence.id.sequence}, ${mergedTemporaryAbsence2.id.sequence})
          """.trimIndent(),
        ).executeUpdate()
        entityManager.createQuery(
          """
            update OffenderTemporaryAbsenceReturn otar
            set otar.scheduledTemporaryAbsenceReturn = null
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
          assertThat(book.temporaryAbsenceApplications.size).isEqualTo(1)
          assertThat(application.movementApplicationId).isEqualTo(mergedApplication.movementApplicationId)
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
          assertThat(book.temporaryAbsenceApplications.size).isEqualTo(1)
          assertThat(application.movementApplicationId).isEqualTo(application.movementApplicationId)
          with(application.absences[1]) {
            assertThat(scheduledTemporaryAbsence!!.eventId).isEqualTo(scheduledTempAbsence.eventId)
            assertThat(temporaryAbsence).isNull()
            assertThat(scheduledTemporaryAbsenceReturn!!.eventId).isEqualTo(scheduledTempAbsenceReturn.eventId)
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
            temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(eventDate = today.toLocalDate()) {
                externalMovement()
                orphanedScheduleReturn = scheduledReturn(eventDate = today.toLocalDate()) {
                  externalMovement()
                }
              }
            }
            // Another TAP that should be included in the migration
            temporaryAbsenceApplication {
              scheduledTemporaryAbsence(eventDate = yesterday.toLocalDate()) {
                externalMovement()
                scheduledReturn(eventDate = yesterday.toLocalDate()) {
                  externalMovement()
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
            where EVENT_ID = ${scheduledTempAbsence.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Reload the scheduled return to reflect the update
        orphanedScheduleReturn = scheduledTemporaryAbsenceReturnRepository.findByIdOrNull(orphanedScheduleReturn.eventId) ?: throw IllegalStateException("Failed to reload scheduled return movement - this should not happen")
      }
    }

    @Test
    fun `should not include the TAP with deleted schedule OUT`() {
      webTestClient.getTapsForMigration()
        .apply {
          val book = bookings.first()
          assertThat(book.temporaryAbsenceApplications.size).isEqualTo(2)
          // The TAP with the deleted scheduled absence is not included in the migration
          assertThat(book.temporaryAbsenceApplications[0].absences).isEmpty()
          // The control TAP is migrated
          with(book.temporaryAbsenceApplications[1].absences.first()) {
            assertThat(scheduledTemporaryAbsence).isNotNull()
            assertThat(temporaryAbsence).isNotNull()
            assertThat(scheduledTemporaryAbsenceReturn).isNotNull()
            assertThat(temporaryAbsenceReturn).isNotNull()
          }
        }
    }

    @Test
    fun `reconciliation should not include the TAP with deleted scheduled OUT`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(applications.count).isEqualTo(2)
          assertThat(scheduledOutMovements.count).isEqualTo(1)
          assertThat(movements.count).isEqualTo(2)
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
            temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence(eventDate = today.toLocalDate()) {
                externalMovement()
                // We used to pick up this scheduled IN record - we should pick up the next one with an external movement
                scheduledReturn()
                scheduledTempAbsenceReturn = scheduledReturn(eventDate = today.toLocalDate()) {
                  externalMovement()
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
            assertThat(scheduledTemporaryAbsenceReturn!!.eventId).isEqualTo(scheduledTempAbsenceReturn.eventId)
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
    private lateinit var application2: OffenderMovementApplication
    private lateinit var scheduledTempAbsence2: OffenderScheduledTemporaryAbsence
    private lateinit var scheduledTempAbsenceReturn2: OffenderScheduledTemporaryAbsenceReturn
    private lateinit var tempAbsence2: OffenderTemporaryAbsence
    private lateinit var tempAbsenceReturn2: OffenderTemporaryAbsenceReturn
    private lateinit var unscheduledTemporaryAbsence2: OffenderTemporaryAbsence
    private lateinit var unscheduledTemporaryAbsenceReturn2: OffenderTemporaryAbsenceReturn

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement()
                scheduledTempAbsenceReturn = scheduledReturn {
                  tempAbsenceReturn = externalMovement()
                }
              }
            }
            unscheduledTemporaryAbsence = temporaryAbsence()
            unscheduledTemporaryAbsenceReturn = temporaryAbsenceReturn()
          }
          booking2 = booking {
            application2 = temporaryAbsenceApplication {
              scheduledTempAbsence2 = scheduledTemporaryAbsence {
                tempAbsence2 = externalMovement()
                scheduledTempAbsenceReturn2 = scheduledReturn {
                  tempAbsenceReturn2 = externalMovement()
                }
              }
            }
            unscheduledTemporaryAbsence2 = temporaryAbsence()
            unscheduledTemporaryAbsenceReturn2 = temporaryAbsenceReturn()
          }
        }
      }
    }

    @Test
    fun `should return all IDs`() {
      webTestClient.getTapIds()
        .apply {
          assertThat(applicationIds).containsExactlyInAnyOrder(
            application.movementApplicationId,
            application2.movementApplicationId,
          )
          assertThat(scheduleIds).containsExactlyInAnyOrder(
            scheduledTempAbsence.eventId,
            scheduledTempAbsence2.eventId,
            scheduledTempAbsenceReturn.eventId,
            scheduledTempAbsenceReturn2.eventId,
          )
          assertThat(scheduledMovementOutIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(tempAbsence.id.offenderBooking.bookingId, tempAbsence.id.sequence),
            OffenderTemporaryAbsenceId(tempAbsence2.id.offenderBooking.bookingId, tempAbsence2.id.sequence),
          )
          assertThat(scheduledMovementInIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(tempAbsenceReturn.id.offenderBooking.bookingId, tempAbsenceReturn.id.sequence),
            OffenderTemporaryAbsenceId(tempAbsenceReturn2.id.offenderBooking.bookingId, tempAbsenceReturn2.id.sequence),
          )
          assertThat(unscheduledMovementOutIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(unscheduledTemporaryAbsence.id.offenderBooking.bookingId, unscheduledTemporaryAbsence.id.sequence),
            OffenderTemporaryAbsenceId(unscheduledTemporaryAbsence2.id.offenderBooking.bookingId, unscheduledTemporaryAbsence2.id.sequence),
          )
          assertThat(unscheduledMovementInIds).containsExactlyInAnyOrder(
            OffenderTemporaryAbsenceId(unscheduledTemporaryAbsenceReturn.id.offenderBooking.bookingId, unscheduledTemporaryAbsenceReturn.id.sequence),
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

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication(
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
              temporaryAbsenceType = "RR",
              temporaryAbsenceSubType = "RDR",
            ) {
              scheduledTemporaryAbsence {
                externalMovement()
                scheduledReturn {
                  externalMovement()
                }
              }
            }
            temporaryAbsence()
            temporaryAbsenceReturn()
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
          application.movementApplicationId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse<TemporaryAbsenceApplicationResponse>()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(movementApplicationId).isEqualTo(application.movementApplicationId)
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
          assertThat(contactPersonName).isEqualTo("Derek")
          assertThat(temporaryAbsenceType).isEqualTo("RR")
          assertThat(temporaryAbsenceSubType).isEqualTo("RDR")
        }
    }
  }

  @Nested
  @DisplayName("PUT /movements/{offenderNo}/temporary-absences/application")
  inner class UpsertTemporaryAbsenceApplication {

    private fun aRequest(id: Long? = null) = UpsertTemporaryAbsenceApplicationRequest(
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
    )

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
            booking = booking()
          }
        }
      }

      @Test
      fun `should create application`() {
        webTestClient.upsertApplicationOk()
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
                assertThat(toAddress).isNull()
                assertThat(toAddressOwnerClass).isNull()
                assertThat(contactPersonName).isEqualTo("Derek")
                assertThat(temporaryAbsenceType?.code).isEqualTo("RR")
                assertThat(temporaryAbsenceSubType?.code).isEqualTo("RDR")
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        // comment is 300 long
        webTestClient.upsertApplicationOk(aRequest().copy(comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(movementApplicationId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
              }
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
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(eventSubType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid application type`() {
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(applicationType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid application status`() {
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(applicationStatus = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid escort code`() {
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(escortCode = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid transport type`() {
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(transportType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid prison ID`() {
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(prisonId = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid temporary absence type`() {
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(temporaryAbsenceType = "UNKNOWN"))
        }

        @Test
        fun `should return bad request for invalid temporary absence sub type`() {
          webTestClient.upsertApplicationBadRequestUnknown(aRequest().copy(temporaryAbsenceSubType = "UNKNOWN"))
        }
      }

      @Nested
      inner class Security {
        @Test
        fun `should return unauthorised for missing token`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/temporary-absences/application")
            .bodyValue(aRequest())
            .exchange()
            .expectStatus().isUnauthorized
        }

        @Test
        fun `should return forbidden for missing role`() {
          webTestClient.post()
            .uri("/movements/$offenderNo/temporary-absences/application")
            .headers(setAuthorisation(roles = listOf()))
            .bodyValue(aRequest())
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
              application = temporaryAbsenceApplication(
                eventSubType = "C5",
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
                toAddress = null,
                contactPersonName = "Adam",
                temporaryAbsenceType = "PP",
                temporaryAbsenceSubType = "ROR",
              ) {
                scheduledTemporaryAbsence(
                  toAddress = offenderAddress,
                  toAgency = "HAZLWD",
                ) {
                  externalMovement()
                  scheduledReturn {
                    externalMovement()
                  }
                }
              }
              temporaryAbsence()
              temporaryAbsenceReturn()
            }
          }
        }
      }

      @Test
      fun `should update application`() {
        webTestClient.upsertApplicationOk(request = aRequest(id = application.movementApplicationId))
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
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddressOwnerClass).isEqualTo("OFF")
                assertThat(contactPersonName).isEqualTo("Derek")
                assertThat(temporaryAbsenceType?.code).isEqualTo("RR")
                assertThat(temporaryAbsenceSubType?.code).isEqualTo("RDR")
              }
            }
          }
      }

      @Test
      fun `should return not found if unknown application id sent`() {
        webTestClient.upsertApplication(request = aRequest(id = 9999))
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }
    }

    private fun WebTestClient.upsertApplication(
      request: UpsertTemporaryAbsenceApplicationRequest = aRequest(),
      offenderNo: String = offender.nomsId,
    ) = put()
      .uri("/movements/$offenderNo/temporary-absences/application")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.upsertApplicationOk(request: UpsertTemporaryAbsenceApplicationRequest = aRequest()) = upsertApplication(request)
      .isOk
      .expectBodyResponse<UpsertTemporaryAbsenceApplicationResponse>()

    private fun WebTestClient.upsertApplicationBadRequest(request: UpsertTemporaryAbsenceApplicationRequest = aRequest()) = upsertApplication(request)
      .isBadRequest

    private fun WebTestClient.upsertApplicationBadRequestUnknown(request: UpsertTemporaryAbsenceApplicationRequest = aRequest()) = upsertApplicationBadRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
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
              application = temporaryAbsenceApplication(
                temporaryAbsenceType = "RR",
                temporaryAbsenceSubType = "RDR",
              ) {
                scheduledTempAbsence = scheduledTemporaryAbsence(
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
                  tempAbsence = externalMovement()
                  scheduledTempAbsenceReturn = scheduledReturn(
                    eventStatus = "SCH",
                  ) {
                    tempAbsenceReturn = externalMovement()
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
            scheduledTempAbsence.eventId,
          )
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<ScheduledTemporaryAbsenceResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(movementApplicationId).isEqualTo(application.movementApplicationId)
            assertThat(eventId).isEqualTo(scheduledTempAbsence.eventId)
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
              application = temporaryAbsenceApplication {
                scheduledTempAbsence = scheduledTemporaryAbsence(
                  toAddress = corporateAddress,
                ) {
                  tempAbsence = externalMovement()
                  scheduledTempAbsenceReturn = scheduledReturn {
                    tempAbsenceReturn = externalMovement()
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
            scheduledTempAbsence.eventId,
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
          agencyLocation(
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
              application = temporaryAbsenceApplication {
                scheduledTempAbsence = scheduledTemporaryAbsence(
                  toAddress = agencyAddress,
                ) {
                  tempAbsence = externalMovement()
                  scheduledTempAbsenceReturn = scheduledReturn {
                    tempAbsenceReturn = externalMovement()
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
            scheduledTempAbsence.eventId,
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
    inner class Validation {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = temporaryAbsenceApplication {
                scheduledTempAbsence = scheduledTemporaryAbsence {
                  tempAbsence = externalMovement()
                  scheduledTempAbsenceReturn = scheduledReturn {
                    tempAbsenceReturn = externalMovement()
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement()
                scheduledTempAbsenceReturn = scheduledReturn(
                  eventDate = yesterday.toLocalDate(),
                  startTime = yesterday,
                  eventSubType = "C5",
                  eventStatus = "SCH",
                  comment = "Scheduled temporary absence return",
                  escort = "L",
                  fromAgency = "HAZLWD",
                  toPrison = "LEI",
                ) {
                  tempAbsenceReturn = externalMovement()
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
          scheduledTempAbsenceReturn.eventId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse<ScheduledTemporaryAbsenceReturnResponse>()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(movementApplicationId).isEqualTo(application.movementApplicationId)
          assertThat(eventId).isEqualTo(scheduledTempAbsenceReturn.eventId)
          assertThat(parentEventId).isEqualTo(scheduledTempAbsence.eventId)
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

    private fun anUpsertRequest(
      movementApplicationId: Long? = null,
      eventId: Long? = null,
      returnEventStatus: String? = null,
      eventStatus: String = "SCH",
      toAddress: UpsertTemporaryAbsenceAddress = UpsertTemporaryAbsenceAddress(id = offenderAddress.addressId),
      comment: String = "Some comment scheduled temporary absence",
    ) = UpsertScheduledTemporaryAbsenceRequest(
      eventId = eventId,
      movementApplicationId = movementApplicationId ?: application.movementApplicationId,
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

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class CreateSchedule {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = temporaryAbsenceApplication()
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
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
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
                assertThat(scheduledTemporaryAbsenceReturns).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should set the address on the movement application`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(temporaryAbsenceApplication.toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(temporaryAbsenceApplication.toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }

      @Test
      fun `should override the address on the movement application if it already exists`() {
        lateinit var existingAddress: OffenderAddress
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            existingAddress = address()
            offenderAddress = address()
            booking = booking {
              application = temporaryAbsenceApplication(toAddress = existingAddress)
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(temporaryAbsenceApplication.toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(temporaryAbsenceApplication.toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }

      @Test
      fun `should truncate comments`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            booking = booking {
              application = temporaryAbsenceApplication()
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk(
          // comment is 300 long
          anUpsertRequest(application.movementApplicationId, comment = "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(comment!!.length).isEqualTo(MAX_TAP_COMMENT_LENGTH)
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
              application = temporaryAbsenceApplication()
            }
          }
        }
      }

      @Test
      fun `should create scheduled temporary absence and its return schedule`() {
        val request = anUpsertRequest().copy(
          eventStatus = "COMP",
          returnEventStatus = "SCH",
        )

        webTestClient.upsertScheduledTemporaryAbsenceOk(request)
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(eventStatus.code).isEqualTo("COMP")
                with(scheduledTemporaryAbsenceReturns.first()) {
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
    inner class UpdateSchedule {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            offenderAddress = address()
            booking = booking {
              application = temporaryAbsenceApplication {
                scheduledTempAbsence = scheduledTemporaryAbsence()
              }
            }
          }
        }
      }

      @Test
      fun `should update scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(anUpsertRequest(eventId = scheduledTempAbsence.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
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
                assertThat(scheduledTemporaryAbsenceReturns).isEmpty()
              }
            }
          }
      }

      @Test
      fun `should update the address on the movement application`() {
        lateinit var existingAddress: OffenderAddress
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            existingAddress = address()
            offenderAddress = address()
            booking = booking {
              application = temporaryAbsenceApplication(toAddress = existingAddress) {
                scheduledTempAbsence = scheduledTemporaryAbsence(toAddress = existingAddress)
              }
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk(request = anUpsertRequest(eventId = scheduledTempAbsence.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(temporaryAbsenceApplication.toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(temporaryAbsenceApplication.toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
              }
            }
          }
      }

      @Test
      fun `should update application release and return time for single schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            booking = booking {
              application = temporaryAbsenceApplication(releaseTime = yesterday, returnTime = today) {
                scheduledTempAbsence = scheduledTemporaryAbsence(startTime = yesterday, returnTime = today)
              }
            }
          }
        }

        // Updates schedule so startTime=twoDaysAgo and returnTime=yesterday
        webTestClient.upsertScheduledTemporaryAbsenceOk(request = anUpsertRequest(eventId = scheduledTempAbsence.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.movementApplicationId)!!) {
                assertThat(releaseTime).isEqualTo(twoDaysAgo)
                assertThat(returnTime).isEqualTo(yesterday)
                assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(toDate).isEqualTo(yesterday.toLocalDate())
              }
            }
          }
      }

      @Test
      fun `should update application release and return time for multiple schedules`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999AD") {
            booking = booking {
              application = temporaryAbsenceApplication {
                scheduledTempAbsence = scheduledTemporaryAbsence(startTime = yesterday, returnTime = today)
                scheduledTemporaryAbsence(startTime = today, returnTime = today.plusDays(1))
              }
            }
          }
        }

        // Updates schedule so startTime=twoDaysAgo and returnTime=yesterday
        webTestClient.upsertScheduledTemporaryAbsenceOk(request = anUpsertRequest(eventId = scheduledTempAbsence.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationRepository.findByIdOrNull(application.movementApplicationId)!!) {
                assertThat(releaseTime).isEqualTo("${twoDaysAgo.truncatedTo(ChronoUnit.DAYS)}")
                assertThat(returnTime).isEqualTo("${today.plusDays(2).truncatedTo(ChronoUnit.DAYS)}")
                assertThat(fromDate).isEqualTo("${twoDaysAgo.toLocalDate()}")
                assertThat(toDate).isEqualTo("${today.plusDays(2).toLocalDate()}")
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
              application = temporaryAbsenceApplication {
                scheduledTempAbsence = scheduledTemporaryAbsence()
              }
            }
          }
        }
      }

      @Test
      fun `should update scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(anUpsertRequest(eventId = scheduledTempAbsence.eventId, eventStatus = "COMP"))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(eventStatus.code).isEqualTo("COMP")
                with(scheduledTemporaryAbsenceReturns.first()) {
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
              application = temporaryAbsenceApplication {
                scheduledTempAbsence = scheduledTemporaryAbsence {
                  scheduledTempAbsenceReturn = scheduledReturn()
                }
              }
            }
          }
        }
      }

      @Test
      fun `should update scheduled temporary absence`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(anUpsertRequest(eventId = scheduledTempAbsence.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
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
                with(scheduledTemporaryAbsenceReturns.first()) {
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
    inner class CreateAddress {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              application = temporaryAbsenceApplication()
            }
          }
        }
      }

      @Test
      fun `should create offender address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertRequest(
            movementApplicationId = application.movementApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(name = null, addressText = "1 House, Street, City", postalCode = "A1 1AA"),
          ),
        )
          .apply {
            val responseAddressId = this.addressId
            val responseOwnerClass = this.addressOwnerClass
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(responseAddressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(responseOwnerClass)
                assertThat(toAddress?.premise).isEqualTo("1 House, Street, City")
                assertThat(toAddress?.postalCode).isEqualTo("A1 1AA")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
                assertThat(toAddress?.usages?.map { it.addressUsage?.code }).contains("ROTL")
              }
            }
          }
      }

      @Test
      fun `should create long offender address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertRequest(
            movementApplicationId = application.movementApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(
              name = null,
              addressText = "1 Very long address that doesn't fit into the PREMISE column so should overflow onto the ........100.........o.........o.........o..... STREET column",
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.premise).isEqualTo("1 Very long address that doesn't fit into the PREMISE column so should overflow onto the ........100.........o.........o.........o.....")
                assertThat(toAddress?.street).isEqualTo("STREET column")
              }
            }
          }
      }

      @Test
      fun `should create offender address when booking on an alias`() {
        var scheduleAddressId: Long
        var scheduleOwnerClass: String
        lateinit var alias: Offender
        nomisDataBuilder.build {
          offender = offender(nomsId = "A9999BC") {
            booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              temporaryAbsenceApplication()
              release(date = yesterday)
            }
            alias = alias {
              booking = booking(bookingSequence = 1, bookingBeginDate = today) {
                application = temporaryAbsenceApplication()
              }
            }
          }
        }

        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertRequest(
            movementApplicationId = application.movementApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(name = null, addressText = "1 House, Street, City", postalCode = "A1 1AA"),
          ),
        )
          .apply {
            scheduleAddressId = this.addressId
            scheduleOwnerClass = this.addressOwnerClass
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(toAddress?.addressId).isEqualTo(scheduleAddressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(scheduleOwnerClass)
                assertThat(toAddress?.premise).isEqualTo("1 House, Street, City")
                assertThat(toAddress?.postalCode).isEqualTo("A1 1AA")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("OFF")
                assertThat(toAddress?.usages?.map { it.addressUsage?.code }).contains("ROTL")
              }
            }
          }

        // And the address is saved against the root offender
        repository.runInTransaction {
          with(repository.getOffender(alias.id)!!) {
            assertThat(addresses).isEmpty()
            assertThat(rootOffender!!.addresses[0].addressId).isEqualTo(scheduleAddressId)
            assertThat(rootOffender!!.addresses[0].addressOwnerClass).isEqualTo(scheduleOwnerClass)
          }
        }
      }

      @Test
      fun `should create corporate address and corporate entity`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertRequest(
            movementApplicationId = application.movementApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(addressText = "1 House, Street, City", postalCode = "A1 1AA", name = "Company"),
          ),
        )
          .apply {
            val responseAddressId = this.addressId
            val responseOwnerClass = this.addressOwnerClass
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.addressId).isEqualTo(responseAddressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(responseOwnerClass)
                assertThat(toAddress?.premise).isEqualTo("1 House, Street, City")
                assertThat(toAddress?.postalCode).isEqualTo("A1 1AA")
                assertThat(toAddress?.addressOwnerClass).isEqualTo("CORP")
                val corporateAddress = corporateAddressRepository.findByIdOrNull(toAddress!!.addressId)!!
                assertThat(corporateAddress.corporate.corporateName).isEqualTo("Company")
              }
            }
          }
      }

      @Test
      fun `should create very long corporate address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertRequest(
            movementApplicationId = application.movementApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(
              addressText = "1 Very long address that doesn't fit into the PREMISE column so should overflow onto the ........100.........o.........o.........o STREET column",
              name = "Company",
            ),
          ),
        )
          .apply {
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(toAddress?.premise).isEqualTo("1 Very long address that doesn't fit into the PREMISE column so should overflow onto the ........100.........o.........o.........o")
                assertThat(toAddress?.street).isEqualTo("STREET column")
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
              application = temporaryAbsenceApplication()
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

        webTestClient.upsertScheduledTemporaryAbsence(request = anUpsertRequest(movementApplicationId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid event sub type`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertRequest().copy(eventSubType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid event status`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertRequest().copy(eventStatus = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from prison`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertRequest().copy(fromPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertRequest().copy(toAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid transport type`() {
        webTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(anUpsertRequest().copy(transportType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(id = 9999)
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("invalid")
          }
      }

      @Test
      fun `should return bad request if address text not passed`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(addressText = null, name = "Business")
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertRequest().copy(toAddress = invalidAddress))
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
              application = temporaryAbsenceApplication()
            }
          }
        }
      }

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .bodyValue(anUpsertRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(anUpsertRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(anUpsertRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.upsertScheduledTemporaryAbsence(
      request: UpsertScheduledTemporaryAbsenceRequest = anUpsertRequest(application.movementApplicationId),
      offenderNo: String = offender.nomsId,
    ) = put()
      .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.upsertScheduledTemporaryAbsenceOk(
      request: UpsertScheduledTemporaryAbsenceRequest = anUpsertRequest(application.movementApplicationId),
    ) = upsertScheduledTemporaryAbsence(request)
      .isOk
      .expectBodyResponse<UpsertScheduledTemporaryAbsenceResponse>()

    private fun WebTestClient.upsertScheduledTemporaryAbsenceBadRequest(
      request: UpsertScheduledTemporaryAbsenceRequest = anUpsertRequest(application.movementApplicationId),
    ) = upsertScheduledTemporaryAbsence(request)
      .isBadRequest

    private fun WebTestClient.upsertScheduledTemporaryAbsenceBadRequestUnknown(
      request: UpsertScheduledTemporaryAbsenceRequest = anUpsertRequest(application.movementApplicationId),
    ) = upsertScheduledTemporaryAbsenceBadRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/temporary-absence/{bookingId}/{movementSeq}")
  inner class GetTemporaryAbsence {

    @Nested
    inner class GetUnscheduledTemporaryAbsence {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              tempAbsence = temporaryAbsence(
                date = twoDaysAgo,
                fromPrison = "LEI",
                toAgency = "HAZLWD",
                movementReason = "C5",
                arrestAgency = "POL",
                escort = "L",
                escortText = "SE",
                comment = "Tap OUT comment",
                toCity = "25343",
              )
            }
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence/1/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence/1/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence/1/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/temporary-absences/temporary-absence/1/1")
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
      fun `should retrieve unscheduled temporary absence`() {
        webTestClient.get()
          .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<TemporaryAbsenceResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(sequence).isEqualTo(tempAbsence.id.sequence)
            assertThat(scheduledTemporaryAbsenceId).isNull()
            assertThat(movementApplicationId).isNull()
            assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
            assertThat(movementTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
            assertThat(movementReason).isEqualTo("C5")
            assertThat(arrestAgency).isEqualTo("POL")
            assertThat(escort).isEqualTo("L")
            assertThat(escortText).isEqualTo("SE")
            assertThat(fromPrison).isEqualTo("LEI")
            assertThat(toAgency).isEqualTo("HAZLWD")
            assertThat(commentText).isEqualTo("Tap OUT comment")
            assertThat(toAddressId).isNull()
            assertThat(toAddressOwnerClass).isNull()
            assertThat(toFullAddress).isEqualTo("Sheffield")
            assertThat(toAddressPostcode).isNull()
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
                application = temporaryAbsenceApplication {
                  scheduledTempAbsence = scheduledTemporaryAbsence {
                    tempAbsence = externalMovement(
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
                    scheduledTempAbsenceReturn = scheduledReturn {
                      tempAbsenceReturn = externalMovement()
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
            .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse<TemporaryAbsenceResponse>()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(tempAbsence.id.sequence)
              assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduledTempAbsence.eventId)
              assertThat(movementApplicationId).isEqualTo(application.movementApplicationId)
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = offenderAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = null,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(tempAbsence.id.sequence)
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = scheduleAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = movementAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = corporateAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = null,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(tempAbsence.id.sequence)
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
                    application = temporaryAbsenceApplication {
                      scheduledTempAbsence = scheduledTemporaryAbsence(
                        toAddress = offenderAddress,
                      ) {
                        tempAbsence = externalMovement(
                          toAddress = corporateAddress,
                        )
                        scheduledTempAbsenceReturn = scheduledReturn {
                          tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
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

      @Nested
      @DisplayName("With agency address")
      inner class WithAgencyAddress {
        private lateinit var agencyAddress: AgencyLocationAddress

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            agencyLocation(
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = agencyAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = null,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(tempAbsence.id.sequence)
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = offenderAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = agencyAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence/${booking.bookingId}/${tempAbsence.id.sequence}")
              .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
              .exchange()
              .expectStatus().isOk
              .expectBodyResponse<TemporaryAbsenceResponse>()
              .apply {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(sequence).isEqualTo(tempAbsence.id.sequence)
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

    private fun aCreateRequest(scheduledTemporaryAbsenceId: Long? = scheduledTempAbsence.eventId) = CreateTemporaryAbsenceRequest(
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence()
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
              with(temporaryAbsenceRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(scheduledTemporaryAbsence?.eventId).isEqualTo(scheduledTempAbsence.eventId)
                assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(movementTime).isEqualTo(twoDaysAgo)
                assertThat(movementReason.code).isEqualTo("C5")
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
              application = temporaryAbsenceApplication()
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
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateRequest(application.movementApplicationId))
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
              with(temporaryAbsenceRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(scheduledTemporaryAbsence).isNull()
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
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduledTempAbsence.eventId),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/temporary-absence")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createTemporaryAbsenceOk(
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduledTempAbsence.eventId),
    ) = createTemporaryAbsence(request)
      .isCreated
      .expectBodyResponse<CreateTemporaryAbsenceResponse>()

    private fun WebTestClient.createTemporaryAbsenceBadRequest(
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduledTempAbsence.eventId),
    ) = createTemporaryAbsence(request)
      .isBadRequest

    private fun WebTestClient.createTemporaryAbsenceBadRequestUnknown(
      request: CreateTemporaryAbsenceRequest = aCreateRequest(scheduledTempAbsence.eventId),
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

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              tempAbsenceReturn = temporaryAbsenceReturn(
                date = twoDaysAgo,
                fromAgency = "HAZLWD",
                toPrison = "LEI",
                movementReason = "C5",
                escort = "L",
                escortText = "SE",
                comment = "Tap IN comment",
                fromCity = "25343",
              )
            }
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/1/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/1/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return/1/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/temporary-absences/temporary-absence-return/1/1")
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
      fun `should retrieve unscheduled temporary absence return`() {
        webTestClient.get()
          .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBodyResponse<TemporaryAbsenceReturnResponse>()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(sequence).isEqualTo(tempAbsenceReturn.id.sequence)
            assertThat(scheduledTemporaryAbsenceId).isNull()
            assertThat(scheduledTemporaryAbsenceReturnId).isNull()
            assertThat(movementApplicationId).isNull()
            assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
            assertThat(movementTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
            assertThat(movementReason).isEqualTo("C5")
            assertThat(escort).isEqualTo("L")
            assertThat(escortText).isEqualTo("SE")
            assertThat(fromAgency).isEqualTo("HAZLWD")
            assertThat(toPrison).isEqualTo("LEI")
            assertThat(commentText).isEqualTo("Tap IN comment")
            assertThat(fromAddressId).isNull()
            assertThat(fromAddressOwnerClass).isNull()
            assertThat(fromFullAddress).isEqualTo("Sheffield")
            assertThat(fromAddressPostcode).isNull()
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
                application = temporaryAbsenceApplication {
                  scheduledTempAbsence = scheduledTemporaryAbsence {
                    tempAbsence = externalMovement()
                    scheduledTempAbsenceReturn = scheduledReturn {
                      tempAbsenceReturn = externalMovement(
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
            .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse<TemporaryAbsenceReturnResponse>()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(tempAbsenceReturn.id.sequence)
              assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduledTempAbsence.eventId)
              assertThat(scheduledTemporaryAbsenceReturnId).isEqualTo(scheduledTempAbsenceReturn.eventId)
              assertThat(movementApplicationId).isEqualTo(application.movementApplicationId)
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = offenderAddress,
                    ) {
                      tempAbsence = externalMovement()
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = scheduledOutAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = movementOutAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement(
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = scheduledOutAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = movementOutAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement(
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = corporateAddress,
                    ) {
                      tempAbsence = externalMovement()
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = scheduledOutAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = corporateAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement(
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = scheduledOutAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = movementOutAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement(
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
            agencyLocation(
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = agencyAddress,
                    ) {
                      tempAbsence = externalMovement()
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement()
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = scheduledOutAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = agencyAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement(
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                  application = temporaryAbsenceApplication {
                    scheduledTempAbsence = scheduledTemporaryAbsence(
                      toAddress = scheduledOutAddress,
                    ) {
                      tempAbsence = externalMovement(
                        toAddress = movementOutAddress,
                      )
                      scheduledTempAbsenceReturn = scheduledReturn {
                        tempAbsenceReturn = externalMovement(
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
              .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
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
                application = temporaryAbsenceApplication {
                  scheduledTempAbsence = scheduledTemporaryAbsence {
                    tempAbsence = externalMovement()
                    scheduledTempAbsenceReturn = scheduledReturn {
                      tempAbsenceReturn = externalMovement()
                    }
                  }
                }
              }
            }

            // there's a bug in NOMIS where the external movement parent event id is null if you perform any unscheduled
            // external movements inbetween confirming the scheduled OUT/IN movements
            tempAbsenceReturn.scheduledTemporaryAbsence = null
          }
        }

        @Test
        fun `should retrieve scheduled temporary absence return`() {
          webTestClient.get()
            .uri("/movements/${offender.nomsId}/temporary-absences/temporary-absence-return/${booking.bookingId}/${tempAbsenceReturn.id.sequence}")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .exchange()
            .expectStatus().isOk
            .expectBodyResponse<TemporaryAbsenceReturnResponse>()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(tempAbsenceReturn.id.sequence)
              assertThat(scheduledTemporaryAbsenceId).isEqualTo(scheduledTempAbsence.eventId)
              assertThat(scheduledTemporaryAbsenceReturnId).isEqualTo(scheduledTempAbsenceReturn.eventId)
              assertThat(movementApplicationId).isEqualTo(application.movementApplicationId)
            }
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /movements/{offenderNo}/temporary-absences/temporary-absence-return")
  inner class CreateTemporaryAbsenceReturn {

    private fun aCreateRequest(scheduledTemporaryAbsenceReturnId: Long? = scheduledTempAbsenceReturn.eventId) = CreateTemporaryAbsenceReturnRequest(
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
            application = temporaryAbsenceApplication {
              scheduledTempAbsence = scheduledTemporaryAbsence {
                tempAbsence = externalMovement()
                scheduledTempAbsenceReturn = scheduledReturn()
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
              with(temporaryAbsenceReturnRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(scheduledTemporaryAbsence?.eventId).isEqualTo(scheduledTempAbsence.eventId)
                assertThat(scheduledTemporaryAbsenceReturn?.eventId).isEqualTo(scheduledTempAbsenceReturn.eventId)
                assertThat(movementDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(movementTime).isEqualTo(twoDaysAgo)
                assertThat(movementReason.code).isEqualTo("C5")
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
              application = temporaryAbsenceApplication()
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
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateRequest(application.movementApplicationId))
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
              with(temporaryAbsenceReturnRepository.findById_OffenderBooking_BookingIdAndId_Sequence(bookingId, movementSequence)!!) {
                assertThat(active).isTrue
                assertThat(scheduledTemporaryAbsence).isNull()
                assertThat(scheduledTemporaryAbsenceReturn).isNull()
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
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduledTempAbsenceReturn.eventId),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/temporary-absence-return")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createTemporaryAbsenceReturnOk(
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduledTempAbsenceReturn.eventId),
    ) = createTemporaryAbsenceReturn(request)
      .isCreated
      .expectBodyResponse<CreateTemporaryAbsenceReturnResponse>()

    private fun WebTestClient.createTemporaryAbsenceReturnBadRequest(
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduledTempAbsenceReturn.eventId),
    ) = createTemporaryAbsenceReturn(request)
      .isBadRequest

    private fun WebTestClient.createTemporaryAbsenceReturnBadRequestUnknown(
      request: CreateTemporaryAbsenceReturnRequest = aCreateRequest(scheduledTempAbsenceReturn.eventId),
    ) = createTemporaryAbsenceReturnBadRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
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
              temporaryAbsenceApplication {
                scheduledTemporaryAbsence {
                  externalMovement()
                  scheduledReturn {
                    externalMovement()
                  }
                }
              }
              temporaryAbsence()
              temporaryAbsenceReturn()
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
              temporaryAbsenceApplication()
            }
            booking {
              temporaryAbsenceApplication {
                scheduledTemporaryAbsence {
                  externalMovement()
                  scheduledReturn()
                }
              }
            }
            booking {
              temporaryAbsence()
              temporaryAbsenceReturn()
              temporaryAbsence()
            }
            booking()
          }
          offender(nomsId = "ANY") {
            booking {
              temporaryAbsenceApplication()
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
              temporaryAbsenceApplication {
                orphanedSchedule = scheduledTemporaryAbsence()
              }
              temporaryAbsenceApplication {
                scheduledTemporaryAbsence {
                  externalMovement()
                  scheduledReturn {
                    externalMovement()
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
            update OffenderScheduledTemporaryAbsence ost
            set ost.temporaryAbsenceApplication = null
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
              temporaryAbsenceApplication {
                scheduledTemporaryAbsence {
                  externalMovement()
                  scheduledReturn {
                    externalMovement()
                  }
                }
              }
              temporaryAbsenceApplication {
                scheduledTemporaryAbsence {
                  externalMovement()
                  scheduledReturn {
                    tempAbsenceReturn = externalMovement()
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
            update OffenderTemporaryAbsenceReturn otar
            set otar.scheduledTemporaryAbsence = null
            where otar.id.offenderBooking.id = ${tempAbsenceReturn.id.offenderBooking.bookingId}
            and otar.id.sequence = ${tempAbsenceReturn.id.sequence}
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
              application = temporaryAbsenceApplication {
                orphanedSchedule = scheduledTemporaryAbsence {
                  externalMovement()
                  orphanedScheduleReturn = scheduledReturn {
                    externalMovement()
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
            where oma.OFFENDER_MOVEMENT_APP_ID = ${application.movementApplicationId}
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

  private fun WebTestClient.getOffenderSummary(offenderNo: String) = get()
    .uri("/movements/$offenderNo/temporary-absences/summary")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getOffenderSummaryOk(offenderNo: String) = getOffenderSummary(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<OffenderTemporaryAbsenceSummaryResponse>()
}
