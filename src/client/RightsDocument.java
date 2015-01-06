package client;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

public class RightsDocument extends PlainDocument {

	private static final long serialVersionUID = -2348261835727331446L;

	public void insertString(int offs, String str, AttributeSet a)
			throws BadLocationException {

		if (str == null) {
			return;
		}
		
		String s = super.getText(0, super.getLength());
		
		if (str.charAt(0) == 'U' && !s.contains("U") ||
			str.charAt(0) == 'A' && !s.contains("A")) {
			super.insertString(offs, str, a);
		}
	}

}
