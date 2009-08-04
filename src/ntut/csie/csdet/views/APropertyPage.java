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
	 * 解析文字(把文字Token拆解，並把每個Token對應一個Style)
	 * @param text		輸入文字
	 * @param textList	儲存每一個Token
	 * @param typeList	儲存每一個Token的Style
	 */
	public void parserText(String text, List<String> textList, List<Integer> typeList) {
		int i = 0;
		boolean isContinue = false;
		String temp = "";
		for (; i < text.length(); i++) {
			if (!isContinue) {
				//遇到進行Parser
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
					//把該符號("\n" "\t"...)加入
					textList.add(String.valueOf(text.charAt(i)));
					typeList.add(STYLE_NULL);
					temp = "";
				//Field
				} else if (temp.equals("logger")) {
					textList.add(temp);
					typeList.add(STYLE_FIELD);
					temp = "";
					temp += text.charAt(i);
				//遇到"/"為註解
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
				//註解 遇空白仍偵測，直至換行為止
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
	 * 將文字Token和Style，取得出起始位置和長度、Style
	 * @param display
	 * @param style		文字Style
	 * @param location	起始位置,長度
	 * @param inpText	輸入文字
	 * @param inpStyle	輸入Style
	 * @param counter	字數(紀錄起點)
	 * @return			回傳字數
	 */
	public int setTemplateStyle(Display display ,List<StyleRange> style, List<Integer> location,
							List<String> inpText, List<Integer> inpStyle,int counter) {
		for (int i = 0; i < inpText.size(); i++) {
			if (inpStyle.get(i) != STYLE_NULL) {
				//Style
				style.add(this.getStyle(display, inpStyle.get(i)));
				//起始位置
				location.add(counter);
				//長度
				location.add(inpText.get(i).length());
			}
			counter += inpText.get(i).length();
		}
		return counter;
	}
	
	/**
	 * 取得程式碼中會用到的字型、顏色
	 * @param display
	 * @param type	Style種類
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
