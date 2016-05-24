package io.kristal.pdfplugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Tools
 * Some basic function used to hamdle filesystem operation
 * Created by Roxane P. on 5/23/16.
 */
public class Tools {

    /**
     * createFolder
     * create PDF folder if not exist yet
     * @param path path of the directory to create
     * @return the created file (a directory)
     */
    public static File createFolder(String path) {
        File fileDir = new File(path);
        // noinspection ResultOfMethodCallIgnored
        fileDir.mkdirs();
        return fileDir;
    }

    /**
     * copyFile
     * Copy a buffer to another buffer
     * @param in Input buffer to copy from
     * @param out Output buffer to paste to
     */
    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1)
        {
            out.write(buffer, 0, read);
        }
    }

    /**
     * deleteFile
     * delete file or directory
     * @param path path of the file to delete
     * @return true if the file not exist anymore, false instead
     */
    public static boolean deleteFile(String path) {
        if (stringIsBlank(path)) return true;
        File file = new File(path);
        if (!file.exists()) {
            return true;
        }
        if (file.isFile()) {
            return file.delete();
        }
        if (!file.isDirectory()) {
            return false;
        }
        for (File f : file.listFiles()) {
            if (f.isFile()) {
                // noinspection ResultOfMethodCallIgnored
                f.delete();
            } else if (f.isDirectory()) {
                deleteFile(f.getAbsolutePath());
            }
        }
        return file.delete();
    }

    /**
     * return true is str is empty or null
     * @param str processed string
     */
    public static boolean stringIsBlank(String str) {
        return (str == null || str.trim().length() == 0);
    }
}
