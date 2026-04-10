package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapApplication
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapApplicationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapMovementOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTapScheduleOutRepository
import java.time.LocalDate
import java.time.LocalDateTime

class TapRepositoryIntTest(
  @Autowired private val tapApplicationRepository: OffenderTapApplicationRepository,
  @Autowired private val tapScheduleOutRepository: OffenderTapScheduleOutRepository,
  @Autowired private val tapScheduleInRepository: OffenderTapScheduleInRepository,
  @Autowired private val tapMovementOutRepository: OffenderTapMovementOutRepository,
  @Autowired private val tapMovementInRepository: OffenderTapMovementInRepository,
  @Autowired private val offenderBookingRepository: OffenderBookingRepository,
  @Autowired private val jdbcTemplate: JdbcTemplate,
) : IntegrationTestBase() {

  @Nested
  inner class TapRepositoryTest {
    lateinit var offender: Offender
    lateinit var booking: OffenderBooking
    lateinit var application: OffenderTapApplication
    lateinit var scheduleOut: OffenderTapScheduleOut
    lateinit var scheduleIn: OffenderTapScheduleIn
    lateinit var movementOut: OffenderTapMovementOut
    lateinit var movementIn: OffenderTapMovementIn

    @AfterEach
    fun `reset data`() {
      repository.delete(offender)
    }

    @Test
    fun `should save and load movement application`() {
      nomisDataBuilder.build {
        offender = offender {
          booking = booking {
            application = tapApplication(
              eventSubType = "C5",
              applicationDate = LocalDateTime.now(),
              applicationTime = LocalDateTime.now(),
              fromDate = LocalDate.now(),
              releaseTime = LocalDateTime.now(),
              toDate = LocalDate.now().plusDays(1),
              returnTime = LocalDateTime.now().plusDays(1),
              applicationType = "SINGLE",
              applicationStatus = "APP-SCH",
              escort = "L",
              transportType = "VAN",
              comment = "Some comment application",
              prison = "LEI",
              toAgency = "HAZLWD",
              contactPersonName = "Derek",
              tapType = "RR",
              tapSubType = "RDR",
            )
          }
        }
      }

      with(tapApplicationRepository.findByIdOrNull(application.tapApplicationId)!!) {
        Assertions.assertThat(tapApplicationId).isGreaterThan(0L)
        Assertions.assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        Assertions.assertThat(eventClass).isEqualTo("EXT_MOV")
        Assertions.assertThat(eventType).isEqualTo(EventType.TAP)
        Assertions.assertThat(eventSubType.code).isEqualTo("C5")
        Assertions.assertThat(applicationDate.toLocalDate()).isEqualTo(LocalDate.now())
        Assertions.assertThat(applicationTime.toLocalDate()).isEqualTo(LocalDate.now())
        Assertions.assertThat(fromDate).isEqualTo(LocalDate.now())
        Assertions.assertThat(releaseTime.toLocalDate()).isEqualTo(LocalDate.now())
        Assertions.assertThat(toDate).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(applicationType.code).isEqualTo("SINGLE")
        Assertions.assertThat(applicationStatus.code).isEqualTo("APP-SCH")
        Assertions.assertThat(escort?.code).isEqualTo("L")
        Assertions.assertThat(transportType?.code).isEqualTo("VAN")
        Assertions.assertThat(comment).isEqualTo("Some comment application")
        Assertions.assertThat(prison.id).isEqualTo("LEI")
        Assertions.assertThat(toAgency?.id).isEqualTo("HAZLWD")
        Assertions.assertThat(contactPersonName).isEqualTo("Derek")
        Assertions.assertThat(tapType?.code).isEqualTo("RR")
        Assertions.assertThat(tapSubType?.code).isEqualTo("RDR")
      }
    }

    @Test
    fun `should save and load TAP OUT`() {
      nomisDataBuilder.build {
        offender = offender {
          booking = booking {
            application = tapApplication(
              applicationDate = LocalDateTime.now(),
              applicationTime = LocalDateTime.now(),
            ) {
              scheduleOut = tapScheduleOut(
                eventDate = LocalDate.now(),
                startTime = LocalDateTime.now(),
                eventSubType = "C5",
                eventStatus = "SCH",
                fromPrison = "LEI",
                toAgency = "HAZLWD",
                comment = "Some comment",
                escort = "U",
                transportType = "VAN",
                returnDate = LocalDate.now().plusDays(1),
                returnTime = LocalDateTime.now().plusDays(1),
              )
            }
          }
        }
      }

      with(tapScheduleOutRepository.findByIdOrNull(scheduleOut.eventId)!!) {
        Assertions.assertThat(eventId).isGreaterThan(0L)
        Assertions.assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        Assertions.assertThat(tapApplication.tapApplicationId).isEqualTo(scheduleOut.tapApplication.tapApplicationId)
        Assertions.assertThat(eventDate).isEqualTo(LocalDate.now())
        Assertions.assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now())
        Assertions.assertThat(eventSubType.code).isEqualTo("C5")
        Assertions.assertThat(eventStatus.code).isEqualTo("SCH")
        Assertions.assertThat(fromAgency?.id).isEqualTo("LEI")
        Assertions.assertThat(toAgency?.id).isEqualTo("HAZLWD")
        Assertions.assertThat(comment).isEqualTo("Some comment")
        Assertions.assertThat(escort?.code).isEqualTo("U")
        Assertions.assertThat(transportType?.code).isEqualTo("VAN")
        Assertions.assertThat(returnDate).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(returnTime?.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(applicationDate.toLocalDate()).isEqualTo(LocalDate.now())
        Assertions.assertThat(applicationTime?.toLocalDate()).isEqualTo(LocalDate.now())
      }
    }

    @Test
    fun `should save and load linked TAP OUT and TAP IN`() {
      nomisDataBuilder.build {
        offender = offender {
          booking = booking {
            tapApplication {
              scheduleOut = tapScheduleOut {
                scheduleIn = tapScheduleIn(
                  eventDate = LocalDate.now().plusDays(1),
                  startTime = LocalDateTime.now().plusDays(1),
                  eventSubType = "R25",
                  eventStatus = "SCH",
                  comment = "Some comment IN",
                  escort = "U",
                  fromAgency = "HAZLWD",
                  toPrison = "LEI",
                )
              }
            }
          }
        }
      }

      with(tapScheduleInRepository.findByIdOrNull(scheduleIn.eventId)!!) {
        Assertions.assertThat(eventId).isGreaterThan(0L)
        Assertions.assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        Assertions.assertThat(tapScheduleOut.eventId).isEqualTo(scheduleOut.eventId)
        Assertions.assertThat(eventDate).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(eventSubType.code).isEqualTo("R25")
        Assertions.assertThat(eventStatus.code).isEqualTo("SCH")
        Assertions.assertThat(toAgency?.id).isEqualTo("LEI")
        Assertions.assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        Assertions.assertThat(comment).isEqualTo("Some comment IN")
        Assertions.assertThat(escort?.code).isEqualTo("U")
      }
    }

    @Test
    fun `Should save and load application address`() {
      lateinit var corporateAddress: CorporateAddress
      lateinit var offenderAddress: OffenderAddress
      lateinit var agencyAddress: AgencyLocationAddress
      lateinit var application1: OffenderTapApplication
      lateinit var application2: OffenderTapApplication
      lateinit var application3: OffenderTapApplication

      nomisDataBuilder.build {
        corporate("Kwikfit") {
          corporateAddress = address()
        }
        agencyLocation("LEI", "HMP Leeds") {
          agencyAddress = address()
        }
        offender = offender {
          offenderAddress = address()
          booking = booking {
            application1 = tapApplication(toAddress = corporateAddress)
            application2 = tapApplication(toAddress = offenderAddress)
            application3 = tapApplication(toAddress = agencyAddress)
          }
        }
      }

      with(tapApplicationRepository.findByIdOrNull(application1.tapApplicationId)!!) {
        Assertions.assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
        Assertions.assertThat(toAddressOwnerClass).isEqualTo("CORP")
      }

      with(tapApplicationRepository.findByIdOrNull(application2.tapApplicationId)!!) {
        Assertions.assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
        Assertions.assertThat(toAddressOwnerClass).isEqualTo("OFF")
      }

      with(tapApplicationRepository.findByIdOrNull(application3.tapApplicationId)!!) {
        Assertions.assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        Assertions.assertThat(toAddressOwnerClass).isEqualTo("AGY")
      }
    }

    @Test
    fun `should save and load TAP OUT addresses`() {
      lateinit var corporateAddress: CorporateAddress
      lateinit var offenderAddress: OffenderAddress
      lateinit var agencyAddress: AgencyLocationAddress
      lateinit var scheduledAbsence2: OffenderTapScheduleOut
      lateinit var scheduledAbsence3: OffenderTapScheduleOut

      nomisDataBuilder.build {
        corporate("Kwikfit") {
          corporateAddress = address()
        }
        agencyLocation("LEI", "HMP Leeds") {
          agencyAddress = address()
        }
        offender = offender {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(toAddress = corporateAddress)
            }
            application = tapApplication {
              scheduledAbsence2 = tapScheduleOut(toAddress = offenderAddress)
            }
            application = tapApplication {
              scheduledAbsence3 = tapScheduleOut(toAddress = agencyAddress)
            }
          }
        }
      }

      with(tapScheduleOutRepository.findByIdOrNull(scheduleOut.eventId)!!) {
        Assertions.assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
        Assertions.assertThat(toAddressOwnerClass).isEqualTo("CORP")
      }

      with(tapScheduleOutRepository.findByIdOrNull(scheduledAbsence2.eventId)!!) {
        Assertions.assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
        Assertions.assertThat(toAddressOwnerClass).isEqualTo("OFF")
      }

      with(tapScheduleOutRepository.findByIdOrNull(scheduledAbsence3.eventId)!!) {
        Assertions.assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        Assertions.assertThat(toAddressOwnerClass).isEqualTo("AGY")
      }
    }

    // This models some strange data in NOMIS where the to address owner class appears to be incorrectly set as the agency location id.
    // Just need to make sure we can read that data from the table
    @Test
    fun `should load a tap application with where address class is the agency id`() {
      lateinit var agencyAddress: AgencyLocationAddress

      nomisDataBuilder.build {
        agencyLocation("LEI", "HMP Leeds") {
          agencyAddress = address()
        }
        offender = offender {
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut(toAddress = agencyAddress)
            }
          }
        }
      }

      jdbcTemplate.update("update offender_ind_schedules set TO_ADDRESS_OWNER_CLASS='${agencyAddress.agencyLocation.id}'")

      with(tapScheduleOutRepository.findByIdOrNull(scheduleOut.eventId)!!) {
        Assertions.assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        Assertions.assertThat(toAddressOwnerClass).isEqualTo(agencyAddress.agencyLocation.id)
      }
    }

    @Test
    fun `should save and load external movement from scheduled OUT`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking = booking {
            application = tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut(
                  date = LocalDateTime.now(),
                  fromPrison = "BXI",
                  toAgency = "HAZLWD",
                  movementReason = "C5",
                  arrestAgency = "POL",
                  escort = "U",
                  escortText = "SE",
                  comment = "TAP OUT comment",
                  toCity = SHEFFIELD,
                  toAddress = offenderAddress,
                )
              }
            }
          }
        }
      }

      with(tapMovementOutRepository.findByIdOrNull(movementOut.id)!!) {
        Assertions.assertThat(movementDate).isEqualTo(LocalDate.now())
        Assertions.assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now())
        Assertions.assertThat(movementType?.code).isEqualTo("TAP")
        Assertions.assertThat(movementReason.code).isEqualTo("C5")
        Assertions.assertThat(movementDirection).isEqualTo(MovementDirection.OUT)
        Assertions.assertThat(arrestAgency?.code).isEqualTo("POL")
        Assertions.assertThat(escort?.code).isEqualTo("U")
        Assertions.assertThat(escortText).isEqualTo("SE")
        Assertions.assertThat(fromAgency?.id).isEqualTo("BXI")
        Assertions.assertThat(toAgency?.id).isEqualTo("HAZLWD")
        Assertions.assertThat(commentText).isEqualTo("TAP OUT comment")
        Assertions.assertThat(toCity?.id?.code).isEqualTo(SHEFFIELD)
        Assertions.assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
      }
    }

    @Test
    fun `should save and load external movement without schedule`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking = booking {
            movementOut = tapMovementOut(
              date = LocalDateTime.now(),
              fromPrison = "BXI",
              toAgency = "HAZLWD",
              movementReason = "C5",
              arrestAgency = "POL",
              escort = "U",
              escortText = "SE",
              comment = "Tap OUT comment",
              toCity = SHEFFIELD,
              toAddress = offenderAddress,
            )
          }
        }
      }

      with(tapMovementOutRepository.findByIdOrNull(movementOut.id)!!) {
        Assertions.assertThat(movementDate).isEqualTo(LocalDate.now())
        Assertions.assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now())
        Assertions.assertThat(movementType?.code).isEqualTo("TAP")
        Assertions.assertThat(movementReason.code).isEqualTo("C5")
        Assertions.assertThat(movementDirection).isEqualTo(MovementDirection.OUT)
        Assertions.assertThat(arrestAgency?.code).isEqualTo("POL")
        Assertions.assertThat(escort?.code).isEqualTo("U")
        Assertions.assertThat(escortText).isEqualTo("SE")
        Assertions.assertThat(fromAgency?.id).isEqualTo("BXI")
        Assertions.assertThat(toAgency?.id).isEqualTo("HAZLWD")
        Assertions.assertThat(commentText).isEqualTo("Tap OUT comment")
        Assertions.assertThat(toCity?.id?.code).isEqualTo(SHEFFIELD)
      }
    }

    @Test
    fun `should save and load external movement from schedule IN`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking {
            tapApplication {
              scheduleOut = tapScheduleOut {
                movementOut = tapMovementOut()
                scheduleIn = tapScheduleIn {
                  movementIn = tapMovementIn(
                    date = LocalDateTime.now().plusDays(1),
                    fromAgency = "HAZLWD",
                    toPrison = "BXI",
                    movementReason = "C5",
                    escort = "U",
                    escortText = "SE",
                    comment = "TAP IN comment",
                    fromCity = SHEFFIELD,
                    fromAddress = offenderAddress,
                  )
                }
              }
            }
          }
        }
      }

      jdbcTemplate.query("select MOVEMENT_TYPE,DIRECTION_CODE,event_id,parent_event_id from offender_external_movements") {
        println("done $it")
      }

      with(tapMovementInRepository.findByIdOrNull(movementIn.id)!!) {
        Assertions.assertThat(movementDate).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(movementType?.code).isEqualTo("TAP")
        Assertions.assertThat(movementReason.code).isEqualTo("C5")
        Assertions.assertThat(movementDirection).isEqualTo(MovementDirection.IN)
        Assertions.assertThat(escort?.code).isEqualTo("U")
        Assertions.assertThat(escortText).isEqualTo("SE")
        Assertions.assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        Assertions.assertThat(toAgency?.id).isEqualTo("BXI")
        Assertions.assertThat(commentText).isEqualTo("TAP IN comment")
        Assertions.assertThat(fromCity?.id?.code).isEqualTo(SHEFFIELD)
        Assertions.assertThat(fromAddress?.addressId).isEqualTo(offenderAddress.addressId)
        Assertions.assertThat(tapScheduleIn?.eventId).isEqualTo(scheduleIn.eventId)
        Assertions.assertThat(tapScheduleIn?.tapScheduleOut?.eventId).isEqualTo(scheduleOut.eventId)
        Assertions.assertThat(tapScheduleOut?.eventId).isEqualTo(scheduleOut.eventId)
        Assertions.assertThat(tapScheduleOut?.tapMovementOut?.id).isEqualTo(movementOut.id)
      }
    }

    @Test
    fun `should save and load external movement return without schedule`() {
      lateinit var offenderAddress: OffenderAddress

      nomisDataBuilder.build {
        offender = offender {
          offenderAddress = address()
          booking = booking {
            tapMovementOut()
            movementIn = tapMovementIn(
              date = LocalDateTime.now().plusDays(1),
              fromAgency = "HAZLWD",
              toPrison = "BXI",
              movementReason = "C5",
              escort = "U",
              escortText = "SE",
              comment = "TAP IN comment",
              fromCity = SHEFFIELD,
              fromAddress = offenderAddress,
            )
          }
        }
      }

      with(tapMovementInRepository.findByIdOrNull(movementIn.id)!!) {
        Assertions.assertThat(movementDate).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        Assertions.assertThat(movementType?.code).isEqualTo("TAP")
        Assertions.assertThat(movementReason.code).isEqualTo("C5")
        Assertions.assertThat(movementDirection).isEqualTo(MovementDirection.IN)
        Assertions.assertThat(escort?.code).isEqualTo("U")
        Assertions.assertThat(escortText).isEqualTo("SE")
        Assertions.assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        Assertions.assertThat(toAgency?.id).isEqualTo("BXI")
        Assertions.assertThat(commentText).isEqualTo("TAP IN comment")
        Assertions.assertThat(fromCity?.id?.code).isEqualTo(SHEFFIELD)
        Assertions.assertThat(fromAddress?.addressId).isEqualTo(offenderAddress.addressId)
        Assertions.assertThat(tapScheduleIn).isNull()
        Assertions.assertThat(tapScheduleOut?.eventId).isNull()
      }
    }

    @Test
    fun `should handle missing direction for TAP external movements (bad data exists in NOMIS)`() {
      nomisDataBuilder.build {
        offender = offender {
          booking = booking()
        }
      }

      jdbcTemplate.update(
        """
        insert into OFFENDER_EXTERNAL_MOVEMENTS
        (OFFENDER_BOOK_ID, MOVEMENT_SEQ, MOVEMENT_DATE, MOVEMENT_TIME, MOVEMENT_TYPE, MOVEMENT_REASON_CODE, DIRECTION_CODE)
        values (${booking.bookingId}, 2, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TAP', 'C5', 'OUT'),
               (${booking.bookingId}, 3, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TAP', 'C5', 'IN'),
               (${booking.bookingId}, 4, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TAP', 'C5', null),
               (${booking.bookingId}, 5, '${LocalDate.now()}', '${LocalDateTime.now()}', 'TRN', 'NOTR', 'OUT')
        """.trimIndent(),
      )

      repository.runInTransaction {
        with(offenderBookingRepository.findByIdOrNull(booking.bookingId)!!) {
          Assertions.assertThat(externalMovements.find { it.id.sequence == 2 })
            .isExactlyInstanceOf(OffenderTapMovementOut::class.java)
          Assertions.assertThat(externalMovements.find { it.id.sequence == 3 })
            .isExactlyInstanceOf(OffenderTapMovementIn::class.java)
          // The TAP missing a direction still comes out as an external movement
          Assertions.assertThat(externalMovements.find { it.id.sequence == 4 })
            .isExactlyInstanceOf(OffenderExternalMovement::class.java)
          Assertions.assertThat(externalMovements.find { it.id.sequence == 5 })
            .isExactlyInstanceOf(OffenderExternalMovement::class.java)
        }
      }
    }

    @Test
    fun `should check for existence of scheduled return movement (rather than outbound) to see if the return movement is unscheduled`() {
      nomisDataBuilder.build {
        offender = offender {
          booking = booking {
            tapApplication {
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

      jdbcTemplate.update(
        """
          update OFFENDER_EXTERNAL_MOVEMENTS set parent_event_id = null where event_id = ${scheduleIn.eventId}
        """.trimIndent(),
      )

      repository.runInTransaction {
        with(tapMovementInRepository.findAllByOffenderBooking_Offender_NomsIdAndTapScheduleInIsNull(offender.nomsId)) {
          Assertions.assertThat(this).isEmpty()
        }
      }
    }
  }
}
