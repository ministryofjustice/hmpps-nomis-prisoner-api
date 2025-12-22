plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "9.3.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  idea
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.8.2")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("org.flywaydb:flyway-core")
  implementation("com.vladmihalcea:hibernate-types-60:2.21.1")
  implementation("org.hibernate.orm:hibernate-community-dialects")
  implementation("com.google.guava:guava:33.5.0-jre")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

  runtimeOnly("com.zaxxer:HikariCP")
  implementation("com.h2database:h2:2.4.240")
  runtimeOnly("com.oracle.database.jdbc:ojdbc10:19.29.0.0")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.8.2")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.41")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

kotlin {
  compilerOptions {
    freeCompilerArgs.addAll("-Xjvm-default=all", "-Xwhen-guards", "-Xannotation-default-target=param-property")
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_25
  targetCompatibility = JavaVersion.VERSION_25
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}

allOpen {
  annotation("uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen")
}

data class ModelConfiguration(val name: String) {
  fun toTestTaskName(): String = "test${nameToCamel()}"
  private val snakeRegex = "-[a-zA-Z]".toRegex()
  private fun nameToCamel(): String = snakeRegex.replace(name) {
    it.value.replace("-", "").uppercase()
  }.replaceFirstChar { it.uppercase() }
}

val testPackages = listOf(
  ModelConfiguration("activities"),
  ModelConfiguration("adjudications"),
  ModelConfiguration("courtsentencing"),
  ModelConfiguration("sentencingadjustments"),
  ModelConfiguration("visitbalances"),
  ModelConfiguration("visits"),
)

testPackages.forEach {
  val test by testing.suites.existing(JvmTestSuite::class)
  val task = tasks.register<Test>(it.toTestTaskName()) {
    testClassesDirs = files(test.map { it.sources.output.classesDirs })
    classpath = files(test.map { it.sources.runtimeClasspath })
    group = "Run tests"
    description = "Run tests for ${it.name}"
    shouldRunAfter("test")
    useJUnitPlatform()
    filter {
      includeTestsMatching("uk.gov.justice.digital.hmpps.nomisprisonerapi.${it.name}.*")
    }
  }
  tasks.check { dependsOn(task) }
}

tasks.test {
  filter {
    testPackages.forEach {
      excludeTestsMatching("uk.gov.justice.digital.hmpps.nomisprisonerapi.${it.name}.*")
    }
  }
}
