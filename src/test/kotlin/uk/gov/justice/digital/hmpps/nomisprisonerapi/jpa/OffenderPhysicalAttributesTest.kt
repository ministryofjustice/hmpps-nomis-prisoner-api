package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OffenderPhysicalAttributesTest {

  val id = OffenderPhysicalAttributeId(
    offenderBooking = OffenderBooking(
      bookingId = 1,
      bookingBeginDate = LocalDateTime.now(),
      offender = Offender(nomsId = "A1234AA", gender = Gender("M", "M"), lastName = "Smith"),
    ),
    sequence = 1,
  )

  @Test
  fun `should return null if empty attributes`() {
    val attributes = OffenderPhysicalAttributes(
      id = id,
      heightCentimetres = null,
      heightFeet = null,
      heightInches = null,
      weightKilograms = null,
      weightPounds = null,
    )

    assertThat(attributes.getHeightInCentimetres()).isNull()
    assertThat(attributes.getWeightInKilograms()).isNull()
  }

  @Test
  fun `should return metric measures if imperial measures are empty`() {
    val attributes = OffenderPhysicalAttributes(
      id = id,
      heightCentimetres = 180,
      heightFeet = null,
      heightInches = null,
      weightKilograms = 80,
      weightPounds = null,
    )

    assertThat(attributes.getHeightInCentimetres()).isEqualTo(180)
    assertThat(attributes.getWeightInKilograms()).isEqualTo(80)
  }

  @Test
  fun `should convert from imperial to metric if metric measures are empty`() {
    val attributes = OffenderPhysicalAttributes(
      id = id,
      heightCentimetres = null,
      heightFeet = 5,
      heightInches = 10,
      weightKilograms = null,
      weightPounds = 180,
    )

    assertThat(attributes.getHeightInCentimetres()).isEqualTo(178)
    assertThat(attributes.getWeightInKilograms()).isEqualTo(82)
  }

  @Test
  fun `should return metric height if both imperial and metric measures are present`() {
    val attributes = OffenderPhysicalAttributes(
      id = id,
      heightCentimetres = 180,
      heightFeet = 5,
      heightInches = 10,
      weightKilograms = null,
      weightPounds = null,
    )

    assertThat(attributes.getHeightInCentimetres()).isEqualTo(180)
  }

  @Test
  fun `should convert from imperial weight if both imperial and metric measures are present`() {
    val attributes = OffenderPhysicalAttributes(
      id = id,
      heightCentimetres = null,
      heightFeet = null,
      heightInches = null,
      weightKilograms = 80,
      weightPounds = 180,
    )

    /*
     * Note this result is different to the weightKilograms on the NOMIS record (80).
     * This is because we know the user entered weightPounds, so we convert that to kilograms.
     * We know the user entered weightPounds because had they entered weightKilograms, weightPounds would be 80/0.45359=176.37, clearly not 180.
     */
    assertThat(attributes.getWeightInKilograms()).isEqualTo(82)
  }
}
