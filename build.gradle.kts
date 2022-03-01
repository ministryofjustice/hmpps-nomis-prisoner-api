plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.0.5-beta"
  kotlin("plugin.spring") version "1.6.10"
  kotlin("plugin.jpa") version "1.6.10"
  idea
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.flywaydb:flyway-core:8.5.1")
  implementation("com.vladmihalcea:hibernate-types-52:2.14.0")

  implementation("org.springdoc:springdoc-openapi-ui:1.6.6")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.6.6")
  implementation("org.springdoc:springdoc-openapi-data-rest:1.6.6")

  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")

  implementation("com.zaxxer:HikariCP:5.0.1")
  runtimeOnly("com.h2database:h2:2.1.210")
  runtimeOnly("com.oracle.database.jdbc:ojdbc10:19.13.0.0.1")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.mockito:mockito-inline:4.3.1")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.0.30")
  testImplementation("org.springframework.security:spring-security-test")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
}
