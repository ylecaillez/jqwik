package net.jqwik.execution.properties.providers;

import net.jqwik.execution.properties.*;
import net.jqwik.properties.*;
import net.jqwik.properties.arbitraries.*;

import java.util.function.*;

public class IntegerArbitraryProvider implements TypedArbitraryProvider {
	@Override
	public boolean canProvideFor(GenericType targetType, boolean withName) {
		return !withName && targetType.isAssignableFrom(Integer.class);
	}

	@Override
	public Arbitrary<?> provideFor(GenericType targetType, Function<GenericType, Arbitrary<?>> subtypeSupplier) {
		return Arbitraries.integer(Integer.MIN_VALUE, Integer.MAX_VALUE);
	}
}
