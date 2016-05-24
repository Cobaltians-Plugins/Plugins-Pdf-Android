package io.kristal.pdfplugin.pdfRenderer;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;

import org.cobaltians.cobalt.Cobalt;

import io.kristal.pdfplugin.PdfPlugin;
import io.kristal.pdfplugin.R;

/**
 * FullScreenPdfActivity
 * A basic activity containing a Pdf rendering fragment
 * Created by Roxane P. on 5/19/16.
 */
public class FullScreenActivity extends AppCompatActivity {
    private PdfContainerFragment pdfContainerFragment;
    private float x1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add layout to Activity
        this.setContentView(R.layout.pdf_activity);
        // Create fragment and commit
        pdfContainerFragment = new PdfContainerFragment();
        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, pdfContainerFragment, null)
                        .commit();
            }
        }
    }

    /**
     * onTouchEvent
     * Catch gesture for PDF page navigation
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            // when user first touches the screen we get x and y coordinate
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                float x2 = event.getX();
                //if left to right sweep event on screen
                if (x1 < x2) {
                    if (Cobalt.DEBUG) Log.d(PdfPlugin.TAG, "Left to Right Swipe, showing previous pdf page");
                    pdfContainerFragment.prevPdfPage();
                }
                // if right to left sweep event on screen
                else if (x1 > x2) {
                    if (Cobalt.DEBUG) Log.d(PdfPlugin.TAG, "Right to Left Swipe, showing next pdf page");
                    pdfContainerFragment.nextPdfPage();
                }
                break;
            }
        return false;
    }

}
