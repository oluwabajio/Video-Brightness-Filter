package video.brightness.darkness.editor.ui.fragments;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import video.brightness.darkness.editor.utils.FilterAdjuster;
import video.brightness.darkness.editor.utils.MovieWrapperView;
import video.brightness.darkness.editor.utils.PlayerTimer;
import video.brightness.darkness.editor.databinding.FragmentEditBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.daasuu.gpuv.egl.filter.GlBrightnessFilter;
import com.daasuu.gpuv.egl.filter.GlFilter;
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


public class EditFragment extends Fragment {

    private static final String STREAM_URL_MP4_VOD_LONG = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";

    FragmentEditBinding binding;
    private GPUPlayerView gpuPlayerView;
    private SimpleExoPlayer player;
    private Button button;
    private PlayerTimer playerTimer;
    private GlFilter filter;
    private FilterAdjuster adjuster;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        setUpViews();
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        setUpSimpleExoPlayer();
        setUoGlPlayerView();
        setUpTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
        if (playerTimer != null) {
            playerTimer.stop();
            playerTimer.removeMessages(0);
        }
    }

    private void setUpViews() {
        // play pause
        button = binding.btn;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player == null) return;

                if (button.getText().toString().equals("Pause")) {
                    player.setPlayWhenReady(false);
                    button.setText("Play");
                } else {
                    player.setPlayWhenReady(true);
                    button.setText("Pause");
                }
            }
        });

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


        binding.filterSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (adjuster != null) {
                    adjuster.adjust(filter, progress);
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

        // list
//        ListView listView = findViewById(R.id.list);
//        final List<FilterType> filterTypes = FilterType.createFilterList();
//        listView.setAdapter(new FilterAdapter(this, R.layout.row_text, filterTypes));
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                filter = FilterType.createGlFilter(filterTypes.get(position), getApplicationContext());
//                adjuster = FilterType.createFilterAdjuster(filterTypes.get(position));
//                findViewById(R.id.filterSeekBarLayout).setVisibility(adjuster != null ? View.VISIBLE : View.GONE);
//                gpuPlayerView.setGlFilter(filter);
//            }
//        });

        binding.btnFilter.setOnClickListener(v-> {

            GlBrightnessFilter glBrightnessFilter = new GlBrightnessFilter();
            glBrightnessFilter.setBrightness(0.2f);
            filter = glBrightnessFilter;

            adjuster = new FilterAdjuster() {
                @Override
                public void adjust(GlFilter filter, int percentage) {
                    ((GlBrightnessFilter) filter).setBrightness(range(percentage, -1.0f, 1.0f));
                }
            };


            binding.filterSeekBar.setVisibility(adjuster != null ? View.VISIBLE : View.GONE);
            gpuPlayerView.setGlFilter(filter);
        });
    }


    private void setUpSimpleExoPlayer() {
        TrackSelector trackSelector = new DefaultTrackSelector();
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(getActivity(), Util.getUserAgent(getActivity(), "yourApplicationName"), defaultBandwidthMeter);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(STREAM_URL_MP4_VOD_LONG));
        // SimpleExoPlayer
        player = ExoPlayerFactory.newSimpleInstance(getActivity(), trackSelector);
        // Prepare the player with the source.
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);

    }


    private void setUoGlPlayerView() {
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
                long position = player.getCurrentPosition();
                long duration = player.getDuration();
                if (duration <= 0) return;
                binding.timeSeekBar.setMax((int) duration / 1000);
                binding.timeSeekBar.setProgress((int) position / 1000);
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

}

