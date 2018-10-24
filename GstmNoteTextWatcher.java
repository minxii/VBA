

import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.widget.EditText;

public class GstmNoteTextWatcher implements TextWatcher {
    EditText m_text;
    public GstmNoteTextWatcher(EditText text){
        m_text = text;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        int lines = m_text.getLineCount();
        String str = s.toString();

        boolean unfixed = false;
        Object[] spanned = s.getSpans(0, s.length(), Object.class);
        if (spanned != null) {
            for (Object obj : spanned) {
                // UnderlineSpan での判定から getSpanFlags への判定に変更。
                // if (obj instanceof android.text.style.UnderlineSpan) {
                if ((s.getSpanFlags(obj) & Spanned.SPAN_COMPOSING) == Spanned.SPAN_COMPOSING) {
                    unfixed = true;
                }
            }
        }

        if (unfixed) return;

        if (lines > 2) {
            if ((str.length() - str.replaceAll("\n","").length()) >= 2  ) {
                int cursorStart = m_text.getSelectionStart();
                int cursorEnd = m_text.getSelectionEnd();
                if (cursorStart == cursorEnd && cursorStart < str.length() && cursorStart >= 1) {
                    str = str.substring(0, cursorStart - 1) + str.substring(cursorStart);
                } else {
                    str = str.substring(0, str.indexOf("\n", str.indexOf("\n") + 1));
                }
                m_text.setText(str);
                m_text.setSelection(m_text.getText().length());
            }
        }

        String strOrg = str;
        int lFIdx = str.indexOf("\n");
        String[] strs = null;
        if (lFIdx != -1) {
            //改行あり
            strs = str.split("\n");
            boolean needBytesCheck = false;
            for (String tmpSs : strs) {
                if (tmpSs.getBytes().length > 20) {
                    needBytesCheck = true;
                }
            }
            if (!needBytesCheck) return;

        } else {
            //一行のみ
            if (str.getBytes().length <= 20) return;
        }

        if (strs != null) {
            //二行
            str = strs[0].substring(0, findLastIdx(strs[0]));
            str += "\n";

            if (strs.length == 2) {
                str += strs[1].substring(0, findLastIdx(strs[1]));
            }
        } else {
            //一行のみ
            String tmpStr = str.substring(findLastIdx(str));
            str = str.substring(0, findLastIdx(str));

            if(tmpStr != null && tmpStr.length() > 0) {
                str += "\n" + tmpStr;
            }
        }
        if (!strOrg.equals(str)) {
            m_text.setText(str);
            m_text.setSelection(m_text.getText().length());
        }
    }

    private int findLastIdx (String str) {
        int lastIdx = 0;
        int currentBytes = 0;
        final int maxBytes = 20;

        for (int i = 0; i < str.length(); i++) {
            String tmp = str.substring(i, i+1);
            if (tmp.getBytes().length == 1) {
                //半角
                currentBytes += 1;
            } else {
                //全角
                currentBytes += 2;
            }

            if (currentBytes <= maxBytes) {
                lastIdx = i + 1;
            }
        }

        return lastIdx;
    }
}
