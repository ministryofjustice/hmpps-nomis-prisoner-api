package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.DiscriminatorFormula
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationType.Companion.PRISON_TYPE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.util.Objects

@Entity
@Table(name = "AGENCY_LOCATIONS")
@EntityOpen
@DiscriminatorFormula(
  """
    case
        when AGENCY_LOCATION_TYPE = 'INST' then 'Prison'
        else 'Agency'
    end
""",
)
@Inheritance
class AgencyLocation(
  @Id
  @Column(name = "AGY_LOC_ID")
  val id: String,

  @Column(name = "DESCRIPTION")
  var description: String,

  @Column(name = "LONG_DESCRIPTION")
  var longDescription: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AgencyLocationType.AGY_LOC_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "AGENCY_LOCATION_TYPE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val type: AgencyLocationType = PRISON_TYPE,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = false,

  @Column(name = "DEACTIVATION_DATE")
  var deactivationDate: LocalDate? = null,

  @OneToMany(mappedBy = "agencyLocation", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${AgencyLocationAddress.ADDR_TYPE}'")
  val addresses: MutableList<AgencyLocationAddress> = mutableListOf(),

  @OneToMany(mappedBy = "agencyLocation", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${AgencyLocationPhone.PHONE_TYPE}'")
  val phones: MutableList<AgencyLocationPhone> = mutableListOf(),

  @OneToMany(mappedBy = "agencyLocation", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${AgencyLocationInternetAddress.TYPE}'")
  val emailAddresses: MutableList<AgencyLocationInternetAddress> = mutableListOf(),

  @Column(name = "UPDATED_ALLOWED_FLAG")
  @Convert(converter = YesNoConverter::class)
  var updateAllowed: Boolean = true,

  // ABBREVIATION: all null
  @Column(name = "CONTACT_NAME")
  var contactName: String? = null,

  // PRINT_QUEUE: all null

  // `OUT` also has a court type, but maybe move to a Court entity later
  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CourtType.JURISDICTION + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "JURISDICTION_CODE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var courtType: CourtType? = null,

  // BAIL_OFFICE_FLAG default true so a mixture of Y and N - but can't see anywhere it used or can be set - so for now do not map
  // LIST_SEQ only set for OUT so do not map for now
  // HOUSING_LEV_*_CODE and PROPERTY_LEV_*_CODE not used outside NOMIS - will not map so will never be changed other than via a NOMIS script
  // LAST_BOOKING_NO always null
  // COMMISSARY_PRIVILEGE always null
  // BUSINESS_HOURS always null
  // ADDRESS_TYPE on set via script new prisons - assume not used

  // reference domain DISABILITY  (BA, N, WC, Y) does not match many of these codes so map as string
  @Column(name = "DISABILITY_ACCESS_CODE")
  var disabilityAccessCode: String? = null,

  // INTAKE_FLAG only a few records have this set to Y, in NOMIS UI but never read - so do not map

  // data is from AREAS with class SUB_AREA
  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "SUB_AREA_CODE", referencedColumnName = "AREA_CODE", nullable = true)
  var subArea: Area? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "AREA_CODE", referencedColumnName = "AREA_CODE", nullable = true)
  var area: Area? = null,

  // NOMIS form maps this to REFERENCE_CODES <GEOGRAPHIC> - but data is from AREAS with class AREA
  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "GEOGRAPHIC_REGION_CODE", referencedColumnName = "AREA_CODE", nullable = true)
  var region: Area? = null,

  // data is from AREAS with class REGION
  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "NOMS_REGION_CODE", referencedColumnName = "AREA_CODE", nullable = true)
  var nomsRegion: Area? = null,

  // JUSTICE_AREA_CODE all null - not mapped

  @Column(name = "CJIT_CODE")
  var cjitCode: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + PayrollRegionType.PAYROLL_REG + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "PAYROLL_REGION",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var payrollRegion: PayrollRegionType? = null,

  @OneToMany(mappedBy = "id.agencyLocation", cascade = [CascadeType.ALL], orphanRemoval = true)
  val localAuthorities: MutableSet<AgencyLocationAuthority> = mutableSetOf(),

) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyLocation
    return id == other.id
  }

  override fun hashCode(): Int = Objects.hashCode(id)

  companion object {
    const val IN = "IN"
    const val OUT = "OUT"
    const val TRN = "TRN"
  }
}

@Entity
class Prison(
  id: String,
  description: String,
  type: AgencyLocationType,
  active: Boolean,
  deactivationDate: LocalDate?,
  updateAllowed: Boolean,
  contactName: String?,
  courtType: CourtType?,
  disabilityAccessCode: String?,
  subArea: Area?,
  area: Area?,
  region: Area?,
  nomsRegion: Area?,
  cjitCode: String?,
  longDescription: String?,
  payrollRegion: PayrollRegionType?,

  // FD (for OUT and TRN) not in reference data, so ignore if not found
  @NotFound(action = NotFoundAction.IGNORE)
  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + GeographicType.GEOGRAPHIC + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "DISTRICT_CODE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var district: GeographicType? = null,

) : AgencyLocation(
  id = id,
  description = description,
  type = type,
  active = active,
  deactivationDate = deactivationDate,
  updateAllowed = updateAllowed,
  contactName = contactName,
  courtType = courtType,
  disabilityAccessCode = disabilityAccessCode,
  subArea = subArea,
  area = area,
  region = region,
  nomsRegion = nomsRegion,
  cjitCode = cjitCode,
  longDescription = longDescription,
  payrollRegion = payrollRegion,
)

@Entity
class Agency(
  id: String,
  description: String,
  type: AgencyLocationType,
  active: Boolean,
  deactivationDate: LocalDate?,
  updateAllowed: Boolean,
  contactName: String?,
  courtType: CourtType?,
  disabilityAccessCode: String?,
  subArea: Area?,
  area: Area?,
  region: Area?,
  nomsRegion: Area?,
  cjitCode: String?,
  longDescription: String?,
  payrollRegion: PayrollRegionType?,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AreaType.AREA + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "DISTRICT_CODE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var district: AreaType? = null,

) : AgencyLocation(
  id = id,
  description = description,
  type = type,
  active = active,
  deactivationDate = deactivationDate,
  updateAllowed = updateAllowed,
  contactName = contactName,
  courtType = courtType,
  disabilityAccessCode = disabilityAccessCode,
  subArea = subArea,
  area = area,
  region = region,
  nomsRegion = nomsRegion,
  cjitCode = cjitCode,
  longDescription = longDescription,
  payrollRegion = payrollRegion,
)
