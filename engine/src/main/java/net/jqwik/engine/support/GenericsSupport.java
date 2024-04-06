package net.jqwik.engine.support;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.locks.*;

import net.jqwik.api.providers.*;

public class GenericsSupport {

	private static final Map<TypeUsage, GenericsClassContext> contextsCache = new LinkedHashMap<>();
	private static final Lock lock = new ReentrantLock();

	/**
	 * Return a context object which can resolve generic types for a given {@code contextClass}.
	 * <p>
	 * Must be synchronized because of caching.
	 *
	 * @param contextClass The class to wrap in a context
	 * @return a potentially cached context object
	 */
	public static GenericsClassContext contextFor(Class<?> contextClass) {
		lock.lock();
		try {
			if (contextClass == null) {
				return GenericsClassContext.NULL;
			}
			return contextFor(TypeUsage.of(contextClass));
		} finally {
			lock.unlock();
		}
	}

	public static GenericsClassContext contextFor(TypeUsage typeUsage) {
		lock.lock();
		try {
			return contextsCache.computeIfAbsent(typeUsage, GenericsSupport::createContext);
		} finally {
			lock.unlock();
		}
	}

	private static GenericsClassContext createContext(TypeUsage typeUsage) {
		Class<?> contextClass = typeUsage.getRawType();
		GenericsClassContext context = new GenericsClassContext(contextClass);
		addOwnResolutions(typeUsage, context);
		addResolutionsForSuperclass(context);
		addResolutionsForInterfaces(context);
		return context;
	}

	private static void addResolutionsForInterfaces(GenericsClassContext context) {
		Class<?>[] interfaces = context.contextClass().getInterfaces();
		Type[] genericInterfaces = context.contextClass().getGenericInterfaces();
		AnnotatedType[] annotatedInterfaces = context.contextClass().getAnnotatedInterfaces();
		for (int i = 0; i < interfaces.length; i++) {
			Class<?> supertype = interfaces[i];
			Type genericSupertype = genericInterfaces[i];
			AnnotatedType annotatedSupertype = annotatedInterfaces[i];
			addResolutionsForSupertype(supertype, genericSupertype, annotatedSupertype, context);
		}
	}

	private static void addResolutionsForSuperclass(GenericsClassContext context) {
		addResolutionsForSupertype(
			context.contextClass().getSuperclass(),
			context.contextClass().getGenericSuperclass(),
			context.contextClass().getAnnotatedSuperclass(),
			context
		);
	}

	private static void addOwnResolutions(TypeUsage typeUsage, GenericsClassContext context) {
		if (typeUsage.getTypeArguments().isEmpty()) {
			return;
		}

		List<TypeUsage> typeArgumentsList = typeUsage.getTypeArguments();
		Type[] typeArguments = new Type[typeArgumentsList.size()];
		AnnotatedType[] annotatedTypeVariables = new AnnotatedType[typeArgumentsList.size()];
		for (int i = 0; i < typeArgumentsList.size(); i++) {
			typeArguments[i] = typeArgumentsList.get(i).getType();
			annotatedTypeVariables[i] = typeArgumentsList.get(i).getAnnotatedType();
		}
		TypeVariable[] typeVariables = typeUsage.getRawType().getTypeParameters();
		addResolutions(context, typeArguments, typeVariables, annotatedTypeVariables);
	}

	private static void addResolutionsForSupertype(
		Class<?> supertype,
		Type genericSupertype,
		AnnotatedType annotatedSupertype,
		GenericsClassContext context
	) {
		if (!(genericSupertype instanceof ParameterizedType)) {
			return;
		}
		ParameterizedType genericParameterizedType = (ParameterizedType) genericSupertype;
		Type[] typeArguments = genericParameterizedType.getActualTypeArguments();
		TypeVariable[] typeVariables = supertype.getTypeParameters();
		AnnotatedType[] annotatedTypeVariables =
			((AnnotatedParameterizedType) annotatedSupertype).getAnnotatedActualTypeArguments();
		addResolutions(context, typeArguments, typeVariables, annotatedTypeVariables);
	}

	private static void addResolutions(
		GenericsClassContext context,
		Type[] typeArguments,
		TypeVariable[] typeVariables,
		AnnotatedType[] annotatedTypeVariables
	) {
		for (int i = 0; i < typeVariables.length; i++) {
			TypeVariable variable = typeVariables[i];
			Type resolvedType = typeArguments[i];
			AnnotatedType annotatedType = annotatedTypeVariables[i];
			context.addResolution(variable, resolvedType, annotatedType);
		}
	}

}
