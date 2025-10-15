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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationMultiRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTemporaryAbsenceReturnRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.profiledetails.roundToNearestSecond
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class MovementsResourceIntTest(
  @Autowired val nomisDataBuilder: NomisDataBuilder,
  @Autowired val repository: Repository,
  @Autowired val applicationRepository: OffenderMovementApplicationRepository,
  @Autowired val applicationMultiRepository: OffenderMovementApplicationMultiRepository,
  @Autowired val scheduledTemporaryAbsenceRepository: OffenderScheduledTemporaryAbsenceRepository,
  @Autowired val scheduledTemporaryAbsenceReturnRepository: OffenderScheduledTemporaryAbsenceReturnRepository,
  @Autowired val temporaryAbsenceRepository: OffenderTemporaryAbsenceRepository,
  @Autowired val temporaryAbsenceReturnRepository: OffenderTemporaryAbsenceReturnRepository,
  @Autowired val offenderExternalMovementRepository: OffenderExternalMovementRepository,
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var offenderAddress: OffenderAddress
  private lateinit var booking: OffenderBooking
  private lateinit var application: OffenderMovementApplication
  private lateinit var applicationOutsideMovement: OffenderMovementApplicationMulti
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

  @AfterEach
  fun `tear down`() {
    if (this::offender.isInitialized) {
      repository.delete(offender)
    }
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
    fun `should retrieve application's outside movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              applicationOutsideMovement = outsideMovement(
                eventSubType = "C5",
                fromDate = twoDaysAgo.toLocalDate(),
                releaseTime = twoDaysAgo,
                toDate = yesterday.toLocalDate(),
                returnTime = yesterday,
                comment = "Some comment application movement",
                toAgency = "HAZLWD",
                toAddress = offenderAddress,
                contactPersonName = "Derek",
                temporaryAbsenceType = "RR",
                temporaryAbsenceSubType = "RDR",
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].outsideMovementId").isEqualTo(applicationOutsideMovement.movementApplicationMultiId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].temporaryAbsenceType").isEqualTo("RR")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].temporaryAbsenceSubType").isEqualTo("RDR")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].fromDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].releaseTime").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].toDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].returnTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].comment").isEqualTo("Some comment application movement")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].toAgencyId").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].toAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].toAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].contactPersonName").isEqualTo("Derek")
    }

    @Test
    fun `should retrieve scheduled temporary absence`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
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
    }

    @Test
    fun `should retrieve scheduled temporary absence's external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
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
          offenderAddress = address()
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
    }

    @Test
    fun `should retrieve unscheduled temporary absence external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
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
    }

    @Test
    fun `should retrieve unscheduled temporary absences return external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
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
    }

    @Test
    fun `should retrieve all temporary absences and external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              applicationOutsideMovement = outsideMovement()
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].outsideMovementId").isEqualTo(applicationOutsideMovement.movementApplicationMultiId)
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
    fun `should retrieve all temporary absences and external movements from a merged prisoner`() {
      lateinit var mergedBooking: OffenderBooking
      lateinit var mergedApplication: OffenderMovementApplication
      lateinit var mergedApplicationOutsideMovement: OffenderMovementApplicationMulti
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

      // Simulate a scenario where a prisoner is merged into another
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          // This booking was moved from the old prisoner during the merge
          mergedBooking = booking(bookingSequence = 2) {
            receive(twoDaysAgo)
            mergedApplication = temporaryAbsenceApplication {
              mergedApplicationOutsideMovement = outsideMovement()
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
          // This the latest booking
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
      }

      webTestClient.get()
        .uri("/movements/${offender.nomsId}/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].bookingId").isEqualTo(mergedBooking.bookingId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications.length()").isEqualTo(1)
        // The TAP from the 1st merged booking exists with correct child entities
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].movementApplicationId").isEqualTo(mergedApplication.movementApplicationId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].outsideMovementId").isEqualTo(mergedApplicationOutsideMovement.movementApplicationMultiId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventId").isEqualTo(mergedScheduledTemporaryAbsence.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.sequence").isEqualTo(mergedTemporaryAbsence.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(mergedScheduledTemporaryAbsenceReturn.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.sequence").isEqualTo(mergedTemporaryAbsenceReturn.id.sequence)
        // The TAP copied onto the latest booking exists with correct child entities (absences[1] because we deliberately made them out of order)
        .jsonPath("$.bookings[1].bookingId").isEqualTo(booking.bookingId)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications.length()").isEqualTo(1)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].outsideMovements.length()").isEqualTo(0)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].outsideMovements.length()").isEqualTo(0)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[1].scheduledTemporaryAbsence.eventId").isEqualTo(scheduledTempAbsence.eventId)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[1].temporaryAbsence").isEmpty
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[1].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduledTempAbsenceReturn.eventId)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[1].temporaryAbsenceReturn").isEmpty
        // The TAP from the 2nd merged booking exists with correct child entities
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[1].scheduledTemporaryAbsence.eventId").isEqualTo(mergedScheduledTemporaryAbsence2.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[1].temporaryAbsence.sequence").isEqualTo(mergedTemporaryAbsence2.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[1].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(mergedScheduledTemporaryAbsenceReturn2.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[1].temporaryAbsenceReturn.sequence").isEqualTo(mergedTemporaryAbsenceReturn2.id.sequence)
        // The TAP copied onto the latest booking exists with correct child entities (absences[1] because we deliberately made them out of order)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.eventId").isEqualTo(scheduledTemporaryAbsence2.eventId)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[0].temporaryAbsence").isEmpty
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduledTemporaryAbsenceReturn2.eventId)
        .jsonPath("$.bookings[1].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn").isEmpty
    }
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
              outsideMovement()
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
  @DisplayName("POST /movements/{offenderNo}/temporary-absences/application")
  inner class CreateTemporaryAbsenceApplication {

    private fun aCreateRequest() = CreateTemporaryAbsenceApplicationRequest(
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
      toAgencyId = "HAZLWD",
      toAddressId = offenderAddress.addressId,
      contactPersonName = "Derek",
      temporaryAbsenceType = "RR",
      temporaryAbsenceSubType = "RDR",
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking()
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
      fun `should create application`() {
        webTestClient.createApplicationOk()
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
                assertThat(prison?.id).isEqualTo("LEI")
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(contactPersonName).isEqualTo("Derek")
                assertThat(temporaryAbsenceType?.code).isEqualTo("RR")
                assertThat(temporaryAbsenceSubType?.code).isEqualTo("RDR")
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.createApplication(offenderNo = "UNKNOWN")
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

        webTestClient.createApplication()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request for invalid event sub type`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(eventSubType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid application type`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(applicationType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid application status`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(applicationStatus = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort code`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(escortCode = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid transport type`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(transportType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid prison ID`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(prisonId = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency ID`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(toAgencyId = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address ID`() {
        webTestClient.createApplicationBadRequest(aCreateRequest().copy(toAddressId = 9999))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("invalid")
          }
      }

      @Test
      fun `should return bad request for invalid temporary absence type`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(temporaryAbsenceType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid temporary absence sub type`() {
        webTestClient.createApplicationBadRequestUnknown(aCreateRequest().copy(temporaryAbsenceSubType = "UNKNOWN"))
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/application")
          .bodyValue(aCreateRequest())
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/application")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest())
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/application")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
      }
    }

    private fun WebTestClient.createApplication(
      request: CreateTemporaryAbsenceApplicationRequest = aCreateRequest(),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/application")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createApplicationOk(request: CreateTemporaryAbsenceApplicationRequest = aCreateRequest()) = createApplication(request)
      .isCreated
      .expectBodyResponse<CreateTemporaryAbsenceApplicationResponse>()

    private fun WebTestClient.createApplicationBadRequest(request: CreateTemporaryAbsenceApplicationRequest = aCreateRequest()) = createApplication(request)
      .isBadRequest

    private fun WebTestClient.createApplicationBadRequestUnknown(request: CreateTemporaryAbsenceApplicationRequest = aCreateRequest()) = createApplicationBadRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence/{eventId}")
  inner class GetScheduledTemporaryAbsence {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
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
                contactPersonName = "Jeff",
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
          assertThat(eventStatus).isEqualTo("SCH")
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
  @DisplayName("POST /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence-return")
  inner class CreateScheduledTemporaryAbsenceReturn {

    private fun aCreateRequest(
      movementApplicationId: Long? = null,
      scheduledTemporaryAbsenceId: Long? = null,
    ) = CreateScheduledTemporaryAbsenceReturnRequest(
      movementApplicationId = movementApplicationId ?: application.movementApplicationId,
      scheduledTemporaryAbsenceEventId = scheduledTemporaryAbsenceId ?: scheduledTempAbsence.eventId,
      eventDate = twoDaysAgo.toLocalDate(),
      startTime = twoDaysAgo,
      eventSubType = "C5",
      eventStatus = "SCH",
      comment = "Some comment scheduled temporary absence",
      escort = "L",
      fromAgency = "HAZLWD",
      toPrison = "LEI",
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
      fun `should create scheduled temporary absence return`() {
        webTestClient.createScheduledTemporaryAbsenceReturnOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceReturnRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication?.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isEqualTo(twoDaysAgo)
                assertThat(comment).isEqualTo("Some comment scheduled temporary absence")
                assertThat(fromAgency?.id).isEqualTo("HAZLWD")
                assertThat(toAgency?.id).isEqualTo("LEI")
                assertThat(escort?.code).isEqualTo("L")
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.createScheduledTemporaryAbsenceReturn(offenderNo = "UNKNOWN")
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

        webTestClient.createScheduledTemporaryAbsenceReturn()
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

        webTestClient.createScheduledTemporaryAbsenceReturn(request = aCreateRequest(movementApplicationId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request if outbound scheduled movement does not exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
            booking = booking {
              application = temporaryAbsenceApplication()
            }
          }
        }

        webTestClient.createScheduledTemporaryAbsenceReturn(request = aCreateRequest(scheduledTemporaryAbsenceId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid event sub type`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestReturnUnknown(aCreateRequest().copy(eventSubType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid event status`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestReturnUnknown(aCreateRequest().copy(eventStatus = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestReturnUnknown(aCreateRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from prison`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestReturnUnknown(aCreateRequest().copy(fromAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestReturnUnknown(aCreateRequest().copy(toPrison = "UNKNOWN"))
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return")
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createScheduledTemporaryAbsenceReturn(
      request: CreateScheduledTemporaryAbsenceReturnRequest = aCreateRequest(application.movementApplicationId, scheduledTempAbsence.eventId),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence-return")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createScheduledTemporaryAbsenceReturnOk(
      request: CreateScheduledTemporaryAbsenceReturnRequest = aCreateRequest(application.movementApplicationId, scheduledTempAbsence.eventId),
    ) = createScheduledTemporaryAbsenceReturn(request)
      .isCreated
      .expectBodyResponse<CreateScheduledTemporaryAbsenceResponse>()

    private fun WebTestClient.createScheduledTemporaryAbsenceBadReturnRequest(
      request: CreateScheduledTemporaryAbsenceReturnRequest = aCreateRequest(application.movementApplicationId, scheduledTempAbsence.eventId),
    ) = createScheduledTemporaryAbsenceReturn(request)
      .isBadRequest

    private fun WebTestClient.createScheduledTemporaryAbsenceBadRequestReturnUnknown(
      request: CreateScheduledTemporaryAbsenceReturnRequest = aCreateRequest(application.movementApplicationId, scheduledTempAbsence.eventId),
    ) = createScheduledTemporaryAbsenceBadReturnRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/temporary-absences/outside-movement/{appMultiId}")
  inner class GetTemporaryAbsenceApplicationOutsideMovement {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              applicationOutsideMovement = outsideMovement(
                eventSubType = "C5",
                fromDate = twoDaysAgo.toLocalDate(),
                releaseTime = twoDaysAgo,
                toDate = yesterday.toLocalDate(),
                returnTime = yesterday,
                comment = "Some comment application movement",
                toAgency = "HAZLWD",
                toAddress = offenderAddress,
                contactPersonName = "Derek",
                temporaryAbsenceType = "RR",
                temporaryAbsenceSubType = "RDR",
              )
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
        .uri("/movements/$offenderNo/temporary-absences/outside-movement/1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/outside-movement/1")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/outside-movement/1")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if offender not found`() {
      webTestClient.get()
        .uri("/movements/UNKNOWN/temporary-absences/outside-movement/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("UNKNOWN").contains("not found")
        }
    }

    @Test
    fun `should return not found if outside movement not found`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/temporary-absences/outside-movement/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("9999").contains("not found")
        }
    }

    @Test
    fun `should retrieve temporary absence application outside movement`() {
      webTestClient.get()
        .uri(
          "/movements/${offender.nomsId}/temporary-absences/outside-movement/{appMultiId}",
          applicationOutsideMovement.movementApplicationMultiId,
        )
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBodyResponse<TemporaryAbsenceApplicationOutsideMovementResponse>()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(movementApplicationId).isEqualTo(application.movementApplicationId)
          assertThat(outsideMovementId).isEqualTo(applicationOutsideMovement.movementApplicationMultiId)
          assertThat(temporaryAbsenceType).isEqualTo("RR")
          assertThat(temporaryAbsenceSubType).isEqualTo("RDR")
          assertThat(eventSubType).isEqualTo("C5")
          assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
          assertThat(releaseTime).isCloseTo(twoDaysAgo, within(1, ChronoUnit.MINUTES))
          assertThat(toDate).isEqualTo(yesterday.toLocalDate())
          assertThat(returnTime).isCloseTo(yesterday, within(1, ChronoUnit.MINUTES))
          assertThat(comment).isEqualTo("Some comment application movement")
          assertThat(toAgencyId).isEqualTo("HAZLWD")
          assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
          assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
          assertThat(contactPersonName).isEqualTo("Derek")
        }
    }
  }

  @Nested
  @DisplayName("POST /movements/{offenderNo}/temporary-absences/outside-movement")
  inner class CreateTemporaryAbsenceOutsideMovement {

    private fun aCreateRequest(movementApplicationId: Long? = null) = CreateTemporaryAbsenceOutsideMovementRequest(
      movementApplicationId = movementApplicationId ?: application.movementApplicationId,
      eventSubType = "C5",
      fromDate = twoDaysAgo.toLocalDate(),
      releaseTime = twoDaysAgo,
      toDate = yesterday.toLocalDate(),
      returnTime = yesterday,
      comment = "Some comment outside movement",
      toAgencyId = "HAZLWD",
      toAddressId = offenderAddress.addressId,
      contactPersonName = "Derek",
      temporaryAbsenceType = "RR",
      temporaryAbsenceSubType = "RDR",
    )

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

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create outside movement`() {
        webTestClient.createOutsideMovementOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(applicationMultiRepository.findByIdOrNull(outsideMovementId)!!) {
                assertThat(offenderMovementApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(temporaryAbsenceType?.code).isEqualTo("RR")
                assertThat(temporaryAbsenceSubType?.code).isEqualTo("RDR")
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(fromDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(releaseTime).isEqualTo(twoDaysAgo)
                assertThat(toDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isEqualTo(yesterday)
                assertThat(comment).isEqualTo("Some comment outside movement")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(contactPersonName).isEqualTo("Derek")
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.createOutsideMovement(offenderNo = "UNKNOWN")
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

        webTestClient.createOutsideMovement()
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

        webTestClient.createOutsideMovement(request = aCreateRequest(movementApplicationId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid event sub type`() {
        webTestClient.createOutsideMovementBadRequestUnknown(aCreateRequest().copy(eventSubType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency id`() {
        webTestClient.createOutsideMovementBadRequestUnknown(aCreateRequest().copy(toAgencyId = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        webTestClient.createOutsideMovementBadRequest(aCreateRequest().copy(toAddressId = 9999L))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("invalid")
          }
      }

      @Test
      fun `should return bad request for invalid temporary absence type`() {
        webTestClient.createOutsideMovementBadRequestUnknown(aCreateRequest().copy(temporaryAbsenceType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid temporary absence sub type`() {
        webTestClient.createOutsideMovementBadRequestUnknown(aCreateRequest().copy(temporaryAbsenceSubType = "UNKNOWN"))
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/outside-movement")
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/outside-movement")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/outside-movement")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createOutsideMovement(
      request: CreateTemporaryAbsenceOutsideMovementRequest = aCreateRequest(application.movementApplicationId),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/outside-movement")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createOutsideMovementOk(
      request: CreateTemporaryAbsenceOutsideMovementRequest = aCreateRequest(application.movementApplicationId),
    ) = createOutsideMovement(request)
      .isCreated
      .expectBodyResponse<CreateTemporaryAbsenceOutsideMovementResponse>()

    private fun WebTestClient.createOutsideMovementBadRequest(
      request: CreateTemporaryAbsenceOutsideMovementRequest = aCreateRequest(application.movementApplicationId),
    ) = createOutsideMovement(request)
      .isBadRequest

    private fun WebTestClient.createOutsideMovementBadRequestUnknown(
      request: CreateTemporaryAbsenceOutsideMovementRequest = aCreateRequest(application.movementApplicationId),
    ) = createOutsideMovementBadRequest(request)
      .expectBody().jsonPath("userMessage").value<String> {
        assertThat(it).contains("UNKNOWN").contains("invalid")
      }
  }

  @Nested
  @DisplayName("POST /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence")
  inner class CreateScheduledTemporaryAbsence {

    private fun aCreateRequest(movementApplicationId: Long? = null) = CreateScheduledTemporaryAbsenceRequest(
      movementApplicationId = movementApplicationId ?: application.movementApplicationId,
      eventDate = twoDaysAgo.toLocalDate(),
      startTime = twoDaysAgo,
      eventSubType = "C5",
      eventStatus = "SCH",
      comment = "Some comment scheduled temporary absence",
      escort = "L",
      fromPrison = "LEI",
      toAgency = "HAZLWD",
      transportType = "VAN",
      returnDate = yesterday.toLocalDate(),
      returnTime = yesterday,
      toAddressId = offenderAddress.addressId,
      applicationDate = twoDaysAgo,
      applicationTime = twoDaysAgo,
    )

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

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should create scheduled temporary absence`() {
        webTestClient.createScheduledTemporaryAbsenceOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(scheduledTemporaryAbsenceRepository.findByEventIdAndOffenderBooking_Offender_NomsId(eventId, offender.nomsId)!!) {
                assertThat(temporaryAbsenceApplication.movementApplicationId).isEqualTo(application.movementApplicationId)
                assertThat(eventSubType.code).isEqualTo("C5")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(eventDate).isEqualTo(twoDaysAgo.toLocalDate())
                assertThat(startTime).isEqualTo(twoDaysAgo)
                assertThat(returnDate).isEqualTo(yesterday.toLocalDate())
                assertThat(returnTime).isEqualTo(yesterday)
                assertThat(comment).isEqualTo("Some comment scheduled temporary absence")
                assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
                assertThat(toAddress?.addressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(toAgency?.id).isEqualTo("HAZLWD")
                assertThat(escort?.code).isEqualTo("L")
                assertThat(fromAgency?.id).isEqualTo("LEI")
                assertThat(transportType?.code).isEqualTo("VAN")
                assertThat(applicationDate).isEqualTo(twoDaysAgo)
                assertThat(applicationTime).isEqualTo(twoDaysAgo)
              }
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.createScheduledTemporaryAbsence(offenderNo = "UNKNOWN")
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

        webTestClient.createScheduledTemporaryAbsence()
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

        webTestClient.createScheduledTemporaryAbsence(request = aCreateRequest(movementApplicationId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid event sub type`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(eventSubType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid event status`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(eventStatus = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from prison`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(fromPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(toAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid transport type`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequestUnknown(aCreateRequest().copy(transportType = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        webTestClient.createScheduledTemporaryAbsenceBadRequest(aCreateRequest().copy(toAddressId = 9999))
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
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateRequest(application.movementApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.createScheduledTemporaryAbsence(
      request: CreateScheduledTemporaryAbsenceRequest = aCreateRequest(application.movementApplicationId),
      offenderNo: String = offender.nomsId,
    ) = post()
      .uri("/movements/$offenderNo/temporary-absences/scheduled-temporary-absence")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun WebTestClient.createScheduledTemporaryAbsenceOk(
      request: CreateScheduledTemporaryAbsenceRequest = aCreateRequest(application.movementApplicationId),
    ) = createScheduledTemporaryAbsence(request)
      .isCreated
      .expectBodyResponse<CreateScheduledTemporaryAbsenceResponse>()

    private fun WebTestClient.createScheduledTemporaryAbsenceBadRequest(
      request: CreateScheduledTemporaryAbsenceRequest = aCreateRequest(application.movementApplicationId),
    ) = createScheduledTemporaryAbsence(request)
      .isBadRequest

    private fun WebTestClient.createScheduledTemporaryAbsenceBadRequestUnknown(
      request: CreateScheduledTemporaryAbsenceRequest = aCreateRequest(application.movementApplicationId),
    ) = createScheduledTemporaryAbsenceBadRequest(request)
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
              tempAbsence = temporaryAbsence(
                date = twoDaysAgo,
                fromPrison = "LEI",
                toAgency = "HAZLWD",
                movementReason = "C5",
                arrestAgency = "POL",
                escort = "L",
                escortText = "SE",
                comment = "Tap OUT comment",
                toAddress = offenderAddress,
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
            assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
            assertThat(toAddressHouse).isEqualTo("Flat 1  41")
            assertThat(toAddressStreet).isEqualTo("High Street")
            assertThat(toAddressLocality).isEqualTo("Hillsborough")
            // City, County and Country would be the description in the real view - we're missing the package OMS_MISCELLANEOUS.GETDESCCODE() so we can't do that
            assertThat(toAddressCity).isEqualTo("25343")
            assertThat(toAddressCounty).isEqualTo("S.YORKSHIRE")
            assertThat(toAddressCountry).isEqualTo("ENG")
            assertThat(toAddressPostcode).isEqualTo("S1 1AB")
          }
      }
    }

    @Nested
    inner class GetScheduledTemporaryAbsence {

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
                    toAddress = offenderAddress,
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
            assertThat(toAddressId).isEqualTo(offenderAddress.addressId)
            assertThat(toAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
            assertThat(toAddressHouse).isEqualTo("Flat 1  41")
            assertThat(toAddressStreet).isEqualTo("High Street")
            assertThat(toAddressLocality).isEqualTo("Hillsborough")
            // City, County and Country would be the description in the real view - we're missing the package OMS_MISCELLANEOUS.GETDESCCODE() so we can't do that
            assertThat(toAddressCity).isEqualTo("25343")
            assertThat(toAddressCounty).isEqualTo("S.YORKSHIRE")
            assertThat(toAddressCountry).isEqualTo("ENG")
            assertThat(toAddressPostcode).isEqualTo("S1 1AB")
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
              tempAbsenceReturn = temporaryAbsenceReturn(
                date = twoDaysAgo,
                fromAgency = "HAZLWD",
                toPrison = "LEI",
                movementReason = "C5",
                escort = "L",
                escortText = "SE",
                comment = "Tap IN comment",
                fromAddress = offenderAddress,
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
            assertThat(fromAddressId).isEqualTo(offenderAddress.addressId)
            assertThat(fromAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
            assertThat(fromAddressHouse).isEqualTo("Flat 1  41")
            assertThat(fromAddressStreet).isEqualTo("High Street")
            assertThat(fromAddressLocality).isEqualTo("Hillsborough")
            // City, County and Country would be the description in the real view - we're missing the package OMS_MISCELLANEOUS.GETDESCCODE() so we can't do that
            assertThat(fromAddressCity).isEqualTo("25343")
            assertThat(fromAddressCounty).isEqualTo("S.YORKSHIRE")
            assertThat(fromAddressCountry).isEqualTo("ENG")
            assertThat(fromAddressPostcode).isEqualTo("S1 1AB")
          }
      }
    }

    @Nested
    inner class GetScheduledTemporaryAbsenceReturn {

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
                      fromAddress = offenderAddress,
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
            assertThat(fromAddressId).isEqualTo(offenderAddress.addressId)
            assertThat(fromAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
            assertThat(fromAddressHouse).isEqualTo("Flat 1  41")
            assertThat(fromAddressStreet).isEqualTo("High Street")
            assertThat(fromAddressLocality).isEqualTo("Hillsborough")
            // City, County and Country would be the description in the real view - we're missing the package OMS_MISCELLANEOUS.GETDESCCODE() so we can't do that
            assertThat(fromAddressCity).isEqualTo("25343")
            assertThat(fromAddressCounty).isEqualTo("S.YORKSHIRE")
            assertThat(fromAddressCountry).isEqualTo("ENG")
            assertThat(fromAddressPostcode).isEqualTo("S1 1AB")
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
}
