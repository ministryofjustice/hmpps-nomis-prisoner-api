package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuthenticationFacade
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyInternalLocationBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.AgencyInternalLocationBuilderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
  AuthenticationFacade::class,
  AuditorAwareImpl::class,
  Repository::class,
  NomisDataBuilder::class,
  AgencyInternalLocationBuilderFactory::class,
  AgencyInternalLocationBuilderRepository::class,
)
@WithMockAuthUser
class LocationRepositoryTest {

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  lateinit var repository: AgencyInternalLocationRepository

  @Test
  fun getAgencyInternalLocation() {
    lateinit var location1: AgencyInternalLocation

    nomisDataBuilder.build {
      location1 = agencyInternalLocation(
        locationCode = "MEDI",
        locationType = "MEDI",
        prisonId = "MDI",
        listSequence = 100,
        active = false,
        deactivationDate = LocalDate.parse("2024-01-01"),
      ) {
        attributes(
          profileType = "HOU_SANI_FIT",
          profileCode = "MOB",
        )
        usages(
          internalLocationUsage = -2,
          capacity = 12,
          usageLocationType = "CELL",
        )
      }
    }
    val agencyInternalLocation = repository.findById(location1.locationId).orElseThrow()

    with(agencyInternalLocation) {
      assertThat(locationCode).isEqualTo("MEDI")
      assertThat(locationType).isEqualTo("MEDI")
      assertThat(agency.id).isEqualTo("MDI")
      assertThat(listSequence).isEqualTo(100)
      assertThat(active).isFalse
      assertThat(profiles).extracting("id.profileType", "id.profileCode").containsExactly(
        Tuple("HOU_SANI_FIT", "MOB"),
      )
      assertThat(usages).extracting("usageLocationType.code", "capacity").containsExactly(
        Tuple("CELL", 12),
      )
    }

    repository.delete(agencyInternalLocation)
  }
}
