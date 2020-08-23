import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.github.smartfootballtable.cognition", importOptions = DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	ArchRule noCycle = slices().matching("com.github.smartfootballtable.cognition.(*)..").should().beFreeOfCycles();

}