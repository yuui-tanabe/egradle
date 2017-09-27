package de.jcup.egradle.eclipse.ui;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import de.jcup.egradle.core.model.Item;
import de.jcup.egradle.eclipse.util.EclipseUtil;

public abstract class AbstractGroovyBasedContentOutlinePage extends ContentOutlinePage  implements IDoubleClickListener{

	protected ITreeContentProvider contentProvider;
	private IExtendedEditor editor;
	private boolean ignoreNextSelectionEvents;
	private Object input;
	private IStyledLabelProvider labelProvider;
	private boolean linkingWithEditorEnabled;
	private ToggleLinkingAction toggleLinkingAction;

	public AbstractGroovyBasedContentOutlinePage() {
		super();
	}

	public AbstractGroovyBasedContentOutlinePage(IAdaptable adaptable) {
		if (adaptable == null) {
			contentProvider = new FallbackOutlineContentProvider();
			return;
		}
		this.editor = adaptable.getAdapter(IExtendedEditor.class);
		this.contentProvider = adaptable.getAdapter(ITreeContentProvider.class);
		if (contentProvider == null) {
			contentProvider = new FallbackOutlineContentProvider();
		}
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
	
		labelProvider = createStyledLabelProvider();
	
		TreeViewer viewer = getTreeViewer();
		viewer.setContentProvider(contentProvider);
		viewer.addDoubleClickListener(this);
		viewer.setLabelProvider(new DelegatingStyledCellLabelProvider(labelProvider));
		viewer.addSelectionChangedListener(this);
	
		/* it can happen that input is already updated before control created */
		if (input != null) {
			viewer.setInput(input);
		}
		BlockSelectionAction blockSelectionAction = new BlockSelectionAction();
		CollapseAllAction collapseAllAction = new CollapseAllAction();
		ExpandAllAction expandAllAction = new ExpandAllAction();
		toggleLinkingAction = new ToggleLinkingAction();
		toggleLinkingAction.setActionDefinitionId(IWorkbenchCommandConstants.NAVIGATE_TOGGLE_LINK_WITH_EDITOR);
		IActionBars actionBars = getSite().getActionBars();
	
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		toolBarManager.add(expandAllAction);
		toolBarManager.add(collapseAllAction);
		toolBarManager.add(toggleLinkingAction);
		toolBarManager.add(new Separator("selectionGroup1"));//$NON-NLS-1$
		toolBarManager.add(blockSelectionAction);
	
		IMenuManager viewMenuManager = actionBars.getMenuManager();
		viewMenuManager.add(new Separator("EndFilterGroup")); //$NON-NLS-1$
	
		handleDebugOptions(viewMenuManager);
		viewMenuManager.add(new Separator("treeGroup")); //$NON-NLS-1$
		viewMenuManager.add(expandAllAction);
		viewMenuManager.add(collapseAllAction);
		viewMenuManager.add(toggleLinkingAction);
		viewMenuManager.add(new Separator("selectionGroup2"));//$NON-NLS-1$
		viewMenuManager.add(blockSelectionAction);
	}

	@Override
	public void doubleClick(DoubleClickEvent event) {
		if (editor == null) {
			return;
		}
		if (linkingWithEditorEnabled) {
			editor.setFocus();
			// selection itself is already handled by single click
			return;
		}
		ISelection selection = event.getSelection();
		editor.openSelectedTreeItemInEditor(selection, true, false);
	}

	public void ignoreNextSelectionEvents(boolean ignore) {
	
	}

	public void inputChanged(Object input) {
		this.input = input;
		if (contentProvider instanceof AbstractGroovyBasedEditorOutlineContentProvider) {
			((AbstractGroovyBasedEditorOutlineContentProvider) contentProvider).clearModelCache();
		}
		TreeViewer treeViewer = getTreeViewer();
		if (treeViewer == null) {
			return;
		}
		treeViewer.setInput(input);
	}

	public void onEditorCaretMoved(int caretOffset) {
		if (!linkingWithEditorEnabled) {
			return;
		}
		ignoreNextSelectionEvents = true;
		if (contentProvider instanceof AbstractGroovyBasedEditorOutlineContentProvider) {
			AbstractGroovyBasedEditorOutlineContentProvider gcp = (AbstractGroovyBasedEditorOutlineContentProvider) contentProvider;
			Item item = gcp.tryToFindByOffset(caretOffset);
			if (item != null) {
				StructuredSelection selection = new StructuredSelection(item);
				getTreeViewer().setSelection(selection, true);
			}
		}
		ignoreNextSelectionEvents = false;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		super.selectionChanged(event);
		if (!linkingWithEditorEnabled) {
			return;
		}
	
		if (ignoreNextSelectionEvents) {
			return;
		}
		ISelection selection = event.getSelection();
		editor.openSelectedTreeItemInEditor(selection, false, false);
	}

	protected abstract IStyledLabelProvider createStyledLabelProvider();

	protected abstract ImageDescriptor getImageDescriptionForLinked();
	
	protected abstract ImageDescriptor getImageDescriptionNotLinked();
	protected abstract String getPluginId();

	protected abstract void handleDebugOptions(IMenuManager viewMenuManager);
	
	class BlockSelectionAction extends Action {

		private BlockSelectionAction() {
			setImageDescriptor(EclipseUtil.createImageDescriptor("/icons/outline/mark_occurrences.png", getPluginId()));
			setText("Mark selected item full");
		}

		@Override
		public void run() {
			if (editor == null) {
				return;
			}
			TreeViewer treeViewer = getTreeViewer();
			if (treeViewer == null) {
				return;
			}
			editor.openSelectedTreeItemInEditor(treeViewer.getSelection(), true, true);
		}

	}

	class CollapseAllAction extends Action {

		private CollapseAllAction() {
			setImageDescriptor(EclipseUtil.createImageDescriptor("/icons/outline/collapseall.png", getPluginId()));
			setText("Collapse all");
		}

		@Override
		public void run() {
			getTreeViewer().collapseAll();
		}
	}

	class ExpandAllAction extends Action {

		private ExpandAllAction() {
			setImageDescriptor(EclipseUtil.createImageDescriptor("/icons/outline/expandall.png", getPluginId()));
			setText("Expand all");
		}

		@Override
		public void run() {
			getTreeViewer().expandAll();
		}
	}

	class ToggleLinkingAction extends Action {

		private ToggleLinkingAction() {
			if (editor != null) {
				linkingWithEditorEnabled = editor.getPreferences().isLinkOutlineWithEditorEnabled();
			}
			setDescription("link with editor");
			initImage();
			initText();
		}

		@Override
		public void run() {
			linkingWithEditorEnabled = !linkingWithEditorEnabled;

			initText();
			initImage();
		}

		private void initImage() {
			setImageDescriptor(
					linkingWithEditorEnabled ? getImageDescriptionForLinked() : getImageDescriptionNotLinked());
		}

		private void initText() {
			setText(linkingWithEditorEnabled ? "Click to unlink from editor" : "Click to link with editor");
		}

	}


}