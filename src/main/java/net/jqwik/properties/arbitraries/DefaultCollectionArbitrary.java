package net.jqwik.properties.arbitraries;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.*;

import java.util.*;
import java.util.stream.*;

abstract class DefaultCollectionArbitrary<T, U> extends AbstractArbitraryBase implements SizableArbitrary<U> {

	private static final int DEFAULT_COLLECTION_SIZE = Short.MAX_VALUE;

	protected final Arbitrary<T> elementArbitrary;
	protected int minSize = 0;
	protected int maxSize = DEFAULT_COLLECTION_SIZE;

	protected DefaultCollectionArbitrary(Arbitrary<T> elementArbitrary) {
		this.elementArbitrary = elementArbitrary;
	}

	protected RandomGenerator<List<T>> listGenerator(int tries) {
		return createListGenerator(elementArbitrary, tries, cutoffSize(tries));
	}

	protected int cutoffSize(int tries) {
		return RandomGenerators.defaultCutoffSize(minSize, maxSize, tries);
	}

	private RandomGenerator<List<T>> createListGenerator(Arbitrary<T> elementArbitrary, int tries, int cutoffPoint) {
		RandomGenerator<T> elementGenerator = elementGenerator(elementArbitrary, tries);
		List<Shrinkable<List<T>>> samples = samplesList(new ArrayList<>());
		return RandomGenerators.list(elementGenerator, minSize, maxSize, cutoffPoint).withShrinkableSamples(samples);
	}

	protected <C extends Collection> List<Shrinkable<C>> samplesList(C sample) {
		return Stream.of(sample).filter(l -> l.size() >= minSize).filter(l -> maxSize == 0 || l.size() <= maxSize)
				.map(Shrinkable::unshrinkable).collect(Collectors.toList());
	}

	protected RandomGenerator<T> elementGenerator(Arbitrary<T> elementArbitrary, int tries) {
		return elementArbitrary.generator(tries);
	}

	@Override
	public SizableArbitrary<U> ofMinSize(int minSize) {
		DefaultCollectionArbitrary<T, U> clone = typedClone();
		clone.minSize = minSize;
		return clone;
	}

	@Override
	public SizableArbitrary<U> ofMaxSize(int maxSize) {
		DefaultCollectionArbitrary<T, U> clone = typedClone();
		clone.maxSize = maxSize;
		return clone;
	}
}
