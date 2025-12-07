package com.openforum.boot;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests to enforce DDD layer boundaries.
 * These tests prevent accidental cross-layer dependencies that could
 * lead to multi-tenancy data leaks or other security vulnerabilities.
 */
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.openforum");
    }

    /**
     * Services in the application layer should NEVER access JPA repositories
     * directly.
     * They must use the domain repository interfaces to ensure proper tenant
     * isolation.
     */
    @Test
    void services_should_never_access_jpa_repositories_directly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat()
                .resideInAPackage("..infra.jpa.repository..")
                .because("Application layer must only use domain repository interfaces, "
                        + "never JPA implementation details. This prevents accidental "
                        + "cross-tenant data access via methods like findAll().");

        rule.check(importedClasses);
    }

    /**
     * Controllers (REST, Admin, AI) should never access JPA repositories directly.
     */
    @Test
    void controllers_should_never_access_jpa_repositories_directly() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..rest..", "..admin..", "..ai..")
                .and().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat()
                .resideInAPackage("..infra.jpa.repository..")
                .because("Controllers must access data through the application service layer.");

        rule.check(importedClasses);
    }

    /**
     * Domain layer should have no dependencies on infrastructure.
     */
    @Test
    void domain_should_not_depend_on_infrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..infra..", "..rest..", "..admin..")
                .because("Domain layer must be independent of infrastructure concerns.");

        rule.check(importedClasses);
    }
}
