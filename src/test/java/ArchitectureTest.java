import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.github.smartfootballtable.cognition.SFTCognition;
import com.github.smartfootballtable.cognition.detector.Detector;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;

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
			.that().resideInAnyPackage(mainPackage()) //
			.should().onlyBeAccessed().byClassesThat().resideInAPackage(mainPackage());

	String detectorPackage() {
		return "com.github.smartfootballtable.cognition.detector";
	};

	String mainPackage() {
		return "com.github.smartfootballtable.cognition.main";
	}

}