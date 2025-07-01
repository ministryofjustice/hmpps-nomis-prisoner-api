package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTapIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledTapOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTapInRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderScheduledTapOutRepository
import java.time.LocalDate
import java.time.LocalDateTime

class TapIntTest(
  @Autowired private val nomisDataBuilder: NomisDataBuilder,
  @Autowired private val scheduledTapOutRepository: OffenderScheduledTapOutRepository,
  @Autowired private val scheduledTapInRepository: OffenderScheduledTapInRepository,
) : IntegrationTestBase() {
  // SDIT-2872 This is a temporary test class to prove that the database is modeled correctly - to be replaced by full integration tests later
  @Nested
  inner class TapOutRepositoryTest {
    lateinit var booking: OffenderBooking
    lateinit var scheduledOut: OffenderScheduledTapOut
    lateinit var scheduledIn: OffenderScheduledTapIn

    @Test
    fun `should save and load TAP OUT`() {
      nomisDataBuilder.build {
        offender {
          booking = booking {
            scheduledOut = scheduledTapOut(
              eventDate = LocalDate.now(),
              startTime = LocalDateTime.now(),
              eventSubType = "C5",
              eventStatus = "SCH",
              prison = "LEI",
              comment = "Some comment",
              escort = "U",
              transportType = "VAN",
              returnDate = LocalDate.now().plusDays(1),
              returnTime = LocalDateTime.now().plusDays(1),
            )
          }
        }
      }

      with(scheduledTapOutRepository.findByIdOrNull(scheduledOut.eventId)!!) {
        assertThat(eventId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(eventDate).isEqualTo(LocalDate.now())
        assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(eventSubType.code).isEqualTo("C5")
        assertThat(eventStatus.code).isEqualTo("SCH")
        assertThat(prison.id).isEqualTo("LEI")
        assertThat(comment).isEqualTo("Some comment")
        assertThat(escort.code).isEqualTo("U")
        assertThat(transportType.code).isEqualTo("VAN")
        assertThat(returnDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(returnTime.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
      }
    }

    @Test
    fun `should save and load linked TAP OUT and TAP IN`() {
      nomisDataBuilder.build {
        offender {
          booking = booking {
            scheduledOut = scheduledTapOut {
              scheduledIn = scheduledTapIn(
                eventDate = LocalDate.now().plusDays(1),
                startTime = LocalDateTime.now().plusDays(1),
                eventSubType = "R25",
                eventStatus = "SCH",
                comment = "Some comment IN",
                escort = "U",
                toPrison = "LEI",
              )
            }
          }
        }
      }

      with(scheduledTapInRepository.findByIdOrNull(scheduledIn.eventId)!!) {
        assertThat(eventId).isGreaterThan(0L)
        assertThat(offenderBooking.bookingId).isEqualTo(booking.bookingId)
        assertThat(scheduledTapOut.eventId).isEqualTo(scheduledOut.eventId)
        assertThat(eventDate).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(startTime?.toLocalDate()).isEqualTo(LocalDate.now().plusDays(1))
        assertThat(eventSubType.code).isEqualTo("R25")
        assertThat(eventStatus.code).isEqualTo("SCH")
        assertThat(toPrison.id).isEqualTo("LEI")
        assertThat(comment).isEqualTo("Some comment IN")
        assertThat(escort.code).isEqualTo("U")
      }
    }
  }
}
