package com.rapsealk.codelab.exoplayer

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.View
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var mPlayer: SimpleExoPlayer? = null

    private lateinit var mPlaybackStateListener: PlaybackStateListener

    private var mPlayWhenReady = false
    private var mCurrentWindow = 0
    private var mPlaybackPosition: Long = 0

    private var mDownloadUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val extractor = object: YouTubeExtractor(this) {
            override fun onExtractionComplete(
                ytFiles: SparseArray<YtFile>?,
                videoMeta: VideoMeta?
            ) {
                if (null != ytFiles) {
                    val itag = 18//22
                    val downloadUrl: String = ytFiles.get(itag).url ?: getString(R.string.media_url_mp3)
                    Log.d(TAG, "Download URL: $downloadUrl")

                    mDownloadUrl = downloadUrl
                    initializePlayer(downloadUrl)
                }
            }
        }
        extractor.extract(getYouTubeUrl("coGpaUZIKtU"), true, true)

        mPlaybackStateListener = PlaybackStateListener()

        //initializePlayer()
    }

    override fun onStart() {
        super.onStart()

        if (Util.SDK_INT >= 24) {
            //initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()

        hideSystemUi()
        if (Util.SDK_INT < 24 || mPlayer == null) {
            //initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()

        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()

        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun initializePlayer(id: String) {
        if (null == mPlayer) {
            val trackSelector = DefaultTrackSelector().apply {
                setParameters(buildUponParameters().setMaxVideoSizeSd())
            }
            mPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        }
        val player = mPlayer ?: ExoPlayerFactory.newSimpleInstance(this)
        playerView.player = player
        val uri = Uri.parse(id)//Uri.parse(getString(R.string.media_url_mp3))
        val mediaSource = buildMediaSource(uri)

        player.playWhenReady = mPlayWhenReady
        player.seekTo(mCurrentWindow, mPlaybackPosition)
        player.addListener(mPlaybackStateListener)
        player.prepare(mediaSource, false, false)
    }

    private fun releasePlayer() {
        mPlayer?.let {
            mPlayWhenReady = it.playWhenReady
            mCurrentWindow = it.currentWindowIndex
            mPlaybackPosition = it.currentPosition
            it.removeListener(mPlaybackStateListener)
            it.release()
            mPlayer = null
        }
    }

    private fun buildMediaSource(uri: Uri): MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, "exoplayer-codelab")
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }

    private fun buildConcatenatingMediaSources(uris: List<Uri>): ConcatenatingMediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, "exoplayer-codelab")
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
        return ConcatenatingMediaSource(
            *uris.map { mediaSourceFactory.createMediaSource(it) }.toTypedArray()
        )
    }

    private fun buildDashMediaSource(uri: Uri): DashMediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(this, "exoplayer-codelab")
        return DashMediaSource.Factory(dataSourceFactory).createMediaSource(uri)
    }


    /*
    private fun buildHlsMediaSource(uri: Uri): HlsMediaSource {

    }
    */

    @SuppressLint("inlinedApi")
    private fun hideSystemUi() {
        playerView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LOW_PROFILE
                                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
    }


    private inner class PlaybackStateListener : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE        -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING   -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY       -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED       -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE"
            }
            Log.d(TAG, "changed state to $stateString playWhenReady: $playWhenReady")
        }
    }


    companion object {
        private val TAG = MainActivity::class.java.simpleName

        fun getYouTubeUrl(id: String): String = "https://www.youtube.com/watch?v=$id"
    }
}
