package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventClass
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleWaitList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIndividualScheduleWaitListRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import java.time.LocalDate

/**
 * Temporary test to exercise the new JPA entities/repositories for transfer schedules and their wait lists,
 * using the new test data builders. This will be superseded by resource-level tests in later PRs.
 */
class TransferScheduleRepositoryIntTest(
  @Autowired private val transferScheduleOutRepository: OffenderTransferScheduleOutRepository,
  @Autowired private val waitListRepository: OffenderIndividualScheduleWaitListRepository,
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var scheduleOut: OffenderTransferScheduleOut
  private lateinit var waitList: OffenderTransferScheduleWaitList
  private lateinit var staff: Staff

  @AfterEach
  fun `reset data`() {
    repository.deleteOffenders()
  }

  @Test
  fun `should save and load a transfer schedule out`() {
    val tomorrow = LocalDate.now().plusDays(1)

    nomisDataBuilder.build {
      offender = offender {
        booking = booking {
          scheduleOut = transferScheduleOut(
            eventDate = tomorrow,
            startTime = tomorrow.atTime(10, 0),
            eventSubType = "NOTR",
            eventStatus = "SCH",
            fromPrison = "BXI",
            toPrison = "LEI",
            comment = "Some comment",
            escort = "U",
            cancellationReasonCode = "TRANS",
          )
        }
      }
    }

    with(transferScheduleOutRepository.findByIdOrNull(scheduleOut.eventId)!!) {
      assertThat(eventId).isGreaterThan(0L)
      assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
      assertThat(eventClass).isEqualTo(EventClass.EXT_MOV)
      assertThat(eventType).isEqualTo(EventType.TRN)
      assertThat(direction).isEqualTo(MovementDirection.OUT)
      assertThat(eventDate).isEqualTo(tomorrow)
      assertThat(eventSubType.code).isEqualTo("NOTR")
      assertThat(eventStatus.code).isEqualTo("SCH")
      assertThat(fromAgency?.id).isEqualTo("BXI")
      assertThat(toAgency?.id).isEqualTo("LEI")
      assertThat(comment).isEqualTo("Some comment")
      assertThat(escort?.code).isEqualTo("U")
      assertThat(cancellationReasonCode?.code).isEqualTo("TRANS")
      assertThat(waitList).isNull()
    }
  }

  @Test
  fun `should save and load a transfer schedule out with a wait list`() {
    nomisDataBuilder.build {
      staff = staff {
        account()
      }
      offender = offender {
        booking = booking {
          scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI") {
            waitList = waitList(
              requestDate = LocalDate.now(),
              waitListStatus = "PEN",
              statusDate = LocalDate.now(),
              transferPriority = "1",
              approvedFlag = true,
              approvedStaff = staff,
              cancellationReasonCode = "ADMI",
              commentText1 = "comment 1",
              commentText2 = "comment 2",
            )
          }
        }
      }
    }

    with(waitListRepository.findByIdOrNull(scheduleOut.eventId)!!) {
      assertThat(id).isEqualTo(scheduleOut.eventId)
      assertThat(schedule.eventId).isEqualTo(scheduleOut.eventId)
      assertThat(requestDate).isEqualTo(LocalDate.now())
      assertThat(waitListStatus.code).isEqualTo("PEN")
      assertThat(statusDate).isEqualTo(LocalDate.now())
      assertThat(transferPriority?.code).isEqualTo("1")
      assertThat(approvedFlag).isTrue()
      assertThat(approvedStaff?.id).isEqualTo(staff.id)
      assertThat(cancellationReasonCode?.code).isEqualTo("ADMI")
      assertThat(commentText1).isEqualTo("comment 1")
      assertThat(commentText2).isEqualTo("comment 2")
    }

    with(transferScheduleOutRepository.findByIdOrNull(scheduleOut.eventId)!!) {
      assertThat(waitList?.id).isEqualTo(scheduleOut.eventId)
    }
  }

  @Test
  fun `should handle a rogue transfer priority code`() {
    nomisDataBuilder.build {
      staff = staff {
        account()
      }
      offender = offender {
        booking = booking {
          scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI") {
            waitList = waitList()
          }
        }
      }
    }

    repository.runInTransaction {
      /*
       * Corrupt the data with an invalid transfer priority code, as found in production
       */
      entityManager.createNativeQuery(
        """
            update OFFENDER_IND_SCH_WAIT_LISTS w
            set w.TRANSFER_PRIORITY = 'PEN'
            where w.EVENT_ID = ${waitList.id}
        """.trimIndent(),
      ).executeUpdate()
    }

    with(waitListRepository.findByIdOrNull(waitList.id)!!) {
      assertThat(transferPriority).isNull()
    }
  }

  @Test
  fun `should update a transfer schedule out and its wait list`() {
    nomisDataBuilder.build {
      staff = staff {
        account()
      }
      offender = offender {
        booking = booking {
          scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI") {
            waitList = waitList(waitListStatus = "PEN", transferPriority = "3")
          }
        }
      }
    }

    transferScheduleOutRepository.findByIdOrNull(scheduleOut.eventId)!!.apply {
      comment = "Updated comment"
    }.also { transferScheduleOutRepository.saveAndFlush(it) }

    waitListRepository.findByIdOrNull(scheduleOut.eventId)!!.apply {
      commentText1 = "Updated wait list comment"
    }.also { waitListRepository.saveAndFlush(it) }

    with(transferScheduleOutRepository.findByIdOrNull(scheduleOut.eventId)!!) {
      assertThat(comment).isEqualTo("Updated comment")
    }
    with(waitListRepository.findByIdOrNull(scheduleOut.eventId)!!) {
      assertThat(commentText1).isEqualTo("Updated wait list comment")
    }
  }

  @Test
  fun `should delete a transfer schedule out and its wait list`() {
    nomisDataBuilder.build {
      staff = staff {
        account()
      }
      offender = offender {
        booking = booking {
          scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI") {
            waitList = waitList()
          }
        }
      }
    }

    val eventId = scheduleOut.eventId
    transferScheduleOutRepository.deleteById(eventId)

    assertThat(waitListRepository.findByIdOrNull(eventId)).isNull()
    assertThat(transferScheduleOutRepository.findByIdOrNull(eventId)).isNull()
  }
}
