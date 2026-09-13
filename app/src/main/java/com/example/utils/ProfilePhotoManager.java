package com.example.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Utility to manage camera capture, EXIF rotation correction, 1:1 square avatar cropping,
 * local persistent storage, and high-performance display for Student and Teacher profile photos.
 */
public class ProfilePhotoManager {

    private static final String TAG = "ProfilePhotoManager";
    private static final String PHOTOS_DIR = "profile_photos";
    private static final int TARGET_AVATAR_SIZE = 512;

    /**
     * Creates a content URI in the app cache directory for the camera capture intent.
     */
    public static Uri createTempCaptureUri(Context context) {
        try {
            File cacheDir = new File(context.getCacheDir(), "camera");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            File tempFile = new File(cacheDir, "temp_avatar_" + System.currentTimeMillis() + ".jpg");
            if (tempFile.exists()) {
                tempFile.delete();
            }
            String authority = context.getPackageName() + ".fileprovider";
            return FileProvider.getUriForFile(context, authority, tempFile);
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp camera capture URI", e);
            return null;
        }
    }

    /**
     * Processes captured camera photo: corrects EXIF orientation, crops to square,
     * scales to TARGET_AVATAR_SIZE, and persists to internal files directory.
     *
     * @return The absolute path of the saved avatar, or null if failed.
     */
    public static String processAndSaveAvatar(Context context, Uri sourceUri, String rolePrefix, String identifier) {
        if (context == null || sourceUri == null) {
            return null;
        }

        InputStream inputStream = null;
        try {
            // 1. Decode bounds and calculate sample size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;
            if (srcWidth <= 0 || srcHeight <= 0) {
                Log.e(TAG, "Invalid image dimensions: " + srcWidth + "x" + srcHeight);
                return null;
            }

            int inSampleSize = 1;
            int maxDim = Math.max(srcWidth, srcHeight);
            while (maxDim / (inSampleSize * 2) >= TARGET_AVATAR_SIZE) {
                inSampleSize *= 2;
            }

            // 2. Decode sampled bitmap
            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            inputStream = context.getContentResolver().openInputStream(sourceUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap from camera URI");
                return null;
            }

            // 3. Check EXIF orientation and apply rotation matrix
            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try {
                InputStream exifStream = context.getContentResolver().openInputStream(sourceUri);
                if (exifStream != null) {
                    ExifInterface exif = new ExifInterface(exifStream);
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    exifStream.close();
                }
            } catch (Throwable t) {
                Log.w(TAG, "Could not read EXIF orientation: " + t.getMessage());
            }

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.postScale(-1, 1);
                    break;
                default:
                    break;
            }

            if (!matrix.isIdentity()) {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                    bitmap = rotated;
                }
            }

            // 4. Center-crop to 1:1 square
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int squareSize = Math.min(width, height);
            int cropX = (width - squareSize) / 2;
            int cropY = (height - squareSize) / 2;

            Bitmap squareBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, squareSize, squareSize);
            if (squareBitmap != bitmap) {
                bitmap.recycle();
            }

            // 5. Scale to target avatar size if needed
            Bitmap finalAvatar;
            if (squareSize != TARGET_AVATAR_SIZE) {
                finalAvatar = Bitmap.createScaledBitmap(squareBitmap, TARGET_AVATAR_SIZE, TARGET_AVATAR_SIZE, true);
                if (finalAvatar != squareBitmap) {
                    squareBitmap.recycle();
                }
            } else {
                finalAvatar = squareBitmap;
            }

            // 6. Save to internal persistent storage
            File dir = new File(context.getFilesDir(), PHOTOS_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String safeId = (identifier != null ? identifier : "default")
                    .replaceAll("[^a-zA-Z0-9_-]", "_");
            File destFile = new File(dir, rolePrefix + "_" + safeId + ".jpg");

            FileOutputStream fos = new FileOutputStream(destFile);
            finalAvatar.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            finalAvatar.recycle();
            Log.d(TAG, "Profile avatar saved successfully at: " + destFile.getAbsolutePath());
            return destFile.getAbsolutePath();

        } catch (Exception e) {
            Log.e(TAG, "Error saving avatar", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Loads and displays the profile photo on the given ImageView.
     * If the photo exists and is valid, loads the bitmap and clears color filters.
     * Otherwise, sets the default placeholder and tints with primary theme color.
     */
    public static boolean displayProfilePhoto(Context context, String photoPathOrUri, ImageView imageView, int defaultPlaceholderRes) {
        if (imageView == null) return false;

        if (photoPathOrUri != null && !photoPathOrUri.trim().isEmpty()) {
            try {
                Bitmap bitmap = null;
                File file = new File(photoPathOrUri);
                if (file.exists() && file.length() > 0) {
                    bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                } else if (context != null && photoPathOrUri.startsWith("content://")) {
                    Uri uri = Uri.parse(photoPathOrUri);
                    InputStream is = context.getContentResolver().openInputStream(uri);
                    if (is != null) {
                        bitmap = BitmapFactory.decodeStream(is);
                        is.close();
                    }
                }

                if (bitmap != null) {
                    imageView.clearColorFilter();
                    imageView.setImageBitmap(bitmap);
                    return true;
                }
            } catch (Throwable t) {
                Log.w(TAG, "Failed to load profile photo from " + photoPathOrUri + ": " + t.getMessage());
            }
        }

        // Default placeholder fallback
        if (context != null) {
            imageView.setImageResource(defaultPlaceholderRes);
            imageView.setColorFilter(ContextCompat.getColor(context, R.color.primary));
        }
        return false;
    }

    /**
     * Deletes stored profile photo file for the specified role and identifier.
     */
    public static boolean deleteProfilePhoto(Context context, String rolePrefix, String identifier) {
        if (context == null) return false;
        try {
            File dir = new File(context.getFilesDir(), PHOTOS_DIR);
            String safeId = (identifier != null ? identifier : "default")
                    .replaceAll("[^a-zA-Z0-9_-]", "_");
            File file = new File(dir, rolePrefix + "_" + safeId + ".jpg");
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting avatar", e);
        }
        return false;
    }
}
