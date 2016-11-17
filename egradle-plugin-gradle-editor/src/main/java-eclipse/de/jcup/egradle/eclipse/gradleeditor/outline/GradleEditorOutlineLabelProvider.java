package de.jcup.egradle.eclipse.gradleeditor.outline;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;

import de.jcup.egradle.core.model.Item;
import de.jcup.egradle.core.model.ItemType;
import de.jcup.egradle.core.model.Modifier;
import de.jcup.egradle.core.token.Token;
import de.jcup.egradle.eclipse.api.ColorManager;
import de.jcup.egradle.eclipse.api.EGradleUtil;
import de.jcup.egradle.eclipse.gradleeditor.Activator;
import de.jcup.egradle.eclipse.gradleeditor.ColorConstants;

public class GradleEditorOutlineLabelProvider extends BaseLabelProvider
		implements IStyledLabelProvider, IColorProvider {
	
	private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("egradle.labelprovider.debug"));

	private static final String ICON_REPOSITORY = "imp_obj.png";
	//private static final String ICON_DEFAULT_CO_PNG = "default_co.png";
	private static final String ICON_PUBLIC_CO_PNG = "public_co.png";
	private static final String ICON_PROTECTED_CO_PNG = "protected_co.png";
	private static final String ICON_PRIVATE_CO_PNG = "private_co.png";
	private static final String ICON_TASK_PNG = "typevariable_obj.png";
	private static final String ICON_APPLY_FROM_PNG = "apply_from.png";
	private static final String ICON_APPLY_PLUGIN_PNG = "plugins.png";
	private static final String ICON_REPOSITORIES = "memory_view.png";
	private static final String ICON_DEPENDENCIES = "module_view.png";
	private static final String ICON_ALL_PROJECTS = "prj_mode.png";
	private static final String ICON_SUB_PROJECTS = "prj_mode.png";
	private static final String ICON_DEPENDENCY = "friend_co.png";
	private static final String ICON_TEST = "test.png";
	private static final String ICON_CLEAN = "removea_exc.png";
	private static final String ICON_CLOSURE = "all_sc_obj.png";
	private static final String ICON_BUILDSCRIPT = "cheatsheet_item_obj.png";
	private static final String ICON_CONFIGURATIONS = "th_single.png";
	

	@Override
	public Image getImage(Object element) {
		if (element instanceof Item) {
			Item item = (Item) element;
			ItemType type = item.getItemType();
			switch (type) {
			case VARIABLE:
				Modifier modifier = item.getModifier();
				String path = null;
				switch (modifier) {
				case PRIVATE:
					path = ICON_PRIVATE_CO_PNG;
					break;
				case PROTECTED:
					path = ICON_PROTECTED_CO_PNG;
					break;
				case PUBLIC:
					path = ICON_PUBLIC_CO_PNG;
					break;
				case DEFAULT:
					path = ICON_PUBLIC_CO_PNG;// default in groovy IS PUBLIC! ICON_DEFAULT_CO_PNG;
					break;
				default:
					return null;
				}
				return getOutlineImage(path);
			case TASK_CLOSURE:
				return getOutlineImage(ICON_TASK_PNG);
			case TASK_SETUP:
				return getOutlineImage(ICON_TASK_PNG);
			case APPLY_FROM:
				return getOutlineImage(ICON_APPLY_FROM_PNG);
			case APPLY_PLUGIN:
				return getOutlineImage(ICON_APPLY_PLUGIN_PNG);
			case REPOSITORIES:
				return getOutlineImage(ICON_REPOSITORIES);
			case ALL_PROJECTS:
				return getOutlineImage(ICON_ALL_PROJECTS);
			case SUB_PROJECTS:
				return getOutlineImage(ICON_SUB_PROJECTS);
			case DEPENDENCIES:
				return getOutlineImage(ICON_DEPENDENCIES);
			case DEPENDENCY:
				return getOutlineImage(ICON_DEPENDENCY);
			case TEST:
				return getOutlineImage(ICON_TEST);
			case CLEAN:
				return getOutlineImage(ICON_CLEAN);
			case CLOSURE:
				return getOutlineImage(ICON_CLOSURE);
			case BUILDSCRIPT:
				return getOutlineImage(ICON_BUILDSCRIPT);
			case CONFIGURATIONS:
				return getOutlineImage(ICON_CONFIGURATIONS);
			case REPOSITORY:
				return getOutlineImage(ICON_REPOSITORY);
			default:
				return null;
			}
		}
		return null;
	}

	private Image getOutlineImage(String name) {
		return EGradleUtil.getImage("/icons/outline/" + name, Activator.PLUGIN_ID);
	}

	private Styler outlineItemTypeStyler = new Styler() {

		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = getColorManager().getColor(ColorConstants.OUTLINE_ITEM__TYPE);
		}
	};

	private Styler outlineItemInfoStyler = new Styler() {

		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = getColorManager().getColor(ColorConstants.BRIGHT_BLUE);
		}
	};

	private Styler outlineItemTargetStyler = new Styler() {

		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = getColorManager().getColor(ColorConstants.DARK_GREEN);
		}
	};
	
	private Styler outlineItemConfigurationStyler = new Styler() {

		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.foreground = getColorManager().getColor(ColorConstants.BROWN);
		}
	};

	@Override
	public StyledString getStyledText(Object element) {
		StyledString styled = new StyledString();
		if (element == null) {
			styled.append("null");
		}
		if (element instanceof Item) {
			Item item = (Item) element;
			
			String configuration = item.getConfiguration();
			if (configuration!=null){
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				sb.append(configuration);
				sb.append("] ");
				styled.append(new StyledString(sb.toString(),outlineItemConfigurationStyler));
			}
			
			String name = item.getName();
			if (name != null) {
				styled.append(name);
			}

			String type = item.getType();
			if (type != null) {
				StringBuilder sb = new StringBuilder();
				sb.append(" :");
				sb.append(type);
				StyledString typeString = new StyledString(sb.toString(), outlineItemTypeStyler);
				styled.append(typeString);
			}
			String target = item.getTarget();
			if (target != null) {
				StyledString targetString = new StyledString(":" + target, outlineItemTargetStyler);
				styled.append(targetString);
			}

			String info = item.getInfo();
			if (info != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("  [");
				sb.append(info);
				sb.append(']');

				StyledString infoString = new StyledString(sb.toString(), outlineItemInfoStyler);
				styled.append(infoString);
			}
			if (DEBUG){
				StringBuilder sb = new StringBuilder();
				sb.append(" --[");
				sb.append(item.getItemType());
				sb.append(']');
				StyledString debugString = new StyledString(sb.toString(), outlineItemConfigurationStyler); 
				styled.append(debugString);
			}
		} else {
			if (element instanceof Token) {
				Token gelement = (Token) element;
				return styled.append(gelement.getValue());
			}
			return styled.append(element.toString());
		}
		
		
		
		return styled;
	}

	private ColorManager getColorManager() {
		return Activator.getDefault().getColorManager();
	}

	@Override
	public Color getForeground(Object element) {
		return null;// getColorManager().getColor(ColorConstants.BLACK);
	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}

}
