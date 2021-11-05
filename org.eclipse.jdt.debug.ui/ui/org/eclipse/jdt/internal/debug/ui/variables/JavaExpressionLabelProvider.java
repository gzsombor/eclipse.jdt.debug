/*******************************************************************************
 * Copyright (c) 2022 Zsombor Gegesy and others.
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
package org.eclipse.jdt.internal.debug.ui.variables;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.model.elements.ExpressionLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jdt.internal.debug.ui.JDIModelPresentation;
import org.eclipse.jdt.internal.debug.ui.display.JavaInspectExpression;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreePath;

/**
 * Custom {@link ILabelProvider} implementation for {@link IExpression}, used to customize the label column of the {@link IWatchExpression} and
 * {@link JavaInspectExpression}.
 *
 */
class JavaExpressionLabelProvider extends ExpressionLabelProvider {

	private final JDIModelPresentation fLabelProvider;

	JavaExpressionLabelProvider(JDIModelPresentation fLabelProvider) {
		this.fLabelProvider = fLabelProvider;
	}

	@Override
	protected String getLabel(TreePath elementPath, IPresentationContext context, String columnId) throws CoreException {
		if (JavaVariableColumnPresentation.COLUMN_LABEL.equals(columnId)) {
			var expression = elementPath.getLastSegment();
			if (expression instanceof IExpression) {
				return fLabelProvider.getLabelColumnText(((IExpression) expression).getValue());
			}
			return null;
		}
		if (JavaVariableColumnPresentation.COLUMN_INSTANCE_COUNT.equals(columnId)) {
			var expression = elementPath.getLastSegment();
			if (expression instanceof IExpression) {
				return fLabelProvider.getInstanceCountColumnText(null, ((IExpression) expression).getValue());
			}
			return null;
		}
		if (JavaVariableColumnPresentation.COLUMN_INSTANCE_ID.equals(columnId)) {
			var expression = elementPath.getLastSegment();
			if (expression instanceof IExpression) {
				return fLabelProvider.getUniqueIdColumnText(((IExpression) expression).getValue());
			}
			return null;
		}
		return super.getLabel(elementPath, context, columnId);
	}

}
