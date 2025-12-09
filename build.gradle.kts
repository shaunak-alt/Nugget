plugins {
    id("org.springframework.boot") version "3.5.8" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.spring") version "1.9.25" apply false
}

allprojects {
    group = "com.nugget"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
