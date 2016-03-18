/***
 * Copyright (c) 2015 CommonsWare, LLC
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.commonsware.cwac.cam2;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import java.io.IOException;


public class ConfirmationFragment extends Fragment {
	private ImageView iv;
	private ImageContext imageContext;
	private Uri videoUri;
	private Uri imageUri;
	private VideoView mVideoView;
	private RelativeLayout mRelativeLayout;
	private MediaController mMediaController;
	private Bitmap previewBitmap;
	private Bitmap adjustedBitmap;

	public interface Contract {
		void completeRequest(ImageContext imageContext, boolean isOK);
		void retakePicture();
	}


	private static int exifToDegrees(int exifOrientation) {
		if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
			return 90;
		} else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
			return 180;
		} else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
			return 270;
		}
		return 0;
	}


	@Override
	public void onAttach(Activity activity) {
		if(!(activity instanceof Contract)) {
			throw new IllegalStateException("Hosting activity must implement Contract interface");
		}

		super.onAttach(activity);
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setRetainInstance(true);
		setHasOptionsMenu(false);
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//    iv=new ImageView(getActivity());
		View v = inflater.inflate(R.layout.cwac_cam2_confirm_fragment, container, false);
		iv = (ImageView) v.findViewById(R.id.cwac_cam2_preview_image);
		mVideoView = (VideoView) v.findViewById(R.id.cwac_cam2_preview_video);
		mRelativeLayout = (RelativeLayout) v.findViewById(R.id.relative_parent);
		if(imageContext != null) {
			loadImage();
		}

		if(videoUri != null) {
			loadVideo();
		}

		ImageView btnConfirm = (ImageView) v.findViewById(R.id.cwac_cam2_confirm_btn);
		ImageView btnCancel = (ImageView) v.findViewById(R.id.cwac_cam2_cancel_btn);

		btnConfirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getContract().completeRequest(imageContext, true);
			}
		});

		btnCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getContract().retakePicture();
				hideMediaControllerAndStopVideo();
				recycleBitmaps();
			}
		});

		return (v);
	}


	public static ConfirmationFragment newInstance() {
		ConfirmationFragment result = new ConfirmationFragment();
		Bundle args = new Bundle();

		result.setArguments(args);

		return (result);
	}


	@Override
	public void onHiddenChanged(boolean isHidden) {
		super.onHiddenChanged(isHidden);

		if(!isHidden) {
			ActionBar ab = getActivity().getActionBar();

			if(ab == null) {
				return;
//        throw new IllegalStateException("CameraActivity confirmation requires an action bar!");
			} else {
				ab.setBackgroundDrawable(getActivity()
						.getResources()
						.getDrawable(R.drawable.cwac_cam2_action_bar_bg_translucent));
				ab.setTitle("");

				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					ab.setDisplayHomeAsUpEnabled(true);
					ab.setHomeAsUpIndicator(R.drawable.cwac_cam2_ic_close_white);
				} else {
					ab.setIcon(R.drawable.cwac_cam2_ic_close_white);
					ab.setDisplayShowHomeEnabled(true);
					ab.setHomeButtonEnabled(true);
				}
			}
		}
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.cwac_cam2_confirm, menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			getContract().completeRequest(imageContext, false);
		} else if(item.getItemId() == R.id.cwac_cam2_ok) {
			getContract().completeRequest(imageContext, true);
		} else if(item.getItemId() == R.id.cwac_cam2_retry) {
			getContract().retakePicture();
		} else {
			return (super.onOptionsItemSelected(item));
		}

		return (true);
	}


	public void setImage(ImageContext imageContext, Uri imageUri) {
		this.imageContext = imageContext;
		this.imageUri = imageUri;
		if(iv != null) {
			loadImage();
		}
	}


	public void setVideo(Uri videoUri) {
		this.videoUri = videoUri;

		if(mVideoView != null) {
			loadVideo();
		}
	}


	private Contract getContract() {
		return ((Contract) getActivity());
	}


	private void loadImage() {
		try {
			ExifInterface exif = new ExifInterface(imageUri.getPath());
			int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			int rotationInDegrees = exifToDegrees(rotation);
			Matrix matrix = new Matrix();
			if(rotation != 0f) {
				matrix.preRotate(rotationInDegrees);
			}

			BitmapFactory.Options options = new BitmapFactory.Options();
			byte[] jpeg = imageContext.getJpeg();
			options.inSampleSize = calculateInSampleSize(jpeg);
			options.inMutable = true;
			options.inPurgeable = true;
			previewBitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);

			adjustedBitmap = Bitmap.createBitmap(previewBitmap, 0, 0, previewBitmap.getWidth(), previewBitmap.getHeight(), matrix, true);

			iv.setImageBitmap(adjustedBitmap);
		} catch(IOException e) {
			iv.setImageBitmap(imageContext.buildPreviewThumbnail());
			e.printStackTrace();
		}

		iv.setVisibility(View.VISIBLE);
		mRelativeLayout.setVisibility(View.GONE);
		mVideoView.setVisibility(View.GONE);
	}


	private void loadVideo() {
		mMediaController = new MediaController(getActivity());
		mMediaController.setAnchorView(mVideoView);
		mVideoView.setMediaController(mMediaController);
		mVideoView.setVideoURI(videoUri);
		iv.setVisibility(View.GONE);
		mRelativeLayout.setVisibility(View.VISIBLE);
		mVideoView.setVisibility(View.VISIBLE);
		mVideoView.start();
	}


	private void hideMediaControllerAndStopVideo() {
		if(mMediaController!=null){
			mMediaController.hide();
		}
		if(mVideoView.isPlaying()) {
			mVideoView.stopPlayback();
			mVideoView.suspend();
		}
	}


	private int calculateInSampleSize(byte[] jpeg) {
		int sampleSize = 1;
		int size = jpeg.length / 1024;
		while(size > 1024) {
			sampleSize *= 2;
			size /= 2;
		}
		sampleSize *= 2;
		return sampleSize;
	}

	private void recycleBitmaps() {
		if(previewBitmap!=null){
			previewBitmap.recycle();
			previewBitmap = null;
		}

		if(adjustedBitmap!=null){
			adjustedBitmap.recycle();
			adjustedBitmap = null;
		}
	}
}
