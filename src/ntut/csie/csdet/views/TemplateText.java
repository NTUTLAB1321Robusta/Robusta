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
	 * parse text into tokens which is corresponding to text style
	 * @param text		
	 * 				input text
	 * @param textList	
	 * 				a list for storing token
	 * @param typeList	
	 * 				a list for storing token's style
	 */
	private void parserText() {
		int i = 0;
		boolean isContinue = false;
		boolean isError = false;
		String temp = "";
		for (; i < text.length(); i++) {
			if (!isContinue && !isError) {
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
					textList.add(String.valueOf(text.charAt(i)));
					typeList.add(STYLE_NULL);
					temp = "";
				//Field
				} else if (temp.equals("logger")) {
					textList.add(temp);
					typeList.add(STYLE_FIELD);
					temp = "";
					temp += text.charAt(i);
				//take "/" as comment
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
				// ignore blank token until meet line wrap token(ex."\n")
				if (text.charAt(i) == '\n') {
					temp += text.charAt(i);
					isContinue = false;
					textList.add(temp);
					typeList.add(STYLE_COMMENT);
					temp = "";
				} else {
					if (text.length() > i+4 &&
						text.substring(i,i+4).equals("TODO")) {
						//store previous comment
						textList.add(temp);
						typeList.add(STYLE_COMMENT);
						temp = "";
						//add new task token
						textList.add("TODO");
						typeList.add(STYLE_TASK);
						//move index to the end of new task token
						i+=3;
					} else {
						temp += text.charAt(i);
					}
				}
			} else if (isError) {
				// ignore blank token until meet line wrap token
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
	 * get the start index, length and style from text token
	 * @param display
	 * @param counter	
	 * 				start index of text's length
	 * @return			length of text
	 */
	public int setTemplateStyle(Display display, int counter) {
		clearLocationData();
		
		for (int i = 0; i < textList.size(); i++) {
			if (typeList.get(i) != STYLE_NULL) {
				//Style
				styleList.add(this.getStyle(display, typeList.get(i)));
				//start index
				locationList.add(counter);
				//length
				locationList.add(textList.get(i).length());
			}
			counter += textList.get(i).length();
		}
		return counter;
	}

	/**
	 * get font style which will be used in code
	 * @param display
	 * @param type 
	 * 	 		Style type
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
		StyleRange[] calleeStyles = styleList.toArray(new StyleRange[styleList.size()]);
		return calleeStyles;
	}

	public List<Integer> getLocationList() {
		return locationList;
	}	
	public int[] getLocationArray() {
		Integer[] rangeInteger = locationList.toArray(new Integer[locationList.size()]);
		//transform Integer Array to int Array
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
