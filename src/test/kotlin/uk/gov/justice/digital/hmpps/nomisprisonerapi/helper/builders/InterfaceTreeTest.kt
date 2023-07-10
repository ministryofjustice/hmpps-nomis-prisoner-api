package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InterfaceTreeTest {
  @Test
  fun `inspect interface`() {
    val sb = StringBuilder()
    val md = inspectInterface(NomisDataDsl::class, sb)

    assertThat(md).isNotNull
  }
}
