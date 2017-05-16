package com.artifex.mupdf.android;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.artifex.mupdf.fitz.Document;
import com.artifex.mupdf.fitz.R;

public class DocActivityView extends FrameLayout implements View.OnClickListener, DocView.SelectionChangeListener {
    /*
    This class displays a DocView, and optionally, a DocListPagesView to its right.
	Both views show the document content, but the DocListPagesView is narrow,
	and always shows one column.

	There is logic here, and in Docview, DocViewBase and DocListPagesView,
	to keep them both in sync, as follows:

	When the DocListPagesView is showing, and the user taps on one of its pages,
	that page is highlighted, and the DocView on the left is auto-scrolled to bring that page into view.
	When the user scrolls the DocListPagesView, nothing more is done.

	When the user scrolls the DocView on the left, a "most visible" page is determined, and the
	DocListPagesView is auto-scrolled to bring that page into view, and that page is
	highlighted.
	*/

    private DocView mDocView;
    private DocListPagesView mDocPagesView;

    private DocReflowView mDocReflowView;

    private Context mContext;

    public DocActivityView(Context context) {
        super(context);
        mContext = context;
    }

    public DocActivityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public DocActivityView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    protected boolean usePagesView() {
        return true;
    }

    protected void showPages() {
        LinearLayout pages = (LinearLayout) findViewById(R.id.pages_container);
        if (null == pages)
            return;

        if (pages.getVisibility() == View.VISIBLE)
            return;

        pages.setVisibility(View.VISIBLE);

        final ViewTreeObserver observer = mDocView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeOnGlobalLayoutListener(this);
                mDocView.onShowPages();
            }
        });

        final ViewTreeObserver observer2 = getViewTreeObserver();
        observer2.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer2.removeOnGlobalLayoutListener(this);
                mDocPagesView.onOrientationChange();
                int page = mDocView.getMostVisiblePage();
                mDocPagesView.setCurrentPage(page);
                mDocPagesView.scrollToPage(page);
            }
        });
    }

    protected void hidePages() {
        LinearLayout pages = (LinearLayout) findViewById(R.id.pages_container);
        if (null == pages)
            return;

        if (pages.getVisibility() == View.GONE)
            return;

        pages.setVisibility(View.GONE);
        ViewTreeObserver observer = mDocView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mDocView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mDocView.onHidePages();
            }
        });
    }

    //  called from the main DocView whenever the pages list needs to be updated.
    public void setCurrentPage(int pageNumber) {
        if (usePagesView()) {
            //  set the new current page and scroll there.
            mDocPagesView.setCurrentPage(pageNumber);
            mDocPagesView.scrollToPage(pageNumber);
        }
    }

    public boolean showKeyboard() {
        //  show keyboard
        InputMethodManager im = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        im.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

        return true;
    }

    public void hideKeyboard() {
        //  hide the keyboard
        InputMethodManager im = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(mDocView.getWindowToken(), 0);
    }

    private boolean started = false;

    public void start(final String path) {
        started = false;

        ((Activity) getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        ((Activity) getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        //  inflate the UI
        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final LinearLayout view = (LinearLayout) inflater.inflate(R.layout.doc_view, null);

        final ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!started) {
                    afterFirstLayoutComplete(path);
                    started = true;
                }
            }
        });

        addView(view);
    }

    public void afterFirstLayoutComplete(final String path) {
        //  main view
        mDocView = (DocView) findViewById(R.id.doc_view_inner);
        mDocView.setHost(this);
        mDocReflowView = (DocReflowView) findViewById(R.id.doc_reflow_view);

        //  page list
        if (usePagesView()) {
            mDocPagesView = new DocListPagesView(getContext());
            mDocPagesView.setSelectionListener(new DocListPagesView.SelectionListener() {
                @Override
                public void onPageSelected(int pageNumber) {
                    mDocView.scrollToPage(pageNumber);
                }
            });

            LinearLayout layout2 = (LinearLayout) findViewById(R.id.pages_container);
            layout2.addView(mDocPagesView);
        }

        //  selection handles
        View v = findViewById(R.id.doc_wrapper);
        RelativeLayout layout = (RelativeLayout) v;
        mDocView.setupHandles(layout);

        //  watch for a possible orientation change
        ViewTreeObserver observer3 = getViewTreeObserver();
        observer3.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                onPossibleOrientationChange();
            }
        });

        mDoc = new Document(path);

        if (mDoc.needsPassword()) {
            askForPassword();
        } else {
            afterPassword();
        }
    }

    private static final int ORIENTATION_PORTAIT = 1;
    private static final int ORIENTATION_LANDSCAPE = 2;
    private int mLastOrientation = 0;

    private void onPossibleOrientationChange() {
        //  get current orientation
        Point p = Utilities.getRealScreenSize((Activity) mContext);
        int orientation = ORIENTATION_PORTAIT;
        if (p.x > p.y)
            orientation = ORIENTATION_LANDSCAPE;

        //  see if it's changed
        if (orientation != mLastOrientation)
            onOrientationChange();

        mLastOrientation = orientation;
    }

    private void onOrientationChange() {
        mDocView.onOrientationChange();

        if (usePagesView()) {
            mDocPagesView.onOrientationChange();
        }
    }

    private Document mDoc;

    private void askForPassword() {
        Utilities.passwordDialog((Activity) getContext(), new Utilities.passwordDialogListener() {
            @Override
            public void onOK(String password) {
                //  yes
                boolean ok = mDoc.authenticatePassword(password);
                if (ok) {
                    afterPassword();
                    mDocView.requestLayout();
                } else {
                    askForPassword();
                }
            }

            @Override
            public void onCancel() {
                mDoc.destroy();
            }
        });
    }

    private void afterPassword() {
        //  start the views
        mDocView.start(mDoc);
        if (usePagesView()) {
            mDocPagesView.clone(mDocView);
        }

        mDocView.setSelectionChangeListener(this);
        onSelectionChanged();
    }

    public void stop() {
        mDocView.finish();
        if (usePagesView()) {
            mDocPagesView.finish();
        }
    }

    @Override
    public void onClick(View v) {
/*        if (v == mReflowButton)
            onReflowButton();

        if (v == mSearchButton)
            onShowSearch();
        if (v == mSearchText)
            onEditSearchText();
        if (v == mSearchNextButton)
            onSearchNextButton();
        if (v == mSearchPreviousButton)
            onSearchPreviousButton();*/

    }
/*
    public void onSearchNextButton() {
        hideKeyboard();
        mDocView.onSearchNext(mSearchText.getText().toString());
    }

    public void onSearchPreviousButton() {
        hideKeyboard();
        mDocView.onSearchPrevious(mSearchText.getText().toString());
    }

    public void onEditSearchText() {
        mSearchText.requestFocus();
        showKeyboard();
    }

    private void goToPage(int pageNumber) {
        mDocView.scrollToPage(pageNumber);

        if (mDocReflowView.getVisibility() == View.VISIBLE) {
            setReflowText(pageNumber);
            mDocPagesView.setCurrentPage(pageNumber);
        }
    }

    private void onReflowButton() {
        if (mDocView.getVisibility() == View.VISIBLE) {
            //  set initial text into reflow view
            setReflowText(mDocPagesView.getMostVisiblePage());

            //  show reflow
            showReflow();
        } else {
            //  hide reflow
            hideReflow();
        }
    }*/

    private void showReflow() {
        mDocView.setVisibility(View.GONE);
        mDocReflowView.setVisibility(View.VISIBLE);
    }

    private void hideReflow() {
        mDocReflowView.setVisibility(View.GONE);
        mDocView.setVisibility(View.VISIBLE);
    }

    private void setReflowText(int pageNumber) {
        DocPageView dpv = (DocPageView) mDocView.getAdapter().getView(pageNumber, null, null);
        byte bytes[] = dpv.getPage().textAsHtml();
        mDocReflowView.setHTML(bytes);
    }

    public void onSelectionChanged() {
    }
}
