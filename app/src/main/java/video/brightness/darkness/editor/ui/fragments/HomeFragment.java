package video.brightness.darkness.editor.ui.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import ir.androidexception.filepicker.dialog.DirectoryPickerDialog;
import ir.androidexception.filepicker.interfaces.OnCancelPickerDialogListener;
import ir.androidexception.filepicker.interfaces.OnConfirmDialogListener;
import video.brightness.darkness.editor.utils.FilterAdjuster;
import video.brightness.darkness.editor.utils.MovieWrapperView;
import video.brightness.darkness.editor.utils.PlayerTimer;
import video.brightness.darkness.editor.databinding.FragmentHomeBinding;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.daasuu.gpuv.composer.GPUMp4Composer;
import com.daasuu.gpuv.egl.filter.GlBrightnessFilter;
import com.daasuu.gpuv.egl.filter.GlContrastFilter;
import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daasuu.gpuv.egl.filter.GlFilterGroup;
import com.daasuu.gpuv.egl.filter.GlSaturationFilter;
import com.daasuu.gpuv.player.GPUPlayerView;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.kbeanie.multipicker.api.Picker;
import com.kbeanie.multipicker.api.VideoPicker;
import com.kbeanie.multipicker.api.callbacks.VideoPickerCallback;
import com.kbeanie.multipicker.api.entity.ChosenVideo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class HomeFragment extends Fragment {

    private static final String STREAM_URL_MP4_VOD_LONG = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
    private static final int PERMISSION_REQUEST_CODE = 104;


    private GPUPlayerView gpuPlayerView;
    private SimpleExoPlayer player;
    private PlayerTimer playerTimer;
    private GlFilter brighnessGlFilter;
    GlBrightnessFilter glBrightnessFilter;
    GlContrastFilter glContrastFilter;
    GlSaturationFilter glSaturationFilter;
    GlBrightnessFilter glBrightnessSaveFilter;
    GlContrastFilter glContrastSaveFilter;
    GlSaturationFilter glSaturationSaveFilter;
    private int brightnessValue = 50;
    private int contrastValue = 50;
    private int saturationValue = 50;
    private GlFilterGroup filter;
    private GlFilterGroup filter2;
    private FilterAdjuster adjuster;
    private FilterAdjuster adjusterContrast;
    private FilterAdjuster adjusterSaturation;
    private String originalPath = "";
    VideoPicker videoPicker;
    FragmentHomeBinding binding;
    private static final String TAG = "HomeFragment";
    private InterstitialAd mInterstitialAd;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        initListeners();
        setUpViews();
        initAds();
        return view;
    }

    private void initAds() {
        mInterstitialAd = new InterstitialAd(getActivity());
        mInterstitialAd.setAdUnitId("ca-app-pub-9562015878942760/1948409284");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void initListeners() {

        videoPicker = new VideoPicker(this);
        videoPicker.setVideoPickerCallback(new VideoPickerCallback() {
            @Override
            public void onError(String s) {
                Toast.makeText(getActivity(), "error", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideosChosen(List<ChosenVideo> list) {
                originalPath = list.get(0).getOriginalPath();
                if (player !=null) releasePlayer();
                playVideo();
                setupFilter();
                setSeekbarToDefaultState();
            }
        });

        binding.btnSelectFile.setOnClickListener(v -> {
            selectVideo();

        });


    }

    private void setSeekbarToDefaultState() {
        binding.brightnesSeekBar.setProgress(50);
        binding.contrastSeekBar.setProgress(50);
        binding.saturationSeekBar.setProgress(50);
    }

    private void playVideo() {
        if (originalPath != "") {
            setUpSimpleExoPlayer();
            setUpGlPlayerView();
            setUpTimer();
            binding.lyAudio.setVisibility(View.VISIBLE);
        }
    }

    public void selectVideo() {

        Dexter.withActivity(getActivity())
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(new MultiplePermissionsListener() {
                                  @Override
                                  public void onPermissionsChecked(MultiplePermissionsReport report) {
                                      if (report.areAllPermissionsGranted()) {
                                          videoPicker.pickVideo();
                                      } else {
                                          Log.e("TAG", "onPermissionsChecked: false");
                                          Toast.makeText(getActivity(), "Kindly Accept all Permissions", Toast.LENGTH_LONG).show();
                                      }
                                  }

                                  @Override
                                  public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                      token.continuePermissionRequest();
                                  }
                              }
                ).check();

    }

    @Override
    public void onResume() {
        super.onResume();
        playVideo();
try {
    setupFilter();
    setSeekbarToDefaultState();
} catch (Exception e) {

}

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            releasePlayer();
        } catch (Exception ex) {

        }
//
        if (playerTimer != null) {
            playerTimer.stop();
            playerTimer.removeMessages(0);
        }
    }

    private void setUpViews() {
        // play pause

        binding.btnPlay.setOnClickListener(v -> playAndPauseVideo());
        binding.btnSaveVideo.setOnClickListener(v -> showSaveDialog());

        // seek

        binding.timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (player == null) return;

                if (!fromUser) {
                    // We're not interested in programmatically generated changes to
                    // the progress bar's position.
                    return;
                }

                player.seekTo(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // do nothing
            }
        });


        binding.brightnesSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (adjuster != null) {
                    adjuster.adjust(glBrightnessFilter, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // do nothing
            }
        });

        binding.contrastSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (adjusterContrast != null) {
                    adjusterContrast.adjust(glContrastFilter, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.saturationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (adjusterSaturation != null) {
                    adjusterSaturation.adjust(glSaturationFilter, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Video Brightness App");
        builder.setMessage("Select a folder to save video to ");
        builder.setIcon(android.R.drawable.ic_dialog_info);

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                chooseLocationDialog();
            }
        });
        // Create the AlertDialog
        AlertDialog alertDialog = builder.create();
        // Set other dialog properties
        alertDialog.setCancelable(true);
        alertDialog.show();

    }

    private void chooseLocationDialog() {
        if (checkPermission()) {
            DirectoryPickerDialog directoryPickerDialog = new DirectoryPickerDialog(getActivity(), new OnCancelPickerDialogListener() {
                @Override
                public void onCanceled() {
                    Toast.makeText(getActivity(), "Canceled!!", Toast.LENGTH_SHORT).show();
                }
            }, new OnConfirmDialogListener() {
                @Override
                public void onConfirmed(File... files) {
                    saveVideo(files[0].getPath());
                }
            });
            directoryPickerDialog.show();
        }
    }

    private void playAndPauseVideo() {
        if (player == null) return;

        if (binding.btnPlay.getText().toString().equals("Pause")) {
            player.setPlayWhenReady(false);
            binding.btnPlay.setText("Play");
        } else {
            player.setPlayWhenReady(true);
            binding.btnPlay.setText("Pause");
        }
    }

    private void setupFilter() {
        glBrightnessFilter = new GlBrightnessFilter();
        glBrightnessFilter.setBrightness(0.0f);

        glContrastFilter = new GlContrastFilter();
        glContrastFilter.setContrast(1.0f);

        glSaturationFilter = new GlSaturationFilter();
        glSaturationFilter.setSaturation(1.0f);

        filter = new GlFilterGroup(glBrightnessFilter, glContrastFilter, glSaturationFilter);

        adjuster = new FilterAdjuster() {
            @Override
            public void adjust(GlFilter filter, int percentage) {
                ((GlBrightnessFilter) filter).setBrightness(range(percentage, -1.0f, 1.0f));
                Log.e(TAG, "adjust: Brightness percent is " + percentage);
                brightnessValue = percentage;
            }
        };

        adjusterContrast = new FilterAdjuster() {
            @Override
            public void adjust(GlFilter filter, int percentage) {
                ((GlContrastFilter) filter).setContrast(range(percentage, 0.0f, 2.0f));
                contrastValue = percentage;
            }
        };

        adjusterSaturation = new FilterAdjuster() {
            @Override
            public void adjust(GlFilter filter, int percentage) {
                ((GlSaturationFilter) filter).setSaturation(range(percentage, 0.0f, 2.0f));
                saturationValue = percentage;
            }
        };


        gpuPlayerView.setGlFilter(filter);
    }


    private void setUpSimpleExoPlayer() {

        TrackSelector trackSelector = new DefaultTrackSelector();

        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getActivity(), Util.getUserAgent(getActivity(), "yourApplicationName"), defaultBandwidthMeter);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(originalPath));

        // SimpleExoPlayer
        player = ExoPlayerFactory.newSimpleInstance(getActivity(), trackSelector);
        // Prepare the player with the source.
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);

    }


    private void setUpGlPlayerView() {
        gpuPlayerView = new GPUPlayerView(getActivity());
        gpuPlayerView.setSimpleExoPlayer(player);
        gpuPlayerView.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ((MovieWrapperView) binding.layoutMovieWrapper).addView(gpuPlayerView);
        gpuPlayerView.onResume();
    }


    private void setUpTimer() {
        playerTimer = new PlayerTimer();
        playerTimer.setCallback(new PlayerTimer.Callback() {
            @Override
            public void onTick(long timeMillis) {

                try {
                    long position = player.getCurrentPosition();
                    long duration = player.getDuration();

                    if (duration <= 0) return;

                    binding.timeSeekBar.setMax((int) duration / 1000);
                    binding.timeSeekBar.setProgress((int) position / 1000);
                } catch (Exception e) {
                    Log.e(TAG, "onTick: Exception Happened here");
                }

            }
        });
        playerTimer.start();
    }


    private void releasePlayer() {
        gpuPlayerView.onPause();
        ((MovieWrapperView) binding.layoutMovieWrapper).removeAllViews();
        gpuPlayerView = null;
        player.stop();
        player.release();
        player = null;
    }

    private static float range(int percentage, float start, float end) {
        return (end - start) * percentage / 100.0f + start;
    }

    public interface selectVideoCallback {
        public void onSelect();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Picker.PICK_VIDEO_DEVICE) {
            videoPicker.submit(data);
        }
    }

    public void saveVideo(String destinationPath) {
        binding.btnSaveVideo.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        glBrightnessSaveFilter = new GlBrightnessFilter();
        glBrightnessSaveFilter.setBrightness((float) ((brightnessValue*2) - 100) *0.01f);

        glContrastSaveFilter = new GlContrastFilter();
        glContrastSaveFilter.setContrast((float) brightnessValue *0.02f);

        glSaturationSaveFilter = new GlSaturationFilter();
        glSaturationSaveFilter.setSaturation((float) saturationValue * 0.02f);

        filter2 = new GlFilterGroup(glBrightnessSaveFilter, glContrastSaveFilter, glSaturationSaveFilter);

String fullDestinationPath = destinationPath +  "/vid_" + new SimpleDateFormat("yyyyMM_dd-HHmmss").format(new Date()).toString() + ".mp4";
        new GPUMp4Composer(originalPath, fullDestinationPath)
                .filter(filter2)
                .listener(new GPUMp4Composer.Listener() {
                    @Override
                    public void onProgress(double progress) {
                        Log.e(TAG, "onProgress = " + progress);
                    }

                    @Override
                    public void onCompleted() {
                        Log.e(TAG, "onCompleted()");
                        getActivity().runOnUiThread(() -> {
                       //     Toast.makeText(getActivity(), "codec complete path =" + destinationPath, Toast.LENGTH_SHORT).show();
                            binding.btnSaveVideo.setVisibility(View.VISIBLE);
                            binding.progressBar.setVisibility(View.GONE);
                            showDownloadSavedDialog(fullDestinationPath);
                        });
                    }

                    @Override
                    public void onCanceled() {
                        Log.d(TAG, "onCanceled");
                    }

                    @Override
                    public void onFailed(Exception exception) {
                        Log.e(TAG, "onFailed()", exception);
                    }
                })
                .start();
    }

    private void showDownloadSavedDialog(String fullDestinationPath) {
        new AlertDialog.Builder(getActivity())
                .setTitle("VIDEO SAVED SUCCESSFULLY!!")
                .setMessage("Your video has been saved to "+ fullDestinationPath)
                .setPositiveButton(
                        "OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                showInterstitialAd();
                            }
                        }
                )
                .show();
    }

    private void showInterstitialAd() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            Log.e("TAG", "The interstitial wasn't loaded yet.");
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        // request permission if it has not been grunted.
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

}

