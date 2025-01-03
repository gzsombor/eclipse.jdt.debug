package org.eclipse.jdt.debug.tests.ui;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;

public class PreferenceServiceMock implements InvocationHandler {

	private Map<String, Object> values;

	public PreferenceServiceMock(Map<String, Object> values) {
		this.values = values;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		System.out.println("called " + method.getName());
		if ("getBoolean".equals(method.getName())) {
			String key = (String) args[1];
			boolean defaultValue = (boolean) args[2];
			return values.getOrDefault(key, defaultValue);
		}
		if ("getString".equals(method.getName())) {
			String key = (String) args[1];
			String defaultValue = (String) args[2];
			return values.getOrDefault(key, defaultValue);
		}
		if ("put".equals(method.getName())) {
			String key = (String) args[0];
			Object value = args[1];
			values.put(key, value);
			return null;
		}
		return null;
	}

	public static IPreferencesService createPreferencesService(Map<String, Object> values) {
		return (IPreferencesService) Proxy.newProxyInstance(IPreferencesService.class.getClassLoader(), new Class[] {
				IPreferencesService.class }, new PreferenceServiceMock(values));
	}

	public static IEclipsePreferences createEclipsePreferences(Map<String, Object> values) {
		return (IEclipsePreferences) Proxy.newProxyInstance(IEclipsePreferences.class.getClassLoader(), new Class[] {
				IEclipsePreferences.class }, new PreferenceServiceMock(values));
	}

}
