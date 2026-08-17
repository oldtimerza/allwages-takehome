package com.allwage.clockin.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(
    packages = "com.allwage.clockin",
    importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {
    private static final DescribedPredicate<JavaClass> REPOSITORY_CLASSES =
            resideInAnyPackage("..repository..")
                    .and(not(resideInAnyPackage("..repository.store..")));

    @ArchTest
    static final ArchRule applicationLayers = layeredArchitecture().consideringOnlyDependenciesInLayers()
        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Client").definedBy("..client..")
        .layer("Repository").definedBy(REPOSITORY_CLASSES)
        .layer("Store").definedBy("..repository.store..")
        .layer("Model").definedBy("..model..")
        .whereLayer("Controller").mayOnlyAccessLayers("Service", "Model")
        .whereLayer("Service").mayOnlyAccessLayers("Client", "Repository", "Model")
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
