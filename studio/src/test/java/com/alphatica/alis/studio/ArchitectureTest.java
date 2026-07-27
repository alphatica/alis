package com.alphatica.alis.studio;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.alphatica.alis.studio", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule topLevelPackagesShouldBeFreeOfCycles = slices()
			.matching("com.alphatica.alis.studio.(*)..")
			.should().beFreeOfCycles();
}
