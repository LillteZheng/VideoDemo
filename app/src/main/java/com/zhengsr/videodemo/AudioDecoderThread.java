package com.zhengsr.videodemo;

import android.content.ContentResolver;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioDecoderThread {
	private static final int TIMEOUT_US = 1000;
	private MediaExtractor mExtractor;
	private MediaCodec mDecoder;
	
	private boolean eosReceived;
	private int mSampleRate = 0;
	private String mime;
	MediaFormat mediaFormat;
	/**
	 * 
	 * @param path
	 */
	public void startPlay(String path) {
		eosReceived = false;
		mExtractor = new MediaExtractor();
		try {
			mExtractor.setDataSource(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		int channel = 0;
		int index =0;

		for (int i = 0; i < mExtractor.getTrackCount(); i++) {
			MediaFormat format = mExtractor.getTrackFormat(i);
			mime = format.getString(MediaFormat.KEY_MIME);
			if (mime.startsWith("audio/")) {
				mExtractor.selectTrack(i);
				mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
				mediaFormat = format;
				channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
				index = i;
				break;
			}
		}
		//MediaFormat format = makeAACCodecSpecificData(MediaCodecInfo.CodecProfileLevel.AACObjectLC, mSampleRate, channel);
		MediaFormat format = mExtractor.getTrackFormat(index);

		try {
			mDecoder = MediaCodec.createDecoderByType(mime);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mDecoder.configure(format, null, null, 0);

		if (mDecoder == null) {
			Log.e("DecodeActivity", "Can't find video info!");
			return;
		}

		mDecoder.start();
	
		new Thread(AACDecoderAndPlayRunnable).start();
	}
	

	
	Runnable AACDecoderAndPlayRunnable = new Runnable() {
		
		@Override
		public void run() {
			AACDecoderAndPlay();
		}
	};

	/**
	 * After decoding AAC, Play using Audio Track.
	 */
	public void AACDecoderAndPlay() {
		ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
		ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
		
		MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

		int channelConfig = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

		int buffsize = AudioTrack.getMinBufferSize(mSampleRate,  AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        // create an audiotrack object
		AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffsize,
                AudioTrack.MODE_STREAM);
		audioTrack.play();
		
		while (!eosReceived) {
			int inIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
			if (inIndex >= 0) {
				ByteBuffer buffer = inputBuffers[inIndex];
				int sampleSize = mExtractor.readSampleData(buffer, 0);
				if (sampleSize < 0) {
					// We shouldn't stop the playback at this point, just pass the EOS
					// flag to mDecoder, we will get it again from the
					// dequeueOutputBuffer
					Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
					mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
					
				} else {
					mDecoder.queueInputBuffer(inIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
					mExtractor.advance();
				}
				
				int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
				switch (outIndex) {
				case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
					Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
					outputBuffers = mDecoder.getOutputBuffers();
					break;
					
				case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
					MediaFormat format = mDecoder.getOutputFormat();
					Log.d("DecodeActivity", "New format " + format);
					audioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
					
					break;
				case MediaCodec.INFO_TRY_AGAIN_LATER:
					Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
					break;
					
				default:
					ByteBuffer outBuffer = outputBuffers[outIndex];
					Log.v("DecodeActivity", "We can't use this buffer but render it due to the API limit, " + outBuffer);
					
					final byte[] chunk = new byte[info.size];
					outBuffer.get(chunk); // Read the buffer all at once
					outBuffer.clear(); // ** MUST DO!!! OTHERWISE THE NEXT TIME YOU GET THIS SAME BUFFER BAD THINGS WILL HAPPEN
					
					audioTrack.write(chunk, info.offset, info.offset + info.size); // AudioTrack write data
					mDecoder.releaseOutputBuffer(outIndex, false);
					break;
				}
				
				// All decoded frames have been rendered, we can stop playing now
				if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
					Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
					break;
				}
			}
		}
		
		mDecoder.stop();
		mDecoder.release();
		mDecoder = null;
		
		mExtractor.release();
		mExtractor = null;
		
		audioTrack.stop();
		audioTrack.release();
		audioTrack = null;
	}
	
	public void stop() {
		eosReceived = true;
	}

}