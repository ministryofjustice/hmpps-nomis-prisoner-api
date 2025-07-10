package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.profiledetails.roundToNearestSecond
import java.time.LocalDateTime

class MovementsResourceIntTest(
  @Autowired val nomisDataBuilder: NomisDataBuilder,
  @Autowired val repository: Repository,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var offenderAddress: OffenderAddress
  private lateinit var booking: OffenderBooking
  private lateinit var application: OffenderMovementApplication
  private lateinit var applicationOutsideMovement: OffenderMovementApplicationMulti
  private lateinit var scheduledTemporaryAbsence: OffenderScheduledTemporaryAbsence
  private lateinit var scheduledTemporaryAbsenceReturn: OffenderScheduledTemporaryAbsenceReturn
  private lateinit var temporaryAbsence: OffenderTemporaryAbsence
  private lateinit var temporaryAbsenceReturn: OffenderTemporaryAbsenceReturn
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
        .uri("/movements/$offenderNo/temporary-absences")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Offender with nomsId=$offenderNo not found")
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].bookingId").isEqualTo(booking.bookingId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].movementApplicationId").isEqualTo(application.movementApplicationId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].applicationDate").isEqualTo("$twoDaysAgo")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].fromDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].releaseTime").isEqualTo("$twoDaysAgo")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].toDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].returnTime").isEqualTo("$yesterday")
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].outsideMovementId").isEqualTo(applicationOutsideMovement.movementApplicationMultiId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].temporaryAbsenceType").isEqualTo("RR")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].temporaryAbsenceSubType").isEqualTo("RDR")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].fromDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].releaseTime").isEqualTo("$twoDaysAgo")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].toDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].returnTime").isEqualTo("$yesterday")
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
              scheduledTemporaryAbsence = scheduledTemporaryAbsence(
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.eventId").isEqualTo(scheduledTemporaryAbsence.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.eventDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.startTime").isEqualTo("$twoDaysAgo")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.eventStatus").isEqualTo("SCH")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.comment").isEqualTo("Scheduled temporary absence")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.escort").isEqualTo("L")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.toAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.transportType").isEqualTo("VAN")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.returnDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.returnTime").isEqualTo("$yesterday")
    }

    @Test
    fun `should retrieve scheduled temporary absence's external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTemporaryAbsence = scheduledTemporaryAbsence {
                temporaryAbsence = externalMovement(
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.sequence").isEqualTo(temporaryAbsence.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.movementDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.movementTime").isEqualTo("$twoDaysAgo")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.arrestAgency").isEqualTo("POL")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.escort").isEqualTo("L")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.toAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.commentText").isEqualTo("Tap OUT comment for scheduled absence")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.toAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.toAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
    }

    @Test
    fun `should retrieve scheduled temporary absences return`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTemporaryAbsence = scheduledTemporaryAbsence {
                scheduledTemporaryAbsenceReturn = scheduledReturn(
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduledTemporaryAbsenceReturn.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.eventDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.startTime").isEqualTo("$yesterday")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.eventSubType").isEqualTo("R25")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.eventStatus").isEqualTo("SCH")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.comment").isEqualTo("Scheduled temporary absence return")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.escort").isEqualTo("U")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.fromAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.toPrison").isEqualTo("LEI")
    }

    @Test
    fun `should retrieve scheduled temporary absence return's external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            application = temporaryAbsenceApplication {
              scheduledTemporaryAbsence = scheduledTemporaryAbsence {
                temporaryAbsence = externalMovement()

                scheduledTemporaryAbsenceReturn = scheduledReturn {
                  temporaryAbsenceReturn = externalMovement(
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.sequence").isEqualTo(temporaryAbsenceReturn.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.movementDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.movementTime").isEqualTo("$yesterday")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.movementReason").isEqualTo("R25")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.escort").isEqualTo("U")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.fromAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.toPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.commentText").isEqualTo("Tap IN comment")
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.fromAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.fromAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
    }

    @Test
    fun `should retrieve unscheduled temporary absence external movements`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          offenderAddress = address()
          booking = booking {
            unscheduledTemporaryAbsence = temporaryAbsence(
              date = LocalDateTime.now().minusDays(1),
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].sequence").isEqualTo(unscheduledTemporaryAbsence.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].movementTime").isEqualTo("$yesterday")
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence").isEqualTo(unscheduledTemporaryAbsenceReturn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementDate").isEqualTo("${today.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].movementTime").isEqualTo("$today")
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
              scheduledTemporaryAbsence = scheduledTemporaryAbsence {
                temporaryAbsence = externalMovement()
                scheduledTemporaryAbsenceReturn = scheduledReturn {
                  temporaryAbsenceReturn = externalMovement()
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_MOVEMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookings[0].bookingId").isEqualTo(booking.bookingId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].outsideMovements[0].outsideMovementId").isEqualTo(applicationOutsideMovement.movementApplicationMultiId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].movementApplicationId").isEqualTo(application.movementApplicationId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsence.eventId").isEqualTo(scheduledTemporaryAbsence.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsence.sequence").isEqualTo(temporaryAbsence.id.sequence)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].scheduledTemporaryAbsenceReturn.eventId").isEqualTo(scheduledTemporaryAbsenceReturn.eventId)
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].temporaryAbsenceReturn.sequence").isEqualTo(temporaryAbsenceReturn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].sequence").isEqualTo(unscheduledTemporaryAbsence.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].sequence").isEqualTo(unscheduledTemporaryAbsenceReturn.id.sequence)
    }
  }
}
