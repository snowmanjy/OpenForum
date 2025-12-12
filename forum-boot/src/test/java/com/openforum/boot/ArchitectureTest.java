package com.openforum.boot;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests to enforce DDD layer boundaries.
 * These tests prevent accidental cross-layer dependencies that could
 * lead to multi-tenancy data leaks or other security vulnerabilities.
 */
@com.tngtech.archunit.junit.AnalyzeClasses(packages = "com.openforum", importOptions = com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule services_should_never_access_jpa_repositories_directly = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAPackage("..infra.jpa.repository..")
            .because("Application layer must only use domain repository interfaces, "
                    + "never JPA implementation details. This prevents accidental "
                    + "cross-tenant data access via methods like findAll().");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule controllers_should_never_access_jpa_repositories_directly = noClasses()
            .that().resideInAnyPackage("..rest..", "..admin..", "..ai..")
            .and().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat()
            .resideInAPackage("..infra.jpa.repository..")
            .because("Controllers must access data through the application service layer.");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule domain_should_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infra..", "..rest..", "..admin..")
            .because("Domain layer must be independent of infrastructure concerns.");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule rule1_auditFields = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().resideInAPackage("..domain.aggregate..")
            .and().areNotInnerClasses()
            .and().areNotMemberClasses()
            .and().areNotEnums()
            .should(haveDependentField("createdAt", java.time.Instant.class))
            .andShould(haveDependentField("lastModifiedAt", java.time.Instant.class))
            .andShould(haveDependentField("createdBy", java.util.UUID.class))
            .andShould(haveDependentField("lastModifiedBy", java.util.UUID.class))
            .as("Rule 1: The Audit Field Mandate - Aggregate Roots must track history with audit fields");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule rule3_repositoryNaming = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().resideInAPackage("..domain.repository..")
            .and().areInterfaces()
            .should().haveSimpleNameEndingWith("Repository")
            .as("Rule 3: Naming Conventions - Domain repositories should end with 'Repository'");

    @com.tngtech.archunit.junit.ArchTest
    static final ArchRule rule3_jpaRepositoryNaming = com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
            .that().resideInAPackage("..infra.jpa.repository..")
            .and().areInterfaces()
            .and().areAssignableTo(org.springframework.data.jpa.repository.JpaRepository.class)
            .should().haveSimpleNameEndingWith("JpaRepository")
            .as("Rule 3: Naming Conventions - JPA Repository interfaces should end with 'JpaRepository'");

    private static com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass> haveDependentField(
            String fieldName, Class<?> type) {
        return new com.tngtech.archunit.lang.ArchCondition<com.tngtech.archunit.core.domain.JavaClass>(
                "have field '" + fieldName + "' of type " + type.getSimpleName()) {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaClass item,
                    com.tngtech.archunit.lang.ConditionEvents events) {
                boolean hasField = item.getAllFields().stream()
                        .anyMatch(f -> f.getName().equals(fieldName) && f.getRawType().isEquivalentTo(type));

                String message = String.format("Class %s does not have field '%s' of type %s", item.getName(),
                        fieldName, type.getSimpleName());

                if (!hasField) {
                    events.add(com.tngtech.archunit.lang.SimpleConditionEvent.violated(item, message));
                }
            }
        };
    }
}
