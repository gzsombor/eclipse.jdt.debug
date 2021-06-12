/*******************************************************************************
 * Copyright (c) 2021, 2025 Zsombor Gegesy and others.
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
package org.eclipse.jdt.internal.debug.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaStackFrame.Category;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.filtertable.FilterManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Provides foreground and background colors for stack frames in a debug view. After usage, it needs to be closed, so it could unregister itself from
 * preference storage.
 *
 */
public final class StackFramePresentationProvider implements IPropertyChangeListener {

	public static final FilterManager PLATFORM_STACK_FRAMES = new FilterManager(IJDIPreferencesConstants.PREF_ACTIVE_PLATFORM_FRAME_FILTER_LIST, IJDIPreferencesConstants.PREF_INACTIVE_PLATFORM_FRAME_FILTER_LIST);
	public static final FilterManager CUSTOM_STACK_FRAMES = new FilterManager(IJDIPreferencesConstants.PREF_ACTIVE_CUSTOM_FRAME_FILTER_LIST, IJDIPreferencesConstants.PREF_INACTIVE_CUSTOM_FRAME_FILTER_LIST);

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
	private final IPreferenceStore store;
	private boolean collapseStackFrames;

	public StackFramePresentationProvider(IPreferenceStore store) {
		this.store = store;
		platform = createActivePlatformFilters(store);
		custom = createActiveCustomFilters(store);
		store.addPropertyChangeListener(this);
		collapseStackFrames = store.getBoolean(IJDIPreferencesConstants.PREF_COLLAPSE_STACK_FRAMES);
	}

	/**
	 * Create a {@link Filters} object to decide if a class is part of the 'platform'. The platform definition is stored in the
	 * {@link IPreferenceStore}. By default, this is the classes provided by the JVM.
	 *
	 */
	private Filters createActivePlatformFilters(IPreferenceStore store) {
		return new Filters(PLATFORM_STACK_FRAMES.getActiveList(store));
	}

	/**
	 * Create a {@link Filters} object to decide if a class is considered part of a custom, very important layer, which needs to be highlighted. This
	 * definition is stored in the {@link IPreferenceStore}. By default, this is an empty list.
	 */
	private Filters createActiveCustomFilters(IPreferenceStore store) {
		return new Filters(CUSTOM_STACK_FRAMES.getActiveList(store));
	}

	public StackFramePresentationProvider() {
		this(JDIDebugUIPlugin.getDefault().getPreferenceStore());
	}

	/**
	 * @return the category specific image for the stack frame, or null, if there is no one defined.
	 */
	public ImageDescriptor getStackFrameImage(IJavaStackFrame frame) {
		var category = getCategory(frame);
		if (category != null) {
			switch (category) {
				case LIBRARY:
				case SYNTHETIC:
				case PLATFORM:
					return JavaPluginImages.DESC_OBJS_JAR;
				default:
					break;
			}
		}
		return null;
	}

	/**
	 * @return the {@link IJavaStackFrame.Category} which matches the rules - and store the category inside the frame, if the category is already
	 *         calculated for the frame this just retrieves that.
	 */
	public IJavaStackFrame.Category getCategory(IJavaStackFrame frame) {
		if (frame.getCategory() != null) {
			return frame.getCategory();
		}
		IJavaStackFrame.Category result = Category.UNKNOWN;
		try {
			result = categorize(frame);
		} catch (DebugException e) {
			JDIDebugUIPlugin.log(e);
		}
		frame.setCategory(result);
		return result;
	}

	private boolean isEnabled(@SuppressWarnings("unused") Category category) {
		// Category will be used in subsequent changes.
		return true;
	}

	/**
	 * Categorize the given {@link IJavaStackFrame} into a {@link Category} based on the rules and filters, and where those classes are in the
	 * project. For example if in a source folder, in a library or in a test source folder, etc.
	 */
	public IJavaStackFrame.Category categorize(IJavaStackFrame frame) throws DebugException {
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

	/**
	 * Unsubscribes to not receive notifications from the {@link IPreferenceStore}.
	 */
	public void close() {
		store.removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String prop = event.getProperty();
		if (IJDIPreferencesConstants.PREF_ACTIVE_PLATFORM_FRAME_FILTER_LIST.equals(prop)) {
			platform = createActivePlatformFilters(store);
		} else if (IJDIPreferencesConstants.PREF_ACTIVE_CUSTOM_FRAME_FILTER_LIST.equals(prop)) {
			custom = createActiveCustomFilters(store);
		} else if (IJDIPreferencesConstants.PREF_COLLAPSE_STACK_FRAMES.equals(prop)) {
			collapseStackFrames = (Boolean) event.getNewValue();
		}
	}

	/**
	 * @return if stack frames should be collapsed.
	 */
	public boolean isCollapseStackFrames() {
		return collapseStackFrames;
	}

}
