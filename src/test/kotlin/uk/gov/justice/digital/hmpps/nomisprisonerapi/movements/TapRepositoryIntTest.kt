package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.IN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.OUT
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
        assertThat(tapApplicationId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(eventClass).isEqualTo("EXT_MOV")
        assertThat(eventType).isEqualTo(EventType.TAP)
        assertThat(eventSubType.code).isEqualTo("C5")
        assertThat(applicationDate.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(applicationTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(fromDate).isEqualTo(LocalDate.now())
        assertThat(releaseTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(toDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(applicationType.code).isEqualTo("SINGLE")
        assertThat(applicationStatus.code).isEqualTo("APP-SCH")
        assertThat(escort?.code).isEqualTo("L")
        assertThat(transportType?.code).isEqualTo("VAN")
        assertThat(comment).isEqualTo("Some comment application")
        assertThat(prison.id).isEqualTo("LEI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(contactPersonName).isEqualTo("Derek")
        assertThat(tapType?.code).isEqualTo("RR")
        assertThat(tapSubType?.code).isEqualTo("RDR")
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
        assertThat(eventId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(tapApplication.tapApplicationId).isEqualTo(scheduleOut.tapApplication.tapApplicationId)
        assertThat(eventDate).isEqualTo(LocalDate.now())
        assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(eventSubType.code).isEqualTo("C5")
        assertThat(eventStatus.code).isEqualTo("SCH")
        assertThat(fromAgency?.id).isEqualTo("LEI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(comment).isEqualTo("Some comment")
        assertThat(escort?.code).isEqualTo("U")
        assertThat(transportType?.code).isEqualTo("VAN")
        assertThat(returnDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(returnTime?.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(applicationDate.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(applicationTime?.toLocalDate()).isEqualTo(LocalDate.now())
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
        assertThat(eventId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(tapScheduleOut.eventId).isEqualTo(scheduleOut.eventId)
        assertThat(eventDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(eventSubType.code).isEqualTo("R25")
        assertThat(eventStatus.code).isEqualTo("SCH")
        assertThat(toAgency?.id).isEqualTo("LEI")
        assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        assertThat(comment).isEqualTo("Some comment IN")
        assertThat(escort?.code).isEqualTo("U")
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
        assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("CORP")
      }

      with(tapApplicationRepository.findByIdOrNull(application2.tapApplicationId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("OFF")
      }

      with(tapApplicationRepository.findByIdOrNull(application3.tapApplicationId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("AGY")
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
        assertThat(toAddress?.addressId).isEqualTo(corporateAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("CORP")
      }

      with(tapScheduleOutRepository.findByIdOrNull(scheduledAbsence2.eventId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("OFF")
      }

      with(tapScheduleOutRepository.findByIdOrNull(scheduledAbsence3.eventId)!!) {
        assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo("AGY")
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
        assertThat(toAddress?.addressId).isEqualTo(agencyAddress.addressId)
        assertThat(toAddressOwnerClass).isEqualTo(agencyAddress.agencyLocation.id)
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
        assertThat(movementDate).isEqualTo(LocalDate.now())
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(OUT)
        assertThat(arrestAgency?.code).isEqualTo("POL")
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("BXI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(commentText).isEqualTo("TAP OUT comment")
        assertThat(toCity?.id?.code).isEqualTo(SHEFFIELD)
        assertThat(toAddress?.addressId).isEqualTo(offenderAddress.addressId)
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
        assertThat(movementDate).isEqualTo(LocalDate.now())
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(OUT)
        assertThat(arrestAgency?.code).isEqualTo("POL")
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("BXI")
        assertThat(toAgency?.id).isEqualTo("HAZLWD")
        assertThat(commentText).isEqualTo("Tap OUT comment")
        assertThat(toCity?.id?.code).isEqualTo(SHEFFIELD)
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
        assertThat(movementDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(IN)
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        assertThat(toAgency?.id).isEqualTo("BXI")
        assertThat(commentText).isEqualTo("TAP IN comment")
        assertThat(fromCity?.id?.code).isEqualTo(SHEFFIELD)
        assertThat(fromAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(tapScheduleIn?.eventId).isEqualTo(scheduleIn.eventId)
        assertThat(tapScheduleIn?.tapScheduleOut?.eventId).isEqualTo(scheduleOut.eventId)
        assertThat(tapScheduleOut?.eventId).isEqualTo(scheduleOut.eventId)
        assertThat(tapScheduleOut?.tapMovementOut?.id).isEqualTo(movementOut.id)
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
        assertThat(movementDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(movementType?.code).isEqualTo("TAP")
        assertThat(movementReason.code).isEqualTo("C5")
        assertThat(movementDirection).isEqualTo(IN)
        assertThat(escort?.code).isEqualTo("U")
        assertThat(escortText).isEqualTo("SE")
        assertThat(fromAgency?.id).isEqualTo("HAZLWD")
        assertThat(toAgency?.id).isEqualTo("BXI")
        assertThat(commentText).isEqualTo("TAP IN comment")
        assertThat(fromCity?.id?.code).isEqualTo(SHEFFIELD)
        assertThat(fromAddress?.addressId).isEqualTo(offenderAddress.addressId)
        assertThat(tapScheduleIn).isNull()
        assertThat(tapScheduleOut?.eventId).isNull()
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
          assertThat(externalMovements.find { it.id.sequence == 2 }).isExactlyInstanceOf(OffenderTapMovementOut::class.java)
          assertThat(externalMovements.find { it.id.sequence == 3 }).isExactlyInstanceOf(OffenderTapMovementIn::class.java)
          // The TAP missing a direction still comes out as an external movement
          assertThat(externalMovements.find { it.id.sequence == 4 }).isExactlyInstanceOf(OffenderExternalMovement::class.java)
          assertThat(externalMovements.find { it.id.sequence == 5 }).isExactlyInstanceOf(OffenderExternalMovement::class.java)
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
          assertThat(this).isEmpty()
        }
      }
    }
  }
}
