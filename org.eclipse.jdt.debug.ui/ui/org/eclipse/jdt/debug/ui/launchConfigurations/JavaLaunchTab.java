/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.jdt.debug.ui.launchConfigurations;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Common function for Java launch configuration tabs.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.2
 */
public abstract class JavaLaunchTab extends AbstractLaunchConfigurationTab {

	/**
	 * Config being modified
	 */
	private ILaunchConfiguration fLaunchConfig;

	/**
	 * Returns the current Java element context in the active workbench page
	 * or <code>null</code> if none.
	 *
	 * @return current Java element in the active page or <code>null</code>
	 */
	protected IJavaElement getContext() {
		IWorkbenchPage page = JDIDebugUIPlugin.getActivePage();
		if (page != null) {
			ISelection selection = page.getSelection();
			if (selection instanceof IStructuredSelection ss) {
				if (!ss.isEmpty()) {
					Object obj = ss.getFirstElement();
					if (obj instanceof IJavaElement) {
						return (IJavaElement)obj;
					}
					if (obj instanceof IResource) {
						IJavaElement je = JavaCore.create((IResource)obj);
						if (je == null) {
							IProject pro = ((IResource)obj).getProject();
							je = JavaCore.create(pro);
						}
						if (je != null) {
							return je;
						}
					}
				}
			}
			IEditorPart part = page.getActiveEditor();
			if (part != null) {
				IEditorInput input = part.getEditorInput();
				return input.getAdapter(IJavaElement.class);
			}
		}
		return null;
	}

	/**
	 * Returns the launch configuration this tab was initialized from.
	 *
	 * @return launch configuration this tab was initialized from
	 */
	protected ILaunchConfiguration getCurrentLaunchConfiguration() {
		return fLaunchConfig;
	}

	/**
	 * Sets the launch configuration this tab was initialized from
	 *
	 * @param config launch configuration this tab was initialized from
	 */
	private void setCurrentLaunchConfiguration(ILaunchConfiguration config) {
		fLaunchConfig = config;
	}

	/**
	 * Sets the Java project attribute on the given working copy to the Java project
	 * associated with the given Java element.
	 *
	 * @param javaElement Java model element this tab is associated with
	 * @param config configuration on which to set the Java project attribute
	 */
	protected void initializeJavaProject(IJavaElement javaElement, ILaunchConfigurationWorkingCopy config) {
		IJavaProject javaProject = javaElement.getJavaProject();
		String name = null;
		if (javaProject != null && javaProject.exists()) {
			name = javaProject.getElementName();
		}
		config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 *
	 * Subclasses may override this method and should call super.initializeFrom(...).
	 */
	@Override
	public void initializeFrom(ILaunchConfiguration config) {
		setCurrentLaunchConfiguration(config);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#initializeAttributes()
	 * @since 3.9
	 */
	@Override
	protected void initializeAttributes() {
		getAttributesLabelsForPrototype().put(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, LauncherMessages.JavaLauncherTab_AttributeLabel_ProjectName);
	}
}

