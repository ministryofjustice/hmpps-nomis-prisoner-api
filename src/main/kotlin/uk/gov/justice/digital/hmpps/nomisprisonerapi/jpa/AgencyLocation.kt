package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.time.LocalDate
import java.util.Objects

@Entity
@Table(name = "AGENCY_LOCATIONS")
data class AgencyLocation(
  @Id
  @Column(name = "AGY_LOC_ID")
  val id: String,

  @Column(name = "DESCRIPTION")
  val description: String,

  @ManyToOne(fetch = FetchType.LAZY)
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
  val type: AgencyLocationType? = null,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @Column(name = "LONG_DESCRIPTION")
  val longDescription: String? = null,

  //    @OneToMany(mappedBy = "agencyLocId", cascade = CascadeType.ALL)
  //    @Default
  //    private List<AgencyLocationEstablishment> establishmentTypes = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "agencyLocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  //    @Default
  //    private List<CaseloadAgencyLocation> caseloadAgencyLocations = new ArrayList<>();
  @Column(name = "DEACTIVATION_DATE")
  val deactivationDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
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
  val courtType: CourtType? = null,

  //    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  //    @Where(clause = "OWNER_CLASS = '"+AgencyAddress.ADDR_TYPE+"'")
  //    @Default
  //    private List<AgencyAddress> addresses = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  //    @Where(clause = "OWNER_CLASS = '"+AgencyPhone.PHONE_TYPE+"'")
  //    @Default
  //    private List<AgencyPhone> phones = new ArrayList<>();
  //
  //    @OneToMany(mappedBy = "agency", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
  //    @Where(clause = "OWNER_CLASS = '"+AgencyInternetAddress.TYPE+"'")
  //    @Default
  //    private List<AgencyInternetAddress> internetAddresses = new ArrayList<>();

) {

  //    public void removeAddress(final AgencyAddress address) {
  //        addresses.remove(address);
  //    }
  //
  //    public AgencyAddress addAddress(final AgencyAddress address) {
  //        address.setAgency(this);
  //        addresses.add(address);
  //        return address;
  //    }
  //    public boolean isPrison() {
  //        return getType().isPrison() && !Arrays.asList(IN, OUT, TRN).contains(getId());
  //    }
  //
  //    public boolean isCourt() {
  //        return getType().isCourt();
  //    }
  //
  //    public boolean isHospital() {
  //        return getType().isHospital();
  //    }
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyLocation
    return id == other.id
  }

  override fun hashCode(): Int {
    return Objects.hashCode(id)
  }

  companion object {
    const val IN = "IN"
    const val OUT = "OUT"
    const val TRN = "TRN"
  }
}
