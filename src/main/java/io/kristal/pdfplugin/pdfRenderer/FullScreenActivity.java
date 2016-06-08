package io.kristal.pdfplugin.pdfRenderer;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import io.kristal.pdfplugin.R;

/**
 * FullScreenPdfActivity
 * A basic activity containing a Pdf rendering fragment
 * Created by Roxane P. on 5/19/16.
 */
public class FullScreenActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add layout to Activity
        this.setContentView(R.layout.pdf_activity);
        // Create fragment and commit
        PdfContainerFragment pdfContainerFragment = new PdfContainerFragment();
        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, pdfContainerFragment, null)
                        .commit();
            }
        }
    }
}
