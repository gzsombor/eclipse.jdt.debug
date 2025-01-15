/*******************************************************************************
 * Copyright (c) 2025 Zsombor Gegesy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Zsombor Gegesy - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaStackFrame.Category;

/**
 * Service to help categorize the stack frames into {@link IJavaStackFrame.Category}, based on the internally stored preferences.
 */
public class StackFrameCategorizer implements IPreferenceChangeListener {
	private final static Map<String, Category> PREFERENCE_MAPPING = Map.of(JDIDebugPlugin.PREF_COLORIZE_CUSTOM_METHODS, Category.CUSTOM_FILTERED, //
			JDIDebugPlugin.PREF_COLORIZE_SYNTHETIC_METHODS, Category.SYNTHETIC, //
			JDIDebugPlugin.PREF_COLORIZE_PLATFORM_METHODS, Category.PLATFORM, //
			JDIDebugPlugin.PREF_COLORIZE_TEST_METHODS, Category.TEST, //
			JDIDebugPlugin.PREF_COLORIZE_PRODUCTION_METHODS, Category.PRODUCTION, //
			JDIDebugPlugin.PREF_COLORIZE_LIBRARY_METHODS, Category.LIBRARY);

	private final static EnumMap<Category, String> REVERSE_MAPPING = new EnumMap<>(PREFERENCE_MAPPING.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey)));

	/**
	 * Class to decide if a particular class name is part of a list of classes and list of packages.
	 */
	record Filters(String[] filters) {
		boolean match(String fqcName) {
			for (String filter : filters) {
				if (filter.endsWith("*")) { //$NON-NLS-1$
					if (fqcName.startsWith(filter.substring(0, filter.length() - 1))) {
						return true;
					}
				} else {
					if (filter.equals(fqcName)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private Filters platform;
	private Filters custom;
	private final EnumMap<Category, Boolean> enabledCategories;
	private final IPreferencesService preferenceService;

	public StackFrameCategorizer(IPreferencesService preferenceService) {
		this.preferenceService = preferenceService;
		enabledCategories = new EnumMap<>(Category.class);
		for (var kv : PREFERENCE_MAPPING.entrySet()) {
			boolean enabled = preferenceService.getBoolean(JDIDebugPlugin.getUniqueIdentifier(), kv.getKey(), true, null);
			enabledCategories.put(kv.getValue(), enabled);
		}

		platform = createActivePlatformFilters();
		custom = createActiveCustomFilters();
	}

	/**
	 * Create a {@link Filters} object to decide if a class is part of the 'platform'. The platform definition is stored in the
	 * {@link IEclipsePreferences}. By default, this is the classes provided by the JVM.
	 *
	 */
	private Filters createActivePlatformFilters() {
		return new Filters(getActivePlatformStackFilter());
	}

	/**
	 * @return the list of <b>active</b> filter expressions that defines {@link IJavaStackFrame.Category#PLATFORM}.
	 */
	public String[] getActivePlatformStackFilter() {
		return getStringList(JDIDebugPlugin.PREF_ACTIVE_PLATFORM_FRAME_FILTER_LIST);
	}

	/**
	 * @return the list of <b>inactive</b> filter expressions that defines {@link IJavaStackFrame.Category#PLATFORM}.
	 */
	public String[] getInactivePlatformStackFilter() {
		return getStringList(JDIDebugPlugin.PREF_INACTIVE_PLATFORM_FRAME_FILTER_LIST);
	}

	/**
	 * @return the list of <b>active</b> filter expressions that defines {@link IJavaStackFrame.Category#CUSTOM_FILTERED}.
	 */
	public String[] getActiveCustomStackFilter() {
		return getStringList(JDIDebugPlugin.PREF_ACTIVE_CUSTOM_FRAME_FILTER_LIST);
	}

	/**
	 * @return the list of <b>inactive</b> filter expressions that defines {@link IJavaStackFrame.Category#CUSTOM_FILTERED}.
	 */
	public String[] getInactiveCustomStackFilter() {
		return getStringList(JDIDebugPlugin.PREF_INACTIVE_CUSTOM_FRAME_FILTER_LIST);
	}

	private String[] getStringList(String key) {
		return parseList(preferenceService.getString(JDIDebugPlugin.getUniqueIdentifier(), key, "", null)); //$NON-NLS-1$
	}

	/**
	 * Create a {@link Filters} object to decide if a class is considered part of a custom, very important layer, which needs to be highlighted. This
	 * definition is stored in the {@link IEclipsePreferences}. By default, this is an empty list.
	 */
	private Filters createActiveCustomFilters() {
		return new Filters(getActiveCustomStackFilter());
	}

	/**
	 * Categorize the given {@link IJavaStackFrame} into a {@link Category} based on the rules and filters, and where those classes are in the
	 * project. For example if in a source folder, in a library or in a test source folder, etc.
	 */
	public IJavaStackFrame.Category categorize(IJavaStackFrame frame) {
		try {
			var refTypeName = frame.getReferenceType().getName();
			if (isEnabled(Category.CUSTOM_FILTERED) && custom.match(refTypeName)) {
				return Category.CUSTOM_FILTERED;
			}
			if (isEnabled(Category.SYNTHETIC) && frame.isSynthetic()) {
				return Category.SYNTHETIC;
			}
			if (isEnabled(Category.PLATFORM) && platform.match(refTypeName)) {
				return Category.PLATFORM;
			}
		} catch (DebugException de) {
			JDIDebugPlugin.log(de);
		}

		return categorizeSourceElement(frame);
	}

	/**
	 * Do the categorization with the help of a {@link org.eclipse.debug.core.model.ISourceLocator} coming from the associated
	 * {@link org.eclipse.debug.core.ILaunch}. This is how we can find, if the class file is in a jar or comes from a source folder.
	 */
	private Category categorizeSourceElement(IJavaStackFrame frame) {
		var sourceLocator = frame.getLaunch().getSourceLocator();
		if (sourceLocator == null) {
			return Category.UNKNOWN;
		}
		var source = sourceLocator.getSourceElement(frame);
		if (source == null) {
			return Category.UNKNOWN;
		}
		if (source instanceof IFile file) {
			if (isEnabled(Category.TEST)) {
				var jproj = JavaCore.create(file.getProject());
				var cp = jproj.findContainingClasspathEntry(file);
				if (cp != null && cp.isTest()) {
					return Category.TEST;
				}
			}
			if (isEnabled(Category.PRODUCTION)) {
				return Category.PRODUCTION;
			}
		} else if (source instanceof IClassFile && isEnabled(Category.LIBRARY)) {
			return Category.LIBRARY;
		}
		return Category.UNKNOWN;
	}

	public boolean isEnabled(Category category) {
		return Boolean.TRUE.equals(enabledCategories.get(category));
	}

	public void setEnabled(Category category, boolean flag) {
		enabledCategories.put(category, flag);
		JDIDebugPlugin.getInstancePreferences().putBoolean(REVERSE_MAPPING.get(category), flag);
	}

	/**
	 * Parses the comma separated string into an array of strings
	 *
	 * @param listString
	 *            the comma separated string
	 * @return list
	 */
	private String[] parseList(String listString) {
		List<String> list = new ArrayList<>(10);
		StringTokenizer tokenizer = new StringTokenizer(listString, ",");//$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			list.add(token);
		}
		return list.toArray(new String[list.size()]);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		String prop = event.getKey();
		if (JDIDebugPlugin.PREF_ACTIVE_PLATFORM_FRAME_FILTER_LIST.equals(prop)) {
			platform = createActivePlatformFilters();
		} else if (JDIDebugPlugin.PREF_ACTIVE_CUSTOM_FRAME_FILTER_LIST.equals(prop)) {
			custom = createActiveCustomFilters();
		} else {
			var category = PREFERENCE_MAPPING.get(prop);
			if (category != null) {
				enabledCategories.put(category, (Boolean) event.getNewValue());
			}
		}
	}

}
