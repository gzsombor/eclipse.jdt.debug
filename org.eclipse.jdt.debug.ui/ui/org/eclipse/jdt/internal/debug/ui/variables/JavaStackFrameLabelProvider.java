/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.debug.ui.variables;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.internal.ui.model.elements.DebugElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.internal.debug.core.model.JDIThread;

/**
 * Provides labels for Java stack frames with a specific scheduling rule as
 * not to conflict with implicit evaluations in the variables view.
 *
 * @since 3.4
 */
public class JavaStackFrameLabelProvider extends DebugElementLabelProvider {

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.model.elements.ElementLabelProvider#retrieveLabel(org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate)
	 */
	@Override
	protected void retrieveLabel(ILabelUpdate update) throws CoreException {
		Object element = update.getElement();
		if (element instanceof IJavaStackFrame frame) {
			if (!frame.getThread().isSuspended()) {
				update.cancel();
				return;
			}
		}
		super.retrieveLabel(update);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.model.elements.ElementLabelProvider#getRule(org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate)
	 */
	@Override
	protected ISchedulingRule getRule(ILabelUpdate update) {
		Object element = update.getElement();
		if (element instanceof IJavaStackFrame frame) {
			return ((JDIThread)frame.getThread()).getThreadRule();
		}
		return null;
	}

}
