package com.utshob.mybikes.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class ImageUtil {
    private ImageUtil() {}

    /** Shrinks a picked photo to max 1280 px and returns JPEG bytes (faster, cheaper upload). */
    public static byte[] compress(Context ctx, Uri uri) throws IOException {
        ImageDecoder.Source src = ImageDecoder.createSource(ctx.getContentResolver(), uri);
        Bitmap bmp = ImageDecoder.decodeBitmap(src, (decoder, info, s) -> {
            int w = info.getSize().getWidth();
            int h = info.getSize().getHeight();
            int max = Math.max(w, h);
            if (max > 1280) {
                float f = 1280f / max;
                decoder.setTargetSize(Math.round(w * f), Math.round(h * f));
            }
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
        return out.toByteArray();
    }

    /** Saves the picked photo inside the app's private storage and returns the file name. */
    public static String saveToDevice(Context ctx, Uri uri) throws IOException {
        byte[] data = compress(ctx, uri);
        File dir = new File(ctx.getFilesDir(), "bike_photos");
        if (!dir.exists()) dir.mkdirs();
        String name = "bike_" + System.currentTimeMillis() + ".jpg";
        try (FileOutputStream out = new FileOutputStream(new File(dir, name))) {
            out.write(data);
        }
        return name;
    }

    public static File file(Context ctx, String name) {
        return new File(new File(ctx.getFilesDir(), "bike_photos"), name);
    }

    public static void delete(Context ctx, String name) {
        if (name == null || name.isEmpty() || name.startsWith("http")) return;
        File f = file(ctx, name);
        if (f.exists()) f.delete();
    }
}
