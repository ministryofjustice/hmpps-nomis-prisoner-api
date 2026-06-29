package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Area
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AreaClass
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TypeOfArea
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AreaRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class RegionDslMarker

@DslMarker
annotation class AreaDslMarker

@AreaDslMarker
interface AreaDsl {
  fun subArea(
    code: String,
    description: String,
    active: Boolean = true,
    expiryDate: LocalDate? = null,
    areaTypeCode: String? = "INST",
  ): Area
}

@RegionDslMarker
interface RegionDsl {
  fun area(
    code: String,
    description: String,
    active: Boolean = true,
    expiryDate: LocalDate? = null,
    areaTypeCode: String? = "INST",
  ): Area
}

@Component
class AreaBuilderRepository(
  private val areaRepository: AreaRepository,
  private val areaClassRepository: ReferenceCodeRepository<AreaClass>,
  private val typeOfAreaRepository: ReferenceCodeRepository<TypeOfArea>,
) {
  fun save(area: Area): Area = areaRepository.saveAndFlush(area)
  fun areaClassOf(code: String): AreaClass = areaClassRepository.findByIdOrNull(AreaClass.pk(code))!!
  fun typeOfAreaOf(code: String?): TypeOfArea? = code?.let { typeOfAreaRepository.findByIdOrNull(TypeOfArea.pk(code)) }
}

@Component
class AreaBuilderFactory(
  private val repository: AreaBuilderRepository,
) {
  fun builder(): AreaBuilder = AreaBuilder(repository)
}

class AreaBuilder(
  private val repository: AreaBuilderRepository,
) : AreaDsl,
  RegionDsl {
  private lateinit var area: Area
  private lateinit var region: Area

  fun build(
    code: String,
    description: String,
    areaClassCode: String,
    active: Boolean,
    expiryDate: LocalDate?,
    areaTypeCode: String?,
  ): Area = Area(
    areaCode = code,
    description = description,
    areaClass = repository.areaClassOf(areaClassCode),
    listSequence = 99,
    active = active,
    expiryDate = expiryDate,
    type = areaTypeCode?.let { repository.typeOfAreaOf(it) },
  )
    .let { repository.save(it) }
    .also { area = it }

  fun buildRegion(
    code: String,
    description: String,
    areaClassCode: String,
    active: Boolean,
    expiryDate: LocalDate?,
    areaTypeCode: String?,
  ): Area = Area(
    areaCode = code,
    description = description,
    areaClass = repository.areaClassOf(areaClassCode),
    listSequence = 99,
    active = active,
    expiryDate = expiryDate,
    type = areaTypeCode?.let { repository.typeOfAreaOf(it) },
  )
    .let { repository.save(it) }
    .also { region = it }

  override fun subArea(
    code: String,
    description: String,
    active: Boolean,
    expiryDate: LocalDate?,
    areaTypeCode: String?,
  ): Area = Area(
    areaCode = code,
    description = description,
    areaClass = repository.areaClassOf("SUB_AREA"),
    listSequence = 99,
    parentAreaCode = area.areaCode,
  )
    .let { repository.save(it) }

  override fun area(
    code: String,
    description: String,
    active: Boolean,
    expiryDate: LocalDate?,
    areaTypeCode: String?,
  ): Area = Area(
    areaCode = code,
    description = description,
    areaClass = repository.areaClassOf("AREA"),
    listSequence = 99,
    parentAreaCode = region.areaCode,
  )
    .let { repository.save(it) }
}
