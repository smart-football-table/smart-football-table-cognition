package com.github.smartfootballtable.cognition.main;

import static com.github.smartfootballtable.cognition.main.EnvVars.envVarsAndArgs;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.github.smartfootballtable.cognition.main.EnvVars.EnvVar;
import com.github.stefanbirkner.systemlambda.SystemLambda.WithEnvironmentVariables;

class EnvVarsTest {

	private static final String OPTION_NAME_AAA = "aaa";
	private static final String ENVVAR_NAME_LC = "bbb";

	public static interface SomeOption {
		String optionName();

		String someOption();
	}

	public static class ParameterClass implements SomeOption {
		private static final String OPTION_NAME = "-" + OPTION_NAME_AAA;
		@Option(name = OPTION_NAME)
		public String someOption;

		@Override
		public String optionName() {
			return OPTION_NAME;
		}

		@Override
		public String someOption() {
			return someOption;
		}
	}

	public static class ParameterClassMultiDashes implements SomeOption {
		private static final String OPTION_NAME = "-----" + OPTION_NAME_AAA;
		@Option(name = OPTION_NAME)
		public String someOption;

		@Override
		public String optionName() {
			return OPTION_NAME;
		}

		@Override
		public String someOption() {
			return someOption;
		}
	}

	public static class ParameterClassWithEnvVar implements SomeOption {
		private static final String OPTION_NAME = "-" + OPTION_NAME_AAA;
		@EnvVar(ENVVAR_NAME_LC)
		@Option(name = OPTION_NAME)
		public String someOption;

		@Override
		public String optionName() {
			return OPTION_NAME;
		}

		@Override
		public String someOption() {
			return someOption;
		}
	}

	@ParameterizedTest
	@MethodSource("parameterClasses")
	void doesReadEnvVarFooIfAnnotatedUsingEnvVarAnno(SomeOption bean) throws Exception {
		parse(bean, withEnvironmentVariable(OPTION_NAME_AAA.toUpperCase(), "---ENV---"));
		assertThat(bean.someOption(), is("---ENV---"));
	}

	@ParameterizedTest
	@MethodSource("parameterClasses")
	void cmdLineOverridesEnvVars(SomeOption bean) throws Exception {
		parse(bean, withEnvironmentVariable(OPTION_NAME_AAA.toUpperCase(), "---ENV---"), bean.optionName(),
				"---CMDLINE---");
		assertThat(bean.someOption(), is("---CMDLINE---"));
	}

	@ParameterizedTest
	@MethodSource("parameterClasses")
	void doesNotConsiderLowerCaseEnvVar(SomeOption bean) throws Exception {
		parse(bean, withEnvironmentVariable(OPTION_NAME_AAA.toLowerCase(), "---ENV---"));
		assertThat(bean.someOption(), is(nullValue()));
	}

	@Test
	void withExplicitEnvVarAnnoIfIfEnvNameIsLowerCase() throws Exception {
		assert ENVVAR_NAME_LC.equals(ENVVAR_NAME_LC.toLowerCase());
		ParameterClassWithEnvVar bean = new ParameterClassWithEnvVar();
		parse(bean, withEnvironmentVariable(ENVVAR_NAME_LC.toUpperCase(), "---UC-ENV---") //
				.and(ENVVAR_NAME_LC, "---LC-ENV---"));
		assertThat(bean.someOption(), is("---LC-ENV---"));
	}

	private static List<SomeOption> parameterClasses() {
		return Arrays.asList(new ParameterClass(), new ParameterClassMultiDashes());
	}

	private static void parse(Object bean, WithEnvironmentVariables withEnvironmentVariable, String... args) {
		try {
			withEnvironmentVariable.execute(() -> parseArgs(bean, args));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void parseArgs(Object bean, String... args) throws CmdLineException {
		CmdLineParser parser = new CmdLineParser(bean);
		parser.parseArgument(envVarsAndArgs(parser, args));
	}

}
