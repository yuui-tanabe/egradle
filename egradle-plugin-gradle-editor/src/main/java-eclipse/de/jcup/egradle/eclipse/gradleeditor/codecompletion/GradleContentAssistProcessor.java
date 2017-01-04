package de.jcup.egradle.eclipse.gradleeditor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContextInformation;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import de.jcup.egradle.core.codecompletion.Proposal;
import de.jcup.egradle.core.codecompletion.ProposalFactory;
import de.jcup.egradle.core.codecompletion.ProposalFactoryContentProvider;
import de.jcup.egradle.core.codecompletion.ItemProposalImpl;
import de.jcup.egradle.core.codecompletion.RelevantCodeCutter;
import de.jcup.egradle.core.codecompletion.VariableNameProposalFactory;
import de.jcup.egradle.core.model.Item;
import de.jcup.egradle.core.model.Itemable;
import de.jcup.egradle.core.model.Model;
import de.jcup.egradle.eclipse.api.EGradleUtil;
import de.jcup.egradle.eclipse.gradleeditor.Activator;
import de.jcup.egradle.eclipse.gradleeditor.outline.GradleEditorOutlineLabelProvider;

/* FIXME ATR, 31.12.2016 - make a generic approach, independent from eclipse, so easy testable*/
public class GradleContentAssistProcessor implements IContentAssistProcessor {
	private String errorMessage;

	private IAdaptable adaptable;

	private List<ProposalFactory> proposalFactories = new ArrayList<>();
	private static GradleEditorOutlineLabelProvider labelProvider =new GradleEditorOutlineLabelProvider();
	
	private RelevantCodeCutter codeCutter;
	
	public GradleContentAssistProcessor(IAdaptable adaptable, RelevantCodeCutter codeCutter) {
		if (adaptable==null){
			throw new IllegalArgumentException("adaptable may not be null!");
		}
		if (codeCutter==null){
			throw new IllegalArgumentException("codeCutter may not be null!");
		}
		this.adaptable=adaptable;
		this.codeCutter=codeCutter;
		
		proposalFactories.add(new VariableNameProposalFactory());
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
		
		ProposalFactoryContentProvider contentProvider=null;
		try {
			int line = document.getLineOfOffset(offset);
			int offsetOfFirstCharacterInLine = document.getLineOffset(line);

			
			contentProvider = new ProposalFactoryContentProvider() {
				
				@Override
				public Model getModel() {
					return adaptable.getAdapter(Model.class);
				}

				@Override
				public String getEditorSourceEnteredAt(int cursorOffset) {
					String code = document.get();
					String relevant = codeCutter.getRelevantCode(code, cursorOffset);
					return relevant;
						
				}

				@Override
				public int getLineAt(int offset) {
					return line;
				}

				@Override
				public int getOffsetOfFirstCharacterInLine(int line) {
					return offsetOfFirstCharacterInLine;
				}
			};
		} catch (BadLocationException e) {
			return new ICompletionProposal[0];
		}
		
		List<ICompletionProposal> list = new ArrayList<>();
		for (ProposalFactory proposalFactory: proposalFactories){
			appendProposals(offset, proposalFactory, list, contentProvider);
		}
		
		return list.toArray(new ICompletionProposal[list.size()]);
	}

	private void appendProposals(int offset, ProposalFactory proposalFactory, List<ICompletionProposal> list, ProposalFactoryContentProvider contentProvider) {
		List<Proposal> result = proposalFactory.createProposals(offset, contentProvider);
		for (Proposal p: result){
			Image image = null;
			if (p instanceof Itemable){
				Itemable a = (Itemable) p;
				Item item = a.getItem();
				if (item!=null){
					image = labelProvider.getImage(item);
				}
			}
			if (image==null){
				image =EGradleUtil.getImage("/icons/gradle-og.png", Activator.PLUGIN_ID); 
			}
			IContextInformation contextInformation =null;
			String additionalProposalInfo = "<html><b>Type:</b>"+p.getType()+"</html>";
			int length = p.getCode().length();
			GradleCompletionProposal proposal = new GradleCompletionProposal(p.getCode(), offset, length, offset+length,image,p.getName(),contextInformation,additionalProposalInfo);
			list.add(proposal);
		}
		
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		ContextInformation info = new ContextInformation("contextDisplayString:"+offset, "informationDisplayString");
		return new IContextInformation[]{info};
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
