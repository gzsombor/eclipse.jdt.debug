/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.sourcelookup;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.jdt.launching.sourcelookup.containers.ClasspathVariableSourceContainer;
import org.eclipse.jdt.ui.wizards.BuildPathDialogAccess;
import org.eclipse.swt.widgets.Shell;

/**
 * Used to choose a classpath variable.
 * 
 * @since 3.0
 */
public class ClasspathVariableSourceContainerBrowser extends AbstractSourceContainerBrowser {
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser#canEditSourceContainers(org.eclipse.debug.core.sourcelookup.ISourceLookupDirector, org.eclipse.debug.core.sourcelookup.ISourceContainer[])
	 */
	public boolean canEditSourceContainers(ISourceLookupDirector director, ISourceContainer[] containers) {
		return containers.length == 1;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.sourcelookup.ISourceContainerBrowser#editSourceContainers(org.eclipse.swt.widgets.Shell, org.eclipse.debug.core.sourcelookup.ISourceLookupDirector, org.eclipse.debug.core.sourcelookup.ISourceContainer[])
	 */
	public ISourceContainer[] editSourceContainers(Shell shell, ISourceLookupDirector director, ISourceContainer[] containers) {
		ClasspathVariableSourceContainer container = (ClasspathVariableSourceContainer) containers[0];
		IPath[] paths = BuildPathDialogAccess.chooseVariableEntries(shell, new IPath[]{container.getPath()});
		if (paths != null) {			
			containers = new ISourceContainer[paths.length];
			for (int i = 0; i < containers.length; i++) {
				containers[i] = new ClasspathVariableSourceContainer(paths[i]);
			}
			return containers;
		}
		return new ISourceContainer[0];
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.sourcelookup.ISourceContainerBrowser#createSourceContainers(org.eclipse.swt.widgets.Shell, org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		IPath[] paths = BuildPathDialogAccess.chooseVariableEntries(shell, new IPath[0]);
		if (paths != null) {			
			ISourceContainer[] containers = new ISourceContainer[paths.length];
			for (int i = 0; i < containers.length; i++) {
				containers[i] = new ClasspathVariableSourceContainer(paths[i]);
			}
			return containers;
		}
		return new ISourceContainer[0];
	}
}
