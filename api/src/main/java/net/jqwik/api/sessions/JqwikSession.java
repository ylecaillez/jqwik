package net.jqwik.api.sessions;

import org.apiguardian.api.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

import static org.apiguardian.api.API.Status.*;

import java.util.concurrent.locks.*;

/**
 * JqwikSession is the abstraction to give users of {@linkplain Arbitrary#sample()}
 * and {@linkplain Arbitrary#sampleStream()} outside the jqwik lifecycle
 * more control over the lifecycle.
 * This has also influence on memory heap usage since an un-finished session will
 * aggregate state, e.g. through caching and other {@linkplain Store stores}.
 */
@API(status = MAINTAINED, since = "1.8.0")
public class JqwikSession {
	private static final Lock lock = new ReentrantLock();

	@FunctionalInterface
	public interface Runnable {
		void run() throws Throwable;
	}

	@API(status = INTERNAL)
	public abstract static class JqwikSessionFacade {
		private static final JqwikSession.JqwikSessionFacade implementation;

		static {
			implementation = FacadeLoader.load(JqwikSession.JqwikSessionFacade.class);
		}

		public abstract void startSession();

		public abstract void finishSession();

		public abstract void finishTry();

		public abstract boolean isSessionOpen();

		public abstract void runInSession(Runnable runnable);
	}

	public static void start() {
		lock.lock();
		try {
			JqwikSessionFacade.implementation.startSession();
		} finally {
			lock.unlock();
		}
	}

	public static boolean isActive() {
		return JqwikSessionFacade.implementation.isSessionOpen();
	}

	public static void finish() {
		lock.lock();
		try {
			JqwikSessionFacade.implementation.finishSession();
		} finally {
			lock.unlock();
		}
	}

	public static void finishTry() {
		lock.lock();
		try {
			JqwikSessionFacade.implementation.finishTry();
		} finally {
			lock.unlock();
		}
	}

	public static void run(Runnable runnable) {
		lock.lock();
		try {
			JqwikSessionFacade.implementation.runInSession(runnable);
		} finally {
			lock.unlock();
		}
	}


}
