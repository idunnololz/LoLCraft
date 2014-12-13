package com.ggstudios.lolcraft;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import com.ggstudios.utils.DiskLruImageCache;
import com.ggstudios.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Executor;

import timber.log.Timber;

public class SplashFetcher {
	private static final String TAG = "SplashFetcher";

	private static final String CACHE_NAME = "SplashCache";

	private static SplashFetcher instance;

	private DiskLruImageCache diskCache = null;

	private Object cacheLock = new Object();
	private Context context;

	private SplashFetcher(final Context context) {
		this.context = context;
	}

	private void initialize() {
		diskCache = new DiskLruImageCache(context, CACHE_NAME, 
				Utils.MB_BYTES * 10, CompressFormat.JPEG, 70);
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
        if (diskCache != null) {
            if (diskCache.isInErrorState()) return;
            final String sanatizedKey = key.toLowerCase(Locale.US);

            if (diskCache.containsKey(sanatizedKey)) {
                diskCache.remove(sanatizedKey);
            }
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

                final String sanatizedKey = key.toLowerCase(Locale.US);

                if (diskCache.containsKey(sanatizedKey)) {
                    diskCache.remove(sanatizedKey);
                }

                return null;
            }

        };

        Utils.executeAsyncTask(task, key);
    }

    public FetchToken fetchChampionSplash(final String key, int reqWidth, int reqHeight, final OnDrawableRetrievedListener listener) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            return fetchChampionSplash(AsyncTask.THREAD_POOL_EXECUTOR, key, reqWidth, reqHeight, listener);
        } else {
            return fetchChampionSplash(null, key, reqWidth, reqHeight, listener);
        }
    }

    public FetchToken fetchChampionSplash(Executor executor, final String key, int reqWidth, int reqHeight, final OnDrawableRetrievedListener listener) {
		AsyncTask<Object, Void, Object> task = new AsyncTask<Object, Void, Object>(){

            String sanatizedKey;

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

                sanatizedKey = key.toLowerCase(Locale.US);

				if (diskCache.containsKey(sanatizedKey)) {
					return new BitmapDrawable(context.getResources(), 
							diskCache.getBitmap(sanatizedKey));
				}

                if (isCancelled()) {
                    return 0;
                }
				
				int reqWidth = (Integer) params[1];
				int reqHeight = (Integer) params[2];

				InputStream stream = null;
				try {

					URL url = new URL("http://ddragon.leagueoflegends.com/cdn/img/champion/splash/" 
							+ params[0] + "_0.jpg");

					Bitmap bmp = decodeSampledBitmapFromResource(url, reqWidth, reqHeight);
                    if (isCancelled()) {
                        return -1;
                    }
                    if (bmp != null) {
                        diskCache.put(sanatizedKey, bmp);
                        return new BitmapDrawable(context.getResources(), bmp);
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
                } finally {
					if (stream != null) {
						try {
							stream.close();
						} catch (IOException e) {
							Timber.e("", e);
						}
					}
				}
				return 0;
			}

            protected void onCancelled(Object d) {
                if (d instanceof Integer) {
                    int code = (Integer) d;
                    if (code < 0) {
                        // remove the bitmap if we were interrupted to prevent corruption...
                        deleteCache(sanatizedKey);
                    }
                }
            }

			protected void onPostExecute(Object d) {
                if (d instanceof Drawable) {
                    listener.onDrawableRetrieved((Drawable) d);
                }
			}

		};

		Utils.executeAsyncTaskOnExecutor(executor, task, key, reqWidth, reqHeight);
        return new FetchToken(task);
	}

	public static interface OnDrawableRetrievedListener {
		public void onDrawableRetrieved(Drawable d);
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
}
