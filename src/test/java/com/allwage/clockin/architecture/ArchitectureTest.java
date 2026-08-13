package com.allwage.clockin.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(
    packages = "com.allwage.clockin",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    static final ArchRule applicationLayers = layeredArchitecture()
        .consideringAllDependencies()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Client").definedBy("..client..")
        .layer("Repository").definedBy("..repository.site..")
        .layer("Store").definedBy("..repository.store..")
        .layer("Model").definedBy("..model..")
        .whereLayer("Controller").mayOnlyAccessLayers("Service", "Model")
        .whereLayer("Service").mayOnlyAccessLayers("Client", "Repository", "Store", "Model")
        .whereLayer("Repository").mayOnlyAccessLayers("Store", "Model")
        .whereLayer("Client").mayNotAccessAnyLayer()
        .whereLayer("Store").mayNotAccessAnyLayer()
        .whereLayer("Model").mayNotAccessAnyLayer();

    @ArchTest
    static final ArchRule domainModelsAreIndependent = noClasses()
        .that().resideInAPackage("..model..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..controller..", "..service..", "..client..", "..repository..");

    @ArchTest
    static final ArchRule repositoriesDoNotDependOnApplicationLayers = noClasses()
        .that().resideInAPackage("..repository..")
        .and().resideOutsideOfPackage("..repository.store..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("..controller..", "..service..", "..client..");
}
