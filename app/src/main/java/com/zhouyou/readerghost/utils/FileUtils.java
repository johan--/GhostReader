package com.zhouyou.readerghost.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.folioreader.util.AppUtil;
import com.folioreader.util.EpubManipulator;
import com.folioreader.util.FileUtil;
import com.zhouyou.readerghost.ui.ReaderActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.siegmann.epublib.domain.Book;

/**
 * 作者：ZhouYou
 * 日期：2017/1/12.
 */
public class FileUtils {

    private static final String TAG = FileUtil.class.getSimpleName();
    private static final String FOLIO_READER_ROOT = "/folioreader/";

    public static Book saveEpubFile(final Context context, ReaderActivity.EpubSourceType epubSourceType, String epubFilePath, int epubRawId, String epubFileName) {
        String filePath;
        InputStream epubInputStream;
        Book book = null;
        boolean isFolderAvalable;
        try {
            isFolderAvalable = isFolderAvailable(epubFileName);
            filePath = getFolioEpubFilePath(epubSourceType, epubFilePath, epubFileName);

            if (!isFolderAvalable) {
                if (epubSourceType.equals(ReaderActivity.EpubSourceType.RAW)) {
                    epubInputStream = context.getResources().openRawResource(epubRawId);
                    saveTempEpubFile(filePath, epubFileName, epubInputStream);
                } else if (epubSourceType.equals(ReaderActivity.EpubSourceType.ASSESTS)) {
                    AssetManager assetManager = context.getAssets();
                    epubInputStream = assetManager.open(epubFilePath);
                    saveTempEpubFile(filePath, epubFileName, epubInputStream);
                } else {
                    filePath = epubFilePath;
                }

                new EpubManipulator(filePath, epubFileName, context);
                book = AppUtil.saveBookToDb(filePath, epubFileName, context);
            } else {
                EpubManipulator epubManipulator= new EpubManipulator(filePath, epubFileName, context);
                book = epubManipulator.getEpubBook();
            }
            return book;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        return book;
    }

    public static String getFolioEpubFolderPath(String epubFileName) {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + FOLIO_READER_ROOT + "/" + epubFileName;
    }

    public static String getFolioEpubFilePath(ReaderActivity.EpubSourceType sourceType, String epubFilePath, String epubFileName) {
        if (ReaderActivity.EpubSourceType.SD_CARD.equals(sourceType)) {
            return epubFilePath;
        } else {
            return getFolioEpubFolderPath(epubFileName) + "/" + epubFileName + ".epub";
        }
    }

    private static boolean isFolderAvailable(String epubFileName) {
        File file = new File(getFolioEpubFolderPath(epubFileName));
        return file.isDirectory();
    }

    public static String getEpubFilename(Context context, ReaderActivity.EpubSourceType epubSourceType,
                                         String epubFilePath, int epubRawId) {
        String epubFileName;
        if (epubSourceType.equals(ReaderActivity.EpubSourceType.RAW)) {
            Resources res = context.getResources();
            epubFileName = res.getResourceEntryName(epubRawId);
        } else {
            String[] temp = epubFilePath.split("/");
            epubFileName = temp[temp.length - 1];
            int fileMaxIndex = epubFileName.length();
            epubFileName = epubFileName.substring(0, fileMaxIndex - 5);
        }

        return epubFileName;
    }

    public static Boolean saveTempEpubFile(String filePath, String fileName, InputStream inputStream) {
        OutputStream outputStream = null;
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                File folder = new File(getFolioEpubFolderPath(fileName));
                folder.mkdirs();

                outputStream = new FileOutputStream(file);
                int read = 0;
                byte[] bytes = new byte[inputStream.available()];

                while ((read = inputStream.read(bytes)) != -1) {
                    outputStream.write(bytes, 0, read);
                }
            } else {
                return true;
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
        return false;
    }
}
