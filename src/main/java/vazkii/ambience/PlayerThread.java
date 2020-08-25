package vazkii.ambience;

import java.io.InputStream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.SoundCategory;
import vazkii.ambience.thirdparty.javazoom.jl.decoder.JavaLayerException;
import vazkii.ambience.thirdparty.javazoom.jl.player.AudioDevice;
import vazkii.ambience.thirdparty.javazoom.jl.player.JavaSoundAudioDevice;
import vazkii.ambience.thirdparty.javazoom.jl.player.advanced.AdvancedPlayer;
import static vazkii.ambience.Ambience.*;

@SuppressWarnings("static-access")
public class PlayerThread extends Thread {
	
	public static long minPause = 200000;
	public static long maxPause = 300000;

	public static float MIN_GAIN = -50F;
	public static final float ORIGINAL_MIN = -50F;
	public static final float MAX_GAIN = 10F;

	public static float[] fadeGains;
	
	static {
		fadeGains = new float[Ambience.FADE_DURATION];
		float totaldiff = MIN_GAIN - MAX_GAIN;
		float diff = totaldiff / fadeGains.length;
		for(int i = 0; i < fadeGains.length; i++)
			fadeGains[i] = MAX_GAIN + diff * i;
	}
	
	public volatile static float gain = MAX_GAIN;
	public volatile static float realGain = 0;
	public final static float realGainPin = 0;
	public volatile static float realGainHolder = 0;

	public volatile static String currentSong = null;
	public volatile static String currentSongChoices = null;
	
	public static AdvancedPlayer player;

	volatile boolean queued = false;

	volatile boolean kill = false;
	volatile static boolean playing = false;
	
	
	public boolean playingSP = false;
	public boolean playingMP = false;
	
	public PlayerThread() {
		setDaemon(true);
		setName("Ambience Player Thread");
		start();
	}

	@Override
	public void run() {
		
		try {
			
			while(!kill) {
	
//				joinWorldEvent();
				
				if (queued && currentSong != null) {
					
					if(player != null) {
						resetPlayer();
					}
					
					InputStream stream = SongLoader.getStream();
					
					if(stream == null) {
						
						continue;
						
					}
					
					player = new AdvancedPlayer(stream);
					setGain(fadeGains[0]);
					
					starting = true;
					queued = false;
				}

				boolean played = false;
				
				if(player != null && player.getAudioDevice() != null && realGain > MIN_GAIN) {
					player.play();
					playing = true;
					played = true;
				}

				if(played && !queued) {
					next();
				}
			}
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
//	public void joinWorldEvent() {
//		
//		boolean sp = Minecraft.getMinecraft().isSingleplayer();
//		boolean mp = !sp || Minecraft.getMinecraft().getIntegratedServer().getPublic();
//		
//		if (sp && !playingSP) {
//			
//			playingSP = true;
//			resetPlayer();
//			
//		} else if (!sp && playingSP) {
//			
//			playingSP = false;
//			resetPlayer();
//			
//		}
//		
//		
//		if (mp && !playingMP) {
//			
//			playingMP = true;
//			resetPlayer();
//			
//		} else if (!mp && playingMP) {
//			
//			playingMP = false;
//			resetPlayer();
//			
//		}
//		
//	}
	
	public void pause() throws JavaLayerException {
		
		long rand = (long) (Math.random() * (maxPause - minPause + 1) + minPause); 
		
		try {
			Thread.sleep(rand);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	public void next() throws JavaLayerException {
		
		pause();
		
		if (!currentSongChoices.contains(",")) {
			play(currentSong);
		}
		else {
			if (SongPicker.getSongsString().equals(currentSongChoices)) {
				String newSong;
				do {
					newSong = SongPicker.getRandomSong();
				} while (newSong.equals(currentSong));
				play(newSong);
			} else {
				play(null);
			}
		}
	}
	
	public static void resetPlayer() {
		playing = false;
		if(player != null)
			player.close();

		currentSong = null;
		player = null;
	}

	public void play(String song) {
		resetPlayer();

		currentSong = song;
		queued = true;
	}
	
	public float getGain() {
		if(player == null)
			return gain;
		
		AudioDevice device = player.getAudioDevice();
		if(device != null && device instanceof JavaSoundAudioDevice)
			return ((JavaSoundAudioDevice) device).getGain();
		return gain;
	}
	
	public void addGain(float gain) {
		setGain(getGain() + gain);
	}
	
	public void setGain(float gain) {
		this.gain = gain;

		if(player == null)
			return;
		
		setRealGain();
	}

	public void fadeStep(float gain){
		float realGain = getGain() + gain;

		this.realGainHolder = realGain;
		this.realGain = realGain;
		if(player != null) {
			AudioDevice device = player.getAudioDevice();
			if (device != null && device instanceof JavaSoundAudioDevice) {
				try {
					((JavaSoundAudioDevice) device).setGain(realGain);
				} catch (IllegalArgumentException e) {
				} // If you can't fix the bug just put a catch around it
			}
		}
	}

	public void setRealGain() {
		GameSettings settings = Minecraft.getMinecraft().gameSettings;
		float musicGain = settings.getSoundLevel(SoundCategory.MUSIC) * settings.getSoundLevel(SoundCategory.MASTER);
		float realGain = MIN_GAIN + (MAX_GAIN - MIN_GAIN) * musicGain;
		
		this.realGain = realGain;
		if(player != null) {
			AudioDevice device = player.getAudioDevice();
			if(device != null && device instanceof JavaSoundAudioDevice) {
				try {
					((JavaSoundAudioDevice) device).setGain(realGain);
				} catch(IllegalArgumentException e) { } // If you can't fix the bug just put a catch around it
			}
		}
		
		if(musicGain == 0)
			play(null);
	}
	
	public float getRelativeVolume() {
		return getRelativeVolume(getGain());
	}
	
	public float getRelativeVolume(float gain) {
		float width = MAX_GAIN - MIN_GAIN;
		float rel = Math.abs(gain - MIN_GAIN);
		return rel / Math.abs(width);
	}

	public int getFramesPlayed() {
		return player == null ? 0 : player.getFrames();
	}
	
	public void forceKill() {
		try {
			resetPlayer();
			interrupt();

			finalize();
			kill = true;
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
}
