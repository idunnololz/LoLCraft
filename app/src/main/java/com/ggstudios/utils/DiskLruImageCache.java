package com.ggstudios.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.jakewharton.disklrucache.DiskLruCache;

public class DiskLruImageCache {
	private static final int IO_BUFFER_SIZE = 1024 * 1024 * 2; // 2 MB
	private static final int APP_VERSION = 1;
	private static final int VALUE_COUNT = 1;

	private final DiskLruCache mDiskCache;
	private CompressFormat mCompressFormat = CompressFormat.JPEG;
	private int mCompressQuality = 70;

	public DiskLruImageCache(Context context, String cacheName, int diskCacheSize,
                             CompressFormat compressFormat, int quality) {
		try {
			final File diskCacheDir = new File(context.getExternalFilesDir(null), cacheName);
			mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
			mCompressFormat = compressFormat;
			mCompressQuality = quality;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor) throws IOException {
		OutputStream out = null;
		try {
			out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
			return bitmap.compress(mCompressFormat, mCompressQuality, out);
		} finally {
			if (out != null) {
				out.close();
			}
		}
	}

	public void put(String key, @NonNull Bitmap data) {
		DiskLruCache.Editor editor = null;
		try {
			editor = mDiskCache.edit(key);
			if (editor == null) {
				return;
			}

			if (writeBitmapToFile(data, editor)) {
				mDiskCache.flush();
				editor.commit();
			} else {
				editor.abort();
			}
		} catch (IOException e) {
			try {
				if (editor != null) {
					editor.abort();
				}
			} catch (IOException ioe) {/* Ignore */}
		}
	}

	@Nullable
	public Bitmap getBitmap(String key) {
		Bitmap bitmap = null;
		DiskLruCache.Snapshot snapshot = null;
		try {
			snapshot = mDiskCache.get(key);
			if (snapshot == null) {
				return null;
			}
			final InputStream in = snapshot.getInputStream(0);
			if (in != null) {
				final BufferedInputStream buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
				bitmap = BitmapFactory.decodeStream(buffIn);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (snapshot != null) {
				snapshot.close();
			}
		}
		return bitmap;
	}

	public void remove(String key) {
		try {
			mDiskCache.remove(key);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean containsKey(String key) {
		boolean contained = false;
		DiskLruCache.Snapshot snapshot = null;
		try {
			snapshot = mDiskCache.get(key);
			contained = snapshot != null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (snapshot != null) {
				snapshot.close();
			}
		}

		return contained;
	}

	public void clearCache() {
		try {
			mDiskCache.delete();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public File getCacheFolder() {
		return mDiskCache.getDirectory();
	}
}