package net.jqwik.api;

import java.util.function.*;

import org.apiguardian.api.*;

import static org.apiguardian.api.API.Status.*;

@API(status = MAINTAINED, since = "1.5.4")
public class Builders {

	private Builders() {
	}

	/**
	 * Combine Arbitraries by means of a builder.
	 *
	 * @param builderSupplier The supplier will be called freshly for each value generation.
	 *                        For exhaustive generation all supplied objects are
	 *                        supposed to be identical.
	 * @return BuilderCombinator instance
	 */
	public static <B> BuilderCombinator<B> withBuilder(Supplier<B> builderSupplier) {
		return new BuilderCombinator<>(Arbitraries.create(builderSupplier));
	}

	/**
	 * Combine Arbitraries by means of a builder.
	 *
	 * @param builderArbitrary The arbitrary is used to generate a builder object
	 *                         as starting point for building on each value generation.
	 * @return BuilderCombinator instance
	 */
	public static <B> BuilderCombinator<B> withBuilder(Arbitrary<B> builderArbitrary) {
		return new BuilderCombinator<>(builderArbitrary);
	}

	/**
	 * Provide access to combinators through builder functionality.
	 *
	 * A builder is created through either {@linkplain #withBuilder(Supplier)}
	 * or {@linkplain #withBuilder(Arbitrary)}.
	 *
	 * @param <B> The builder's type
	 */
	public static class BuilderCombinator<B> {
		private final Arbitrary<B> starter;

		private BuilderCombinator(Arbitrary<B> delegate) {
			this.starter = delegate;
		}

		public <T> CombinableBuilder<B, T> use(Arbitrary<T> arbitrary) {
			return maybeUse(arbitrary, 1.0);
		}

		public <T> CombinableBuilder<B, T> maybeUse(Arbitrary<T> arbitrary, double probabilityOfUsage) {
			if (probabilityOfUsage < 0.0 || probabilityOfUsage > 1.0) {
				String message = String.format("Usage probability of [%s] is outside allowed range (0;1)", probabilityOfUsage);
				throw new IllegalArgumentException(message);
			}
			return new CombinableBuilder<>(this, arbitrary);
		}

		/**
		 * Create the final arbitrary.
		 *
		 * @param buildFunction Function to map a builder to an object
		 * @param <T>           the target object's type
		 * @return arbitrary of target object
		 */
		public <T> Arbitrary<T> build(Function<B, T> buildFunction) {
			return null;
			// return builder.map(buildFunction);
		}

		/**
		 * Create the final arbitrary if it's the builder itself.
		 *
		 * @return arbitrary of builder
		 */
		public Arbitrary<B> build() {
			return build(Function.identity());
		}
	}

	/**
	 * Functionality to manipulate a builder. Instances are created through
	 * {@link BuilderCombinator#use(Arbitrary)}.
	 *
	 * @param <B> The builder's type
	 */
	public static class CombinableBuilder<B, T> {
		private final BuilderCombinator<B> combinator;
		private final Arbitrary<T> arbitrary;

		private CombinableBuilder(BuilderCombinator<B> combinator, Arbitrary<T> arbitrary) {
			this.combinator = combinator;
			this.arbitrary = arbitrary;
		}

		/**
		 * Use the last provided arbitrary to change the builder object.
		 * Potentially create a different kind of builder.
		 *
		 * @param toFunction Use value provided by arbitrary to set current builder
		 *                   and return builder of same type.
		 * @return new {@linkplain BuilderCombinator} instance
		 */
		public BuilderCombinator<B> in(BiFunction<B, T, B> toFunction) {
			// TODO: Add something to the combinator
			return combinator;
		}

		/**
		 * Use the last provided arbitrary to change the builder object
		 * and proceed with the same builder. The most common scenario is
		 * a builder the methods of which do not return a new builder.
		 *
		 * @param setter Use value provided by arbitrary to change a builder's property.
		 * @return new {@linkplain BuilderCombinator} instance with same embedded builder
		 */
		public BuilderCombinator<B> inSetter(BiConsumer<B, T> setter) {
			BiFunction<B, T, B> toFunction = (b, t) -> {
				setter.accept(b, t);
				return b;
			};
			return in(toFunction);
		}
	}

}
