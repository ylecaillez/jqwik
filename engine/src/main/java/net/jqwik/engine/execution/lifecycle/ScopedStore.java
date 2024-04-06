package net.jqwik.engine.execution.lifecycle;

import java.util.concurrent.locks.*;
import java.util.function.*;
import java.util.logging.*;

import org.junit.platform.engine.*;

import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.support.*;

import static net.jqwik.engine.support.JqwikStringSupport.*;

public class ScopedStore<T> implements Store<T> {

	private static final Logger LOG = Logger.getLogger(ScopedStore.class.getName());

	private final Object identifier;
	private final Lifespan lifespan;
	private final TestDescriptor scope;
	private final Supplier<T> initialValueSupplier;
	private final Lock lock = new ReentrantLock();

	private T value;
	private boolean initialized = false;

	public ScopedStore(
		Object identifier,
		Lifespan lifespan,
		TestDescriptor scope,
		Supplier<T> initialValueSupplier
	) {
		this.identifier = identifier;
		this.lifespan = lifespan;
		this.scope = scope;
		this.initialValueSupplier = initialValueSupplier;
	}

	@Override
	public T get() {
		lock.lock();
		try {
			if (!initialized) {
				value = initialValueSupplier.get();
				initialized = true;
			}
			return value;
		} finally {
			lock.unlock();
		}
	}

	@Override
	public Lifespan lifespan() {
		return lifespan;
	}

	@Override
	public void update(Function<T, T> updater) {
		lock.lock();
		try {
			value = updater.apply(get());
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void reset() {
		lock.lock();
		try {
			close();
			initialized = false;

			// Free memory as soon as possible, the store object might go live on for a while:
			value = null;
		} finally {
			lock.unlock();
		}
	}

	public Object getIdentifier() {
		return identifier;
	}

	public TestDescriptor getScope() {
		return scope;
	}

	public boolean isVisibleFor(TestDescriptor retriever) {
		return isInScope(retriever);
	}

	private boolean isInScope(TestDescriptor retriever) {
		if (retriever == scope) {
			return true;
		}
		return retriever.getParent().map(this::isInScope).orElse(false);
	}

	@Override
	public String toString() {
		return String.format(
			"Store(%s, %s, %s): [%s]",
			displayString(identifier),
			lifespan.name(),
			scope.getUniqueId(),
			displayString(value)
		);
	}

	public void close() {
		if (!initialized) {
			return;
		}
		closeOnReset();
	}

	private void closeOnReset() {
		if (value instanceof Store.CloseOnReset) {
			try {
				((Store.CloseOnReset) value).close();
			} catch (Throwable throwable) {
				JqwikExceptionSupport.rethrowIfBlacklisted(throwable);
				String message = String.format("Exception while closing store [%s]", this);
				LOG.log(Level.SEVERE, message, throwable);
			}
		}
	}

}

