import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.github.smartfootballtable.cognition.detector.Detector;
import com.github.smartfootballtable.cognition.main.Main;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.github.smartfootballtable.cognition", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	ArchRule noCycle = slices().matching("com.github.smartfootballtable.cognition.(*)..").should().beFreeOfCycles();

	@ArchTest
	ArchRule detectorsMustNotDependOnEachOther = noClasses() //
			.that().resideInAnyPackage(detectorPackage()) //
			.and().areTopLevelClasses() //
			.should().onlyBeAccessed().byClassesThat().resideInAPackage(detectorPackage()) //
	;

	@ArchTest
	ArchRule detectorsShouldBeNamedAccordinglyAndResideInTheInterfacesPackage = classes() //
			.that().implement(Detector.class) //
			.should().haveSimpleNameEndingWith("Detector") //
			.andShould().resideInAPackage(Detector.class.getPackage().getName()) //
	;

	@ArchTest
	ArchRule classesInMainShouldNotReferedByOtherPackages = classes() //
			.that().resideInAnyPackage(packageOf(Main.class)) //
			.should().onlyBeAccessed().byClassesThat().resideInAPackage(packageOf(Main.class));

	String detectorPackage() {
		return "com.github.smartfootballtable.cognition.detector";
	};

	String packageOf(Class<?> clazz) {
		return clazz.getPackage().getName();
	}

}