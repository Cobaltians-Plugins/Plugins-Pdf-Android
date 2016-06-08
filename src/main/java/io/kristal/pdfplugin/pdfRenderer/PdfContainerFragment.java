package io.kristal.pdfplugin.pdfRenderer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.cobaltians.cobalt.Cobalt;

import java.io.File;
import java.io.IOException;

import io.kristal.pdfplugin.PdfPlugin;
import io.kristal.pdfplugin.R;

/**
 * PdfContainerFragment
 * This fragment shows PDF pages (need API > 21).
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PdfContainerFragment extends Fragment {
    private ParcelFileDescriptor mFileDescriptor; // File descriptor of the PDF
    private PdfRenderer.Page mCurrentPage;
    private float x1;

    boolean startlongpress = false;

    /**
     * {@link android.graphics.pdf.PdfRenderer} to render the PDF.
     */
    private PdfRenderer mPdfRenderer;
    /**
     * {@link android.widget.ImageView} that shows a PDF page as a {@link android.graphics.Bitmap}
     */
    private com.polites.android.GestureImageView mImageView;

    public PdfContainerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pdf_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Retain view references.
        mImageView = (com.polites.android.GestureImageView) view.findViewById(R.id.image);
        // Open the first(0) page of the PDF.
        showPage(0);
        /** Tap listener */
        // Thread needed to check the tap duration
        final Handler handler = new Handler();
        final Runnable mLongPressed = new Runnable() {
            public void run() {
                startlongpress = true; // recording a long press
            }
        };
        // Set touch listener to switch to prev/next page
        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int firstTier = mImageView.getWidth() / 3;
                final int secondTier = (mImageView.getWidth() / 3) * 2;
                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    handler.removeCallbacks(mLongPressed); // reset long press thread
                }
                if ((event.getAction() == MotionEvent.ACTION_MOVE) || (event.getAction() == MotionEvent.ACTION_UP)) {
                    x1 = event.getX();
                    if (startlongpress) {
                        // do switch page
                        if (x1 > 0 && x1 < firstTier) { // tapped on left side
                            prevPdfPage();
                            startlongpress = false;
                        } else if (x1 > firstTier && x1 < secondTier) { // tapped on middle
                            // do nothing: let gesture-imageView do this part of the job (drag && zoom)
                        } else if (x1 > secondTier && x1 < mImageView.getWidth()) { // tapped on right side
                            nextPdfPage();
                            startlongpress = false;
                        } else {
                            if (Cobalt.DEBUG) Log.d(PdfPlugin.TAG, "Dropped a tap with unknown location.");
                        }
                    }
                    handler.postDelayed(mLongPressed, 1000);
                }
                return false;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            openRenderer(); // onAttach: open Pdf Renderer
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error! " + e.getMessage(), Toast.LENGTH_SHORT).show();
            activity.finish();
        }
    }

    @Override
    public void onDetach() {
        try {
            closeRenderer(); // onDetach: close Pdf Renderer
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDetach();
    }

    /**
     * Sets up a {@link PdfRenderer} and related resources.
     */
    private void openRenderer() throws IOException {
        File file = new File(PdfPlugin.downloadedPdfPath, PdfPlugin.pdfFileName);
        if (file.exists()) {
            mFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            // This is the PdfRenderer we use to render the PDF.
            mPdfRenderer = new PdfRenderer(mFileDescriptor);
        } else {
            Log.e(PdfPlugin.TAG, "Can't read file at path " + file.getAbsolutePath());
        }
    }

    /**
     * Closes the {@link android.graphics.pdf.PdfRenderer} and related resources.
     * @throws java.io.IOException When the PDF file cannot be closed.
     */
    private void closeRenderer() throws IOException {
        if (null != mCurrentPage) {
            mCurrentPage.close();
        }
        mPdfRenderer.close();
        mFileDescriptor.close();
    }

    /**
     * Shows the specified page of PDF to the screen.
     * @param index The page index.
     */
    private void showPage(int index) {
        if (mPdfRenderer.getPageCount() <= index) {
            return;
        }
        if (null != mCurrentPage) {
            mCurrentPage.close(); // Close the current page before opening another one.
        }
        // Open a specific page in PDF.
        mCurrentPage = mPdfRenderer.openPage(index);
        Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(),
                Bitmap.Config.ARGB_8888);
        // Render the page onto the Bitmap.
        mCurrentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        // Show Bitmap to user.
        mImageView.setImageBitmap(bitmap);
        updateUi();
    }

    /**
     * Navigate PDF to the previous page
     */
    private void nextPdfPage() {
        int next = mCurrentPage.getIndex() + 1;
        if (next < getPageCount() && next >= 0) {
            showPage(next);
        }
    }

    /**
     * Navigate PDF to the previous page
     */
    private void prevPdfPage() {
        int prev = mCurrentPage.getIndex() - 1;
        if (prev < getPageCount() && prev >= 0) {
            showPage(prev);
        }
    }

    /**
     * Show a simple toast with page number
     */
    private void updateUi() {
        int currentPage = mCurrentPage.getIndex() + 1;
        String str = "Page " + currentPage + "/" + getPageCount();
        Toast.makeText(getActivity(), str, Toast.LENGTH_SHORT).show();
    }

    /**
     * Gets the number of pages in the PDF.
     * @return The number of pages.
     */
    private int getPageCount() {
        return mPdfRenderer.getPageCount();
    }
}
