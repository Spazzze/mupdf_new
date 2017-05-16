package com.example.mupdf_new_example;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.artifex.mupdf.android.DocActivityView;

public class DocViewActivity extends Activity {
    private DocActivityView mDocActivityView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //  get the file path
        Uri uri = getIntent().getData();
        final String path = Uri.decode(uri.getEncodedPath());

        //  set up UI
        setContentView(R.layout.activity_doc_view);
        mDocActivityView = (DocActivityView) findViewById(R.id.doc_view);

        //  Go!
        mDocActivityView.start(path);

    }

    @Override
    public void finish() {
        //  stop the view
        mDocActivityView.stop();

        super.finish();
    }
}
