plugins {
    java
    id("io.papermc.paperweight.userdev") version "1.7.1"
}

group = "world.elyona"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    compileOnly(files("libs/ElyonaCore.jar"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

tasks.jar {
    // dev サフィックスを付けることで reobfJar の入出力が別ファイルになり
    // Gradle のステール出力クリーンアップによる削除を防ぐ
    archiveClassifier.set("dev")
}

tasks.reobfJar {
    outputJar.convention(layout.buildDirectory.file("libs/ElyonaEconomy-${version}.jar"))
}
