import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

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
	ArchRule detectorsMustNotDependOnEachOther = classes() //
			.that().resideInAPackage("com.github.smartfootballtable.cognition.detector") //
			.and().areTopLevelClasses() //
			.should(notDependOnClassesInSamePackage()) //
	;

	@ArchTest
	ArchRule detectorsShouldBeNamedAccordinglyAndResideInTheInterfacesPackage = classes() //
			.that().implement(Detector.class) //
			.should().haveSimpleNameEndingWith("Detector") //
			.andShould().resideInAPackage(Detector.class.getPackage().getName());

	String detectorPackage() {
		return "com.github.smartfootballtable.cognition.detector";
	};

	private ArchCondition<JavaClass> notDependOnClassesInSamePackage() {
		return new ArchCondition<JavaClass>("not depend on classes in same package") {
			@Override
			public void check(JavaClass item, ConditionEvents events) {
				for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
					if (samePackage(dependency.getOriginClass(), dependency.getTargetClass())
							&& !dependency.getTargetClass().isNestedClass()
							&& !implementing(Detector.class, dependency)) {
						String message = String.format("Class %s has dependency %s", item.getFullName(), dependency);
						events.add(violated(item, message));
					}
				}
			}

			private boolean samePackage(JavaClass class1, JavaClass class2) {
				return class1.getPackage().equals(class2.getPackage());
			}

			private boolean implementing(Class<?> iface, Dependency dependency) {
				return dependency.getTargetClass().isEquivalentTo(iface)
						&& dependency.getOriginClass().getInterfaces().stream().anyMatch(c -> c.isEquivalentTo(iface));
			}

		};
	}

}