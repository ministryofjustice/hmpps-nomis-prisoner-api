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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderMovementApplicationMulti
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationMultiRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderMovementApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTemporaryAbsenceRepository
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("41  High Street  Sheffield    S1 1AA")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("41  High Street  Sheffield    S2 2AA")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].scheduledTemporaryAbsence.toFullAddress").isEqualTo("2  Gloucester Terrace  Stanningley Road  29059  W.YORKSHIRE  LS3 3AA  ENG")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("41  High Street  Sheffield    S1 1AA")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("41  High Street  Sheffield    S2 2AA")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("2  Gloucester Terrace  Stanningley Road  29059  W.YORKSHIRE  LS3 3AA  ENG")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsence.toFullAddress").isEqualTo("2  Gloucester Terrace  Stanningley Road  29059  W.YORKSHIRE  LS3 3AA  ENG")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("41  High Street  Sheffield    S1 1AA")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("41  High Street  Sheffield    S2 2AA")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("2  Gloucester Terrace  Stanningley Road  29059  W.YORKSHIRE  LS3 3AA  ENG")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("2  Gloucester Terrace  Stanningley Road  29059  W.YORKSHIRE  LS3 3AA  ENG")
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
        .jsonPath("$.bookings[0].temporaryAbsenceApplications[0].absences[0].temporaryAbsenceReturn.fromFullAddress").isEqualTo("2  Gloucester Terrace  Stanningley Road  29059  W.YORKSHIRE  LS3 3AA  ENG")
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
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsences[0].toFullAddress").isEqualTo("41  High Street  Sheffield    S1 1AA")
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
        .jsonPath("$.bookings[0].unscheduledTemporaryAbsenceReturns[0].fromFullAddress").isEqualTo("41  High Street  Sheffield    S1 1AA")
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
                outsideMovement()
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
            assertThat(toFullAddress).isEqualTo("3B  Brown Court  Scotland Street  Hunters Bar  25343  S.YORKSHIRE  S1 3GG  ENG")
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
            assertThat(toFullAddress).isEqualTo("2  Herries Road  Stanningley Road  25343  S.YORKSHIRE  S5 7AU  ENG")
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
  @DisplayName("PUT /movements/{offenderNo}/temporary-absences/scheduled-temporary-absence")
  inner class UpsertScheduledTemporaryAbsence {

    private fun anUpsertRequest(
      movementApplicationId: Long? = null,
      eventId: Long? = null,
      returnEventStatus: String? = null,
      eventStatus: String = "SCH",
      toAddress: UpsertTemporaryAbsenceAddress = UpsertTemporaryAbsenceAddress(id = offenderAddress.addressId),
    ) = UpsertScheduledTemporaryAbsenceRequest(
      eventId = eventId,
      movementApplicationId = movementApplicationId ?: application.movementApplicationId,
      eventDate = twoDaysAgo.toLocalDate(),
      startTime = twoDaysAgo,
      eventSubType = "C5",
      eventStatus = eventStatus,
      comment = "Some comment scheduled temporary absence",
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
            toAddress = UpsertTemporaryAbsenceAddress(ownerClass = "OFF", addressText = "1 House, Street, City", postalCode = "A1 1AA"),
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
              ownerClass = "OFF",
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
      fun `should create corporate address and corporate entity`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertRequest(
            movementApplicationId = application.movementApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(ownerClass = "CORP", addressText = "1 House, Street, City", postalCode = "A1 1AA", name = "Company"),
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
      fun `should create corporate address and corporate entity for an agency address`() {
        webTestClient.upsertScheduledTemporaryAbsenceOk(
          request = anUpsertRequest(
            movementApplicationId = application.movementApplicationId,
            toAddress = UpsertTemporaryAbsenceAddress(ownerClass = "AGY", addressText = "1 House, Street, City", postalCode = "A1 1AA", name = "Agency"),
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
                assertThat(corporateAddress.corporate.corporateName).isEqualTo("Agency")
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
              ownerClass = "CORP",
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
      fun `should return bad request for invalid to address owner class`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(ownerClass = "INVALID", addressText = "address")
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("INVALID").contains("not supported")
          }
      }

      @Test
      fun `should return bad request if address owner class not passed`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(ownerClass = null, addressText = "address", name = "Business")
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("owner class")
          }
      }

      @Test
      fun `should return bad request if address text not passed`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(ownerClass = "CORP", addressText = null, name = "Business")
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("address text")
          }
      }

      @Test
      fun `should return bad request if name missing for corporate address`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(ownerClass = "CORP", addressText = "address", name = null)
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("name is required")
          }
      }

      @Test
      fun `should return bad request if name missing for agency address`() {
        val invalidAddress = UpsertTemporaryAbsenceAddress(ownerClass = "AGY", addressText = "address", name = null)
        webTestClient.upsertScheduledTemporaryAbsenceBadRequest(anUpsertRequest().copy(toAddress = invalidAddress))
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("name is required")
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
                assertThat(toFullAddress).isEqualTo("Flat 1  41  High Street  Hillsborough  25343  S.YORKSHIRE  S1 1AB  ENG")
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
                assertThat(toFullAddress).isEqualTo("3B  Brown Court  Scotland Street  Hunters Bar  25343  S.YORKSHIRE  S1 3GG  ENG")
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
                assertThat(toFullAddress).isEqualTo("3B  Brown Court  Scotland Street  Hunters Bar  25343  S.YORKSHIRE  S1 3GG  ENG")
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
                assertThat(toFullAddress).isEqualTo("2  Herries Road  Stanningley Road  25343  S.YORKSHIRE  S5 7AU  ENG")
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
                assertThat(toFullAddress).isEqualTo("2  Herries Road  Stanningley Road  25343  S.YORKSHIRE  S5 7AU  ENG")
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
                assertThat(fromFullAddress).isEqualTo("Flat 1  41  High Street  Hillsborough  25343  S.YORKSHIRE  S1 1AB  ENG")
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
                assertThat(fromFullAddress).isEqualTo("3B  Brown Court  Scotland Street  Hunters Bar  25343  S.YORKSHIRE  S1 3GG  ENG")
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
                assertThat(fromFullAddress).isEqualTo("3B  Brown Court  Scotland Street  Hunters Bar  25343  S.YORKSHIRE  S1 3GG  ENG")
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
                assertThat(fromFullAddress).isEqualTo("3B  Brown Court  Scotland Street  Hunters Bar  25343  S.YORKSHIRE  S1 3GG  ENG")
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
                assertThat(fromFullAddress).isEqualTo("2  Herries Road  Stanningley Road  25343  S.YORKSHIRE  S5 7AU  ENG")
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
                assertThat(fromFullAddress).isEqualTo("2  Herries Road  Stanningley Road  25343  S.YORKSHIRE  S5 7AU  ENG")
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
                assertThat(fromFullAddress).isEqualTo("2  Herries Road  Stanningley Road  25343  S.YORKSHIRE  S5 7AU  ENG")
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
}
