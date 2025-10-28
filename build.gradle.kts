import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
  id("xyz.jpenilla.run-paper") version "3.0.0" // Adds runServer and runMojangMappedServer tasks for testing
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0" // Generates plugin.yml based on the Gradle config
}

group = "me.zombie_striker.psudocommands"
version = "1.0.40-SNAPSHOT"
description = "Allows plugin commands to accept base-minecraft selectors"
java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
}

dependencies {
  paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}
tasks {
  compileJava {
    options.release = 21
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
  }
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
  withSourcesJar()
  withJavadocJar()
}


bukkitPluginYaml {
  main = "me.zombie_striker.psudocommands.PsudoCommands"
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
  authors.addAll("Zombie_Striker (original author)",
    "cricri_21 (lot of fixes and completion)",
    "Kamesuta (base of execute feature)",
    "Loewenjunges(migrated to Paper Brigadier)")
  apiVersion = "1.21.8"
}
