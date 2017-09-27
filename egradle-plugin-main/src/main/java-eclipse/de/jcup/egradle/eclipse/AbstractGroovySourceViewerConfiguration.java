package de.jcup.egradle.eclipse;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.RGB;

import de.jcup.egradle.eclipse.document.GroovyDefaultTextScanner;
import de.jcup.egradle.eclipse.document.GroovyEditorAnnotationHoover;
import de.jcup.egradle.eclipse.preferences.IEditorPreferences;
import de.jcup.egradle.eclipse.ui.ExtendedSourceViewerConfiguration;
import de.jcup.egradle.eclipse.util.ColorManager;
import de.jcup.egradle.eclipse.util.PreferenceIdentifiable;

public abstract class AbstractGroovySourceViewerConfiguration extends SourceViewerConfiguration implements ExtendedSourceViewerConfiguration{

	protected GroovyDefaultTextScanner gradleScanner;
	protected ColorManager colorManager;
	protected TextAttribute defaultTextAttribute;
	protected IAnnotationHover annotationHoover;
	protected IAdaptable adaptable;
	protected ContentAssistant contentAssistant;
	
	public AbstractGroovySourceViewerConfiguration(IAdaptable adaptable, PreferenceIdentifiable colorNormalText) {
		this.adaptable = adaptable;
		this.colorManager = adaptable.getAdapter(ColorManager.class);
		Assert.isNotNull(colorManager, " adaptable must support color manager");
		
		this.annotationHoover = new GroovyEditorAnnotationHoover();
		
		this.defaultTextAttribute = new TextAttribute(
				colorManager.getColor(getPreferences().getColor(colorNormalText)));

	}

	protected abstract IEditorPreferences getPreferences();
	
	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return annotationHoover;
	}


	protected void addDefaultPresentation(PresentationReconciler reconciler) {
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getGroovyDefaultTextScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	}

	protected IToken createColorToken(RGB rgb) {
		Token token = new Token(new TextAttribute(colorManager.getColor(rgb)));
		return token;
	}

	protected void addPresentation(PresentationReconciler reconciler, String id, RGB rgb, int style) {
		TextAttribute textAttribute = new TextAttribute(colorManager.getColor(rgb),
				defaultTextAttribute.getBackground(), style);
		PresentationSupport presentation = new PresentationSupport(textAttribute);
		reconciler.setDamager(presentation, id);
		reconciler.setRepairer(presentation, id);
	}

	protected GroovyDefaultTextScanner getGroovyDefaultTextScanner() {
		if (gradleScanner == null) {
			gradleScanner = new GroovyDefaultTextScanner(colorManager);
			updateTextScannerDefaultColorToken();
		}
		return gradleScanner;
	}

	

}