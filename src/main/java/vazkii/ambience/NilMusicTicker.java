package vazkii.ambience;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;

import static vazkii.ambience.Ambience.*;
import static vazkii.ambience.PlayerThread.*;

public class NilMusicTicker extends MusicTicker {

	public NilMusicTicker(Minecraft p_i45112_1_) {
		super(p_i45112_1_);
	}
	
	public void update() {
		if(nextSong != null && player != null){
			thread.fadeStep(-0.25f);
			fading = true;
		}
		if(nextSong == null && player != null && fading == true){
			fading = false;
		}
		if (realGainHolder < 0f){
			thread.fadeStep(0.2f);
		}
	}

}
