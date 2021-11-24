import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.github.smartfootballtable.cognition.detector.Detector;
import com.github.smartfootballtable.cognition.main.Main;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = ArchitectureTest.COGNITION_BASE, importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

	protected static final String COGNITION_BASE = "com.github.smartfootballtable.cognition";

	@ArchTest
	ArchRule packagesFreeOfCycles = slices().matching(COGNITION_BASE + ".(*)..").should().beFreeOfCycles();

	@ArchTest
	ArchRule detectorsMustNotDependOnEachOther = noClasses() //
			.that().resideInAPackage(packageOf(Detector.class)) //
			.and().areTopLevelClasses() //
			.should().onlyBeAccessed().byClassesThat().resideInAPackage(packageOf(Detector.class)) //
	;

	@ArchTest
	ArchRule detectorsShouldBeNamedAccordinglyAndResideInTheInterfacesPackage = classes() //
			.that().implement(Detector.class) //
			.should().haveSimpleNameEndingWith("Detector") //
			.andShould().resideInAPackage(packageOf(Detector.class)) //
	;

	@ArchTest
	ArchRule classesInMainShouldNotReferedByOtherPackages = classes() //
			.that().resideInAnyPackage(packageOf(Main.class)) //
			.should().onlyBeAccessed().byClassesThat().resideInAPackage(packageOf(Main.class));

	String packageOf(Class<?> clazz) {
		return clazz.getPackage().getName();
	}

}