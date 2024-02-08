package uk.gov.justice.digital.hmpps.nomisprisonerapi.alerts

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.ACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AlertStatus.INACTIVE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class AlertsResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @DisplayName("Temporary test to check JPA")
  @Nested
  inner class TestJPA {
    var bookingId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234AB") {
          bookingId = booking {
            alert { }
            alert(
              sequence = 2,
              alertCode = "SC",
              typeCode = "S",
              date = LocalDate.parse("2020-07-19"),
              expiryDate = LocalDate.parse("2025-07-19"),
              authorizePersonText = "Security Team",
              verifiedFlag = true,
              status = INACTIVE,
              commentText = "At risk",
            ) { }
          }.bookingId
        }
      }
    }

    @Test
    fun `can insert and read alerts`() {
      repository.runInTransaction {
        val booking = offenderBookingRepository.findByIdOrNull(bookingId)

        assertThat(booking!!.alerts).hasSize(2)
        with(booking.alerts.firstOrNull { it.id.sequence == 1L }!!) {
          assertThat(alertDate).isEqualTo(LocalDate.now())
          assertThat(alertStatus).isEqualTo(ACTIVE)
          assertThat(alertCode.code).isEqualTo("XA")
          assertThat(alertCode.description).isEqualTo("Arsonist")
          assertThat(alertType.code).isEqualTo("X")
          assertThat(alertType.description).isEqualTo("Security")
          assertThat(authorizePersonText).isNull()
          assertThat(caseloadType).isEqualTo("INST")
          assertThat(commentText).isNull()
          assertThat(expiryDate).isNull()
          assertThat(verifiedFlag).isFalse()
          assertThat(createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(createUserId).isEqualTo("SA")
        }
        with(booking.alerts.firstOrNull { it.id.sequence == 2L }!!) {
          assertThat(alertDate).isEqualTo(LocalDate.parse("2020-07-19"))
          assertThat(alertStatus).isEqualTo(INACTIVE)
          assertThat(alertCode.code).isEqualTo("SC")
          assertThat(alertCode.description).isEqualTo("Risk to Children")
          assertThat(alertType.code).isEqualTo("S")
          assertThat(alertType.description).isEqualTo("Sexual Offence")
          assertThat(authorizePersonText).isEqualTo("Security Team")
          assertThat(caseloadType).isEqualTo("INST")
          assertThat(commentText).isEqualTo("At risk")
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2025-07-19"))
          assertThat(verifiedFlag).isTrue()
          assertThat(createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          assertThat(createUserId).isEqualTo("SA")
        }
      }
    }
  }
}
