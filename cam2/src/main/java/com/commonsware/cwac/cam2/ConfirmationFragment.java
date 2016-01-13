/***
 Copyright (c) 2015 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.cam2;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;


public class ConfirmationFragment extends Fragment {
  public interface Contract {
    void completeRequest(ImageContext imageContext, boolean isOK);
    void retakePicture();
  }

  private ImageView iv;
  private ImageContext imageContext;
  private Uri videoUri;
  private VideoView mVideoView;

  public static ConfirmationFragment newInstance() {
    ConfirmationFragment result=new ConfirmationFragment();
    Bundle args=new Bundle();

    result.setArguments(args);

    return(result);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setRetainInstance(true);
    setHasOptionsMenu(true);
  }

  @Override
  public void onAttach(Activity activity) {
    if (!(activity instanceof Contract)) {
      throw new IllegalStateException("Hosting activity must implement Contract interface");
    }

    super.onAttach(activity);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v =inflater.inflate(R.layout.cwac_cam2_confirm_fragment, container, false);
    iv = (ImageView) v.findViewById(R.id.cwac_cam2_fragment_confirmation_preview_image);
    mVideoView = (VideoView) v.findViewById(R.id.cwac_cam2_fragment_confirmation_preview_video);
    if (imageContext!=null) {
      loadImage();
    }

    if(videoUri!=null){
      loadVideo();
    }

    Button btnConfirm = (Button) v.findViewById(R.id.cwac_cam2_fragment_confirmation_confirm_btn);
    Button btnCancel = (Button) v.findViewById(R.id.cwac_cam2_fragment_confirmation_retake_btn);

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
      }
    });

    return(v);
  }

//  @Override
//  public void onHiddenChanged(boolean isHidden) {
//    super.onHiddenChanged(isHidden);
//
//    if (!isHidden) {
//      ActionBar ab=getActivity().getActionBar();
//
//      if (ab==null) {
//        throw new IllegalStateException("CameraActivity confirmation requires an action bar!");
//      }
//      else {
//        ab.setBackgroundDrawable(getActivity()
//            .getResources()
//            .getDrawable(R.drawable.cwac_cam2_action_bar_bg_translucent));
//        ab.setTitle("");
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//          ab.setDisplayHomeAsUpEnabled(true);
//          ab.setHomeAsUpIndicator(R.drawable.cwac_cam2_ic_close_white);
//        }
//        else {
//          ab.setIcon(R.drawable.cwac_cam2_ic_close_white);
//          ab.setDisplayShowHomeEnabled(true);
//          ab.setHomeButtonEnabled(true);
//        }
//      }
//    }
//  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.cwac_cam2_confirm, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId()==android.R.id.home) {
      getContract().completeRequest(imageContext, false);
    }
    else if (item.getItemId()==R.id.cwac_cam2_ok) {
      getContract().completeRequest(imageContext, true);
    }
    else if (item.getItemId()==R.id.cwac_cam2_retry) {
      getContract().retakePicture();
    }
    else {
      return(super.onOptionsItemSelected(item));
    }

    return(true);
  }

  public void setImage(ImageContext imageContext) {
    this.imageContext=imageContext;

    if (iv!=null) {
      loadImage();
    }
  }

  public void setVideo(Uri videoUri) {
    this.videoUri=videoUri;

    if (mVideoView!=null) {
      loadVideo();
    }
  }

  private Contract getContract() {
    return((Contract)getActivity());
  }

  private void loadImage() {
//    iv.setImageBitmap(imageContext.buildPreviewThumbnail());
    Bitmap bm = BitmapFactory.decodeByteArray(imageContext.getJpeg(), 0, imageContext.getJpeg().length);
    iv.setImageBitmap(bm);
    iv.setVisibility(View.VISIBLE);
    mVideoView.setVisibility(View.GONE);
  }

  private void loadVideo() {
    MediaController mediacontroller = new MediaController(getActivity());
    mediacontroller.setAnchorView(mVideoView);
    mVideoView.setMediaController(mediacontroller);
    mVideoView.setVideoURI(videoUri);
    iv.setVisibility(View.GONE);
    mVideoView.setVisibility(View.VISIBLE);
    mVideoView.start();
  }
}
