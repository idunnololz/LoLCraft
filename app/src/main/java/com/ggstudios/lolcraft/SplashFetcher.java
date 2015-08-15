package com.ggstudios.lolcraft;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;

import com.ggstudios.utils.DiskLruImageCache;
import com.ggstudios.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import timber.log.Timber;

public class SplashFetcher {
	private static final String TAG = "SplashFetcher";

	private static final String CACHE_NAME = "SplashCache";

    private static final int DISK_CACHE_SIZE = Utils.MB_BYTES * 64;

	private static SplashFetcher instance;

	private LruCache<String, Bitmap> memoryCache = null;
	private DiskLruImageCache diskCache = null;

	private final Object cacheLock = new Object();
	private Context context;

	private SplashFetcher(final Context context) {
		this.context = context;
	}

	private void initialize() {
		diskCache = new DiskLruImageCache(context, CACHE_NAME,
                DISK_CACHE_SIZE, CompressFormat.JPEG, 70);


        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 4;
        Timber.d("Memory cache size:" + cacheSize);

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            protected int sizeOf(String key, Bitmap value) {
                return (int) (getSizeInBytes(value) / 1024);
            }
        };
	}

    private static long getSizeInBytes(Bitmap bitmap) {
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

	public static void initInstance(Context context) {
		instance = new SplashFetcher(context);
	}

	public static SplashFetcher getInstance() {
		return instance;
	}

	public static int calculateInSampleSize(
			BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {

			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight
					&& (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromResource(URL url,
			int reqWidth, int reqHeight) throws IOException {
		
		InputStream stream = url.openConnection().getInputStream();

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeStream(stream, null, options);
		stream.close();

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		
		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		stream = url.openConnection().getInputStream();
		
		Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
		stream.close();
		
		return bitmap;
	}

    public void deleteCache(final String key) {
        if (memoryCache != null) {
            memoryCache.remove(key);
        }

        if (diskCache != null) {
            if (diskCache.isInErrorState()) return;
            final String sanitizedKey = key.toLowerCase(Locale.US);

            if (diskCache.containsKey(sanitizedKey)) {
                diskCache.remove(sanitizedKey);
            }
            return;
        }

        AsyncTask<Object, Void, Void> task = new AsyncTask<Object, Void, Void>(){

            @Override
            protected Void doInBackground(Object... params) {
                if (diskCache == null) {
                    synchronized(cacheLock) {
                        if (diskCache == null) {
                            initialize();
                        }
                    }
                }

                if (diskCache.isInErrorState()) return null;

                final String sanitizedKey = key.toLowerCase(Locale.US);

                if (diskCache.containsKey(sanitizedKey)) {
                    diskCache.remove(sanitizedKey);
                }

                return null;
            }

        };

        Utils.executeAsyncTask(task, key);
    }

    public FetchToken fetchChampionSplash(final String key, int reqWidth, int reqHeight, final OnDrawableRetrievedListener listener) {
        if (memoryCache != null) {
            Bitmap cached = memoryCache.get(key);
            if (cached != null) {
                listener.onDrawableRetrieved(new BitmapDrawable(context.getResources(), cached));
                return NO_OP_FETCH_TOKEN;
            }
        }
        AsyncTask<Object, Void, Object> task = new AsyncTask<Object, Void, Object>(){

            String sanitizedKey;

			@Override
			protected Object doInBackground(Object... params) {
				if (diskCache == null) {
					synchronized(cacheLock) {
						if (diskCache == null) {
							initialize();
						}
					}
				}

                if (isCancelled()) {
                    return 0;
                }

				if (diskCache.isInErrorState()) return 0;

                sanitizedKey = key.toLowerCase(Locale.US);

				if (diskCache.containsKey(sanitizedKey)) {
					return diskCache.getBitmap(sanitizedKey);
				}

                if (isCancelled()) {
                    return 0;
                }
				
				int reqWidth = (Integer) params[1];
				int reqHeight = (Integer) params[2];

				try {

					URL url = new URL("http://ddragon.leagueoflegends.com/cdn/img/champion/splash/" 
							+ params[0] + "_0.jpg");

					Bitmap bmp = decodeSampledBitmapFromResource(url, reqWidth, reqHeight);
                    if (isCancelled()) {
                        return -1;
                    }
                    if (bmp != null) {
                        diskCache.put(sanitizedKey, bmp);
                        return bmp;
                    }
				} catch (MalformedURLException e) {
					Timber.e("", e);
                    return -1;
				} catch (InterruptedIOException e) {
                    Timber.d("Handled InterruptedIOException");
                    return -1;
                } catch (IOException e) {
					Timber.e("", e);
                    return -1;
				} catch (OutOfMemoryError e) {
                    Timber.e("", e);
                    return -1;

                    // free some memory...
                } catch (Exception e) {
                    Timber.e("", e);
                }
				return 0;
			}

            protected void onCancelled(Object d) {
                if (d instanceof Integer) {
                    int code = (Integer) d;
                    if (code < 0) {
                        // remove the bitmap if we were interrupted to prevent corruption...
                        deleteCache(sanitizedKey);
                    }
                }
            }

			protected void onPostExecute(Object d) {
                if (d instanceof Bitmap) {
                    Bitmap bmp = (Bitmap) d;
                    memoryCache.put(key, bmp);
                    listener.onDrawableRetrieved(new BitmapDrawable(context.getResources(), bmp));
                }
			}

		};

		Utils.executeAsyncTaskOnExecutor(THREAD_POOL_EXECUTOR, task, key, reqWidth, reqHeight);
        return new FetchToken(task);
	}

	public interface OnDrawableRetrievedListener {
		void onDrawableRetrieved(Drawable d);
	}

    public static class FetchToken {
        private AsyncTask task;

        private FetchToken(AsyncTask task) {
            this.task = task;
        }

        public void cancel() {
            task.cancel(true);
        }
    }

    private static final FetchToken NO_OP_FETCH_TOKEN = new FetchToken(null) {
        @Override
        public void cancel() {/* Do nothing */}
    };

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT == 1 ? 1 : CPU_COUNT - 1;
    private static final int MAXIMUM_POOL_SIZE = CORE_POOL_SIZE + 1;
    private static final int KEEP_ALIVE = 1;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>();

    /**
     * An {@link java.util.concurrent.Executor} that can be used to execute tasks in parallel.
     */
    public static final Executor THREAD_POOL_EXECUTOR
            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
}
