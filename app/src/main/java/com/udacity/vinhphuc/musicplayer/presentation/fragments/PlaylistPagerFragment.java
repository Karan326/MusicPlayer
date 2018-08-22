package com.udacity.vinhphuc.musicplayer.presentation.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.udacity.vinhphuc.musicplayer.R;
import com.udacity.vinhphuc.musicplayer.data.loaders.FavouriteTracksLoader;
import com.udacity.vinhphuc.musicplayer.data.loaders.LastAddedLoader;
import com.udacity.vinhphuc.musicplayer.data.loaders.PlaylistLoader;
import com.udacity.vinhphuc.musicplayer.data.loaders.PlaylistSongLoader;
import com.udacity.vinhphuc.musicplayer.data.loaders.SongLoader;
import com.udacity.vinhphuc.musicplayer.data.loaders.TopTracksLoader;
import com.udacity.vinhphuc.musicplayer.data.model.Playlist;
import com.udacity.vinhphuc.musicplayer.data.model.Song;
import com.udacity.vinhphuc.musicplayer.utils.AppUtils;
import com.udacity.vinhphuc.musicplayer.utils.Constants;
import com.udacity.vinhphuc.musicplayer.utils.NavigationUtils;
import com.udacity.vinhphuc.musicplayer.utils.PreferencesUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by VINH PHUC on 29/7/2018
 */
public class PlaylistPagerFragment extends Fragment {
    private static final String ARG_PAGE_NUMBER = "pageNumber";
    private int[] foregroundColors = {
            R.color.pink_transparent,
            R.color.green_transparent,
            R.color.blue_transparent,
            R.color.red_transparent,
            R.color.purple_transparent
    };
    private int pageNumber, songCountInt, totalRuntime;
    private int foregroundColor;
    private long firstAlbumID = -1;
    private Playlist playlist;

    @BindView(R.id.playlist_name)
    TextView playlistName;
    @BindView(R.id.song_count)
    TextView songCount;
    @BindView(R.id.playlist_number)
    TextView playlistNumber;
    @BindView(R.id.playlist_type)
    TextView playlistType;
    @BindView(R.id.runtime)
    TextView runtime;
    @BindView(R.id.playlist_image)
    ImageView playlistImage;
    @BindView(R.id.foreground)
    View foreground;

    private Context mContext;
    private boolean showAuto;

    public static PlaylistPagerFragment newInstance(int pageNumber) {
        PlaylistPagerFragment fragment = new PlaylistPagerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_PAGE_NUMBER, pageNumber);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        showAuto = PreferencesUtility.getInstance(getActivity()).showAutoPlaylist();
        View rootView = inflater.inflate(R.layout.fragment_playlist_pager, container, false);
        ButterKnife.bind(this, rootView);

        final List<Playlist> playlists = PlaylistLoader.getPlaylists(getActivity(), showAuto);

        pageNumber = getArguments().getInt(ARG_PAGE_NUMBER);
        playlist = playlists.get(pageNumber);

        playlistImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Pair> tranitionViews = new ArrayList<>();
                tranitionViews.add(0, Pair.create((View) playlistName, "transition_playlist_name"));
                tranitionViews.add(1, Pair.create((View) playlistImage, "transition_album_art"));
                tranitionViews.add(2, Pair.create(foreground, "transition_foreground"));
                NavigationUtils.navigateToPlaylistDetail(
                        getActivity(),
                        getPlaylistType(),
                        firstAlbumID,
                        String.valueOf(playlistName.getText()),
                        foregroundColor,
                        playlist.id, tranitionViews
                );
            }
        });

        mContext = this.getContext();
        setUpPlaylistDetails();
        return rootView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedinstancestate) {
        new loadPlaylistImage().execute("");
    }

    private void setUpPlaylistDetails() {
        playlistName.setText(playlist.name);

        int number = getArguments().getInt(ARG_PAGE_NUMBER) + 1;
        String playlistnumberstring;

        if (number > 9) {
            playlistnumberstring = String.valueOf(number);
        } else {
            playlistnumberstring = "0" + String.valueOf(number);
        }
        playlistNumber.setText(playlistnumberstring);

        Random random = new Random();
        int rndInt = random.nextInt(foregroundColors.length);

        foregroundColor = foregroundColors[rndInt];
        foreground.setBackgroundColor(foregroundColor);

        if (showAuto) {
            if (pageNumber <= 2)
                playlistType.setVisibility(View.VISIBLE);
        }

    }

    private String getPlaylistType() {
        if (showAuto) {
            switch (pageNumber) {
                case 0:
                    return Constants.NAVIGATE_PLAYLIST_LASTADDED;
                case 1:
                    return Constants.NAVIGATE_PLAYLIST_RECENT;
                case 2:
                    return Constants.NAVIGATE_PLAYLIST_TOPTRACKS;
                case 3:
                    return Constants.NAVIGATE_PLAYLIST_FAVOURITETRACKS;
                default:
                    return Constants.NAVIGATE_PLAYLIST_USERCREATED;
            }
        } else return Constants.NAVIGATE_PLAYLIST_USERCREATED;
    }


    private class loadPlaylistImage extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (getActivity() != null) {
                if (showAuto) {
                    switch (pageNumber) {
                        case 0:
                            List<Song> lastAddedSongs = LastAddedLoader.getLastAddedSongs(getActivity());
                            songCountInt = lastAddedSongs.size();
                            for(Song song : lastAddedSongs) {
                                totalRuntime += song.duration / 1000; //for some reason default playlists have songs with durations 1000x larger than they should be
                            }
                            if (songCountInt != 0) {
                                firstAlbumID = lastAddedSongs.get(0).albumId;
                                return AppUtils.getAlbumArtUri(firstAlbumID).toString();
                            } else return "nosongs";
                        case 1:
                            TopTracksLoader recentloader = new TopTracksLoader(getActivity(), TopTracksLoader.QueryType.RecentSongs);
                            List<Song> recentsongs = SongLoader.getSongsForCursor(TopTracksLoader.getCursor());
                            songCountInt = recentsongs.size();
                            for(Song song : recentsongs){
                                totalRuntime += song.duration / 1000;
                            }

                            if (songCountInt != 0) {
                                firstAlbumID = recentsongs.get(0).albumId;
                                return AppUtils.getAlbumArtUri(firstAlbumID).toString();
                            } else return "nosongs";
                        case 2:
                            TopTracksLoader topTracksLoader = new TopTracksLoader(getActivity(), TopTracksLoader.QueryType.TopTracks);
                            List<Song> topsongs = SongLoader.getSongsForCursor(TopTracksLoader.getCursor());
                            songCountInt = topsongs.size();
                            for(Song song : topsongs){
                                totalRuntime += song.duration / 1000;
                            }
                            if (songCountInt != 0) {
                                firstAlbumID = topsongs.get(0).albumId;
                                return AppUtils.getAlbumArtUri(firstAlbumID).toString();
                            } else return "nosongs";
                        case 3:
                            List<Song> favouriteSongs = FavouriteTracksLoader.getSongsInPlaylist(getActivity(), playlist.id);
                            songCountInt = favouriteSongs.size();
                            for (Song song : favouriteSongs) {
                                totalRuntime += song.duration;
                            }
                            if (songCountInt != 0) {
                                firstAlbumID = favouriteSongs.get(0).albumId;
                                return AppUtils.getAlbumArtUri(firstAlbumID).toString();
                            } else return "nosongs";
                        default:
                            List<Song> playlistsongs = PlaylistSongLoader.getSongsInPlaylist(getActivity(), playlist.id);
                            songCountInt = playlistsongs.size();
                            for(Song song : playlistsongs){
                                totalRuntime += song.duration;
                            }
                            if (songCountInt != 0) {
                                firstAlbumID = playlistsongs.get(0).albumId;
                                return AppUtils.getAlbumArtUri(firstAlbumID).toString();
                            } else return "nosongs";

                    }
                } else {
                    List<Song> playlistsongs = PlaylistSongLoader.getSongsInPlaylist(getActivity(), playlist.id);
                    songCountInt = playlistsongs.size();
                    for(Song song : playlistsongs){
                        totalRuntime += song.duration;
                    }
                    if (songCountInt != 0) {
                        firstAlbumID = playlistsongs.get(0).albumId;
                        return AppUtils.getAlbumArtUri(firstAlbumID).toString();
                    } else return "nosongs";
                }
            } else return "context is null";

        }

        @Override
        protected void onPostExecute(String uri) {
            ImageLoader.getInstance().displayImage(uri, playlistImage,
                    new DisplayImageOptions.Builder().cacheInMemory(true)
                            .showImageOnFail(R.drawable.ic_empty_music2)
                            .resetViewBeforeLoading(true)
                            .build(), new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        }
                    });
            songCount.setText(" " + String.valueOf(songCountInt) + " " + mContext.getString(R.string.songs));
            runtime.setText(" " + AppUtils.makeShortTimeString(mContext, totalRuntime));
        }

        @Override
        protected void onPreExecute() {
        }
    }
}
