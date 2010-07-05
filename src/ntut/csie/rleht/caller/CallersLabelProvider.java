package ntut.csie.rleht.caller;

import java.util.List;

import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.rleht.views.RLMethodModel;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class CallersLabelProvider extends LabelProvider implements ITableLabelProvider {
	private static Logger logger = LoggerFactory.getLogger(CallersLabelProvider.class);

	private JavaElementLabelProvider javaElementLabelProvider = createJavaLabelProvider();

	private String colRLInfo = "";
	private String colExInfo = "";

	private JavaElementLabelProvider createJavaLabelProvider() {
		return new JavaElementLabelProvider();
	}

	public String getColumnText(Object element, int columnIndex) {

		String text = null;
		switch (columnIndex) {
		case 0:
			if (element != null) {
				if (element instanceof MethodWrapper) {
					try {
						MethodWrapper wrapper = (MethodWrapper) element;

						logger.debug("[getColumnText]=" + wrapper.getName());

						if (wrapper.getMember() instanceof IMethod) {
							IMethod method = (IMethod) wrapper.getMember();
							IType type = method.getDeclaringType();

							text = method.getElementName() + "() - "
									+ type.getFullyQualifiedName();
						} else if (wrapper.getMember() instanceof IType) {
							text = ((IType) wrapper.getMember())
									.getFullyQualifiedName();
						}

					} catch (Exception e) {
						logger.error("[getColumnText] EXCEPTION ", e);
					}
				}
			}
			break;
		case 1:
			if (element instanceof MethodWrapper) {
				this.getRLMessage((MethodWrapper) element);
				text = this.colRLInfo;
			}
			break;
		case 2:
			if (element instanceof MethodWrapper) {
				text = this.colExInfo;
			}

			break;

		}

		return text;
	}

	private void getRLMessage(MethodWrapper wrapper) {
		this.colExInfo = "";
		this.colRLInfo = "";
		if (wrapper != null) {

			RLMethodModel model = new RLMethodModel();
			try {
				IOpenable input = wrapper.getMember().getOpenable();
				int offset = wrapper.getMember().getSourceRange().getOffset();
				int length = wrapper.getMember().getSourceRange().getLength();

				// 將offset取到該method的最後面，是因為若有註解，則RL會取不出來，則需要指到method內
				offset = offset + length - 10;
				length = 0;

				if (!model.createAST(input, offset)) {
					RLEHTPlugin.logError("AST could not be created." + input,
							null);
				} else {

					model.parseDocument(offset, length);

					List<RLMessage> rlmsgs = model.getRLAnnotationList();

					if (rlmsgs != null) {
						for (RLMessage rlmsg : rlmsgs) {
							this.colRLInfo += ("{ "
									+ rlmsg.getRLData().getLevel() + " , "
									+ rlmsg.getRLData().getExceptionType() + " } ");
						}
						rlmsgs.clear();

					} else {
						this.colRLInfo = "NULL";
					}

					rlmsgs = model.getExceptionList();
					if (rlmsgs != null) {
						for (RLMessage rlmsg : rlmsgs) {
							if (rlmsg.getRLData().getLevel() < 0) {
								continue;
							}
							if (rlmsg.isHandleByCatch()) {
								continue;
							}
							if (this.colExInfo.indexOf(rlmsg.getRLData()
									.getExceptionType()) == -1) {
								this.colExInfo += (rlmsg.getRLData()
										.getExceptionType() + ", ");
							}
						}
						rlmsgs.clear();
					} else {
						this.colExInfo = "NULL";
					}

				}
			} catch (Exception ex) {
				logger.error("[getRLMessage] Error!", ex);
				RLEHTPlugin.logError(
						"[CallersLabelProvider][getRLMessage] Error!", null);
				this.colRLInfo = "ERROR";
			} finally {
				if (model != null) {
					model.clear();
				}

			}
		}
		// logger.debug("[getRLMessage] wrapper="+wrapper +" --->"+text +"\n");
		// return this.colRLInfo;
	}

	public Image getColumnImage(Object element, int columnIndex) {
		Image image = null;
		switch (columnIndex) {
		case 0:
			if (element instanceof MethodWrapper) {
				MethodWrapper methodWrapper = (MethodWrapper) element;

				if (methodWrapper.getMember() != null) {
					switch (methodWrapper.getMember().getElementType()) {
					case IJavaElement.METHOD:
						image = javaElementLabelProvider
								.getImage((IMethod) methodWrapper.getMember());
						break;
					case IJavaElement.TYPE:
						image = javaElementLabelProvider
								.getImage((IType) methodWrapper.getMember());
						break;
					}

				}
			}
			break;
		case 1:
			image = javaElementLabelProvider.getImage(element);
			break;

		}
		return image;
	}
}
