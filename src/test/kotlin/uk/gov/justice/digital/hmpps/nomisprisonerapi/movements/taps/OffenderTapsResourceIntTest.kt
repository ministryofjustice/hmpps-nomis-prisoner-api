package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender.BookingTaps
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender.OffenderTapMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender.OffenderTapsIdsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender.OffenderTapsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender.TapSummary
import java.time.LocalDate
import java.time.LocalDateTime

class OffenderTapsResourceIntTest(
  @Autowired private val scheduleOutRepository: OffenderTapScheduleOutRepository,
  @Autowired private val scheduleInRepository: OffenderTapScheduleInRepository,
  @Autowired private val offenderRepository: OffenderRepository,
  @Autowired private val agencyLocationRepository: AgencyLocationRepository,
  @Autowired private val corporateRepository: CorporateRepository,
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var offenderAddress: OffenderAddress
  private lateinit var booking: OffenderBooking
  private lateinit var application: OffenderTapApplication
  private lateinit var scheduleOut: OffenderTapScheduleOut
  private lateinit var scheduleIn: OffenderTapScheduleIn
  private lateinit var movementOut: OffenderTapMovementOut
  private lateinit var movementIn: OffenderTapMovementIn
  private lateinit var unscheduledMovementOut: OffenderTapMovementOut
  private lateinit var unscheduledMovementIn: OffenderTapMovementIn
  private lateinit var agencyLocation: AgencyLocation
  private lateinit var orphanedScheduleOut: OffenderTapScheduleOut
  private lateinit var orphanedScheduleIn: OffenderTapScheduleIn

  private val offenderNo = "D6347ED"
  private val today: LocalDateTime = LocalDateTime.now().roundToNearestSecond()
  private val yesterday: LocalDateTime = today.minusDays(1)
  private val twoDaysAgo: LocalDateTime = today.minusDays(2)

  @AfterEach
  fun `tear down`() {
    // This must be removed before the offender booking due to a foreign key constraint (Hibernate is no longer managing this entity)
    if (this::orphanedScheduleOut.isInitialized) {
      scheduleOutRepository.delete(orphanedScheduleOut)
    }
    if (this::orphanedScheduleIn.isInitialized) {
      scheduleInRepository.delete(orphanedScheduleIn)
    }
    offenderRepository.deleteAll()
    if (this::agencyLocation.isInitialized) {
      agencyLocationRepository.delete(agencyLocation)
    }
    corporateRepository.deleteAll()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/taps")
  inner class GetOffenderTaps {
    private lateinit var agencyAddress: AgencyLocationAddress

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/$offenderNo/taps")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if offender not found`() {
      webTestClient.get()
        .uri("/movements/UNKNOWN/taps")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].bookingId").isEqualTo(booking.bookingId)
        .jsonPath("$.bookings[0].activeBooking").isEqualTo(true)
        .jsonPath("$.bookings[0].latestBooking").isEqualTo(true)
        .jsonPath("$.bookings[0].tapApplications[0].tapApplicationId").isEqualTo(application.tapApplicationId)
        .jsonPath("$.bookings[0].tapApplications[0].eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].tapApplications[0].applicationDate").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].fromDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].tapApplications[0].releaseTime").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].toDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].tapApplications[0].returnTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].applicationType").isEqualTo("SINGLE")
        .jsonPath("$.bookings[0].tapApplications[0].applicationStatus").isEqualTo("APP-SCH")
        .jsonPath("$.bookings[0].tapApplications[0].escortCode").isEqualTo("L")
        .jsonPath("$.bookings[0].tapApplications[0].transportType").isEqualTo("VAN")
        .jsonPath("$.bookings[0].tapApplications[0].comment").isEqualTo("Some comment application")
        .jsonPath("$.bookings[0].tapApplications[0].prisonId").isEqualTo("LEI")
        .jsonPath("$.bookings[0].tapApplications[0].toAgencyId").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].tapApplications[0].toAddressId").isEqualTo(offenderAddress.addressId)
        .jsonPath("$.bookings[0].tapApplications[0].toAddressOwnerClass").isEqualTo(offenderAddress.addressOwnerClass)
        .jsonPath("$.bookings[0].tapApplications[0].toAddressOwnerDescription").doesNotExist()
        .jsonPath("$.bookings[0].tapApplications[0].toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].tapApplications[0].toAddressPostcode").isEqualTo("S1 1AA")
        .jsonPath("$.bookings[0].tapApplications[0].contactPersonName").isEqualTo("Derek")
        .jsonPath("$.bookings[0].tapApplications[0].tapType").isEqualTo("RR")
        .jsonPath("$.bookings[0].tapApplications[0].tapSubType").isEqualTo("RDR")
    }

    @Test
    fun `should retrieve schedule outs`() {
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
                comment = "Tap schedule out",
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.eventId").isEqualTo(scheduleOut.eventId)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.eventDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.startTime").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.eventSubType").isEqualTo("C5")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.eventStatus").isEqualTo("SCH")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.comment").isEqualTo("Tap schedule out")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.escort").isEqualTo("L")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.transportType").isEqualTo("VAN")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.returnDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.returnTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressId").isEqualTo("${offenderAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressOwnerClass").isEqualTo("OFF")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressPostcode").isEqualTo("S1 1AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressId").isEqualTo("${corporateAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressOwnerClass").isEqualTo("CORP")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressPostcode").isEqualTo("S2 2AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve schedule out's external movements`() {
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.sequence").isEqualTo(movementOut.id.sequence)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.movementDate").isEqualTo("${twoDaysAgo.toLocalDate()}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.movementTime").value<String> {
          assertThat(it).startsWith("${twoDaysAgo.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.arrestAgency").isEqualTo("POL")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.escort").isEqualTo("L")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.commentText").isEqualTo("Tap OUT comment for scheduled absence")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressId").isEqualTo("${offenderAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressOwnerClass").isEqualTo("OFF")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressPostcode").isEqualTo("S1 1AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressId").isEqualTo("${corporateAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressOwnerClass").isEqualTo("CORP")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressPostcode").isEqualTo("S2 2AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressPostcode").isEqualTo("LS3 3AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.toAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve tap schedule in`() {
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
                  comment = "Tap schedule in",
                  escort = "U",
                  fromAgency = "HAZLWD",
                  toPrison = "LEI",
                )
              }
            }
          }
        }
      }

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.eventId").isEqualTo(scheduleIn.eventId)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.eventDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.startTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.eventSubType").isEqualTo("R25")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.eventStatus").isEqualTo("SCH")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.comment").isEqualTo("Tap schedule in")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.escort").isEqualTo("U")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.fromAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.toPrison").isEqualTo("LEI")
    }

    @Test
    fun `should retrieve tap schedule out's external movements`() {
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.sequence").isEqualTo(movementIn.id.sequence)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.movementDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.movementTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.movementReason").isEqualTo("R25")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.escort").isEqualTo("U")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAgency").isEqualTo("HAZLWD")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.toPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.commentText").isEqualTo("Tap IN comment")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressId").isEqualTo("${offenderAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressOwnerClass").isEqualTo("OFF")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressPostcode").isEqualTo("S1 1AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressId").isEqualTo("${corporateAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressOwnerClass").isEqualTo("CORP")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromFullAddress").isEqualTo("41 High Street, Sheffield")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressPostcode").isEqualTo("S2 2AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressPostcode").isEqualTo("LS3 3AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressPostcode").isEqualTo("LS3 3AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressId").isEqualTo("${agencyAddress.addressId}")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressDescription").isEqualTo("Big Hospital")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromFullAddress").isEqualTo("2 Gloucester Terrace, Stanningley Road, Leeds, West Yorkshire, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.fromAddressPostcode").isEqualTo("LS3 3AA")
    }

    @Test
    fun `should retrieve unscheduled tap movements with agency address`() {
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].sequence").isEqualTo(unscheduledMovementOut.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].movementDate").isEqualTo("${yesterday.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].movementTime").value<String> {
          assertThat(it).startsWith("${yesterday.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].escort").isEqualTo("U")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].fromPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAgency").isEqualTo("NGENHO")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].commentText").isEqualTo("Tap OUT comment")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressId").isEqualTo(agencyAddress.addressId)
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressDescription").isEqualTo("Northern General Hospital")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toFullAddress").isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressPostcode").isEqualTo("S5 7AU")
    }

    @Test
    fun `should retrieve city description if no address on unscheduled tap movement`() {
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressId").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressOwnerClass").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toFullAddress").isEqualTo("Sheffield")
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].toAddressPostcode").doesNotExist()
    }

    @Test
    fun `should retrieve unscheduled tap movement in with agency address`() {
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].sequence")
        .isEqualTo(unscheduledMovementIn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].movementDate")
        .isEqualTo("${today.toLocalDate()}")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].movementTime").value<String> {
          assertThat(it).startsWith("${today.toLocalDate()}")
        }
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].movementReason").isEqualTo("C5")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].escort").isEqualTo("U")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].escortText").isEqualTo("SE")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAgency").isEqualTo("NGENHO")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].toPrison").isEqualTo("LEI")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].commentText").isEqualTo("Tap IN comment")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressId")
        .isEqualTo(agencyAddress.addressId)
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressOwnerClass").isEqualTo("AGY")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressDescription").isEqualTo("Northern General Hospital")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromFullAddress").isEqualTo("2 Herries Road, Stanningley Road, Sheffield, South Yorkshire, England")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressPostcode").isEqualTo("S5 7AU")
    }

    @Test
    fun `should retrieve city description from unscheduled tap movement in`() {
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressId").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressOwnerClass").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressDescription").doesNotExist()
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromFullAddress").isEqualTo("Sheffield")
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].fromAddressPostcode").doesNotExist()
    }

    @Test
    fun `should retrieve all tap schedules and movements`() {
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].bookingId").isEqualTo(booking.bookingId)
        .jsonPath("$.bookings[0].tapApplications.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].tapApplications[0].tapApplicationId").isEqualTo(application.tapApplicationId)
        .jsonPath("$.bookings[0].tapApplications[0].taps.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.eventId").isEqualTo(scheduleOut.eventId)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementOut.sequence").isEqualTo(movementOut.id.sequence)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.eventId").isEqualTo(scheduleIn.eventId)
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapMovementIn.sequence").isEqualTo(movementIn.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTapMovementOuts[0].sequence").isEqualTo(unscheduledMovementOut.id.sequence)
        .jsonPath("$.bookings[0].unscheduledTapMovementIns.length()").isEqualTo(1)
        .jsonPath("$.bookings[0].unscheduledTapMovementIns[0].sequence").isEqualTo(unscheduledMovementIn.id.sequence)
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.returnTime").isEqualTo(tomorrow)
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].toAddressId").doesNotExist()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressId").doesNotExist()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleIn.fromAddressId").doesNotExist()
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toFullAddress").isEqualTo("High Street, Sheffield, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressPostcode").isEqualTo("S2 2AA")
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

      webTestClient.getOffenderTaps()
        .expectBody()
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressDescription").isEqualTo("Boots")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toFullAddress").isEqualTo("41 High Street, Sheffield, England")
        .jsonPath("$.bookings[0].tapApplications[0].taps[0].tapScheduleOut.toAddressPostcode").isEqualTo("")
    }
  }

  @Nested
  @DisplayName("Migration with merged data")
  inner class GetOffenderTapsForMergedData {
    lateinit var mergedBooking: OffenderBooking
    lateinit var mergedApplication: OffenderTapApplication
    lateinit var mergedTapScheduleOut: OffenderTapScheduleOut
    lateinit var mergedTapScheduleIn: OffenderTapScheduleIn
    lateinit var mergedTapMovementOut: OffenderTapMovementOut
    lateinit var mergedTapMovementIn: OffenderTapMovementIn

    lateinit var tapScheduleOut2: OffenderTapScheduleOut
    lateinit var tapScheduleIn2: OffenderTapScheduleIn
    lateinit var mergedTapScheduleOut2: OffenderTapScheduleOut
    lateinit var mergedTapScheduleIn2: OffenderTapScheduleIn
    lateinit var mergedTapMovementOut2: OffenderTapMovementOut
    lateinit var mergedTapMovementIn2: OffenderTapMovementIn

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
              mergedTapScheduleOut = tapScheduleOut(eventDate = today) {
                mergedTapMovementOut = tapMovementOut()
                mergedTapScheduleIn = tapScheduleIn(eventDate = today) {
                  mergedTapMovementIn = tapMovementIn()
                }
              }
              mergedTapScheduleOut2 = tapScheduleOut(eventDate = tomorrow) {
                mergedTapMovementOut2 = tapMovementOut()
                mergedTapScheduleIn2 = tapScheduleIn(eventDate = tomorrow) {
                  mergedTapMovementIn2 = tapMovementIn()
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
              tapScheduleOut2 = tapScheduleOut(eventDate = tomorrow) {
                tapScheduleIn2 = tapScheduleIn(eventDate = tomorrow)
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
            set ostr.tapScheduleOut = (from OffenderTapScheduleOut where eventId = ${mergedTapScheduleOut.eventId}),
            ostr.tapApplication = (from OffenderTapApplication  where tapApplicationId = ${application.tapApplicationId})
            where eventId = ${scheduleIn.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Also corrupt the same data for the 2nd scheduled absence from the application to test repeating applications
        entityManager.createQuery(
          """
            update OffenderTapScheduleIn ostr
            set ostr.tapScheduleOut = (from OffenderTapScheduleOut where eventId = ${mergedTapScheduleOut2.eventId}),
            ostr.tapApplication = (from OffenderTapApplication  where tapApplicationId = ${application.tapApplicationId})
            where eventId = ${tapScheduleIn2.eventId}
          """.trimIndent(),
        ).executeUpdate()

        // Also corrupt the merged movements by removing the link to the schedules in the same way the NOMIS merge process does
        entityManager.createQuery(
          """
            update OffenderTapMovementOut ota
            set ota.tapScheduleOut = null
            where id.offenderBooking.id = ${mergedBooking.bookingId}
            and id.sequence in (${mergedTapMovementOut.id.sequence}, ${mergedTapMovementOut2.id.sequence})
          """.trimIndent(),
        ).executeUpdate()
        entityManager.createQuery(
          """
            update OffenderTapMovementIn otar
            set otar.tapScheduleIn = null
            where id.offenderBooking.id = ${mergedBooking.bookingId} 
            and id.sequence in (${mergedTapMovementIn.id.sequence}, ${mergedTapMovementIn2.id.sequence})
          """.trimIndent(),
        ).executeUpdate()
      }
    }

    @Test
    fun `should retrieve the tap movement out from the merged booking's first application schedule`() {
      webTestClient.getOffenderTapsWithResponse()
        .apply {
          val book = bookings.first()
          val application = book.tapApplications.first()
          assertThat(book.bookingId).isEqualTo(mergedBooking.bookingId)
          assertThat(book.activeBooking).isEqualTo(false)
          assertThat(book.latestBooking).isEqualTo(false)
          assertThat(book.tapApplications.size).isEqualTo(1)
          assertThat(application.tapApplicationId).isEqualTo(mergedApplication.tapApplicationId)
          with(application.taps.first()) {
            assertThat(tapScheduleOut!!.eventId).isEqualTo(mergedTapScheduleOut.eventId)
            assertThat(tapMovementOut).isNull()
            assertThat(tapScheduleIn!!.eventId).isEqualTo(mergedTapScheduleIn.eventId)
            assertThat(tapMovementIn).isNull()
          }
          // The actual movements were unlinked by the merge process so should appear as unscheduled
          assertThat(bookings.first().unscheduledTapMovementOuts[0].sequence).isEqualTo(mergedTapMovementOut.id.sequence)
          assertThat(bookings.first().unscheduledTapMovementIns[0].sequence).isEqualTo(mergedTapMovementIn.id.sequence)
        }
    }

    @Test
    fun `should retrieve the tap movement out from the merged booking's second application schedule`() {
      webTestClient.getOffenderTapsWithResponse()
        .apply {
          with(bookings.first().tapApplications.first().taps[1]) {
            assertThat(tapScheduleOut!!.eventId).isEqualTo(mergedTapScheduleOut2.eventId)
            assertThat(tapMovementOut).isNull()
            assertThat(tapScheduleIn!!.eventId).isEqualTo(mergedTapScheduleIn2.eventId)
            assertThat(tapMovementIn).isNull()
          }
          // The actual movements were unlinked by the merge process so should appear as unscheduled
          assertThat(bookings.first().unscheduledTapMovementOuts[1].sequence).isEqualTo(mergedTapMovementOut2.id.sequence)
          assertThat(bookings.first().unscheduledTapMovementIns[1].sequence).isEqualTo(mergedTapMovementIn2.id.sequence)
        }
    }

    @Test
    fun `should retrieve the tap movement out from the TAP copied onto the latest booking`() {
      webTestClient.getOffenderTapsWithResponse()
        .apply {
          val book = bookings[1]
          val application = book.tapApplications.first()
          assertThat(book.bookingId).isEqualTo(booking.bookingId)
          assertThat(book.activeBooking).isEqualTo(true)
          assertThat(book.latestBooking).isEqualTo(true)
          assertThat(book.tapApplications.size).isEqualTo(1)
          assertThat(application.tapApplicationId).isEqualTo(application.tapApplicationId)
          with(application.taps[1]) {
            assertThat(tapScheduleOut!!.eventId).isEqualTo(scheduleOut.eventId)
            assertThat(tapMovementOut).isNull()
            assertThat(tapScheduleIn!!.eventId).isEqualTo(scheduleIn.eventId)
            assertThat(tapMovementIn).isNull()
          }
        }
    }

    @Test
    fun `should retrieve the tap schedule from the second TAP copied onto the latest booking`() {
      webTestClient.getOffenderTapsWithResponse()
        .apply {
          with(bookings[1].tapApplications.first().taps[0]) {
            assertThat(tapScheduleOut!!.eventId).isEqualTo(tapScheduleOut2.eventId)
            assertThat(tapMovementOut).isNull()
            assertThat(tapScheduleIn!!.eventId).isEqualTo(tapScheduleIn2.eventId)
            assertThat(tapMovementIn).isNull()
          }
        }
    }

    @Test
    fun `reconciliation should reflect the merged movements correctly`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(applications.count).isEqualTo(2)
          assertThat(scheduledOuts.count).isEqualTo(4)
          assertThat(movements.count).isEqualTo(4)
          // The merged movements are treated as unscheduled because the merge process removes the link to the underlying scheduled movement
          assertThat(movements.scheduled.outCount).isEqualTo(0)
          assertThat(movements.scheduled.inCount).isEqualTo(0)
          assertThat(movements.unscheduled.outCount).isEqualTo(2)
          assertThat(movements.unscheduled.inCount).isEqualTo(2)
        }
    }
  }

  @Nested
  @DisplayName("Migration for deleted schedule with movements")
  inner class GetOffenderTapsForDeletedScheduleWthMovement {
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
                orphanedScheduleIn = tapScheduleIn(eventDate = today.toLocalDate()) {
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
        orphanedScheduleIn = scheduleInRepository.findByIdOrNull(orphanedScheduleIn.eventId) ?: throw IllegalStateException("Failed to reload scheduled return movement - this should not happen")
      }
    }

    @Test
    fun `should include the TAP with deleted schedule OUT as unscheduled`() {
      webTestClient.getOffenderTapsWithResponse()
        .apply {
          val book = bookings.first()
          assertThat(book.tapApplications.size).isEqualTo(2)
          // The TAP with the deleted scheduled absence is not included in the migration as a scheduled movement
          assertThat(book.tapApplications[0].taps).isEmpty()
          // The control TAP is migrated
          with(book.tapApplications[1].taps.first()) {
            assertThat(tapScheduleOut).isNotNull()
            assertThat(tapMovementOut).isNotNull()
            assertThat(tapScheduleIn).isNotNull()
            assertThat(tapMovementIn).isNotNull()
          }
          // The TAP with deleted scheduled absence has both movements included as unscheduled
          assertThat(book.unscheduledTapMovementOuts.size).isEqualTo(1)
          assertThat(book.unscheduledTapMovementIns.size).isEqualTo(1)
        }
    }

    @Test
    fun `reconciliation should include the TAP with deleted scheduled OUT as unscheduled`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(applications.count).isEqualTo(2)
          assertThat(scheduledOuts.count).isEqualTo(1)
          assertThat(movements.count).isEqualTo(4)
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(1)
          assertThat(movements.unscheduled.outCount).isEqualTo(1)
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }
  }

  @Nested
  @DisplayName("Migration for multiple schedule IN movements")
  inner class GetOffenderTapsForMultipleScheduledInMovements {
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
      webTestClient.getOffenderTapsWithResponse()
        .apply {
          val book = bookings.first()
          assertThat(book.tapApplications.size).isEqualTo(1)
          // The correct scheduled IN movement is chosen
          with(bookings[0].tapApplications[0].taps[0]) {
            assertThat(tapScheduleIn!!.eventId).isEqualTo(scheduleIn.eventId)
            assertThat(tapMovementIn).isNotNull
          }
        }
    }

    @Test
    fun `reconciliation should include only one of the scheduled IN movements`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(scheduledOuts.count).isEqualTo(1)
          assertThat(movements.scheduled.outCount).isEqualTo(1)
          assertThat(movements.scheduled.inCount).isEqualTo(1)
          assertThat(movements.unscheduled.outCount).isEqualTo(0)
          assertThat(movements.unscheduled.inCount).isEqualTo(0)
        }
    }
  }

  @Nested
  @DisplayName("GET /movements/booking/{bookingId}/taps")
  inner class GetBookingTaps {
    @Test
    fun `should return unauthorized for missing token`() {
      webTestClient.get()
        .uri("/movements/booking/12345/taps")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/booking/12345/taps")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/booking/12345/taps")
        .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found for unknown booking id`() {
      webTestClient.getBookingTaps(12345)
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

      webTestClient.getBookingTapsOk()
        .apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(activeBooking).isTrue()
          assertThat(latestBooking).isTrue()
          assertThat(tapApplications).hasSize(1)
          assertThat(tapApplications[0].tapApplicationId).isEqualTo(firstApplication.tapApplicationId)
          assertThat(tapApplications[0].taps).hasSize(1)
          assertThat(tapApplications[0].taps[0].tapScheduleOut?.eventId).isEqualTo(scheduleOut.eventId)
          assertThat(tapApplications[0].taps[0].tapScheduleIn?.eventId).isEqualTo(scheduleIn.eventId)
          assertThat(tapApplications[0].taps[0].tapMovementOut?.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(tapApplications[0].taps[0].tapMovementIn?.sequence).isEqualTo(movementIn.id.sequence)
          assertThat(unscheduledTapMovementOuts).extracting<Int> { it.sequence }.containsExactly(unscheduledMovementOut.id.sequence)
          assertThat(unscheduledTapMovementIns).extracting<Int> { it.sequence }.containsExactly(unscheduledMovementIn.id.sequence)
        }

      webTestClient.getBookingTapsOk(secondBooking.bookingId)
        .apply {
          assertThat(bookingId).isEqualTo(secondBooking.bookingId)
          assertThat(tapApplications).hasSize(1)
          assertThat(unscheduledTapMovementOuts).isEmpty()
          assertThat(unscheduledTapMovementIns).isEmpty()
        }
    }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/taps/summary")
  inner class GetOffenderTapsSummary {

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
            assertThat(scheduledOuts.count).isEqualTo(1)
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
            assertThat(scheduledOuts.count).isEqualTo(0)
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
            assertThat(scheduledOuts.count).isEqualTo(1)
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
                orphanedScheduleOut = tapScheduleOut()
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
           * Corrupt the data by nulling one of the schedule's tap application - such data exists in NOMIS but we ignore them from the migration and reconciliation
           */
          entityManager.createQuery(
            """
            update OffenderTapScheduleOut ost
            set ost.tapApplication = null
            where eventId = ${orphanedScheduleOut.eventId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderSummaryOk(offenderNo)
          .apply {
            assertThat(applications.count).isEqualTo(2)
            // We don't count the orphaned schedule in the reconciliation
            assertThat(scheduledOuts.count).isEqualTo(1)
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
            assertThat(scheduledOuts.count).isEqualTo(2)
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
                orphanedScheduleOut = tapScheduleOut {
                  tapMovementOut()
                  orphanedScheduleIn = tapScheduleIn {
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
            where ois.EVENT_ID = ${orphanedScheduleOut.eventId}
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
            assertThat(scheduledOuts.count).isEqualTo(0)
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
          .uri("/movements/$offenderNo/taps/summary")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/summary")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/summary")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/taps/ids")
  inner class GetTapsIds {
    private lateinit var booking2: OffenderBooking
    private lateinit var application2: OffenderTapApplication
    private lateinit var scheduleOut2: OffenderTapScheduleOut
    private lateinit var scheduleIn2: OffenderTapScheduleIn
    private lateinit var movementOut2: OffenderTapMovementOut
    private lateinit var movementIn2: OffenderTapMovementIn
    private lateinit var unscheduledMovementOut2: OffenderTapMovementOut
    private lateinit var unscheduledMovementIn2: OffenderTapMovementIn

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
              scheduleOut2 = tapScheduleOut {
                movementOut2 = tapMovementOut()
                scheduleIn2 = tapScheduleIn {
                  movementIn2 = tapMovementIn()
                }
              }
            }
            unscheduledMovementOut2 = tapMovementOut()
            unscheduledMovementIn2 = tapMovementIn()
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
            scheduleOut2.eventId,
          )
          assertThat(scheduleInIds).containsExactlyInAnyOrder(
            scheduleIn.eventId,
            scheduleIn2.eventId,
          )
          assertThat(scheduledMovementOutIds).containsExactlyInAnyOrder(
            OffenderTapMovementId(movementOut.id.offenderBooking.bookingId, movementOut.id.sequence),
            OffenderTapMovementId(movementOut2.id.offenderBooking.bookingId, movementOut2.id.sequence),
          )
          assertThat(scheduledMovementInIds).containsExactlyInAnyOrder(
            OffenderTapMovementId(movementIn.id.offenderBooking.bookingId, movementIn.id.sequence),
            OffenderTapMovementId(movementIn2.id.offenderBooking.bookingId, movementIn2.id.sequence),
          )
          assertThat(unscheduledMovementOutIds).containsExactlyInAnyOrder(
            OffenderTapMovementId(
              unscheduledMovementOut.id.offenderBooking.bookingId,
              unscheduledMovementOut.id.sequence,
            ),
            OffenderTapMovementId(
              unscheduledMovementOut2.id.offenderBooking.bookingId,
              unscheduledMovementOut2.id.sequence,
            ),
          )
          assertThat(unscheduledMovementInIds).containsExactlyInAnyOrder(
            OffenderTapMovementId(
              unscheduledMovementIn.id.offenderBooking.bookingId,
              unscheduledMovementIn.id.sequence,
            ),
            OffenderTapMovementId(
              unscheduledMovementIn2.id.offenderBooking.bookingId,
              unscheduledMovementIn2.id.sequence,
            ),
          )
        }
    }

    @Test
    fun `should return correct summary counts`() {
      webTestClient.getOffenderSummaryOk(offenderNo)
        .apply {
          assertThat(applications.count).isEqualTo(2)
          assertThat(scheduledOuts.count).isEqualTo(2)
          assertThat(movements.scheduled.outCount).isEqualTo(2)
          assertThat(movements.scheduled.inCount).isEqualTo(2)
          assertThat(movements.unscheduled.outCount).isEqualTo(2)
          assertThat(movements.unscheduled.inCount).isEqualTo(2)
        }
    }

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/movements/${offender.nomsId}/taps/ids")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/movements/${offender.nomsId}/taps/ids")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/movements/${offender.nomsId}/taps/ids")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }
  }

  private fun WebTestClient.getTapIds() = get()
    .uri("/movements/${offender.nomsId}/taps/ids")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<OffenderTapsIdsResponse>()

  private fun WebTestClient.getOffenderTaps() = get()
    .uri("/movements/${offender.nomsId}/taps")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk

  private fun WebTestClient.getOffenderTapsWithResponse() = getOffenderTaps()
    .expectBodyResponse<OffenderTapsResponse>()

  private fun WebTestClient.getBookingTaps(bookingId: Long) = get()
    .uri("/movements/booking/$bookingId/taps")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getBookingTapsOk(bookingId: Long = booking.bookingId) = getBookingTaps(bookingId)
    .expectStatus().isOk
    .expectBodyResponse<BookingTaps>()

  private fun WebTestClient.getOffenderSummary(offenderNo: String) = get()
    .uri("/movements/$offenderNo/taps/summary")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getOffenderSummaryOk(offenderNo: String) = getOffenderSummary(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<TapSummary>()
}
