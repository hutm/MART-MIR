MART -- Music Analysis, Recognition and Transcription framework is aimed at creating tools to extract high-level information (chords, key, rhythm) from music signals.

Installation instructions.

Requirements:

1. Sun JDK 1.6
2. Maven 2.2
3. HTK (http://htk.eng.cam.ac.uk/)
4. SRILM (http://www.speech.sri.com/projects/srilm/)


Installation
1. Create $JAVA_HOME variable and set it to your current JDK installation path.
2. Create $M2_HOME variable and set it to your current maven installation path. Add $M2_HOME/bin to the $PATH variable
3. Follow the instructions of HTK and SRILM toolkits. Copy the compiled binaries into $PROJECT_ROOT/bin directory.
4. echo build.dir=/path/to/build/directory  > $PROJECT_ROOT/mart.properties, where  /path/to/build/directory is the build directory.
5. chmod +x  $PROJECT_ROOT/crs.sh $PROJECT_ROOT/bin/*
6. mvn -Dmaven.test.skip=true -P development package



Test phase:
cd /path/to/build/directory
./crs.sh -c ./cfg/configBeats.cfg    #Test beat extraction system
./crs.sh -c ./cfg/configChords.cfg   #Test chord recognition system
