package ntut.csie.csdet.views;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

public class TemplateText {
	final static public int STYLE_NULL = 0;
	final static public int STYLE_KEYWORD = 1;
	final static public int STYLE_COMMENT = 2;
	final static public int STYLE_FIELD = 3;
	final static public int STYLE_BOLD = 4;
	final static public int STYLE_TASK = 5;
	final static public int STYLE_ERROR = 6;
	
	private boolean showWarning = false;
	private String text;
	private List<String> textList = new ArrayList<String>();
	private List<Integer> typeList = new ArrayList<Integer>();

	private List<Integer> locationList = new ArrayList<Integer>();
	private List<StyleRange> styleList = new ArrayList<StyleRange>();

	TemplateText(String text, boolean isShowWarning) {
		this.text = text;
		this.showWarning = isShowWarning;
		
		parserText();
	}
	
	public void setTemplateText(String text, boolean isShowWarning) {
		this.text = text;
		this.showWarning = isShowWarning;
		
		clearLocationData();
		clearTextData();
		
		parserText();
	}

	/**
	 * 解析文字(把文字Token拆解，並把每個Token對應一個Style)
	 * @param text		輸入文字
	 * @param textList	儲存每一個Token
	 * @param typeList	儲存每一個Token的Style
	 */
	private void parserText() {
		int i = 0;
		boolean isContinue = false;
		boolean isError = false;
		String temp = "";
		for (; i < text.length(); i++) {
			if (!isContinue && !isError) {
				//遇到進行Parser
				if (i == text.length()-1 || text.charAt(i) ==  ' ' ||
					text.charAt(i) ==  '\n' || text.charAt(i) ==  '\t'){
					textList.add(temp);
					//key Word
					if (temp.equals("public") || temp.equals("void")   ||
						temp.equals("throw")  || temp.equals("throws") ||
						temp.equals("try")    || temp.equals("catch")  ||
						temp.equals("new")    || temp.equals("finally")||
						temp.equals("static")) {
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
				} else if (text.charAt(i) == '$') {
					if (showWarning) {
						isError = true;
					}
					textList.add(temp);
					typeList.add(STYLE_NULL);
					temp = "";
				} else {
					temp += text.charAt(i);
				}
			} else if (isContinue) {
				//註解 遇空白仍偵測，直至換行為止
				if (text.charAt(i) == '\n') {
					temp += text.charAt(i);
					isContinue = false;
					textList.add(temp);
					typeList.add(STYLE_COMMENT);
					temp = "";
				} else {
					if (text.length() > i+4 &&
						text.substring(i,i+4).equals("TODO")) {
						//把之前的註解存起來
						textList.add(temp);
						typeList.add(STYLE_COMMENT);
						temp = "";
						//加入新的Task註解
						textList.add("TODO");
						typeList.add(STYLE_TASK);
						//指標移到Task Token結束位置
						i+=3;
					} else {
						temp += text.charAt(i);
					}
				}
			} else if (isError) {
				//註解 遇空白仍偵測，直至換行為止
				if (text.charAt(i) == '$') {
					isError = false;
					textList.add(temp);
					typeList.add(STYLE_ERROR);
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
	 * @param counter	字數(紀錄起點)
	 * @return			回傳字數
	 */
	public int setTemplateStyle(Display display, int counter) {
		clearLocationData();
		
		for (int i = 0; i < textList.size(); i++) {
			if (typeList.get(i) != STYLE_NULL) {
				//Style
				styleList.add(this.getStyle(display, typeList.get(i)));
				//起始位置
				locationList.add(counter);
				//長度
				locationList.add(textList.get(i).length());
			}
			counter += textList.get(i).length();
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
			//Task
			case STYLE_TASK:
				style.fontStyle = SWT.BOLD;
				style.foreground = new Color(display, 127 ,159 ,191);
				break;
			//ERROR
			case STYLE_ERROR:
				style.fontStyle = SWT.DEFAULT;
				style.foreground = display.getSystemColor(SWT.COLOR_RED);
				break;
		}
		return style;
	}
	
	private void clearLocationData() {
		styleList.clear();
		locationList.clear();
	}

	private void clearTextData() {
		textList.clear();
		typeList.clear();
	}

	public String getText() {
		String temp = text.replace("$", "");
		return temp;
	}

	public List<StyleRange> getStyleList() {
		return styleList;
	}
	public StyleRange[] getStyleArrray() {
		//將List轉成Array
		StyleRange[] calleeStyles = styleList.toArray(new StyleRange[styleList.size()]);
		return calleeStyles;
	}

	public List<Integer> getLocationList() {
		return locationList;
	}	
	public int[] getLocationArray() {
		//將List轉成Array
		Integer[] rangeInteger = locationList.toArray(new Integer[locationList.size()]);
		//將Integer Array 轉成 int Array
		int[] rangeInt = ArrayUtils.toPrimitive(rangeInteger);
		return rangeInt;
	}

	public List<String> getTextList() {
		return textList;
	}

	public void setShowWarning(boolean showWarning) {
		this.showWarning = showWarning;

		clearLocationData();
		clearTextData();
		
		parserText();
	}
}
