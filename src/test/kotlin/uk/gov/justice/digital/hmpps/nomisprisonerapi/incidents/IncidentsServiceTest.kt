package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import com.google.common.base.Utf8
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.LocalDateTime

private const val TWO_UNICODE_CHARS = "⌘⌥"

class IncidentsServiceTest {
  private val incidentsService = IncidentService(
    incidentRepository = mock(),
    offenderBookingRepository = mock(),
    agencyLocationRepository = mock(),
    incidentStatusRepository = mock(),
    questionnaireRepository = mock(),
    staffUserAccountRepository = mock(),
    outcomeRepository = mock(),
    offenderRoleRepository = mock(),
    staffRoleRepository = mock(),
  )

  @Nested
  internal inner class ReconstructText {
    @Test
    fun `plain unamended text`() {
      assertThat(
        incidentsService.reconstructText(
          upsertIncidentRequest().copy(
            description = "some text",
            descriptionAmendments = emptyList(),
          ),
        ),
      ).isEqualTo("some text")
    }

    @Test
    fun `one amendment`() {
      assertThat(
        incidentsService.reconstructText(
          upsertIncidentRequest().copy(
            description = "some text",
            descriptionAmendments = listOf(
              UpsertDescriptionAmendmentRequest(
                text = "change",
                firstName = "STEVE",
                lastName = "R",
                createdDateTime = LocalDateTime.parse("2024-05-06T07:08:09"),
              ),
            ),
          ),
        ),
      ).isEqualTo("some textUser:R,STEVE Date:06-May-2024 07:08change")
    }

    @Test
    fun `multiple descriptionAmendments`() {
      assertThat(
        incidentsService.reconstructText(
          upsertIncidentRequest().copy(
            description = "some text",
            descriptionAmendments = listOf(
              UpsertDescriptionAmendmentRequest(
                text = "change",
                firstName = "STEVE",
                lastName = "R",
                createdDateTime = LocalDateTime.parse("2024-05-06T07:08:09"),
              ),
              UpsertDescriptionAmendmentRequest(
                text = "another",
                firstName = "STE",
                lastName = "REN",
                createdDateTime = LocalDateTime.parse("2024-05-07T07:08:10"),
              ),
            ),
          ),
        ),
      ).isEqualTo("some textUser:R,STEVE Date:06-May-2024 07:08changeUser:REN,STE Date:07-May-2024 07:08another")
    }

    @Test
    fun `truncation without descriptionAmendments`() {
      assertThat(
        incidentsService.reconstructText(
          upsertIncidentRequest().copy(
            description = "s".repeat(4000),
            descriptionAmendments = emptyList(),
          ),
        ),
      ).isEqualTo("s".repeat(4000))

      assertThat(
        incidentsService.reconstructText(
          upsertIncidentRequest().copy(
            description = "s".repeat(4001),
            descriptionAmendments = emptyList(),
          ),
        ),
      ).isEqualTo("${"s".repeat(3975)}... see DPS for full text")
    }

    @Test
    fun `truncation with descriptionAmendments`() {
      assertThat(
        incidentsService.reconstructText(
          upsertIncidentRequest().copy(
            description = "s".repeat(3960),
            descriptionAmendments = listOf(
              UpsertDescriptionAmendmentRequest(
                text = "change",
                firstName = "STEVE",
                lastName = "R",
                createdDateTime = LocalDateTime.parse("2024-05-06T07:08:09"),
              ),
            ),
          ),
        ),
      ).isEqualTo("${"s".repeat(3960)}User:R,STEVE Da... see DPS for full text")
    }

    @Test
    fun `truncation with unicode length ok`() {
      val textOk = "s".repeat(3994) + TWO_UNICODE_CHARS
      assertThat(
        incidentsService.reconstructText(
          upsertIncidentRequest().copy(
            description = textOk,
            descriptionAmendments = emptyList(),
          ),
        ),
      ).isEqualTo(textOk)
    }

    @Test
    fun `truncation with unicode too long unicode at end`() {
      // ... see DPS for full text = 25 chars TWO_UNICODE_CHARS have length 6 chars
      val textTooLong = "s".repeat(3967) + TWO_UNICODE_CHARS.repeat(6)
      val result = incidentsService.reconstructText(
        upsertIncidentRequest().copy(
          description = textTooLong,
          descriptionAmendments = emptyList(),
        ),
      )
      assertThat(Utf8.encodedLength(result!!)).isEqualTo(3998) // shorter than 4000 because some unicode has been truncated
      assertThat(result).isEqualTo("${"s".repeat(3967) + TWO_UNICODE_CHARS}... see DPS for full text")
    }

    @Test
    fun `truncation with unicode too long unicode at start`() {
      val textTooLongUnicodeAtStart = TWO_UNICODE_CHARS + "s".repeat(3995)
      val result = incidentsService.reconstructText(
        upsertIncidentRequest().copy(
          description = textTooLongUnicodeAtStart,
          descriptionAmendments = emptyList(),
        ),
      )
      assertThat(result).isEqualTo("${textTooLongUnicodeAtStart.substring(0..3970)}... see DPS for full text")
      assertThat(Utf8.encodedLength(result!!)).isEqualTo(4000)
    }
  }

  private val upsertIncidentRequest: () -> UpsertIncidentRequest = {
    UpsertIncidentRequest(
      title = "Some title",
      description = "Some description",
      descriptionAmendments = emptyList(),
      location = "MDI",
      statusCode = "AWAN",
      typeCode = "TYPE",
      incidentDateTime = LocalDateTime.parse("2025-12-20T01:02:03"),
      reportedDateTime = LocalDateTime.parse("2025-12-20T01:02:03"),
      reportedBy = "USER",
    )
  }
}
