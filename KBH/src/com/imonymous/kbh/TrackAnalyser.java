package com.imonymous.kbh;

import android.content.Context;
import android.os.Vibrator;

import com.echonest.api.v4.Artist;
import com.echonest.api.v4.Params;
import com.echonest.api.v4.Song;
import com.echonest.api.v4.TimedEvent;
import com.echonest.api.v4.Track;
import com.echonest.api.v4.TrackAnalysis;
import com.echonest.api.v4.EchoNestAPI;
import com.echonest.api.v4.EchoNestException;

import java.util.ArrayList;
import java.util.List;

public class TrackAnalyser {
	
	String API_KEY = "R1LPX9ILG1ZJBXJAY";	
	
	public List<Long> analyse(String songName) throws EchoNestException {
		
		EchoNestAPI echoNest = new EchoNestAPI(API_KEY);
		List<Long> beats = new ArrayList<Long>();
		
		Params p = new Params();
        p.add("title", songName);
        p.add("results", 1);
        List<Song> song = echoNest.searchSongs(p);
        
        TrackAnalysis analysis = song.get(0).getAnalysis();
        
        for (TimedEvent beat : analysis.getTatums()) {
            System.out.println("beat " + beat.getStart());
        	beats.add((long) (1000*beat.getStart()));
        }
        
        return beats;
	}
}
