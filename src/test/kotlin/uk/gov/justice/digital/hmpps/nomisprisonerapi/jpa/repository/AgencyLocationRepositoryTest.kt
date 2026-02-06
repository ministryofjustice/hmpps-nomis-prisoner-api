package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AgencyLocationRepositoryTest {

  @Autowired
  lateinit var repository: AgencyLocationRepository

  @Test
  fun getAgencyLocation() {
    val agencyLocations = repository.findByTypeAndActiveAndDeactivationDateIsNullAndIdNotInOrderById(
      type = AgencyLocationType.PRISON_TYPE,
      active = true,
    )
    assertThat(agencyLocations.size).isEqualTo(11)
    assertThat(agencyLocations.map { it.id }).containsExactly("BMI", "BXI", "LEI", "MDI", "MUL", "OUT", "RNI", "SYI", "TRN", "TRO", "WAI")
  }

  @Test
  fun getAgencyLocationInactive() {
    val agencyLocations = repository.findByTypeAndActiveAndDeactivationDateIsNullAndIdNotInOrderById(
      type = AgencyLocationType.PRISON_TYPE,
      active = false,
    )
    assertThat(agencyLocations.size).isEqualTo(2)
    assertThat(agencyLocations[0].id).isEqualTo("*ALL*")
    assertThat(agencyLocations[0].description).isEqualTo("Dummy entry for service switching")
    assertThat(agencyLocations[1].id).isEqualTo("ZZGHI")
    assertThat(agencyLocations[1].description).isEqualTo("GHOST")
  }

  @Test
  fun getAgencyLocationIgnoreList() {
    val agencyLocations = repository.findByTypeAndActiveAndDeactivationDateIsNullAndIdNotInOrderById(
      type = AgencyLocationType.PRISON_TYPE,
      active = false,
      ignoreList = listOf("*ALL*"),
    )
    assertThat(agencyLocations.size).isEqualTo(1)
    assertThat(agencyLocations[0].id).isEqualTo("ZZGHI")
    assertThat(agencyLocations[0].description).isEqualTo("GHOST")
  }
}
