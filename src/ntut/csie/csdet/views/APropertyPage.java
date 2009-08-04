package ntut.csie.csdet.views;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public abstract class APropertyPage {
	final static public int STYLE_NULL = 0;
	final static public int STYLE_KEYWORD = 1;
	final static public int STYLE_COMMENT = 2;
	final static public int STYLE_FIELD = 3;
	final static public int STYLE_BOLD = 4;
	
	protected CSPropertyPage page;
	
	public APropertyPage(Composite composite,CSPropertyPage page){
		this.page = page;
	}
	
	abstract public boolean storeSettings();
	
	protected void setVaild(boolean valid){
		page.setValid(valid);
	}
	
	/**
	 * �ѪR��r(���rToken��ѡA�ç�C��Token�����@��Style)
	 * @param text		��J��r
	 * @param textList	�x�s�C�@��Token
	 * @param typeList	�x�s�C�@��Token��Style
	 */
	public void parserText(String text, List<String> textList, List<Integer> typeList) {
		int i = 0;
		boolean isContinue = false;
		String temp = "";
		for (; i < text.length(); i++) {
			if (!isContinue) {
				//�J��i��Parser
				if (i == text.length()-1 || text.charAt(i) ==  ' ' ||
					text.charAt(i) ==  '\n' || text.charAt(i) ==  '\t'){
					textList.add(temp);
					//key Word
					if (temp.equals("public") || temp.equals("void") ||
						temp.equals("throw") || temp.equals("throws") ||
						temp.equals("try") || temp.equals("catch") ||
						temp.equals("new")) {
						typeList.add(STYLE_KEYWORD);
					} else {
						typeList.add(STYLE_NULL);
					}
					//��ӲŸ�("\n" "\t"...)�[�J
					textList.add(String.valueOf(text.charAt(i)));
					typeList.add(STYLE_NULL);
					temp = "";
				//Field
				} else if (temp.equals("logger")) {
					textList.add(temp);
					typeList.add(STYLE_FIELD);
					temp = "";
					temp += text.charAt(i);
				//�J��"/"������
				} else if (text.charAt(i) == '/') {
					isContinue = true;
					textList.add(temp);
					typeList.add(STYLE_NULL);
					temp = "";
					temp += text.charAt(i);
				} else {
					temp += text.charAt(i);
				}
			} else {
				//���� �J�ťդ������A���ܴ��欰��
				if (text.charAt(i) == '\n') {
					temp += text.charAt(i);
					isContinue = false;
					textList.add(temp);
					typeList.add(STYLE_COMMENT);
					temp = "";
				} else {
					temp += text.charAt(i);
				}
			}
		}
	}

	/**
	 * �N��rToken�MStyle�A���o�X�_�l��m�M���סBStyle
	 * @param display
	 * @param style		��rStyle
	 * @param location	�_�l��m,����
	 * @param inpText	��J��r
	 * @param inpStyle	��JStyle
	 * @param counter	�r��(�����_�I)
	 * @return			�^�Ǧr��
	 */
	public int setTemplateStyle(Display display ,List<StyleRange> style, List<Integer> location,
							List<String> inpText, List<Integer> inpStyle,int counter) {
		for (int i = 0; i < inpText.size(); i++) {
			if (inpStyle.get(i) != STYLE_NULL) {
				//Style
				style.add(this.getStyle(display, inpStyle.get(i)));
				//�_�l��m
				location.add(counter);
				//����
				location.add(inpText.get(i).length());
			}
			counter += inpText.get(i).length();
		}
		return counter;
	}
	
	/**
	 * ���o�{���X���|�Ψ쪺�r���B�C��
	 * @param display
	 * @param type	Style����
	 */
	private StyleRange getStyle(Display display, int type) {
		StyleRange style = new StyleRange();
		switch (type) {
			//Key Word
			case STYLE_KEYWORD:
				style.fontStyle = SWT.BOLD;
				style.foreground = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
				break;
			//Comment
			case STYLE_COMMENT:
				style.fontStyle = SWT.ITALIC;
				style.foreground = display.getSystemColor(SWT.COLOR_DARK_GREEN);
				break;
			//Field
			case STYLE_FIELD:
				style.fontStyle = SWT.ITALIC;
				style.foreground = display.getSystemColor(SWT.COLOR_BLUE);
				break;
			//Bold
			case STYLE_BOLD:
				style.fontStyle = SWT.BOLD;
				style.foreground = display.getSystemColor(SWT.DEFAULT);
				break;
		}
		return style;
	}
}
