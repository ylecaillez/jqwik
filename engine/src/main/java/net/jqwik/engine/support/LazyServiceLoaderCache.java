package net.jqwik.engine.support;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

public class LazyServiceLoaderCache<S> {
	private static final Logger LOG = Logger.getLogger(LazyServiceLoaderCache.class.getName());

	private final Class<S> clz;
	private final Lock lock = new ReentrantLock();
	private List<S> services;

	public LazyServiceLoaderCache(Class<S> clz) {
		this.clz = clz;
	}

	public List<S> getServices() {
		if (services == null) {
			loadServices();
		}
		return services;
	}

	private void loadServices() {
		lock.lock();
		try {
			services = new CopyOnWriteArrayList<>();
			try {
				for (S s : ServiceLoader.load(clz)) {
					services.add(s);
				}
			} catch (ServiceConfigurationError serviceConfigurationError) {
				String message = String.format(
						"Cannot load services of type [%s].%n    %s",
						clz.getName(),
						serviceConfigurationError.getMessage()
				);
				LOG.log(Level.SEVERE, message);
			}
		} finally {
			lock.unlock();
		}
	}
}
