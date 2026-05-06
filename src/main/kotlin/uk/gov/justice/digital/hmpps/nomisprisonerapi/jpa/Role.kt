package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.io.Serializable

@Entity
@Table(name = "OMS_ROLES")
data class Role(
  @Id
  @SequenceGenerator(name = "ROLE_ID", sequenceName = "ROLE_ID", allocationSize = 1)
  @GeneratedValue(generator = "ROLE_ID")
  @Column(name = "ROLE_ID", nullable = false)
  val id: Long = 0,

  @Column(name = "ROLE_CODE", nullable = false, unique = true)
  val code: String,

  @Column(name = "ROLE_NAME", nullable = false)
  var name: String,

  @Column(name = "ROLE_SEQ", nullable = false)
  var sequence: Int = 1,

  // TODO - check if needed
  // @ManyToOne(fetch = FetchType.LAZY, optional = true)
  // @JoinColumn(name = "PARENT_ROLE_CODE", nullable = true, referencedColumnName = "ROLE_CODE")
  // var parent: Role? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + RoleCaseloadType.CLOAD_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "ROLE_TYPE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var type: RoleCaseloadType? = RoleCaseloadType.APP,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + UserAccountType.USER_AC_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "ROLE_FUNCTION",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var userAccountType: UserAccountType,

  @Column(name = "SYSTEM_DATA_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val systemData: Boolean = true,

  // TODO - check if needed
  // @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], mappedBy = "parent")
  // val childRoles: List<Role> = listOf(),

) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Role

    return code == other.code
  }

  override fun hashCode(): Int = code.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(code = $code )"
}
