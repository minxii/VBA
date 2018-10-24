package gstm.tab.reader;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.radaee.pdf.Global;
import com.radaee.pdf.Ink;
import com.radaee.pdf.Matrix;
import com.radaee.pdf.Page;
import com.radaee.pdf.ViewerConstants;
import com.radaee.reader.PDFLayoutOPStack;
import com.radaee.reader.PDFLayoutView;
import com.radaee.util.CommonUtil;
import com.radaee.view.PDFLayout;
import com.radaee.view.VPage;

import java.io.File;
import java.util.Calendar;

public class GstmPDFLayoutView extends PDFLayoutView {
    private int m_gstm_status;
    private int m_gstm_ink_status;
    private float m_annot_x1;
    private float m_annot_y1;

    private boolean isNewCheckMark = false;

    protected Ink m_check = null;
    private Canvas m_canvas = null;

    String m_timeStamp = null;
    String m_content = null;

    int m_cnt = 0;

    /**
     * イメージ入力モード用のFile
     */
    private File imageFile;
    /**
     * タイムスタンプ用のPaint
     */
    private Paint time_paint;

    static final protected int GSTM_STA_CONTI_INK = 9;
    static final protected int GSTM_STA_IMAGE = 10;
    static final protected int GSTM_STA_IMAGE_RESIZE = 11;
    static final protected int GSTM_STA_CHECK = 14;

    static final protected int ANNOT_TYPE_NOTE = 1;
    static final protected int ANNOT_TYPE_IMAGE = 13;
    static final protected int ANNOT_TYPE_CHECK = 15;

    /**
     * 画像サイズ変更モード区分
     */
    private int resizeMode;
    /**
     * 左上
     */
    private final int ANNOT_LEFT_TOP = 1;
    /**
     * 右下
     */
    private final int ANNOT_RIGHT_BOTTOM = 2;
    /**
     * 左下
     */
    private final int ANNOT_LEFT_BOTTOM = 3;
    /**
     * 右上
     */
    private final int ANNOT_RIGHT_TOP = 4;
    /**
     * ノーマルモード
     */
    private final int ANNOT_NONE = -1;

    private boolean m_readOnly = false;

    ViewerCallback act_listener = null;


    public GstmPDFLayoutView(Context context) {
        super(context);
        initTimePaint();
    }

    public GstmPDFLayoutView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initTimePaint();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (m_gstm_ink_status == GSTM_STA_CHECK) {
            onDrawCheck(canvas);
        } else {
            onDrawNote(canvas);
        }
    }

    private void onDrawCheck(Canvas canvas) {
        // Check
        if (m_layout != null) {
            // TimeStamp
            String timeStamp = null;
            if (m_status == STA_NOTE) {

                if (!isNewCheckMark && m_timeStamp == null) return;

                if (isNewCheckMark) {
                    timeStamp = getTime();
                    m_timeStamp = timeStamp;
                } else {
                    if (m_timeStamp != null) {
                        timeStamp = m_timeStamp;
                    }
                }

                Page page = m_doc.GetPage(m_pageno);
                if (page != null) {
                    page.ObjsStart();
                    Page.Annotation annot = page.GetAnnot(page.GetAnnotCount() - 1);
                    if (annot == null) {
                        isNewCheckMark = false;
                        m_timeStamp = null;
                        return;
                    }
                    annot.SetPopupText(timeStamp);
                    annot.SetModifyDate(CommonUtil.getCurrentDate());
                    if (m_listener != null)
                        m_listener.OnPDFPageModified(m_pageno);

                    VPage annot_page = m_layout.vGetPage(m_pageno);
                    float annot_rect[] = annot.GetRect();
                    float tmp = annot_rect[1];
                    annot_rect[0] = annot_page.GetVX(annot_rect[0]) - m_layout.vGetX();
                    annot_rect[1] = annot_page.GetVY(annot_rect[3]) - m_layout.vGetY();
                    annot_rect[2] = annot_page.GetVX(annot_rect[2]) - m_layout.vGetX();
                    annot_rect[3] = annot_page.GetVY(tmp) - m_layout.vGetY();

                    if (timeStamp != null && timeStamp.length() == 19) {
                        canvas.drawText(timeStamp, 0, 10, annot_rect[0] - 30, annot_rect[1] - 35, time_paint);
                        canvas.drawText(timeStamp, 11, 19, annot_rect[0] - 30, annot_rect[1] - 15, time_paint);
                    }
                    isNewCheckMark = false;

                    page.Close();
                }
            } else {
                m_timeStamp = null;
            }
        }
    }

    private void onDrawNote(Canvas canvas) {
        // Check
        if (m_layout != null) {
            if (m_content == null) return;

            Page page = m_doc.GetPage(m_pageno);
            if (page != null) {
                System.out.println("COunt:"+m_cnt);
                page.ObjsStart();
                Page.Annotation annot = page.GetAnnot(page.GetAnnotCount() - 1);
                if (annot == null) {
                    return;
                }

                VPage annot_page = m_layout.vGetPage(m_pageno);
                float annot_rect[] = annot.GetRect();
                float tmp = annot_rect[1];
                annot_rect[0] = annot_page.GetVX(annot_rect[0]) - m_layout.vGetX();
                annot_rect[1] = annot_page.GetVY(annot_rect[3]) - m_layout.vGetY();
                annot_rect[2] = annot_page.GetVX(annot_rect[2]) - m_layout.vGetX();
                annot_rect[3] = annot_page.GetVY(tmp) - m_layout.vGetY();

                if (m_content != null && m_content.length() > 0) {
                    String[] contents = m_content.split("\n");
                    if (contents.length == 1) {
                        canvas.drawText(contents[0], annot_rect[0] - 30, annot_rect[1] - 15, time_paint);
                    } else if (contents.length == 2) {
                        canvas.drawText(contents[0], annot_rect[0] - 30, annot_rect[1] - 35, time_paint);
                        canvas.drawText(contents[1], annot_rect[0] - 30, annot_rect[1] - 15, time_paint);
                    }
                }
                m_cnt ++;

                if (m_cnt >= 2) {
                    m_content = null;
                }

                page.Close();
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (m_layout == null) return false;

        if (m_status == STA_ANNOT && m_gstm_status == GSTM_STA_IMAGE) {
            onTouchResizeHandler(event);
        } else if (m_status == STA_ANNOT && m_gstm_status == GSTM_STA_IMAGE_RESIZE) {
            onResize(event);
        } else {
            boolean ret = super.onTouchEvent(event);
            if (MotionEvent.ACTION_UP == event.getActionMasked()) {
                switch (m_status) {
                    case STA_INK:
                        // 連続鉛筆以外の場合、一回書く後保存
                        if (GSTM_STA_CONTI_INK != m_gstm_ink_status) {
                            PDFSetInk(1);
                            PDFSetInk(0);
                        }
                        break;
                    case STA_RECT:
                        PDFSetRect(1);
                        PDFSetRect(0);
                        break;
                    case STA_ELLIPSE:
                        PDFSetEllipse(1);
                        PDFSetEllipse(0);
                        break;
                    case STA_LINE:
                        PDFSetLine(1);
                        PDFSetLine(0);
                        break;
                    case STA_NOTE:
                        if (m_gstm_ink_status == GSTM_STA_CHECK) {
                            PDFSetCheckPre(1);
                            isNewCheckMark = true;
                            PDFSetCheckPre(0);
                        } else {
                            PDFEditAnnot();
                        }
                        break;
                    default:
                        break;
                }
            }
            return ret;
        }
        return true;
    }

    private boolean onTouchResizeHandler(MotionEvent event) {
        float left = m_annot_rect[0];
        float top = m_annot_rect[1];
        float right = m_annot_rect[2];
        float bottom = m_annot_rect[3];

        // 画像外のポイント
        float size_1 = 15 * PDFGetScale();
        // 画像内のポイント
        float size_2 = 10 * PDFGetScale();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                m_annot_x1 = event.getX();
                m_annot_y1 = event.getY();
                if (m_annot_x1 <= left + size_2 && m_annot_y1 <= top + size_2 &&
                        m_annot_x1 >= left - size_1 && m_annot_y1 >= top - size_1) {
                    resizeMode = ANNOT_LEFT_TOP;
                } else if (m_annot_x1 >= right - size_2 && m_annot_y1 >= bottom - size_2 &&
                        m_annot_x1 <= right + size_1 && m_annot_y1 <= bottom + size_1) {
                    resizeMode = ANNOT_RIGHT_BOTTOM;
                } else if (m_annot_x1 <= left + size_2 && m_annot_y1 >= bottom - size_2 &&
                        m_annot_x1 >= left - size_1 && m_annot_y1 <= bottom + size_1) {
                    resizeMode = ANNOT_LEFT_BOTTOM;
                } else if (m_annot_x1 >= right - size_2 && m_annot_y1 <= top + size_2 &&
                        m_annot_x1 <= right + size_1 && m_annot_y1 >= top - size_1) {
                    resizeMode = ANNOT_RIGHT_TOP;
                    // 上記以外画像のリサイズエリア範囲以内、または以外の場合
                } else {
                    resizeMode = ANNOT_NONE;
                    return super.onTouchEvent(event);
                }
                m_gstm_status = GSTM_STA_IMAGE_RESIZE;

                break;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return super.onTouchEvent(event);
        }
        invalidate();
        return true;
    }

    private boolean onResize(MotionEvent event) {
        // 最小サイズ
        float size_3 = 10 * PDFGetScale();

        float x = event.getX();
        float y = event.getY();

        float dx;
        float dy;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                dx = x - m_annot_x1;
                dy = y - m_annot_y1;

                if (resizeMode == ANNOT_LEFT_TOP) {
                    if (m_annot_rect[0] + dx <= m_annot_rect[2] - size_3)
                        m_annot_rect[0] += dx;
                    if (m_annot_rect[1] + dy <= m_annot_rect[3] - size_3)
                        m_annot_rect[1] += dy;
                } else if (resizeMode == ANNOT_RIGHT_BOTTOM) {
                    if (m_annot_rect[2] + dx >= m_annot_rect[0] + size_3)
                        m_annot_rect[2] += dx;
                    if (m_annot_rect[3] + dy >= m_annot_rect[1] + size_3)
                        m_annot_rect[3] += dy;
                } else if (resizeMode == ANNOT_LEFT_BOTTOM) {
                    if (m_annot_rect[0] + dx <= m_annot_rect[2] - size_3)
                        m_annot_rect[0] += dx;
                    if (m_annot_rect[3] + dy >= m_annot_rect[1] + size_3)
                        m_annot_rect[3] += dy;
                } else if (resizeMode == ANNOT_RIGHT_TOP) {
                    if (m_annot_rect[2] + dx >= m_annot_rect[0] + size_3)
                        m_annot_rect[2] += dx;
                    if (m_annot_rect[1] + dy <= m_annot_rect[3] - size_3)
                        m_annot_rect[1] += dy;
                }

                m_annot_x1 = x;
                m_annot_y1 = y;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dx = x - m_annot_x1;
                dy = y - m_annot_y1;

                if (resizeMode == ANNOT_LEFT_TOP) {
                    if (m_annot_rect[0] + dx <= m_annot_rect[2] - size_3)
                        m_annot_rect[0] += dx;
                    if (m_annot_rect[1] + dy <= m_annot_rect[3] - size_3)
                        m_annot_rect[1] += dy;
                } else if (resizeMode == ANNOT_RIGHT_BOTTOM) {
                    if (m_annot_rect[2] + dx >= m_annot_rect[0] + size_3)
                        m_annot_rect[2] += dx;
                    if (m_annot_rect[3] + dy >= m_annot_rect[1] + size_3)
                        m_annot_rect[3] += dy;
                } else if (resizeMode == ANNOT_LEFT_BOTTOM) {
                    if (m_annot_rect[0] + dx <= m_annot_rect[2] - size_3)
                        m_annot_rect[0] += dx;
                    if (m_annot_rect[3] + dy >= m_annot_rect[1] + size_3)
                        m_annot_rect[3] += dy;
                } else if (resizeMode == ANNOT_RIGHT_TOP) {
                    if (m_annot_rect[2] + dx >= m_annot_rect[0] + size_3)
                        m_annot_rect[2] += dx;
                    if (m_annot_rect[1] + dy <= m_annot_rect[3] - size_3)
                        m_annot_rect[1] += dy;
                }

                m_annot_x1 = x;
                m_annot_y1 = y;

                float rect[] = new float[4];
                rect[0] = m_annot_page.ToPDFX(m_annot_rect[0], m_layout.vGetX());
                rect[1] = m_annot_page.ToPDFY(m_annot_rect[3], m_layout.vGetY());
                rect[2] = m_annot_page.ToPDFX(m_annot_rect[2], m_layout.vGetX());
                rect[3] = m_annot_page.ToPDFY(m_annot_rect[1], m_layout.vGetY());

                m_annot.SetRect(rect[0], rect[1], rect[2], rect[3]);
                m_annot.SetModifyDate(CommonUtil.getCurrentDate());
                m_layout.vRenderSync(m_annot_page);
                if (m_listener != null)
                    m_listener.OnPDFPageModified(m_annot_page.GetPageNo());

                m_gstm_status = 0;
                m_status = STA_NONE;

                PDFEndAnnot();
                break;
        }
        invalidate();
        return true;
    }

    // 手書き画像選択線表示色変更
    @Override
    protected void onDrawAnnot(Canvas canvas) {
        // 編集不可の場合、手書きの選択もできなくなるように
        if (m_readOnly) {
            if (m_status == STA_ANNOT) {
                m_status = STA_NONE;
                return;
            }
        }
        if (m_status == STA_ANNOT && Global.highlight_annotation) {
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setARGB(0xFF, 0x1D, 0xDB, 0x16);

            float left = m_annot_rect[0];
            float top = m_annot_rect[1];
            float right = m_annot_rect[2];
            float bottom = m_annot_rect[3];
            canvas.drawRect(left,
                    top,
                    right,
                    bottom, paint);

            // annot_imageの場合
            if (m_annot.GetType() == ANNOT_TYPE_IMAGE) {
                paint.setStyle(android.graphics.Paint.Style.FILL);
                // サイズ変更点表示
                float size = 5 * PDFGetScale();
                canvas.drawCircle(left, top, size, paint);
                canvas.drawCircle(left, bottom, size, paint);
                canvas.drawCircle(right, top, size, paint);
                canvas.drawCircle(right, bottom, size, paint);

                if (m_gstm_status != GSTM_STA_IMAGE_RESIZE)
                    m_gstm_status = GSTM_STA_IMAGE;
            } else {
                m_gstm_status = 0;
            }

            // タイムスタンプがあれば表示する
            String timeStamp = m_annot.GetPopupText();
            if (m_annot.GetType() == ANNOT_TYPE_CHECK) {
                if (timeStamp != null && timeStamp.length() == 19) {
                    canvas.drawText(timeStamp, 0, 10, m_annot_rect[0] - 30, m_annot_rect[1] - 35, time_paint);
                    canvas.drawText(timeStamp, 11, 19, m_annot_rect[0] - 30, m_annot_rect[1] - 15, time_paint);
                }
            } else if (m_annot.GetType() == ANNOT_TYPE_NOTE) {
                if (timeStamp != null) {
                    String[] contents = timeStamp.split("\n");
                    if (contents.length == 1) {
                        canvas.drawText(contents[0], m_annot_rect[0] - 30, m_annot_rect[1] - 15, time_paint);
                    } else if (contents.length == 2) {
                        canvas.drawText(contents[0], m_annot_rect[0] - 30, m_annot_rect[1] - 35, time_paint);
                        canvas.drawText(contents[1], m_annot_rect[0] - 30, m_annot_rect[1] - 15, time_paint);
                    }
                }
            }
        } else {
            m_gstm_status = 0;
        }
    }


    @Override
    public void PDFSetNote(int code) {
        if (m_gstm_ink_status == GSTM_STA_CHECK) {
            PDFSetCheck(code);
        } else {
            super.PDFSetNote(code);
        }
    }

    private void PDFSetCheck(int code) {
        if (code == 0)//start
        {
            m_status = STA_NOTE;
//            m_check = new Ink(Global.inkWidth);
        } else if (code == 1)//end
        {
            m_status = STA_NONE;
            if (m_annot_page != null) {
                Page page = m_doc.GetPage(m_annot_page.GetPageNo());
                if (page != null) {
                    page.ObjsStart();
                    Matrix mat = m_annot_page.CreateInvertMatrix(m_layout.vGetX(), m_layout.vGetY());
                    mat.TransformInk(m_check);
                    page.AddAnnotInk(m_check);
                    mat.Destroy();
                    onAnnotCreated(page.GetAnnot(page.GetAnnotCount() - 1));
                    //add to redo/undo stack.
                    addToStack(m_annot_page.GetPageNo(), page, page.GetAnnotCount() - 1);
                    m_layout.vRenderSync(m_annot_page);

                    page.Close();

                    if (m_listener != null)
                        m_listener.OnPDFPageModified(m_annot_page.GetPageNo());
                }
            }
            if (m_check != null) m_check.Destroy();
            m_check = null;
            m_annot_page = null;
            invalidate();
        } else//cancel
        {
            m_status = STA_NONE;
            m_check.Destroy();
            m_check = null;
            m_annot_page = null;
            invalidate();
        }
    }

    @Override
    public void PDFSetStamp(int code) {
        PDFSetImage(code);
    }

    public boolean PDFSetImage(int code) {
        if (code == 0)//start
        {
            m_status = STA_STAMP;

            imageFile = new File(ViewerConstants.CAMERA_PATH);
            if (!imageFile.exists())
                return false;
            else {
                if (m_icon != null)
                    m_icon = null;
                m_icon = BitmapFactory.decodeFile(imageFile.getPath());
                if (m_icon != null) {
                    m_dicon = m_doc.NewImage(m_icon, true);
                }
            }
        } else if (code == 1)//end
        {
            if (m_rects != null) {
                int len = m_rects.length;
                int cur;
                PDFVPageSet pset = new PDFVPageSet(len);
                for (cur = 0; cur < len; cur += 4) {
                    PDFLayout.PDFPos pos = m_layout.vGetPos((int) m_rects[cur], (int) m_rects[cur + 1]);
                    VPage vpage = m_layout.vGetPage(pos.pageno);
                    Page page = m_doc.GetPage(vpage.GetPageNo());
                    if (page != null) {
                        Matrix mat = vpage.CreateInvertMatrix(m_layout.vGetX(), m_layout.vGetY());
                        float rect[] = new float[4];
                        if (m_rects[cur] > m_rects[cur + 2]) {
                            rect[0] = m_rects[cur + 2];
                            rect[2] = m_rects[cur];
                        } else {
                            rect[0] = m_rects[cur];
                            rect[2] = m_rects[cur + 2];
                        }
                        if (m_rects[cur + 1] > m_rects[cur + 3]) {
                            rect[1] = m_rects[cur + 3];
                            rect[3] = m_rects[cur + 1];
                        } else {
                            rect[1] = m_rects[cur + 1];
                            rect[3] = m_rects[cur + 3];
                        }
                        mat.TransformRect(rect);
                        page.ObjsStart();
                        page.AddAnnotBitmap(m_dicon, rect);
                        mat.Destroy();
                        onAnnotCreated(page.GetAnnot(page.GetAnnotCount() - 1));
                        //add to redo/undo stack.
                        addToStack(pos.pageno, page, page.GetAnnotCount() - 1);
                        page.Close();
                        pset.Insert(vpage);
                    }
                }
                for (cur = 0; cur < pset.pages_cnt; cur++) {
                    VPage vpage = pset.pages[cur];
                    m_layout.vRenderSync(vpage);
                    if (m_listener != null)
                        m_listener.OnPDFPageModified(vpage.GetPageNo());
                }
            }
            m_status = STA_NONE;
            m_rects = null;
            invalidate();
            if (m_icon != null)
                m_icon.recycle();
            m_icon = null;

            if (imageFile.exists())
                imageFile.delete();

            if (act_listener != null)
                act_listener.onImageSetted();
        } else//cancel
        {
            m_status = STA_NONE;
            m_rects = null;
            invalidate();
            if (m_icon != null)
                m_icon.recycle();
            m_icon = null;

            if (imageFile.exists())
                imageFile.delete();

            if (act_listener != null)
                act_listener.onImageSetted();
        }
        return true;
    }

    @Override
    protected boolean onTouchNote(MotionEvent event) {
        if (m_gstm_ink_status == GSTM_STA_CHECK) {
            return onTouchCheck(event);
        } else {
            return super.onTouchNote(event);
        }
    }

    private boolean onTouchCheck(MotionEvent event) {

        if (m_status != STA_NOTE) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_UP:
                float ratio = m_layout.vGetZoom();
                float checkX = event.getX();
                float checkY = event.getY();

                if (m_annot_page == null) {
                    PDFLayout.PDFPos pos = m_layout.vGetPos((int) event.getX(), (int) event.getY());
                    m_annot_page = m_layout.vGetPage(pos.pageno);

                    if (m_check == null)
                        m_check = new Ink(Global.inkWidth * ratio);
                    m_check.OnDown(checkX, checkY);
                    m_check.OnUp(checkX - (ViewerConstants.checkHeightX * ratio), checkY - (ViewerConstants.checkHeightY * ratio));
                    m_check.OnDown(checkX, checkY);
                    m_check.OnUp(checkX + (ViewerConstants.checkHeightX * ratio), checkY - (ViewerConstants.checkHeightY * ratio));

                }
                break;
            case MotionEvent.ACTION_CANCEL:
                m_check.OnUp(event.getX(), event.getY());
                break;
        }
        invalidate();
        return true;
    }

    @Override
    protected boolean onTouchStamp(MotionEvent event) {
        return onTouchImage(event);
    }

    private boolean onTouchImage(MotionEvent event) {
        if (m_status != STA_STAMP) return false;
        int len = 0;
        if (m_rects != null) len = m_rects.length;
        int cur = 0;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                float rects[] = new float[len + 4];
                for (cur = 0; cur < len; cur++)
                    rects[cur] = m_rects[cur];
                len += 4;
                rects[cur + 0] = event.getX();
                rects[cur + 1] = event.getY();
                rects[cur + 2] = event.getX();
                rects[cur + 3] = event.getY();
                m_rects = rects;
                break;
            case MotionEvent.ACTION_MOVE:
                m_rects[len - 2] = event.getX();
                m_rects[len - 1] = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                PDFSetImage(1);
                break;
            case MotionEvent.ACTION_CANCEL:
                m_rects[len - 2] = event.getX();
                m_rects[len - 1] = event.getY();
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public void PDFCancelAnnot() {
        super.PDFCancelAnnot();
        m_gstm_status = 0;
        m_gstm_ink_status = 0;
    }

    public void PDFSetContiInk(int code) {
        super.PDFSetInk(code);
        if (code == 0) {
            m_gstm_ink_status = GSTM_STA_CONTI_INK;
        } else {
            m_gstm_ink_status = 0;
        }
    }

    public void PDFSetCheckPre(int code) {
        m_gstm_ink_status = GSTM_STA_CHECK;
        PDFSetNote(code);
    }

    /**
     * すべてのAnnot終了
     */
    public void endAllAnnot() {
        switch (m_status) {
            case STA_INK:
                PDFSetInk(1);
                break;
            case STA_RECT:
                PDFSetRect(1);
                break;
            case STA_ELLIPSE:
                PDFSetEllipse(1);
                break;
            case STA_LINE:
                PDFSetLine(1);
                break;
            case STA_NOTE:
                PDFSetNote(1);
                m_gstm_ink_status = 0;
                break;
            default:
                break;
        }

        if (GSTM_STA_CONTI_INK == m_gstm_ink_status) {
            PDFSetContiInk(1);
            m_gstm_ink_status = 0;
        }
    }

    @Override
    public void PDFSetLine(int code) {
        float rects[] = m_rects;
        super.PDFSetLine(code);
        setStrokeDash(code, 0, rects);
    }

    @Override
    public void PDFSetRect(int code) {
        float rects[] = m_rects;
        super.PDFSetRect(code);
        setStrokeDash(code, 1, rects);
    }

    @Override
    public void PDFSetEllipse(int code) {
        float rects[] = m_rects;
        super.PDFSetEllipse(code);
        setStrokeDash(code, 2, rects);
    }

    @Override
    public void PDFEditAnnot() {
        if (m_status != STA_ANNOT && m_status != STA_NOTE) return;

        RelativeLayout layout = (RelativeLayout) LayoutInflater.from(getContext()).inflate(R.layout.dlg_note, null);
        final EditText content = (EditText) layout.findViewById(R.id.gstm_txt_content);
        content.addTextChangedListener(new GstmNoteTextWatcher(content));

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String str_content = content.getText().toString();

                dialog.dismiss();
                if (m_status == STA_NOTE) {
                    PDFSetNote(1);
                    if (act_listener != null)
                        act_listener.onNoteCommited();

                    Page page = m_doc.GetPage(m_pageno);
                    if (page != null) {
                        Page.Annotation annot = page.GetAnnot(page.GetAnnotCount() - 1);

                        if (annot != null) {
                            annot.SetPopupText(str_content);
                            annot.SetModifyDate(CommonUtil.getCurrentDate());

                            m_content = str_content;
                            m_cnt = 0;

                            if (m_listener != null)
                                m_listener.OnPDFPageModified(m_pageno);
                        }
                    }
                } else if (m_annot != null) {
                    m_annot.SetPopupText(str_content);
                    m_annot.SetModifyDate(CommonUtil.getCurrentDate());

//                    m_content = str_content;
//                    m_cnt = 1;

                    if (m_listener != null) {
                        m_listener.OnPDFPageModified(m_annot_page.GetPageNo());
                        m_listener.OnPDFAnnotTapped(m_annot_page, m_annot);
                    }

//                    PDFEndAnnot();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // キャンセル
                if (m_status == STA_NOTE) {
                    PDFSetNote(2);
                    if (act_listener != null)
                        act_listener.onNoteCommited();
                } else {
                    PDFEndAnnot();
                }
            }
        });
        builder.setTitle(R.string.note_content);
        builder.setCancelable(false);
        builder.setView(layout);

        if (m_status == STA_ANNOT) {
            content.setText(m_annot.GetPopupText());
        }

        AlertDialog dlg = builder.create();
        dlg.show();
    }


    /**
     * 手書き画像の数をreturn
     */
    public int getAnnotCount() {
        return m_doc.GetPage(m_pageno).GetAnnotCount();
    }

    /**
     * RemoveAllボタンが押下される時呼び出される
     */
    public void removeAllAnnot() {
        Page page = m_doc.GetPage(m_pageno);
        // Annotetion全て削除するまで
        while (page.GetAnnotCount() > 0) {
            Page.Annotation annot = page.GetAnnot(page.GetAnnotCount() - 1);
            annot.RemoveFromPage();
        }
        page.Close();

        // undoスタッククリア
        m_opstack = new PDFLayoutOPStack();

        //　画面を最新化
        VPage annot_page = m_layout.vGetPage(m_pageno);
        m_layout.vRenderSync(annot_page);
        invalidate();
        if (m_listener != null)
            m_listener.OnPDFPageModified(annot_page.GetPageNo());
        PDFEndAnnot();
    }

    /**
     * 現在のページ番号をreturnする
     *
     * @return m_pageno
     */
    public int getPageno() {
        return m_pageno;
    }

    /**
     * 1ページ前に戻る
     */
    public void gotoPrevPage() {
        PDFGotoPage(m_pageno - 1);
    }

    /**
     * 1ページ分進む
     */
    public void gotoNextPage() {
        PDFGotoPage(m_pageno + 1);
    }

    public void setReadOnlyStatus(boolean readOnly) {
        m_readOnly = readOnly;
    }

    public boolean getReadOnlyStatus() {
        return m_readOnly;
    }

    private void initTimePaint() {
        time_paint = new Paint();

        time_paint.setAntiAlias(true);
        time_paint.setColor(Color.RED);
        time_paint.setTextSize(20);
    }

    /**
     * 手書きチェックの画像に時間を登録
     *
     * @return　time　時、分、秒の値
     */
    private String getTime() {
        Calendar cal = Calendar.getInstance();
        String ymd = GstmUtil.getCurrentDate("/", 0);
        String time = "";
        if (cal.get(Calendar.HOUR_OF_DAY) < 10)
            time += "0" + cal.get(Calendar.HOUR_OF_DAY) + ":";
        else
            time += cal.get(Calendar.HOUR_OF_DAY) + ":";

        if (cal.get(Calendar.MINUTE) < 10)
            time += "0" + cal.get(Calendar.MINUTE) + ":";
        else
            time += cal.get(Calendar.MINUTE) + ":";

        if (cal.get(Calendar.SECOND) < 10)
            time += "0" + cal.get(Calendar.SECOND);
        else
            time += cal.get(Calendar.SECOND);

        return ymd + " " + time;
    }

    public void setActListener(ViewerCallback actListener) {
        act_listener = actListener;
    }

    public void setStrokeDash(int code, int annotCode, float[] rects) {
        if (code != 1) return;
        if (rects == null) return;

        VPage vpage = m_layout.vGetPage(m_pageno);
        Page page = m_doc.GetPage(vpage.GetPageNo());
        if (page != null) {
            Page.Annotation annot = page.GetAnnot(page.GetAnnotCount() - 1);
            if (annot != null) {

                int strokeDashKbn = -1;
                if (annotCode == 0) {
                    strokeDashKbn = ViewerConstants.ANNOT_LINE_STROKE_DASH;
                } else if (annotCode == 1) {
                    strokeDashKbn = ViewerConstants.ANNOT_RECT_STROKE_DASH;
                } else {
                    strokeDashKbn = ViewerConstants.ANNOT_OVAL_STROKE_DASH;
                }

                if (strokeDashKbn == 1) {
                    annot.SetStrokeDash(new float[]{4f, 2f});
                } else {
                    annot.SetStrokeDash(new float[]{Float.MAX_VALUE});
                }

                m_layout.vRenderSync(vpage);
                if (m_listener != null)
                    m_listener.OnPDFPageModified(m_pageno);
                invalidate();
            }
        }

//        if (m_annot != null) {
//            if (m_annot.GetType() != 4) return;
//
//            if (dash) {
//                m_annot.SetStrokeDash(new float[]{4f, 2f});
//            } else {
//                m_annot.SetStrokeDash(new float[]{Float.MAX_VALUE});
//            }
//
//            VPage annot_page = m_layout.vGetPage(m_pageno);
//            m_layout.vRenderSync(annot_page);
//            if (m_listener != null)
//                m_listener.OnPDFPageModified(m_pageno);
//            invalidate();
//        }
    }

    public void setCheckMarkInvisible() {
        isNewCheckMark = false;
        m_timeStamp = null;
    }

    public void setCheckEnd() {
        PDFSetCheckPre(1);
        m_gstm_ink_status = 0;
    }
}
