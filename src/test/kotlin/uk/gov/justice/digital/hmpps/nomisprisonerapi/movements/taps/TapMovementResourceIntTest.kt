package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement.CreateTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement.CreateTapMovementInResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement.CreateTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement.CreateTapMovementOutResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement.TapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.movement.TapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender.OffenderTapsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.offender.TapSummary
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps.schedule.TapScheduleOut
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class TapMovementResourceIntTest(
  @Autowired val scheduleOutRepository: OffenderTapScheduleOutRepository,
  @Autowired val scheduleInRepository: OffenderTapScheduleInRepository,
  @Autowired val movementOutRepository: OffenderTapMovementOutRepository,
  @Autowired val movementInRepository: OffenderTapMovementInRepository,
  @Autowired val offenderExternalMovementRepository: OffenderExternalMovementRepository,
  @Autowired val corporateRepository: CorporateRepository,
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
  private lateinit var agencyLocation: AgencyLocation

  private val offenderNo = "D6347ED"
  private val today: LocalDateTime = LocalDateTime.now().roundToNearestSecond()
  private val twoDaysAgo: LocalDateTime = today.minusDays(2)
  private lateinit var orphanedSchedule: OffenderTapScheduleOut
  private lateinit var orphanedScheduleIn: OffenderTapScheduleIn

  @AfterEach
  fun `tear down`() {
    // This must be removed before the offender booking due to a foreign key constraint (Hibernate is no longer managing this entity)
    if (this::orphanedSchedule.isInitialized) {
      scheduleOutRepository.delete(orphanedSchedule)
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
  @DisplayName("GET /movements/{offenderNo}/taps/movement/out/{bookingId}/{movementSeq}")
  inner class GetTapMovementOut {

    @Nested
    inner class GetUnscheduledTapMovementOut {
      private lateinit var agencyAddress: AgencyLocationAddress
      private lateinit var cityBooking: OffenderBooking
      private lateinit var agencyBooking: OffenderBooking
      private lateinit var cityTapMovementOut: OffenderTapMovementOut
      private lateinit var agencyTapMovementOut: OffenderTapMovementOut

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
              cityTapMovementOut = tapMovementOut(
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
              agencyTapMovementOut = tapMovementOut(
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
          .uri("/movements/$offenderNo/taps/movement/out/${cityBooking.bookingId}/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/movement/out/${cityBooking.bookingId}/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/movement/out/${cityBooking.bookingId}/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/taps/movement/out/${cityBooking.bookingId}/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if tap movement out not found`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/movement/out/9999/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }

      @Test
      fun `should retrieve unscheduled tap movement out with city for address`() {
        webTestClient.getTapMovementOutOk(bookingId = cityBooking.bookingId, movementSeq = cityTapMovementOut.id.sequence)
          .apply {
            assertThat(bookingId).isEqualTo(cityBooking.bookingId)
            assertThat(sequence).isEqualTo(cityTapMovementOut.id.sequence)
            assertThat(tapScheduleOutId).isNull()
            assertThat(tapApplicationId).isNull()
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
      fun `should retrieve unscheduled tap movement out with agency for address`() {
        webTestClient.getTapMovementOutOk(bookingId = agencyBooking.bookingId, movementSeq = agencyTapMovementOut.id.sequence)
          .apply {
            assertThat(bookingId).isEqualTo(agencyBooking.bookingId)
            assertThat(sequence).isEqualTo(agencyTapMovementOut.id.sequence)
            assertThat(tapScheduleOutId).isNull()
            assertThat(tapApplicationId).isNull()
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
    inner class GetScheduledTapMovementOut {

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
        fun `should retrieve scheduled tap movement out`() {
          webTestClient.getTapMovementOutOk()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(movementOut.id.sequence)
              assertThat(tapScheduleOutId).isEqualTo(scheduleOut.eventId)
              assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
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
          fun `should retrieve address from tap schedule out`() {
            webTestClient.getTapMovementOutOk()
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
          fun `should take the address from the tap movement out`() {
            webTestClient.getTapMovementOutOk()
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
          fun `should take corporate address from tap schedule out`() {
            webTestClient.getTapMovementOutOk()
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
          fun `should take corporate address from tap movement out`() {
            webTestClient.getTapMovementOutOk()
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

        webTestClient.getTapScheduleOut()
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
          fun `should take agency address from tap schedule out`() {
            webTestClient.getTapMovementOutOk()
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
          fun `should take agency address from tap movement out`() {
            webTestClient.getTapMovementOutOk()
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
  @DisplayName("POST /movements/{offenderNo}/taps/movement/out")
  inner class CreateTapMovementOutTest {

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
      fun `should create tap movement out`() {
        webTestClient.createTapMovementOutOk()
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
                assertThat(commentText).isEqualTo("comment tap movement out")
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
        webTestClient.createTapMovementOut(offenderNo = "UNKNOWN")
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

        webTestClient.createTapMovementOut()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if tap schedule out does not exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }

        webTestClient.createTapMovementOut(request = aCreateTapMovementOutRequest(tapScheduleOutId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid movement reason`() {
        webTestClient.createTapMovementOutBadRequestUnknown(aCreateTapMovementOutRequest().copy(movementReason = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid arrest agency`() {
        webTestClient.createTapMovementOutBadRequestUnknown(aCreateTapMovementOutRequest().copy(arrestAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.createTapMovementOutBadRequestUnknown(aCreateTapMovementOutRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from prison`() {
        webTestClient.createTapMovementOutBadRequestUnknown(aCreateTapMovementOutRequest().copy(fromPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to agency`() {
        webTestClient.createTapMovementOutBadRequestUnknown(aCreateTapMovementOutRequest().copy(toAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        webTestClient.createTapMovementOutBadRequest(aCreateTapMovementOutRequest().copy(toAddressId = 9999))
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
          .uri("/movements/$offenderNo/taps/movement/out")
          .bodyValue(aCreateTapMovementOutRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/taps/movement/out")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateTapMovementOutRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/taps/movement/out")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateTapMovementOutRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class CreateUnscheduledTapMovementOut {
      @Test
      fun `should create unscheduled tap movement out`() {
        webTestClient.createTapMovementOutOk(aCreateTapMovementOutRequest(tapScheduleOutId = null))
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
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/taps/movement/in/{bookingId}/{movementSeq}")
  inner class GetTapMovementIn {

    @Nested
    inner class GetUnscheduledTapMovementIn {
      private lateinit var agencyAddress: AgencyLocationAddress
      private lateinit var cityBooking: OffenderBooking
      private lateinit var agencyBooking: OffenderBooking
      private lateinit var cityTapMovementIn: OffenderTapMovementIn
      private lateinit var agencyTapMovementIn: OffenderTapMovementIn

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
              cityTapMovementIn = tapMovementIn(
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
              agencyTapMovementIn = tapMovementIn(
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
          .uri("/movements/$offenderNo/taps/movement/in/${cityBooking.bookingId}/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/movement/in/${cityBooking.bookingId}/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/movement/in/${cityBooking.bookingId}/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return not found if offender not found`() {
        webTestClient.get()
          .uri("/movements/UNKNOWN/taps/movement/in/${cityBooking.bookingId}/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if tap movement in not found`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/taps/movement/in/9999/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("not found")
          }
      }

      @Test
      fun `should retrieve unscheduled tap movement in with city address`() {
        webTestClient.getTapMovementInOk(bookingId = cityBooking.bookingId, movementSeq = cityTapMovementIn.id.sequence)
          .apply {
            assertThat(bookingId).isEqualTo(cityBooking.bookingId)
            assertThat(sequence).isEqualTo(cityTapMovementIn.id.sequence)
            assertThat(tapScheduleOutId).isNull()
            assertThat(tapScheduleInId).isNull()
            assertThat(tapApplicationId).isNull()
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
      fun `should retrieve unscheduled tap movement in with agency address`() {
        webTestClient.getTapMovementInOk(bookingId = agencyBooking.bookingId, movementSeq = agencyTapMovementIn.id.sequence)
          .apply {
            assertThat(bookingId).isEqualTo(agencyBooking.bookingId)
            assertThat(sequence).isEqualTo(agencyTapMovementIn.id.sequence)
            assertThat(tapScheduleOutId).isNull()
            assertThat(tapScheduleInId).isNull()
            assertThat(tapApplicationId).isNull()
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
    inner class GetScheduledTapMovementIn {

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
        fun `should retrieve scheduled tap movement in`() {
          webTestClient.getTapMovementInOk()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(movementIn.id.sequence)
              assertThat(tapScheduleOutId).isEqualTo(scheduleOut.eventId)
              assertThat(tapScheduleInId).isEqualTo(scheduleIn.eventId)
              assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
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
          fun `should retrieve address from tap scheduled OUT`() {
            webTestClient.getTapMovementInOk()
              .apply {
                assertThat(fromAddressId).isEqualTo(offenderAddress.addressId)
                assertThat(fromAddressOwnerClass).isEqualTo(offenderAddress.addressOwnerClass)
                assertThat(fromFullAddress).isEqualTo("Flat 1, 41 High Street, Hillsborough, Sheffield, South Yorkshire, England")
                assertThat(fromAddressPostcode).isEqualTo("S1 1AB")
              }
          }
        }

        @Nested
        @DisplayName("With address on tap scheduled OUT and tap movement OUT but not tap movement IN")
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
          fun `should retrieve address from tap movement OUT`() {
            webTestClient.getTapMovementInOk()
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
            webTestClient.getTapMovementInOk()
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
          fun `should retrieve corporate address from tap scheduled OUT`() {
            webTestClient.getTapMovementInOk()
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
            webTestClient.getTapMovementInOk()
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
            webTestClient.getTapMovementInOk()
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
            webTestClient.getTapMovementInOk()
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
            webTestClient.getTapMovementInOk()
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
            webTestClient.getTapMovementInOk()
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
        fun `should retrieve scheduled tap movement in as unscheduled`() {
          webTestClient.getTapMovementInOk()
            .apply {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(movementIn.id.sequence)
              assertThat(tapScheduleOutId).isNull()
              assertThat(tapScheduleInId).isNull()
              assertThat(tapApplicationId).isNull()
            }
        }
      }
    }
  }

  @Nested
  @DisplayName("POST /movements/{offenderNo}/taps/movement/in")
  inner class CreateTapMovementInTest {

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
      fun `should create tap movement in`() {
        webTestClient.createTapMovementInOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            // Note there is already an admission external movement on sequence 1 and the tap movement out on sequence 2
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
                assertThat(commentText).isEqualTo("comment tap movement in")
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
        webTestClient.createTapMovementIn(offenderNo = "UNKNOWN")
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

        webTestClient.createTapMovementIn()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if tap schedule out does not exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE") {
            offenderAddress = address()
            booking = booking {
              application = tapApplication()
            }
          }
        }

        webTestClient.createTapMovementIn(request = aCreateTapMovementInRequest(tapScheduleInId = 9999))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("9999").contains("does not exist")
          }
      }

      @Test
      fun `should return bad request for invalid movement reason`() {
        webTestClient.createTapMovementInBadRequestUnknown(aCreateTapMovementInRequest().copy(movementReason = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid arrest agency`() {
        webTestClient.createTapMovementInBadRequestUnknown(aCreateTapMovementInRequest().copy(arrestAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid escort`() {
        webTestClient.createTapMovementInBadRequestUnknown(aCreateTapMovementInRequest().copy(escort = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid from agency`() {
        webTestClient.createTapMovementInBadRequestUnknown(aCreateTapMovementInRequest().copy(fromAgency = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to prison`() {
        webTestClient.createTapMovementInBadRequestUnknown(aCreateTapMovementInRequest().copy(toPrison = "UNKNOWN"))
      }

      @Test
      fun `should return bad request for invalid to address id`() {
        webTestClient.createTapMovementInBadRequest(aCreateTapMovementInRequest().copy(fromAddressId = 9999))
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
          .uri("/movements/$offenderNo/taps/movement/in")
          .bodyValue(aCreateTapMovementInRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/taps/movement/in")
          .headers(setAuthorisation(roles = listOf()))
          .bodyValue(aCreateTapMovementInRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.post()
          .uri("/movements/$offenderNo/taps/movement/in")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aCreateTapMovementInRequest(application.tapApplicationId))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Nested
    inner class CreateUnscheduledTapMovementIn {
      @Test
      fun `should create unscheduled tap movement in`() {
        webTestClient.createTapMovementInOk(aCreateTapMovementInRequest(tapScheduleInId = null))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            // Note there is already an admission external movement on sequence 1 and the tap movement out on sequence 2
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
  inner class GetUnscheduledTapMovementOutDueToBadData {

    @Test
    fun `scheduled OUT has been deleted`() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                orphanedScheduleIn = tapScheduleIn {
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
        orphanedScheduleIn = scheduleInRepository.findByIdOrNull(orphanedScheduleIn.eventId) ?: throw IllegalStateException("Failed to reload scheduled return movement - this should not happen")
      }

      // Sync movement OUT is unscheduled
      webTestClient.getTapMovementOutOk(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
        }

      // Sync the movement IN is unscheduled
      webTestClient.getTapMovementInOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender, the movements are unscheduled
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].tapApplications[0].taps).isEmpty()
          assertThat(bookings[0].unscheduledTapMovementOuts[0].sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(movementIn.id.sequence)
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
      lateinit var wrongScheduleIn: OffenderTapScheduleIn
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
                wrongScheduleIn = tapScheduleIn()
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
              set EVENT_ID = ${wrongScheduleIn.eventId}
              where OFFENDER_BOOK_ID=${booking.bookingId} and MOVEMENT_SEQ=${movementIn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement OUT is scheduled
      webTestClient.getTapMovementOutOk(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(tapScheduleOutId).isEqualTo(scheduleOut.eventId)
        }

      // Sync movement IN is unscheduled
      webTestClient.getTapMovementInOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender matches the sync
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].tapApplications[0].taps[0].tapMovementOut!!.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(movementIn.id.sequence)
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
      webTestClient.getTapMovementInOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender is the same as the sync
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(movementIn.id.sequence)
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
      webTestClient.getTapMovementOutOk(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(tapScheduleOutId).isEqualTo(scheduleOut.eventId)
        }

      // Sync movement IN is unscheduled
      webTestClient.getTapMovementInOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].tapApplications[0].taps[0].tapMovementOut!!.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(movementIn.id.sequence)
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
      webTestClient.getTapMovementOutOk(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(tapApplicationId).isEqualTo(application.tapApplicationId)
          assertThat(tapScheduleOutId).isEqualTo(scheduleOut.eventId)
        }

      // Sync movement IN is unscheduled
      webTestClient.getTapMovementInOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].tapApplications[0].taps[0].tapMovementOut!!.sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(movementIn.id.sequence)
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
      lateinit var wrongTapMovementIn: OffenderTapMovementIn
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                orphanedScheduleIn = tapScheduleIn()
              }
            }
            wrongTapMovementIn = tapMovementIn()
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
              set EVENT_ID = ${orphanedScheduleIn.eventId}
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ = ${wrongTapMovementIn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()

        // Reload the scheduled return to reflect the update
        orphanedScheduleIn = scheduleInRepository.findByIdOrNull(orphanedScheduleIn.eventId) ?: throw IllegalStateException("Failed to reload scheduled return movement - this should not happen")
      }

      // Sync movement IN is unscheduled
      webTestClient.getTapMovementInOk(movementSeq = wrongTapMovementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(wrongTapMovementIn.id.sequence)
        }

      // Reconciliation counts the movement as unscheduled
      webTestClient.getOffenderSummaryOk(offender.nomsId)
        .apply {
          assertThat(movements.unscheduled.inCount).isEqualTo(1)
        }
    }

    @Test
    fun `unscheduled movement IN points at a schedule OUT and IN but it shouldn't`() {
      lateinit var wrongTapMovementIn: OffenderTapMovementIn
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn()
              }
            }
            wrongTapMovementIn = tapMovementIn()
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
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ = ${wrongTapMovementIn.id.sequence}
          """.trimIndent(),
        ).executeUpdate()
      }

      // Sync movement IN is unscheduled
      webTestClient.getTapMovementInOk(movementSeq = wrongTapMovementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(wrongTapMovementIn.id.sequence)
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
      webTestClient.getTapMovementOutOk(movementSeq = movementOut.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
        }

      // Sync movement IN is unscheduled
      webTestClient.getTapMovementInOk(movementSeq = movementIn.id.sequence)
        .apply {
          assertThat(tapApplicationId).isNull()
          assertThat(tapScheduleOutId).isNull()
          assertThat(tapScheduleInId).isNull()
        }

      // Resync offender is same as for sync
      webTestClient.getOffenderTapsOk()
        .apply {
          assertThat(bookings[0].unscheduledTapMovementOuts[0].sequence).isEqualTo(movementOut.id.sequence)
          assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(movementIn.id.sequence)
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
        webTestClient.getTapMovementOutOk(movementSeq = movementOut.id.sequence)
          .apply {
            assertThat(tapApplicationId).isNull()
            assertThat(tapScheduleOutId).isNull()
          }

        // Sync movement IN is unscheduled
        webTestClient.getTapMovementInOk(movementSeq = movementIn.id.sequence)
          .apply {
            assertThat(tapApplicationId).isNull()
            assertThat(tapScheduleOutId).isNull()
            assertThat(tapScheduleInId).isNull()
          }

        // Resync offender is same as for sync
        webTestClient.getOffenderTapsOk()
          .apply {
            assertThat(bookings[0].unscheduledTapMovementOuts[0].sequence).isEqualTo(movementOut.id.sequence)
            assertThat(bookings[0].unscheduledTapMovementIns[0].sequence).isEqualTo(movementIn.id.sequence)
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
  }

  private fun WebTestClient.getTapMovementOutOk(offenderNo: String = offender.nomsId, bookingId: Long = booking.bookingId, movementSeq: Int = movementOut.id.sequence) = get()
    .uri("/movements/$offenderNo/taps/movement/out/$bookingId/$movementSeq")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<TapMovementOut>()

  private fun WebTestClient.getTapMovementInOk(offenderNo: String = offender.nomsId, bookingId: Long = booking.bookingId, movementSeq: Int = movementIn.id.sequence) = get()
    .uri("/movements/$offenderNo/taps/movement/in/$bookingId/$movementSeq")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<TapMovementIn>()

  private fun WebTestClient.getOffenderTaps(offenderNo: String = offender.nomsId) = get()
    .uri("/movements/$offenderNo/taps")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getOffenderTapsOk(offenderNo: String = offender.nomsId) = getOffenderTaps(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<OffenderTapsResponse>()

  private fun WebTestClient.getOffenderSummaryOk(offenderNo: String) = getOffenderSummary(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<TapSummary>()

  private fun WebTestClient.getOffenderSummary(offenderNo: String) = get()
    .uri("/movements/$offenderNo/taps/summary")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.createTapMovementOut(
    request: CreateTapMovementOut = aCreateTapMovementOutRequest(scheduleOut.eventId),
    offenderNo: String = offender.nomsId,
  ) = post()
    .uri("/movements/$offenderNo/taps/movement/out")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()

  private fun WebTestClient.createTapMovementOutOk(
    request: CreateTapMovementOut = aCreateTapMovementOutRequest(scheduleOut.eventId),
  ) = createTapMovementOut(request)
    .isCreated
    .expectBodyResponse<CreateTapMovementOutResponse>()

  private fun WebTestClient.createTapMovementOutBadRequest(
    request: CreateTapMovementOut = aCreateTapMovementOutRequest(scheduleOut.eventId),
  ) = createTapMovementOut(request)
    .isBadRequest

  private fun WebTestClient.createTapMovementOutBadRequestUnknown(
    request: CreateTapMovementOut = aCreateTapMovementOutRequest(scheduleOut.eventId),
  ) = createTapMovementOutBadRequest(request)
    .expectBody().jsonPath("userMessage").value<String> {
      assertThat(it).contains("UNKNOWN").contains("invalid")
    }

  private fun WebTestClient.createTapMovementIn(
    request: CreateTapMovementIn = aCreateTapMovementInRequest(scheduleIn.eventId),
    offenderNo: String = offender.nomsId,
  ) = post()
    .uri("/movements/$offenderNo/taps/movement/in")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .bodyValue(request)
    .exchange()
    .expectStatus()

  private fun WebTestClient.createTapMovementInOk(
    request: CreateTapMovementIn = aCreateTapMovementInRequest(scheduleIn.eventId),
  ) = createTapMovementIn(request)
    .isCreated
    .expectBodyResponse<CreateTapMovementInResponse>()

  private fun WebTestClient.createTapMovementInBadRequest(
    request: CreateTapMovementIn = aCreateTapMovementInRequest(scheduleIn.eventId),
  ) = createTapMovementIn(request)
    .isBadRequest

  private fun WebTestClient.createTapMovementInBadRequestUnknown(
    request: CreateTapMovementIn = aCreateTapMovementInRequest(scheduleIn.eventId),
  ) = createTapMovementInBadRequest(request)
    .expectBody().jsonPath("userMessage").value<String> {
      assertThat(it).contains("UNKNOWN").contains("invalid")
    }

  private fun WebTestClient.getTapScheduleOut() = get()
    .uri("/movements/${offender.nomsId}/taps/schedule/out/${scheduleOut.eventId}")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()
    .expectStatus().isOk
    .expectBodyResponse<TapScheduleOut>()

  private fun aCreateTapMovementOutRequest(tapScheduleOutId: Long? = scheduleOut.eventId) = CreateTapMovementOut(
    tapScheduleOutId = tapScheduleOutId,
    movementDate = twoDaysAgo.toLocalDate(),
    movementTime = twoDaysAgo,
    movementReason = "C5",
    arrestAgency = "POL",
    escort = "L",
    escortText = "SE",
    fromPrison = "LEI",
    toAgency = "HAZLWD",
    commentText = "comment tap movement out",
    toCity = offenderAddress.city?.code,
    toAddressId = offenderAddress.addressId,
  )

  private fun aCreateTapMovementInRequest(tapScheduleInId: Long? = scheduleIn.eventId) = CreateTapMovementIn(
    tapScheduleInId = tapScheduleInId,
    movementDate = twoDaysAgo.toLocalDate(),
    movementTime = twoDaysAgo,
    movementReason = "C5",
    arrestAgency = "POL",
    escort = "L",
    escortText = "SE",
    fromAgency = "HAZLWD",
    toPrison = "LEI",
    commentText = "comment tap movement in",
    fromAddressId = offenderAddress.addressId,
  )
}
