/*
 * Copyright (c) 2008-2013 Maksim Khadkevich and Fondazione Bruno Kessler.
 *
 * This file is part of MART.
 * MART is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2, as published
 * by the Free Software Foundation.
 *
 * MART is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with MART; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.mart.crs.exec.operation.mix;

import org.mart.crs.config.Extensions;
import org.mart.crs.core.AudioReader;
import org.mart.crs.exec.operation.Operation;
import org.mart.crs.utils.filefilter.ExtensionFileFilter;
import org.mart.crs.utils.helper.HelperFile;

import java.io.File;
import java.util.*;

/**
 * NoteMixer provides functionality for mixing 3 noteStrings into a chord and save into a separate file
 * @version 1.0 21-Sep-2010 00:04:12
 * @author: Hut
 */
public class NoteMixer extends Operation{

    protected Map<Integer, List<String>> noteFiles;

    protected String noteSubDir = "/notes";
    protected String chordSubDir = "/chords";


    /**
     * Number of output chords to generate
     */
    protected int chordNumber;

    public NoteMixer(String workingDir, int chordNumber) {
        super(workingDir);
        this.chordNumber = chordNumber;
    }

    @Override
    public void initialize() {
        noteFiles = new HashMap<Integer, List<String>>();
        File[] noteFileList = (new File(workingDir + noteSubDir)).listFiles(new ExtensionFileFilter(Extensions.WAV_EXT));

        for(File noteFile:noteFileList){
            String note = noteFile.getName().substring(0, noteFile.getName().indexOf("_"));
            int midiNumber = Integer.parseInt(note);
            if(!noteFiles.containsKey(midiNumber)){
                List<String> list = new ArrayList<String>();
                list.add(noteFile.getPath());
                noteFiles.put(midiNumber, list);
            } else{
                noteFiles.get(midiNumber).add(noteFile.getPath());
            }
        }
    }

    @Override
    public void operate() {
        for(int i = 0; i < chordNumber; i++){
            generateChord(i);
        }
    }

    protected void generateChord(int number){
//        int chordNumber = (int)Math.floor(Math.random() * 24);          //generate full dictionary
        int chordNumber = (int)Math.floor(Math.random()*2) * 12;

        int baseOctaveStartNote = 60;

//        String  chordName = LabelsParser.getChordForNumber(String.valueOf(chordNumber));          //TODO refactor notesFromRandomChord
        int[] notes = new int[3];
        notes[0] = baseOctaveStartNote + chordNumber % 12;
        if (chordNumber < 12) {
            notes[1] = baseOctaveStartNote + (chordNumber + 4) % 12;
        } else{
            notes[1] = baseOctaveStartNote + (chordNumber + 3) % 12;
        }
        notes[2] = baseOctaveStartNote + (chordNumber + 7) % 12;


        String[] fileNamesToMix = new String[3];
        for(int i = 0; i < fileNamesToMix.length; i++){
            int index = (int)Math.floor(Math.random() * (noteFiles.get(notes[i]).size()));
            fileNamesToMix[i] = noteFiles.get(notes[i]).get(index);
        }

        String modality = chordNumber >= 12 ? "min" : "maj";
        String chordName = String.format("%s:%s", noteStrings[chordNumber % 12], modality);

        mixFilesAndSave(fileNamesToMix, chordName, number);
    }

    


    protected void mixFilesAndSave(String[] files, String chordName, int number){
        AudioReader[] readers = new AudioReader[files.length];
        float[][] samples = new float[files.length][];
        int minSamples = Integer.MAX_VALUE;
        for(int i = 0; i < files.length; i++){
            readers[i] = new AudioReader(files[i]);
            samples[i] = readers[i].getSamples();
            if(minSamples > samples[i].length ){
                minSamples = samples[i].length;
            }
        }
        float[] outSamples = new float[minSamples];
        for(int i = 0; i < readers.length; i++){
            for (int j = 0; j < minSamples; j++) {
                if (j < samples[i].length) {
                    outSamples[j] += samples[i][j] / readers.length;
                }
            }
        }
//        StringBuilder filesStringData = new StringBuilder();
//        for(int i = 0; i < files.length; i++){
//            filesStringData.append(HelperFile.getNameWithoutExtension((new File(files[i])).getName())).append("_");
//        }
        String fileName = String.format("%s/%s/%s_%d", workingDir, chordSubDir, chordName, number);
//        readers[0].setPlayInOriginalFormat(true);
        AudioReader.storeAPieceOfMusicAsWav(outSamples, readers[0].getAudioFormat(), fileName + Extensions.WAV_EXT);

    }




    public static void transformFileNames(String directory){

        Map<String, List<Note>> tracks = new HashMap<String, List<Note>>();
        for (File f: new File(directory).listFiles()){
            String name = f.getName();
            String prefix = name.substring(0, name.indexOf("_")).toLowerCase();
            if(tracks.containsKey(prefix)){
                tracks.get(prefix).add(new Note(name));
            } else {
                List<Note> noteList = new ArrayList<Note>();
                noteList.add(new Note(name));
                tracks.put(prefix, noteList);
            }
        }

        //Now sort and rename
        for (String key:tracks.keySet()){
            String noteRange = key.substring(key.lastIndexOf('.') + 1);
            int endIndex = noteRange.charAt(1) == 'b' ? 3 : 2;
            int startNote = getNoteNumber(noteRange.substring(0, endIndex));
            int endNote = noteRange.length()>3 ? getNoteNumber(noteRange.substring(endIndex)) : startNote;

            List<Note> notes = tracks.get(key);
            Collections.sort(notes);

            if(notes.size() != endNote - startNote + 1){
//                 throw new IllegalArgumentException(noteRange);
                continue;
            }

            for(int i = 0; i < notes.size(); i++){
                Note note = notes.get(i);
                int index = startNote + i;
                File newFile = new File(String.format("%s/%d_%s", directory, index, note.fileNAme));
//                HelperFile.copyFile(String.format("%s/%s", directory, note.fileNAme), newFile.getPath());
            }
        }
    }




    static class Note implements Comparable<Note>{
        String fileNAme;
        int startTime;

        Note(String fileNAme) {
            this.fileNAme = fileNAme;
            String startTimeString = fileNAme.substring(fileNAme.lastIndexOf("_") + 1, fileNAme.lastIndexOf("-"));
            startTime = Integer.valueOf(startTimeString);
        }

        @Override
        public int compareTo(Note note) {
            return startTime - note.startTime;
        }
    }

    public static final String[] noteStrings = new String[]{"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};

    public static int getNoteNumber(String noteString){
        String noteName = "";
        int octave = -1;

        if (noteString.length() == 3){
            noteName = noteString.substring(0, 2);
            octave = Integer.parseInt(noteString.substring(2));
        } else
        if(noteString.length() == 2){
            noteName = noteString.substring(0, 1);
            octave = Integer.parseInt(noteString.substring(1));
        } else{
            throw new IllegalArgumentException(noteString);
        }
        int index = -1;
        for(int i = 0; i < noteStrings.length; i++){
            if(noteStrings[i].equalsIgnoreCase(noteName)){
                index = i;
                break;
            }
        }

        if(index < 0){
            throw new IllegalArgumentException(noteString);
        }

        return 24 + (octave - 1) * 12 + index;
    }



    public static void main(String[] args) {
//        transformFileNames("/home/hut/PhD/data/our/iowa/all");

        Operation noteMixer = new NoteMixer("/home/hut/PhD/data/our/iowa/", 200);
        noteMixer.initialize();
        noteMixer.operate();

    }

}
