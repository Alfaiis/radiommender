/**
 * Copyright 2012 CSG@IFI
 * 
 * This file is part of Radiommender.
 * 
 * Radiommender is free software: you can redistribute it and/or modify it under the terms of the GNU General 
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your 
 * option) any later version.
 * 
 * Radiommender is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the 
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Radiommender. If not, see 
 * http://www.gnu.org/licenses/.
 * 
 */
package org.radiommender.player;

import org.radiommender.model.PlayListEntry;
import org.radiommender.model.Song;
import org.radiommender.model.SongFile;
import org.radiommender.songhandler.MusicStorageWatcher;
import org.radiommender.songhandler.SongHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The SongPlayer loads the next song from the SongHandler and 
 * starts playing it. The BasicPlayer from javazoom is used to play songs.
 * It will update the player with the current song playing.
 * 
 * @author nicolas baer
 */
public class SongPlayer implements Runnable{
	// logger
	Logger logger = LoggerFactory.getLogger(MusicStorageWatcher.class);
	
	// module controller
	private Player player; 
	private SongHandler songHandler;
	
	// thread stopper
	private boolean active = true;
	
	private final VLCJPlayer vlcjPlayer;

    // currently playing song
    private SongFile songFile;
    
    private boolean songSkipped; 
    private boolean songStopped;
   
	
	/**
	 * default constructor
	 * @param player
	 */
	public SongPlayer(Player player){
		this.player = player;
		this.songFile = null;
		this.songSkipped = false;
		
		this.vlcjPlayer = new VLCJPlayer(null);
	}
	
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		boolean start = true;
		while(this.active){
			try {
				// wait until song has finished
				while (this.vlcjPlayer.isPlaying() && !start && !this.songStopped) {
					Thread.sleep(1000);
				}
				
				// check if song was skipped
				if(!start && !this.songSkipped && !this.songStopped){
					player.songPlayed(this.songFile.getSong());
				}
				
				while(this.songStopped){
					Thread.sleep(1000);
				}
				
				start = false;
				
				// fetch next song
				this.songSkipped = false;
				this.songStopped = false;
				PlayListEntry playlistEntry = null;
				while(active && (playlistEntry = songHandler.getRdySong()) == null){
					Thread.sleep(50);
				}
				if (playlistEntry!=null) {
					songFile = playlistEntry.getSongFile();
					
					// open song
					this.vlcjPlayer.open(songFile.getFile().getCanonicalPath());
					
					// play song
					this.vlcjPlayer.play();
					this.player.songPlaying(songFile.getSong(), playlistEntry.getOrigin());				
	
					// wait a few seconds -> player starts delayed
					int counter = 0;
					while((!this.vlcjPlayer.isPlaying() && counter < 5) && !this.songSkipped && !this.songStopped){
						counter++;
						Thread.sleep(1000);
					}
				}				
			} catch (Exception e) {
				logger.error("couldnt play song, message: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Stops the current song and forces to play the next one in the queue.
	 * @return the current song playing
	 * @throws BasicPlayerException couldn't stop the current track
	 */
	public Song skipSong() {
		this.songSkipped = true;
		this.songStopped = false;
		this.vlcjPlayer.stop();
		
		if (this.songFile!=null) {
			return this.songFile.getSong();
		}
		else {
			return null;
		}
	}
		
	/**
	 * Stops the current song. This will automatically force the player to play the next song.
	 * @throws BasicPlayerException
	 */
	public void stop() {
		this.vlcjPlayer.stop();
		this.songStopped = true;
	}
	/**
	 * Starts the player.
	 */
	public void play(){
		this.songStopped = false;
	}
	
	/**
	 * Register the SongHandler. This needs to be done in order to update the gui.
	 * @param songHandler the songHandler to set
	 */
	public void setSongHandler(SongHandler songHandler) {
		this.songHandler = songHandler;
	}


	/**
	 * @return the active
	 */
	public boolean isActive() {
		return active;
	}


	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * @return the songFile
	 */
	public SongFile getSongFile() {
		return songFile;
	}


	/**
	 * @param songFile the songFile to set
	 */
	public void setSongFile(SongFile songFile) {
		this.songFile = songFile;
	}

}
