package uk.gov.justice.digital.hmpps.nomisprisonerapi.locations

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocationProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocationProfileId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.HousingUnitType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationUsageLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.LivingUnitReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.InternalLocationUsageRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.util.Optional

private const val PRISON_ID = "SWI"
private const val PRISON_DESCRIPTION = "Swansea"
private const val LOCATION_ID = 12345L

internal class LocationServiceTest {

  private val agencyInternalLocationRepository: AgencyInternalLocationRepository = mock()
  private val agencyLocationRepository: AgencyLocationRepository = mock()
  private val internalLocationTypeRepository: ReferenceCodeRepository<InternalLocationType> = mock()
  private val housingUnitTypeRepository: ReferenceCodeRepository<HousingUnitType> = mock()
  private val livingUnitReasonRepository: ReferenceCodeRepository<LivingUnitReason> = mock()
  private val internalLocationUsageRepository: InternalLocationUsageRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val locationService = LocationService(
    agencyInternalLocationRepository,
    agencyLocationRepository,
    internalLocationTypeRepository,
    housingUnitTypeRepository,
    livingUnitReasonRepository,
    internalLocationUsageRepository,
    telemetryClient,
  )

  val agencyLocation = AgencyLocation(PRISON_ID, PRISON_DESCRIPTION)

  @BeforeEach
  fun setup() {
    whenever(internalLocationTypeRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(InternalLocationType((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(housingUnitTypeRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(HousingUnitType((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(livingUnitReasonRepository.findById(any())).thenAnswer {
      return@thenAnswer Optional.of(LivingUnitReason((it.arguments[0] as ReferenceCode.Pk).code, "desc"))
    }
    whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(
      Optional.of(agencyLocation),
    )
    whenever(
      internalLocationUsageRepository.findOneByAgency_IdAndInternalLocationUsage(
        eq(PRISON_ID),
        any(),
      ),
    ).thenAnswer {
      return@thenAnswer InternalLocationUsage(99L, agencyLocation, it.arguments[1] as String)
    }
  }

  @Nested
  internal inner class Create {
    private val createRequest = CreateLocationRequest(
      prisonId = PRISON_ID,
      locationType = "CELL",
      description = "A-1-001",
      capacity = 3,
      operationalCapacity = 2,
      certified = true,
      locationCode = "code",
      listSequence = 1,
      comment = "Some comment",
      unitType = "HC",
      userDescription = "Some description",
      profiles = listOf(
        ProfileRequest(profileType = "HOU_SANI_FIT", profileCode = "MOB"),
      ),
      usages = listOf(
        UsageRequest(capacity = 12, sequence = 1, internalLocationUsageType = "GENERAL"),
      ),
    )

    @BeforeEach
    fun setup() {
      whenever(agencyInternalLocationRepository.save(any())).thenAnswer {
        (it.arguments[0] as AgencyInternalLocation).copy(locationId = LOCATION_ID)
      }
    }

    @Test
    fun `data is mapped correctly`() {
      assertThat(locationService.createLocation(createRequest))
        .isEqualTo(LocationIdResponse(LOCATION_ID))

      verify(agencyInternalLocationRepository).save(
        check {
          assertThat(it.active).isTrue
          assertThat(it.description).isEqualTo("A-1-001")
          assertThat(it.certified).isTrue
          assertThat(it.tracking).isTrue
          assertThat(it.locationType).isEqualTo("CELL")
          assertThat(it.unitType?.code).isEqualTo("HC")
          assertThat(it.agency.id).isEqualTo(PRISON_ID)
          assertThat(it.parentLocation).isNull()
          assertThat(it.currentOccupancy).isEqualTo(0)
          assertThat(it.operationalCapacity).isEqualTo(2)
          assertThat(it.userDescription).isEqualTo("Some description")
          assertThat(it.locationCode).isEqualTo("code")
          assertThat(it.capacity).isEqualTo(3)
          assertThat(it.listSequence).isEqualTo(1)
          assertThat(it.cnaCapacity).isNull()
          assertThat(it.comment).isEqualTo("Some comment")
          assertThat(it.unitType?.code).isEqualTo("HC")

          assertThat(it.profiles).extracting("id.profileType", "id.profileCode").containsExactly(
            tuple("HOU_SANI_FIT", "MOB"),
          )
          assertThat(it.usages).extracting(
            "capacity",
            "internalLocationUsage.internalLocationUsage",
            "listSequence",
          ).containsExactly(
            tuple(12, "GENERAL", 1),
          )
        },
      )
    }

    @Test
    fun prisonNotFound() {
      whenever(agencyLocationRepository.findById(PRISON_ID)).thenReturn(Optional.empty())

      val thrown = assertThrows<BadDataException> {
        locationService.createLocation(createRequest)
      }
      assertThat(thrown.message).isEqualTo("Prison with id=$PRISON_ID does not exist")
    }
  }

  @Nested
  internal inner class Update {
    private val updateRequest = UpdateLocationRequest(
      locationType = "CELL",
      description = "NEW-LOC",
      locationCode = "002",
      listSequence = 2,
      comment = "Some comment",
      unitType = "HC",
      userDescription = "New description",
      profiles = listOf(
        ProfileRequest(profileType = "HOU_SANI_FIT", profileCode = "MOB"),
        ProfileRequest(profileType = "HOU_SANI_ATT", profileCode = "NEW"),
      ),
      usages = listOf(
        UsageRequest(capacity = 14, sequence = 2, internalLocationUsageType = "GENERAL"),
        UsageRequest(capacity = 15, sequence = 3, internalLocationUsageType = "NEW"),
      ),
    )

    val agencyInternalLocation = AgencyInternalLocation(
      locationId = LOCATION_ID,
      active = true,
      locationCode = "001",
      description = "A-1-001",
      certified = false,
      tracking = false,
      locationType = "CELL",
      agency = AgencyLocation(PRISON_ID, PRISON_DESCRIPTION),
      userDescription = "User description",
      listSequence = 1,
    )
      .apply {
        profiles.add(
          AgencyInternalLocationProfile(
            AgencyInternalLocationProfileId(LOCATION_ID, "HOU_SANI_FIT", "MOB"),
            this,
          ),
        )
        profiles.add(
          AgencyInternalLocationProfile(
            AgencyInternalLocationProfileId(LOCATION_ID, "HOU_SANI_ATT", "OLD"),
            this,
          ),
        )
        usages.add(
          InternalLocationUsageLocation(
            internalLocationUsage = InternalLocationUsage(1L, this.agency, "OLD"),
            agencyInternalLocation = this,
            capacity = 12,
            listSequence = 1,
          ),
        )
        usages.add(
          InternalLocationUsageLocation(
            internalLocationUsage = InternalLocationUsage(2L, this.agency, "GENERAL"),
            agencyInternalLocation = this,
            capacity = 13,
            listSequence = 2,
          ),
        )
      }

    @BeforeEach
    fun setup() {
      whenever(agencyInternalLocationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(agencyInternalLocation))
    }

    @Test
    fun `data is updated correctly`() {
      locationService.updateLocation(LOCATION_ID, updateRequest)

      assertThat(agencyInternalLocation.locationCode).isEqualTo("002")
      assertThat(agencyInternalLocation.description).isEqualTo("NEW-LOC")
      assertThat(agencyInternalLocation.locationType).isEqualTo("CELL")
      assertThat(agencyInternalLocation.unitType?.code).isEqualTo("HC")
      assertThat(agencyInternalLocation.userDescription).isEqualTo("New description")
      assertThat(agencyInternalLocation.listSequence).isEqualTo(2)
      assertThat(agencyInternalLocation.profiles).extracting("id.profileType", "id.profileCode").containsExactly(
        tuple("HOU_SANI_FIT", "MOB"),
        tuple("HOU_SANI_ATT", "NEW"),
      )
      assertThat(agencyInternalLocation.usages).extracting(
        "capacity",
        "internalLocationUsage.internalLocationUsage",
        "listSequence",
      ).containsExactly(
        tuple(14, "GENERAL", 2),
        tuple(15, "NEW", 3),
      )
    }

    @Test
    fun `invalid location usage code`() {
      whenever(
        internalLocationUsageRepository.findOneByAgency_IdAndInternalLocationUsage(PRISON_ID, "NEW"),
      ).thenReturn(null)

      assertThat(
        assertThrows<BadDataException> { locationService.updateLocation(LOCATION_ID, updateRequest) }.message,
      ).isEqualTo("Internal location usage with code=NEW at prison $PRISON_ID does not exist")
    }

    @Test
    fun `addition to lists`() {
      agencyInternalLocation.profiles.clear()
      agencyInternalLocation.usages.clear()

      locationService.updateLocation(LOCATION_ID, updateRequest)

      assertThat(agencyInternalLocation.profiles).extracting("id.profileType", "id.profileCode").containsExactly(
        tuple("HOU_SANI_FIT", "MOB"),
        tuple("HOU_SANI_ATT", "NEW"),
      )
      assertThat(agencyInternalLocation.usages).extracting(
        "capacity",
        "internalLocationUsage.internalLocationUsage",
        "listSequence",
      ).containsExactly(
        tuple(14, "GENERAL", 2),
        tuple(15, "NEW", 3),
      )
    }

    @Test
    fun `removal from lists`() {
      locationService.updateLocation(LOCATION_ID, updateRequest.copy(profiles = null, usages = null))

      assertThat(agencyInternalLocation.profiles).isEmpty()
      assertThat(agencyInternalLocation.usages).isEmpty()
    }
  }
}
