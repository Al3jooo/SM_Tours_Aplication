package co.edu.unimagdalena.samaria360.auth.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "co.edu.unimagdalena.samaria360.auth",
        importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule dominioSinFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "com.fasterxml.jackson..",
                    "lombok..")
            .because("domain debe ser Java puro");

    @ArchTest
    static final ArchRule dominioNoDependeDeOtrasCapas = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..")
            .because("en la arquitectura hexagonal las dependencias apuntan hacia domain");

    @ArchTest
    static final ArchRule aplicacionSinInfraestructuraNiFrameworks = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infrastructure..",
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..")
            .because("los casos de uso solo dependen de domain y de sus puertos; la transacción va en infraestructura");

    @ArchTest
    static final ArchRule adaptadoresDeEntradaSoloUsanPuertosDeEntrada = noClasses()
            .that().resideInAPackage("..adapter.in..")
            .should().dependOnClassesThat().resideInAnyPackage("..application.service..", "..application.port.out..")
            .because("los controladores invocan los casos de uso solo a través de los puertos de entrada");
}
